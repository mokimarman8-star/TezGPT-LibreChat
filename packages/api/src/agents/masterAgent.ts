import type { Agent } from 'librechat-data-provider';

export type MasterAgentRisk = 'read' | 'write' | 'execute' | 'network' | 'device' | 'unknown';

export interface MasterAgentPolicy {
  enabled: boolean;
  allowSubagents: boolean;
  requireApproval: boolean;
  maxSteps: number;
  maxToolCalls: number;
  maxExecutionMs: number;
}

export interface MasterAgentRuntime {
  mode: 'single-master';
  policy: MasterAgentPolicy;
  toolNames: string[];
  createdAt: string;
}

export interface MasterAgentPlanStep {
  id: string;
  title: string;
  objective: string;
  status: 'pending' | 'active' | 'completed' | 'blocked';
  toolNames: string[];
}

export interface MasterAgentPlan {
  goal: string;
  steps: MasterAgentPlanStep[];
  maxSteps: number;
}

const DEFAULT_POLICY: MasterAgentPolicy = Object.freeze({
  enabled: true,
  allowSubagents: false,
  requireApproval: true,
  maxSteps: 32,
  maxToolCalls: 64,
  maxExecutionMs: 15 * 60 * 1000,
});

const RISKY_TOOL_PATTERNS: Array<[MasterAgentRisk, RegExp]> = [
  ['device', /(phone|device|clipboard|browser_control|automation|sms|call)/i],
  ['execute', /(bash|shell|terminal|execute|run|python|node|code|docker|kubernetes)/i],
  ['write', /(write|create|edit|delete|remove|move|rename|upload|publish|send)/i],
  ['network', /(http|fetch|request|webhook|mcp|github|database|postgres|search)/i],
  ['read', /(read|list|search|fetch|query|inspect|stat|describe|view|get|file_search)/i],
];

function asRecord(value: unknown): Record<string, unknown> {
  return value != null && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
}

function asBoolean(value: unknown, fallback: boolean): boolean {
  return typeof value === 'boolean' ? value : fallback;
}

function asBoundedInteger(value: unknown, fallback: number, min: number, max: number): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return fallback;
  return Math.min(max, Math.max(min, Math.trunc(value)));
}

/**
 * Reads the optional runtime policy from either `config.masterAgent` or
 * `config.endpoints.agents.masterAgent`. Keeping this structural avoids a UI
 * or data-provider schema migration for an operator-only backend feature.
 */
export function resolveMasterAgentPolicy(config: unknown): MasterAgentPolicy {
  const root = asRecord(config);
  const endpoints = asRecord(root.endpoints);
  const agents = asRecord(endpoints.agents);
  const rootPolicy = asRecord(root.masterAgent);
  const raw = rootPolicy.enabled !== undefined ? rootPolicy : asRecord(agents.masterAgent);

  return {
    enabled: asBoolean(raw.enabled, DEFAULT_POLICY.enabled),
    allowSubagents: asBoolean(raw.allowSubagents, DEFAULT_POLICY.allowSubagents),
    requireApproval: asBoolean(raw.requireApproval, DEFAULT_POLICY.requireApproval),
    maxSteps: asBoundedInteger(raw.maxSteps, DEFAULT_POLICY.maxSteps, 1, 128),
    maxToolCalls: asBoundedInteger(raw.maxToolCalls, DEFAULT_POLICY.maxToolCalls, 1, 256),
    maxExecutionMs: asBoundedInteger(raw.maxExecutionMs, DEFAULT_POLICY.maxExecutionMs, 10_000, 60 * 60 * 1000),
  };
}

export function classifyMasterAgentToolRisk(toolName: string): MasterAgentRisk {
  const name = String(toolName || '').trim();
  for (const [risk, pattern] of RISKY_TOOL_PATTERNS) {
    if (pattern.test(name)) return risk;
  }
  return 'unknown';
}

export function isDelegationToolName(toolName: string): boolean {
  return /(subagent|sub_agent|delegate|spawn.*agent|agent.*spawn)/i.test(toolName);
}

/**
 * Produces a stable, de-duplicated tool catalog for one master agent. When
 * sub-agents are disabled, delegation tools are deliberately omitted instead
 * of merely being hidden in a prompt.
 */
export function buildMasterToolCatalog(
  toolNames: readonly string[],
  policy: MasterAgentPolicy,
): string[] {
  const unique = [...new Set(toolNames.filter((name): name is string => typeof name === 'string' && name.trim() !== ''))];
  if (policy.allowSubagents) return unique;
  return unique.filter((name) => !isDelegationToolName(name));
}

export function createMasterAgentPlan(
  goal: string,
  toolNames: readonly string[],
  policy: MasterAgentPolicy,
): MasterAgentPlan {
  const trimmedGoal = goal.trim() || 'Complete the user request safely and verify the result.';
  const catalog = buildMasterToolCatalog(toolNames, policy);
  const hasRead = catalog.some((name) => classifyMasterAgentToolRisk(name) === 'read');
  const hasWrite = catalog.some((name) => classifyMasterAgentToolRisk(name) === 'write');
  const hasExecute = catalog.some((name) => classifyMasterAgentToolRisk(name) === 'execute');

  const steps: MasterAgentPlanStep[] = [
    {
      id: 'understand',
      title: 'Understand and scope the request',
      objective: trimmedGoal,
      status: 'pending',
      toolNames: [],
    },
  ];

  if (hasRead) {
    steps.push({
      id: 'inspect',
      title: 'Inspect relevant context and sources',
      objective: 'Read only the files, conversation context, memories, or external sources needed for a grounded plan.',
      status: 'pending',
      toolNames: catalog.filter((name) => classifyMasterAgentToolRisk(name) === 'read'),
    });
  }

  if (hasExecute) {
    steps.push({
      id: 'execute',
      title: 'Run bounded work in the authorized sandbox',
      objective: 'Use the smallest safe execution steps, preserve artifacts, and stop on timeout or policy violation.',
      status: 'pending',
      toolNames: catalog.filter((name) => classifyMasterAgentToolRisk(name) === 'execute'),
    });
  }

  if (hasWrite) {
    steps.push({
      id: 'change',
      title: 'Apply requested changes with approval when needed',
      objective: policy.requireApproval
        ? 'Ask for approval before destructive, external, or device-affecting changes.'
        : 'Apply only changes explicitly authorized by the user and current policy.',
      status: 'pending',
      toolNames: catalog.filter((name) => classifyMasterAgentToolRisk(name) === 'write'),
    });
  }

  steps.push({
    id: 'verify',
    title: 'Verify and report the result',
    objective: 'Run focused checks, summarize changed artifacts, and state limitations instead of claiming unverified success.',
    status: 'pending',
    toolNames: catalog.filter((name) => classifyMasterAgentToolRisk(name) === 'read'),
  });

  return { goal: trimmedGoal, steps: steps.slice(0, policy.maxSteps), maxSteps: policy.maxSteps };
}

export function buildMasterAgentInstructions(policy: MasterAgentPolicy): string {
  const approval = policy.requireApproval
    ? 'Before any write, delete, code execution, network side effect, message send, or device action, request explicit user approval unless an existing host policy already authorizes that exact operation.'
    : 'Even though approval prompts are disabled by operator policy, perform only actions explicitly requested by the user and remain within the sandbox and capability boundaries.';

  const delegation = policy.allowSubagents
    ? 'Delegation tools may be used only when the configured policy exposes them.'
    : 'Operate as one master agent. Do not spawn or delegate to sub-agents; decompose the work internally and use the available tools directly.';

  return [
    '# TezGPT Master Agent Runtime',
    'You are the single TezGPT master agent. Plan multi-step work internally, select the smallest set of capable tools, execute in bounded steps, and verify outcomes before responding.',
    delegation,
    'Use the current conversation as short-term working memory. Use the configured memory tools for durable user preferences, facts, and project context; never store secrets, credentials, or sensitive data unless the user explicitly asks and the memory policy permits it.',
    'Use the code-execution sandbox for files and code. Prefer workspace paths, keep outputs reproducible, respect file and process limits, and never claim unrestricted host or phone control.',
    approval,
    `Execution budget: at most ${policy.maxSteps} plan steps, ${policy.maxToolCalls} tool calls, and ${Math.round(policy.maxExecutionMs / 1000)} seconds per run unless the host runtime imposes a stricter limit.`,
    'If a tool, provider, sandbox, or memory store is unavailable, say so and continue with a safe fallback rather than fabricating access.',
  ].join('\n');
}

export function createMasterAgentRuntime(
  policy: MasterAgentPolicy,
  toolNames: readonly string[],
): MasterAgentRuntime {
  return {
    mode: 'single-master',
    policy,
    toolNames: buildMasterToolCatalog(toolNames, policy),
    createdAt: new Date().toISOString(),
  };
}

/**
 * Applies single-master semantics without changing the agent's visual or voice
 * configuration. The property is intentionally structural because this module
 * must support older persisted Agent documents during a rolling deployment.
 */
export function applySingleMasterPolicy(agent: Agent, policy: MasterAgentPolicy): void {
  if (!policy.enabled || policy.allowSubagents) return;
  const candidate = agent as Agent & { subagents?: unknown };
  if (candidate.subagents != null) {
    candidate.subagents = { enabled: false, allowSelf: false, agent_ids: [] };
  }
}

export const MASTER_AGENT_DEFAULT_POLICY: MasterAgentPolicy = DEFAULT_POLICY;
