# TezGPT Master Agent

## Purpose

TezGPT now exposes a backend-only **single-master-agent runtime**. The existing chat UI, tone, response streaming contract, and voice configuration remain unchanged. The master agent is an orchestration policy that composes the tools, code execution, MCP, skills, and memory capabilities already present in the TezGPT-LibreChat backend.

The design is inspired by the public DeerFlow architecture, particularly its lead-agent middleware model, thread workspace boundaries, tool catalog, sandbox abstraction, and memory-aware execution flow. No DeerFlow source files are copied into TezGPT. The DeerFlow repository is MIT-licensed; its copyright and permission notices must be retained for any direct derivative code. TezGPT instead ports the architectural ideas and continues to use its existing TypeScript/Node implementations.

## Runtime behavior

During agent initialization, TezGPT resolves an operator policy from either `config.masterAgent` or `config.endpoints.agents.masterAgent`. With the default policy, the runtime:

1. Treats the current model invocation as one master agent and does not expose delegation or sub-agent spawning tools.
2. Builds one de-duplicated catalog from the agent's existing built-in, code-execution, skill, provider, memory, and MCP tools.
3. Keeps short-term context in the current conversation and uses the existing permissioned `set_memory`, `delete_memory`, and formatted-memory APIs for durable user/project memories.
4. Uses the existing `bash_tool`, `read_file`, `create_file`, `edit_file`, artifact, and code-session paths for sandbox work rather than bypassing the host runtime.
5. Adds an internal execution policy that encourages planning, bounded steps, verification, and explicit reporting of unavailable capabilities.
6. Requires confirmation for destructive writes, code execution, network side effects, external sends, and device-related operations when the host approval path is enabled.

The runtime does not grant unlimited host or phone control. Device control remains dependent on an explicitly configured connector or tool, and the existing capability and permission gates remain authoritative.

## Operator configuration

The feature is backend-configurable and does not require a new UI control. Add the following to the server configuration if an operator wants to override the safe defaults:

```yaml
endpoints:
  agents:
    masterAgent:
      enabled: true
      allowSubagents: false
      requireApproval: true
      maxSteps: 32
      maxToolCalls: 64
      maxExecutionMs: 900000
```

`enabled` activates the policy. `allowSubagents` is false by default because the TezGPT product requirement is one master agent. `requireApproval` keeps risky actions behind the host's confirmation mechanism. The numeric values are bounded by the backend even if an operator supplies an excessive value.

## Files

| File | Role |
|---|---|
| `packages/api/src/agents/masterAgent.ts` | Policy resolution, risk classification, tool catalog, bounded planning, runtime instructions, and single-master enforcement. |
| `packages/api/src/agents/initialize.ts` | Applies the policy during real agent initialization, filters delegation tools, injects the runtime instructions, and exposes metadata on `InitializedAgent`. |
| `packages/api/src/agents/memory.ts` | Existing durable memory implementation reused by the master agent. |
| `packages/api/src/agents/tools.ts` | Existing code/file authoring and sandbox tool registration reused by the master agent. |
| `api/app/clients/tools/util/handleTools.js` | Existing legacy tool loader for execute-code, file search, web search, memory, and MCP paths. |
| `packages/api/src/agents/masterAgent.spec.ts` | Focused tests for the new policy and planning helpers. |

## Validation

The API package builds successfully with the repository's `npm run build` command. The focused master-agent test suite passes six tests covering default policy resolution, operator overrides, delegation filtering, risk classification, bounded plans, and approval instructions.

## References

[1]: https://github.com/bytedance/deer-flow "ByteDance DeerFlow repository"
[2]: https://github.com/bytedance/deer-flow/blob/main/LICENSE "DeerFlow MIT License"
