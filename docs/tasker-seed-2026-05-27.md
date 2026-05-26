# Tasker Seed 2026-05-27

## Scope

- Branch: `feature/nightly-2025-05-27`
- Project: `json-tree`
- Automation: `json-tree-autonomous-product-owner-agent-prompt`

## Repository Audit

The current repository is still at archetype level:

- JavaFX + Spring Boot bootstrap is present and build-oriented.
- The layered package structure already matches the intended architecture.
- The main viewer shell exists in FXML and CSS.
- `JsonViewerWorkflowService` and `FileSystemJsonHistoryRepository` are still pending.
- No drag and drop, import workflow, validation service, tree rendering, or history flow is implemented yet.

## Tasker Baseline

- `TREE-0000` completed as the bootstrap baseline ticket.
- `TREE-0001` is the single active WIP ticket for global drag and drop.
- `TREE-0002` to `TREE-0010` were seeded in backlog in the requested order.

## Delivery Intent

The next delivery slice should implement global window drag and drop without adding a dedicated dropzone, preserving the existing controller and screen boundaries.
