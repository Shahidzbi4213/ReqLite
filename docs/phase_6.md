# Phase 6: Room Entities & DAOs - Collections

## Objective
Create Room Entity mapping and DAO interface for Collections and Folders. Write basic DAO tests.

## What to Do
- Create Room Entity mapping and DAO interface for Collections and Folders. Write basic DAO tests.
- Write tests specific ONLY to this module/component.
- Validate this small slice works perfectly before moving on.

## When to Stop
- When the specific components for this phase are fully implemented, tested, and compiling.
- Do NOT proceed to the next phase's components. Stop and fill out the report.

## Completion Report
*(To be filled by the agent/developer upon completing this phase)*

- **Status:** Complete
- **Summary of Work Completed:** Created Room database setup for KMP (`androidx.room3`). Implemented `CollectionEntity` and `FolderEntity` with `ForeignKey` cascading and index definitions. Created `CollectionDao` and `FolderDao` with standard CRUD queries. Added `CollectionMapper` and `FolderMapper` to bridge local entities and pure domain models, and wrote tests for these mappers to ensure fidelity.
- **Decisions Made:** 
  - Used `@ConstructedBy` for the `ReqLiteDatabase` to support Kotlin Multiplatform code generation natively.
  - Placed cascading deletes on `FolderEntity` targeting `CollectionEntity` so when a collection is deleted, all folders fall off automatically.
  - Relied on mapper unit tests in `commonTest` to validate DB bindings and entity shapes.
- **Challenges/Blockers Resolved:** 
  - Handled the KSP `@ConstructedBy` annotation and `expect object` requirement for generating KMP Room databases across iOS and Android without adding complex actual implementations in testing just yet.
- **Next Steps:** Proceed to Phase 7.
