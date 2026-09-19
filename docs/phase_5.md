# Phase 5: Core Domain Models - History & Artifacts

## Objective
Define the pure domain models for History Entries and Response Artifacts (large file references).

## What to Do
- Define the pure domain models for History Entries and Response Artifacts (large file references).
- Write tests specific ONLY to this module/component.
- Validate this small slice works perfectly before moving on.

## When to Stop
- When the specific components for this phase are fully implemented, tested, and compiling.
- Do NOT proceed to the next phase's components. Stop and fill out the report.

## Completion Report
*(To be filled by the agent/developer upon completing this phase)*

- **Status:** Complete
- **Summary of Work Completed:** Created pure domain models for `HistoryEntry` and `ResponseArtifact`. Added robust test cases to ensure these schemas handle both successful responses with artifacts, and failed responses where status and file paths remain null.
- **Decisions Made:** 
  - `HistoryEntry` points to a `ResponseArtifact` via an ID reference rather than nesting the payload. This prevents out-of-memory errors on large payload history tracking.
  - Allowed `statusCode`, `durationMs`, and `responseArtifactId` to be nullable in `HistoryEntry` to accurately capture requests that fail to reach the server (e.g. timeout or network loss).
- **Challenges/Blockers Resolved:** 
  - Modeled large payload references successfully decoupled from memory-heavy strings, preparing for file-backed storage.
- **Next Steps:** Proceed to Phase 6.
