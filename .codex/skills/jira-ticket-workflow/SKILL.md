---
name: jira-ticket-workflow
description: Create or reference Jira tickets for the spring-agent repository and its SPAG project only when the user explicitly instructs Codex to do so. Use for explicit requests to create, find, or link spring-agent Jira work. Never create tickets implicitly and never change Jira ticket status.
---

# Spring Agent Jira Workflow

Use Jira project `spring-agent` (`SPAG`).

## Non-negotiable rules

1. Create a Jira ticket only when the user explicitly instructs you to create one. Do not treat an implementation request, repository instruction, branch, commit, or pull request as permission to create a ticket.
2. Never change a Jira ticket's status. Do not call a transition operation for any reason. Reading and reporting the current status is allowed.

## Creation criteria

When explicitly instructed to create a ticket:

- Follow the user's requested scope and fields exactly.
- Inspect repository evidence only to fill missing implementation details.
- Search `SPAG` for an equivalent ticket before creating a duplicate.
- If an equivalent ticket exists, report it and ask whether a separate ticket is still required unless the user already answered that question.
- Keep unrelated changes and inferred future work out of the ticket.

Do not create a ticket for questions, read-only investigation, reviews, status checks, or implementation work unless the user explicitly asks for ticket creation.

## Ticket format

- Use `Task` by default and `Bug` only for a confirmed defect. Use another type only when the user requests it or the scope clearly requires it.
- Assign the current Atlassian user unless another assignee is named.
- Keep the ticket concise: summary, reason, scope, and verifiable completion criteria.
- Do not invent estimates, priority, labels, releases, or requirements.
- Add branches, commits, pull requests, comments, or links only when explicitly requested.
