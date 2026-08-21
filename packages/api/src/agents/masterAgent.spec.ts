import {
  buildMasterAgentInstructions,
  buildMasterToolCatalog,
  classifyMasterAgentToolRisk,
  createMasterAgentPlan,
  MASTER_AGENT_DEFAULT_POLICY,
  resolveMasterAgentPolicy,
} from './masterAgent';

describe('TezGPT master agent', () => {
  it('uses safe single-master defaults', () => {
    const policy = resolveMasterAgentPolicy({});

    expect(policy.enabled).toBe(true);
    expect(policy.allowSubagents).toBe(false);
    expect(policy.requireApproval).toBe(true);
    expect(policy.maxSteps).toBe(MASTER_AGENT_DEFAULT_POLICY.maxSteps);
  });

  it('reads operator overrides from the agents endpoint config', () => {
    const policy = resolveMasterAgentPolicy({
      endpoints: {
        agents: {
          masterAgent: {
            enabled: false,
            allowSubagents: true,
            requireApproval: false,
            maxSteps: 3,
            maxToolCalls: 4,
            maxExecutionMs: 20_000,
          },
        },
      },
    });

    expect(policy).toEqual({
      enabled: false,
      allowSubagents: true,
      requireApproval: false,
      maxSteps: 3,
      maxToolCalls: 4,
      maxExecutionMs: 20_000,
    });
  });

  it('removes sub-agent delegation tools in single-master mode', () => {
    expect(
      buildMasterToolCatalog(['read_file', 'subagent', 'delegate_task', 'bash_tool'], {
        ...MASTER_AGENT_DEFAULT_POLICY,
        allowSubagents: false,
      }),
    ).toEqual(['read_file', 'bash_tool']);
  });

  it('classifies execution and read-only tools', () => {
    expect(classifyMasterAgentToolRisk('bash_tool')).toBe('execute');
    expect(classifyMasterAgentToolRisk('read_file')).toBe('read');
    expect(classifyMasterAgentToolRisk('mcp__github__create_issue')).toBe('write');
  });

  it('creates a bounded internal plan', () => {
    const plan = createMasterAgentPlan(
      'Build and verify a project',
      ['read_file', 'bash_tool', 'write_file'],
      { ...MASTER_AGENT_DEFAULT_POLICY, maxSteps: 3 },
    );

    expect(plan.goal).toBe('Build and verify a project');
    expect(plan.steps.length).toBe(3);
    expect(plan.steps.map((step) => step.id)).toEqual(['understand', 'inspect', 'execute']);
  });

  it('requires approval in the generated runtime instructions', () => {
    const instructions = buildMasterAgentInstructions(MASTER_AGENT_DEFAULT_POLICY);
    expect(instructions).toContain('single TezGPT master agent');
    expect(instructions).toContain('request explicit user approval');
    expect(instructions).toContain('Do not spawn or delegate to sub-agents');
  });
});
