# Phase 3: Core Domain Models - Requests

## Objective
Define the pure domain models for Requests, Drafts, Request Fields, and Request Bodies.

## What to Do
- Define the pure domain models for Requests, Drafts, Request Fields, and Request Bodies.
- Write tests specific ONLY to this module/component.
- Validate this small slice works perfectly before moving on.

## When to Stop
- When the specific components for this phase are fully implemented, tested, and compiling.
- Do NOT proceed to the next phase's components. Stop and fill out the report.

## Completion Report
*(To be filled by the agent/developer upon completing this phase)*

- **Status:** Complete
- **Summary of Work Completed:** Created pure domain models (`Request`, `Draft`, `RequestField`, `RequestBody`, `HttpMethod`) inside the `:domain` module. Added sealed hierarchy for different body types (`NoBody`, `TextBody`, `FormDataBody`, `UrlEncodedBody`). Created and executed unit tests for each model.
- **Decisions Made:** 
  - Used `sealed interface` for `RequestBody` to safely model the different HTTP body content types in a type-safe and pure way.
  - Modelled `RequestField` for both headers and query parameters (and form data parts) to reuse the key-value schema.
- **Challenges/Blockers Resolved:** 
  - Modeled complex structures (like varied payloads) cleanly without serialization logic, maintaining domain purity.
- **Next Steps:** Proceed to Phase 4.
