# Architecture Decision Records (ADR)

## Why We Use ADRs

We use ADRs to document architecture decisions so they are:
- explicit and reviewable
- traceable over time
- easy for future contributors to understand

An ADR captures the problem, options, decision, and tradeoffs at the time the choice was made.

## ADR Index

- `001-init.md`: Identity Service Foundation baseline decisions.

Next expected ADR topics:
- identity AuthN/AuthZ extraction trigger (when to split policy service)
- token context model updates (`aud`, `org_id`, `env`, impersonation safeguards)

## Naming

- Format: `NNN-short-title.md`
- Example: `001-init.md`, `002-audience-model.md`

## Standard ADR Format

Each ADR should include the sections below.

1. Title
2. Status
3. Date
4. Deciders
5. Summary
6. Problem Statement
7. Decision Drivers
8. Options Considered
9. Decision
10. Examples
11. Consequences
12. Implementation Plan
13. References

## ADR Template

```md
# ADR NNN: <Title>

- Status: Proposed | Accepted | Superseded
- Date: YYYY-MM-DD
- Deciders: <team or names>

## Summary
<Short summary of the decision in 2-5 lines.>

## Problem Statement
<What problem are we solving and why now?>

## Decision Drivers
- <driver 1>
- <driver 2>
- <driver 3>

## Options Considered
1. <Option A>
2. <Option B>
3. <Option C>

## Decision
<Chosen option and the specific scope of the decision.>

## Examples
- <Example request/flow/model that demonstrates decision usage>
- <Example edge case>

## Consequences
Positive:
- <benefit 1>
- <benefit 2>

Tradeoffs:
- <tradeoff 1>
- <tradeoff 2>

## Implementation Plan
1. <step 1>
2. <step 2>
3. <step 3>

## References
- <related doc/path/link>
```
