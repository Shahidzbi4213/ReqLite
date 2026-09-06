# Phase 17: Error Mapping Taxonomy

## Objective
Implement the specific error classification mapping (DNS, Timeout, TLS, Persistence) outlined in the plan.

## What to Do
- Implement the specific error classification mapping (DNS, Timeout, TLS, Persistence) outlined in the plan.
- Write tests specific ONLY to this module/component.
- Validate this small slice works perfectly before moving on.

## When to Stop
- When the specific components for this phase are fully implemented, tested, and compiling.
- Do NOT proceed to the next phase's components. Stop and fill out the report.

## Completion Report
*(To be filled by the agent/developer upon completing this phase)*

- **Status:** Completed
- **Summary of Work Completed:** Created the `NetworkErrorType` taxonomy enum in `domain`. Extended `HistoryEntry` and its Room entity `HistoryEntryEntity` to include `errorCode` and `errorMessage` for telemetry and debugging. Implemented `ErrorMapper` in `data` to map Ktor-specific network exceptions (`HttpRequestTimeoutException`, `UnresolvedAddressException`, etc.) and fallback message patterns to the standardized taxonomy. Added `ErrorMapperTest` and updated `RequestExecutionEngineImpl` to classify errors before storing them in history.
- **Decisions Made:** Decided to place the taxonomy enum (`NetworkErrorType`) in the domain layer so the UI can safely reference it without knowing implementation details. `ErrorMapper` is housed in the `data` layer since it specifically handles `Ktor` library exceptions. String matching is used as a fallback for platform-specific exceptions that cross into common code.
- **Challenges/Blockers Resolved:** KMP test targets failed initially because of an accidentally referenced `isProtected` property on `Environment`, which was quickly corrected.
- **Next Steps:** Proceed to Phase 18.
