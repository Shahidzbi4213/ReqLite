# RequestLab Mobile - Complete Product and Engineering Plan

> Working title: **RequestLab**  
> Document version: **2.0**  
> Last updated: **2026-08-18**  
> Product type: **Local-first mobile REST API client**  
> Supported platforms: **Android and iOS only**  
> UI strategy: **Compose Multiplatform with deliberate platform adaptation**  
> Required database: **AndroidX Room for Kotlin Multiplatform**  
> Desktop status: **Explicitly out of scope**

---

## Table of contents

1. [Revision summary and non-negotiable decisions](#1-revision-summary-and-non-negotiable-decisions)
2. [Executive summary](#2-executive-summary)
3. [Improved product concept](#3-improved-product-concept)
4. [Product goals, users, and success measures](#4-product-goals-users-and-success-measures)
5. [Scope and release boundaries](#5-scope-and-release-boundaries)
6. [Mobile platform strategy](#6-mobile-platform-strategy)
7. [Information architecture and core flows](#7-information-architecture-and-core-flows)
8. [Functional requirements](#8-functional-requirements)
9. [System architecture](#9-system-architecture)
10. [Recommended repository and module structure](#10-recommended-repository-and-module-structure)
11. [Technology stack](#11-technology-stack)
12. [Room database policy](#12-room-database-policy)
13. [Room data model](#13-room-data-model)
14. [Persistence behavior and migrations](#14-persistence-behavior-and-migrations)
15. [Request execution engine](#15-request-execution-engine)
16. [Environment variables and secrets](#16-environment-variables-and-secrets)
17. [Import, export, and sharing](#17-import-export-and-sharing)
18. [Security and privacy](#18-security-and-privacy)
19. [Performance and reliability](#19-performance-and-reliability)
20. [Testing strategy](#20-testing-strategy)
21. [Observability and diagnostics](#21-observability-and-diagnostics)
22. [CI/CD and release engineering](#22-cicd-and-release-engineering)
23. [Implementation milestones](#23-implementation-milestones)
24. [Prioritized issue backlog](#24-prioritized-issue-backlog)
25. [Definition of done](#25-definition-of-done)
26. [Risk register](#26-risk-register)
27. [Architecture decision records](#27-architecture-decision-records)
28. [Post-MVP roadmap](#28-post-mvp-roadmap)
29. [Launch checklist](#29-launch-checklist)
30. [Immediate implementation sequence](#30-immediate-implementation-sequence)
31. [Official references](#31-official-references)

---

## 1. Revision summary and non-negotiable decisions

This version replaces the previous cross-platform plan and narrows the product to mobile phones.

### 1.1 Decisions that are now fixed

- The app targets **Android and iOS only**.
- There is **no desktop application, desktop layout, desktop build, desktop QA matrix, or desktop roadmap commitment**.
- **AndroidX Room for Kotlin Multiplatform is the mandatory structured persistence layer**.
- SQLDelight is removed completely.
- No alternative database abstraction may be introduced without a new architecture decision.
- Room is the local source of truth for all persistent structured product data.
- Direct low-level database access is prohibited in feature code.
- Raw secret values are the only deliberate exception to Room persistence; they live in Android Keystore-backed storage or iOS Keychain and are referenced from Room by an opaque key.
- The network layer uses Ktor Client with platform-appropriate engines.
- The shared UI uses Compose Multiplatform, while platform-specific behavior is allowed where Android and iOS conventions differ.
- The MVP is local-first and works without an account or backend.

### 1.2 What "Room as a rule" means

The following rules are part of the architecture contract:

1. Every persistent feature starts with a Room entity, DAO, repository, migration plan, and test.
2. Screens observe data from repositories backed by Room `Flow` queries.
3. Writes go through suspend repository operations and Room transactions.
4. UI state may be in memory, but durable user work must be committed to Room quickly.
5. Filesystem storage is allowed only for large response bodies, imported files, and exported archives. Room stores the metadata, ownership, path, checksum, and lifecycle state for those files.
6. Keychain or Keystore stores raw secrets. Room stores only secret metadata and the secure-storage reference.
7. No feature may use SQLDelight, Core Data, Realm, DataStore as a parallel database, or direct SQLite APIs.
8. Room schema files are checked into version control and migration validation is required before release.

### 1.3 Product improvement over a basic Postman clone

RequestLab should not simply compress a desktop API client into a phone screen. Its differentiation is a **mobile-first API debugging workflow**:

- Paste a cURL command from chat, email, browser, or documentation and run it quickly.
- Use large touch targets and a progressive request editor instead of a dense desktop grid.
- Pin favorite requests for one-tap reruns.
- Display the active environment prominently and warn before sending to a protected production environment.
- Preserve every unfinished request as a recoverable draft.
- Make JSON readable on a phone with tree, raw, search, wrap, and copy-path tools.
- Treat history as a debugging journal with notes, pins, and immutable snapshots.
- Support system share sheets for importing and exporting requests and workspace files.
- Remain useful offline for organizing requests and reviewing saved responses.

---

## 2. Executive summary

RequestLab is a lightweight, local-first REST API client for Android and iOS. It allows developers, QA engineers, students, support engineers, and technical operators to build, send, save, inspect, and repeat HTTP requests from a phone.

The first release focuses on the daily REST workflow:

1. Start a blank request or paste cURL.
2. Select an environment.
3. Edit URL, parameters, headers, authentication, and body.
4. Preview resolved values without revealing protected secrets.
5. Send or cancel the request.
6. Inspect status, timing, headers, raw content, and formatted JSON.
7. Save the request to a collection or pin it as a favorite.
8. Reopen an immutable request snapshot from history.
9. Export or import a local workspace without creating an account.

The app shares domain logic, persistence, networking abstractions, state management, and most UI code through Kotlin Multiplatform. Android and iOS contain only the platform entry points and integrations that must use native APIs.

### 2.1 Release promise

A new user should be able to install the app and send a simple GET request in under one minute without creating an account, workspace, collection, or environment.

### 2.2 Product principles

1. **Mobile first, not desktop reduced:** flows are designed around touch, virtual keyboards, compact screens, interruptions, and one-handed use.
2. **Fast path first:** a blank request and cURL paste are available immediately.
3. **Room is authoritative:** durable app data is stored through shared Room entities and DAOs.
4. **Local by default:** no account or server is required for the core product.
5. **Safe around secrets:** secret values are masked, redacted, and stored through platform security APIs.
6. **Environment awareness:** users should always know whether they are targeting local, staging, or production.
7. **Faithful HTTP behavior:** duplicate headers and parameters are preserved, and unusual requests are warned about rather than silently changed.
8. **Recoverable work:** draft edits survive navigation, process death, and app restarts.
9. **Bounded resource use:** large bodies are streamed and previewed within configurable limits.
10. **Portable data:** exports use a documented, versioned RequestLab format.

---

## 3. Improved product concept

### 3.1 Product statement

For people who need to test or debug REST APIs away from a laptop, RequestLab is a fast mobile API workspace that combines a touch-friendly request builder, safe environments, local collections, searchable history, and readable responses without the overhead of a collaboration platform.

### 3.2 Core product pillars

#### Pillar A - Quick Run

- Start with a blank GET request.
- Paste cURL from the clipboard.
- Reopen a recent or favorite request.
- Choose an active environment from the top bar.
- Send with a persistent, reachable action.

#### Pillar B - Safe Environments

- Color and label the active environment.
- Allow environments to be marked `local`, `development`, `staging`, `production`, or `custom`.
- Require an optional confirmation for protected environments.
- Preview the resolved host before sending.
- Never expose secret values in previews, logs, or default exports.

#### Pillar C - Mobile Response Lab

- Show status, duration, size, and content type at a glance.
- Provide JSON tree and raw views.
- Search within text responses.
- Copy a JSON value or JSON path.
- Share or save a response body.
- Preserve useful response metadata in history.

#### Pillar D - Local Workspace

- Collections and nested folders.
- Autosaved drafts.
- Favorites and recents.
- Immutable history entries.
- Room-backed search and filtering.
- Import and export through the platform file picker and share sheet.

#### Pillar E - Trust and Recovery

- Clear network error categories.
- No hidden rewrites of requests.
- Draft recovery after process termination.
- Database migrations that never use destructive fallback in production.
- Safe cleanup of large response files.

### 3.3 Primary differentiators

- Mobile-native interaction instead of a desktop-like table interface.
- cURL paste as a first-class onboarding and request-creation path.
- Production environment guardrails.
- Room-backed draft recovery and history.
- Strong secret handling with Keychain and Keystore.
- Response viewing designed for narrow screens.

### 3.4 Product vocabulary

- **Workspace:** the complete local RequestLab data set on a device.
- **Collection:** a top-level group of saved requests.
- **Folder:** a nested group inside a collection.
- **Saved request:** a reusable editable request definition.
- **Draft:** an autosaved request that has not necessarily been placed in a collection.
- **Run:** one execution of a request.
- **History entry:** an immutable snapshot of a run and selected response metadata.
- **Environment:** a named set of variables and safety settings.
- **Protected environment:** an environment that may require confirmation before send.
- **Secret variable:** a variable whose raw value is stored outside Room in secure platform storage.

---

## 4. Product goals, users, and success measures

### 4.1 Primary users

- Mobile developers testing staging or local-network APIs.
- Backend developers checking endpoints while away from a workstation.
- QA engineers reproducing request and response defects.
- Students learning HTTP and REST.
- Technical support engineers validating customer integrations.
- DevOps and operations staff running known health or diagnostic requests.

### 4.2 Jobs to be done

- "Let me paste this cURL command and see what the endpoint returns."
- "Let me switch the same request from staging to production safely."
- "Let me rerun a known health-check request from my phone."
- "Let me inspect and search a JSON response without opening a laptop."
- "Let me keep a small set of debugging requests organized and offline."
- "Let me reproduce the exact request I sent earlier."
- "Let me share a request without exposing my token."

### 4.3 Business and product goals

- Deliver a credible mobile REST workflow rather than every feature in a full desktop API suite.
- Achieve strong retention through favorites, recents, saved collections, and reliable drafts.
- Earn user trust through safe environment and secret behavior.
- Keep the MVP maintainable with a shared codebase and one database architecture.
- Leave clean extension points for OAuth, OpenAPI, GraphQL, and optional sync later.

### 4.4 Non-goals

- Desktop support.
- Browser support.
- Team collaboration in the MVP.
- Cloud synchronization in the MVP.
- Pre-request or test scripting in the MVP.
- WebSocket, SSE, GraphQL, gRPC, or SOAP in the MVP.
- Full Postman collection compatibility in the MVP.
- Rendering arbitrary HTML as active web content.
- Disabling TLS verification in the MVP.
- Acting as an HTTP interception proxy.

### 4.5 MVP success criteria

The MVP is ready when all of the following are true on both Android and iOS:

- A clean installation can create, send, cancel, and inspect a REST request.
- A valid cURL command can be pasted into a preview and imported.
- Request drafts, collections, environments, settings, and history survive process death and restart.
- All durable structured data is written through Room.
- Room migrations are verified from every released schema version.
- The same common repository and domain tests run for Android and iOS targets.
- Secret variables are absent from logs, crash reports, previews, and default exports.
- Protected environments display a visible warning and honor confirmation settings.
- A workspace can be exported, cleared, imported, and restored without secret values.
- Network errors are presented as understandable categories rather than raw exceptions.
- VoiceOver and TalkBack can navigate the primary request and response flows.

### 4.6 Suggested product metrics

Metrics are optional and must be privacy-preserving and opt-in if analytics are added.

- Time from launch to first successful request.
- Percentage of users who use cURL paste.
- Percentage of successful runs opened from favorites or recents.
- Draft recovery success rate after forced process termination.
- Request failure categories by aggregate count, without URLs or payloads.
- Crash-free sessions.
- Room migration success rate.
- Export and import completion rate.

---

## 5. Scope and release boundaries

## 5.1 MVP - P0

### Request creation

- Blank request creation.
- Paste and parse cURL from clipboard or share input.
- Methods: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS.
- Custom method entry.
- URL field with `{{variable}}` placeholders.
- Query parameter rows with enabled state and duplicate keys.
- Header rows with enabled state and duplicate names.
- Authentication:
  - None.
  - Basic.
  - Bearer token.
  - API key in header.
  - API key in query string.
- Body types:
  - None.
  - Raw text.
  - JSON.
  - XML text.
  - Form URL encoded.
  - Multipart form data with text and file fields.
- Per-request timeout override.
- Follow redirects preference where supported consistently.
- Resolved request preview with secret redaction.
- Save as draft automatically.
- Save to a collection.
- Duplicate, rename, move, favorite, and delete.
- Copy request as cURL with secrets excluded by default.

### Request execution

- Send one active request.
- Cancel an in-flight request.
- Keep execution running while navigating within the request workspace when the OS allows.
- Show sending progress and elapsed time.
- Classify timeout, cancellation, DNS, connection, TLS, malformed URL, and unknown failures.
- Store an immutable request snapshot for history.

### Response inspection

- Status code and reason phrase when available.
- Success, redirect, client error, and server error classification.
- Duration and body size.
- Content type.
- Response headers.
- Raw text view.
- Pretty JSON view.
- Expandable JSON tree.
- Search in textual responses.
- Line-wrap toggle.
- Copy body, selected value, JSON path, header, or response summary.
- Save or share body through platform APIs.
- Binary-response summary instead of unsafe text rendering.
- Truncation indicator when preview limits are reached.

### Local organization

- Home screen with quick request, cURL paste, favorites, and recents.
- Collections.
- Nested folders.
- Saved requests.
- Draft list and recovery.
- Search collections and saved requests.
- History list with filters for method, result, and date.
- Pin and delete history entries.
- Add an optional note to a history entry.
- Configurable history retention count.

### Environments

- Multiple environments.
- One active environment at a time.
- Environment type: local, development, staging, production, or custom.
- Protected environment flag.
- Optional confirm-before-send setting.
- Enabled and disabled variables.
- Public and secret variables.
- Missing-variable detection.
- Recursive resolution with cycle detection.
- Resolved host preview.
- Environment export without secrets by default.

### Settings and portability

- Theme: system, light, dark.
- Default timeout.
- Redirect preference.
- Response preview size limit.
- History retention limit.
- Biometric gate option for revealing secret variables.
- Clear response cache.
- Clear history.
- Clear all local data with confirmation.
- Versioned RequestLab workspace export and import.
- Import preview and conflict handling.
- Local diagnostic report with redaction.

## 5.2 P1 - Workflow depth

- Cookie jar and cookie inspector.
- Request and response comparison.
- JSON schema-friendly navigation and breadcrumb path.
- XML formatting and tree view.
- Image response preview.
- Certificate and TLS connection details where available.
- Temporary request variables with higher precedence than environment variables.
- Global variables with lower precedence than environment variables.
- Request templates.
- Deep links for opening a local request template.
- QR code import and export for redacted request definitions.
- HAR export for a single run.
- Postman collection import subset.
- OpenAPI import for endpoint generation.
- Optional encrypted export containing secrets.
- Tablet adaptive layouts if product demand justifies them.

## 5.3 P2 - Protocol expansion

- OAuth 2.0 authorization code with PKCE.
- OpenID Connect helper.
- GraphQL editor and variables.
- WebSocket client.
- Server-Sent Events client.
- Code snippet generation for common languages.
- Mock response definitions.
- Request chains and simple workflow runner.
- Client certificates where platform support and UX are safe.

## 5.4 P3 - Collaboration

- Optional account.
- End-to-end encrypted cloud sync.
- Team workspaces.
- Shared collections.
- Role-based access.
- Audit history.
- Conflict resolution.

---

## 6. Mobile platform strategy

### 6.1 Targets

The initial targets are:

- Android phone application.
- iOS phone application.

The project does not configure a desktop target. Desktop-specific code, dependencies, layouts, keyboard shortcuts, menus, packaging, and tests are excluded.

### 6.2 Shared responsibilities in `commonMain`

- Domain models and validation.
- Variable resolution.
- Request building policy.
- Ktor client orchestration.
- Error mapping.
- Room entities, DAOs, database declaration, and repositories.
- Import and export models.
- JSON formatting and response classification.
- Application navigation model.
- ViewModels and immutable UI state.
- Compose screens and shared components.
- Design tokens and themes.
- Common unit tests.

### 6.3 Android-specific responsibilities

- Android application entry point.
- Android Room database path builder.
- Ktor Android engine configuration.
- Android Keystore-backed secret store.
- File picker and document provider integration.
- Android share intents.
- Clipboard integration.
- BiometricPrompt integration.
- System bars and back navigation behavior.
- Notifications only if later required for long-running operations.
- Play Store packaging and signing.

### 6.4 iOS-specific responsibilities

- iOS application host and entry point.
- iOS Room database path builder using an application support or documents directory selected by policy.
- Ktor Darwin engine configuration.
- Keychain-backed secret store.
- UIDocumentPicker integration.
- Share sheet integration.
- UIPasteboard integration.
- LocalAuthentication integration for biometric reveal.
- Safe area, interactive swipe-back, and iOS sheet behavior.
- App Store packaging and signing.

### 6.5 UI sharing policy

The default is shared Compose UI. Platform adaptation is required when a native convention materially affects usability:

- Android uses Material 3 navigation, dialogs, back behavior, and system bar treatment.
- iOS uses familiar tab, navigation stack, sheet, swipe-back, and destructive-action patterns.
- The information architecture and visual identity stay consistent.
- The code should avoid forking entire screens unless platform behavior cannot be expressed cleanly through shared components.

### 6.6 Phone-first responsive breakpoints

- Compact phone: primary design target.
- Large phone: supported through adaptive spacing and wider editors.
- Foldable or tablet: graceful behavior is desirable, but dedicated multi-pane layouts are not an MVP requirement.
- Landscape: request and response screens must remain usable, especially for code and JSON, but portrait is the primary design orientation.

---

## 7. Information architecture and core flows

### 7.1 Top-level navigation

Use four top-level destinations:

1. **Home**
2. **Collections**
3. **History**
4. **More**

`More` contains Environments, Imports and Exports, Settings, Diagnostics, and About.

The active environment appears as a compact, persistent control in the top app bar on Home and request-related screens.

### 7.2 Home screen

The Home screen includes:

- New request action.
- Paste cURL action.
- Active environment indicator.
- Favorites.
- Recent requests.
- Recoverable drafts.
- Empty-state education for first use.

### 7.3 Request workspace

A mobile request uses a focused workspace rather than desktop tabs.

- Header: back, request name or `Untitled`, environment badge, overflow.
- Composer row: method selector, URL field, send or cancel button.
- Editor sections: Params, Headers, Auth, Body.
- Optional resolved-preview panel.
- After a run, a response summary appears and the user can switch between Request and Response.
- The draft is autosaved through Room.

### 7.4 Core flow A - First request

1. Launch app.
2. Tap New Request.
3. Enter URL.
4. Tap Send.
5. View status and formatted response.
6. Optionally save to a collection.

Acceptance target: no mandatory onboarding step blocks this flow.

### 7.5 Core flow B - Paste cURL

1. Tap Paste cURL on Home.
2. Read clipboard only after explicit user action.
3. Parse method, URL, headers, authentication, and body.
4. Show an import preview and warnings.
5. Redact suspected secrets in the preview.
6. Confirm import.
7. Open an editable draft.
8. Send or save.

### 7.6 Core flow C - Switch environment safely

1. Tap the environment badge.
2. Select an environment.
3. The URL preview updates.
4. A production or protected environment uses a high-visibility badge.
5. On Send, show confirmation when required.
6. Record the environment ID and resolved host in history, but not raw secrets.

### 7.7 Core flow D - Save and organize

1. Open an unsaved draft.
2. Tap Save.
3. Choose collection and optional folder.
4. Enter a name.
5. Save through a Room transaction.
6. Keep the draft linked to the new saved request.

### 7.8 Core flow E - Rerun from history

1. Open History.
2. Search or filter.
3. Open an entry.
4. Review the immutable request snapshot and previous response metadata.
5. Tap Rerun.
6. Create a new editable draft from the snapshot.
7. Never mutate the old history entry.

### 7.9 Core flow F - Export and restore

1. Open More > Import and Export.
2. Choose workspace or selected collections.
3. Review whether secrets are excluded.
4. Export a versioned JSON package.
5. Import on another installation.
6. Show validation and conflict preview.
7. Apply through a Room transaction.
8. Report imported, skipped, renamed, and failed items.

---

## 8. Functional requirements

## 8.1 Request editor

### FR-REQ-001 - Method and URL

- The method selector supports standard and custom methods.
- The URL supports variable placeholders.
- Whitespace around a URL is trimmed only at send time and does not silently modify the saved text.
- The UI shows malformed URL and unresolved variable errors before send.
- The Send action is reachable above the keyboard or in a stable top-bar position.

Acceptance criteria:

- Duplicate methods are not created in recent custom-method history.
- The exact user-entered URL template is preserved in Room.
- The resolved URL is stored only in a redacted run snapshot.

### FR-REQ-002 - Query parameters

- Each row has enabled state, key, value, and optional description.
- Duplicate keys are allowed.
- Blank enabled keys are warned about.
- URL query text and row editor can be synchronized through a deterministic parser.
- Reordering preserves order in the built request.

### FR-REQ-003 - Headers

- Each row has enabled state, name, value, and optional secret flag.
- Duplicate names are preserved.
- Header validation is advisory unless the engine cannot send the value.
- Restricted or engine-managed headers are explained rather than silently dropped.
- Secret header values are redacted in logs and exports.

### FR-REQ-004 - Authentication

- Basic auth stores username metadata in Room and secret password in secure storage when marked secret.
- Bearer token supports plain or environment-variable input.
- API key supports header or query placement.
- Switching auth type preserves previous values in the draft unless the user clears them.
- Generated auth headers are shown in request preview as redacted values.

### FR-REQ-005 - Request body

- Raw, JSON, XML, form, and multipart modes are supported.
- JSON validation is available without blocking send.
- A content type may be added automatically only after visible confirmation or according to a documented setting.
- Multipart file fields store a durable bookmark or imported app copy where platform APIs allow.
- Missing files are shown before send.

### FR-REQ-006 - Autosaved drafts

- A new request creates a Room draft record immediately.
- Changes are debounced and persisted after a short interval.
- Important actions such as backgrounding, leaving the screen, or sending trigger an immediate save attempt.
- Drafts include active editor section and safe UI restoration metadata.
- Drafts older than the configured retention period may be cleaned only when not pinned or linked to a saved request.

### FR-REQ-007 - Favorites and recents

- Saved requests and drafts may be favorited.
- Favorites are ordered manually or by most recent use.
- Recents are derived from durable run and open events stored through Room.
- Removing a favorite does not delete the request.

## 8.2 cURL import and export

### FR-CURL-001 - Parse cURL

- Support common cURL flags for method, header, data, form, authentication, URL, and redirect behavior.
- Unknown flags produce warnings and preserve the original command for reference.
- Shell expansion is not executed.
- Local file references require explicit user selection because clipboard paths are not portable.
- Potential secret values are identified heuristically and offered as secret variables.

### FR-CURL-002 - Copy as cURL

- Generate a readable command from the request draft.
- Exclude secret values by default.
- Provide placeholders for excluded secrets.
- Require explicit confirmation to include any raw secret.
- Use quoting rules that preserve spaces and special characters.

## 8.3 Environment variables

### FR-ENV-001 - Syntax

- Variables use `{{variable_name}}`.
- Keys are case-sensitive.
- Variable names may contain letters, numbers, underscores, dots, and hyphens.
- Escaped literal braces are supported through a documented syntax.

### FR-ENV-002 - Resolution

MVP precedence from highest to lowest:

1. Request-local temporary values generated by auth or body processing.
2. Active environment values.
3. Built-in safe values such as a generated request ID, only if introduced and documented.

- Recursive values are allowed up to a fixed depth.
- Cycles produce a validation error.
- Missing variables produce a validation error unless the user explicitly sends unresolved text.
- Secret values are resolved only at execution time.

### FR-ENV-003 - Protected environments

- Environment type and protection status are stored in Room.
- Protected environments use visible labels and iconography, not color alone.
- Optional confirmation displays the method and resolved host.
- A per-environment setting controls confirmation behavior.
- The confirmation cannot reveal secret query or header values.

## 8.4 Execution

### FR-EXEC-001 - Send

- Validate the draft.
- Resolve variables.
- Build the Ktor request.
- Create a pending history record before network execution.
- Execute on a background coroutine.
- Stream the body with bounded preview capture.
- Update history atomically with result metadata.

### FR-EXEC-002 - Cancel

- The Send action becomes Cancel while active.
- Cancellation propagates to the Ktor request job.
- The history entry records user cancellation distinctly from timeout or failure.
- Partial body files are deleted or marked for cleanup.

### FR-EXEC-003 - Error model

Required categories:

- Invalid request.
- Unresolved variable.
- User canceled.
- Timeout.
- DNS failure.
- Connection refused or unreachable.
- TLS or certificate failure.
- Redirect failure.
- Request body file unavailable.
- Response too large for configured storage policy.
- Unknown network failure.
- Persistence failure.

Each error includes:

- User-facing title.
- Actionable explanation.
- Optional safe technical details.
- Retry eligibility.
- Redaction-safe diagnostic code.

## 8.5 Response viewer

### FR-RESP-001 - Summary

Show:

- Status code.
- Status family.
- Duration.
- Size.
- Content type.
- Timestamp.
- Active environment label.
- Resolved host.

### FR-RESP-002 - Text and JSON

- Raw text mode preserves response text.
- Pretty JSON mode validates and formats JSON.
- Tree mode supports expand and collapse.
- Search highlights matches and provides next and previous navigation.
- Copy JSON path uses a documented dot or bracket notation.
- Large JSON uses lazy rendering where possible.

### FR-RESP-003 - Binary responses

- Do not decode arbitrary binary data as text.
- Display content type, size, and filename suggestion.
- Allow save or share.
- Image preview is P1 unless it is trivial and safe to add after MVP stability.

### FR-RESP-004 - Large responses

- Capture a configurable in-memory preview.
- Stream the full body to an app-managed file when allowed by policy.
- Store file metadata in Room.
- Clearly label truncation.
- Allow the user to delete the full body while keeping history metadata.
- Cleanup jobs must never delete files still referenced by Room.

## 8.6 Collections and folders

### FR-COL-001 - Structure

- A collection has a name, description, order, and timestamps.
- Folders may be nested to a documented maximum depth.
- Requests can live at collection root or inside a folder.
- Move and reorder operations are transactional.
- Deleting a collection requires confirmation and explains contained data.

### FR-COL-002 - Search

- Search name, URL template, description, and tags if tags are added.
- Search uses Room queries and debounced input.
- Search results identify the collection and folder path.

## 8.7 History

### FR-HIS-001 - Immutable snapshot

A history entry stores:

- Request template snapshot.
- Redacted resolved summary.
- Method and URL host.
- Environment ID and label snapshot.
- Start and finish timestamps.
- Duration.
- Result category.
- Status code.
- Response content type and size.
- Response header snapshot after redaction.
- Preview text or file reference according to limits.
- Optional user note.
- Pin state.

Editing or rerunning creates new data; the original snapshot does not change.

### FR-HIS-002 - Retention

- Default retention is bounded.
- Pinned entries are not removed automatically.
- Cleanup removes Room rows and owned response files transactionally where possible.
- Failed file deletion is retried and recorded without blocking the app.

## 8.8 Import and export

### FR-IO-001 - Export

- Export all workspace data or selected collections.
- Exclude raw secrets by default.
- Include schema version, app version, export timestamp, and content manifest.
- Large response bodies are excluded from the default workspace export.
- The user can choose whether to include history metadata.

### FR-IO-002 - Import

- Validate format and version before writing.
- Show counts and conflicts.
- Support keep both, replace, and skip.
- Apply accepted changes in a Room transaction where feasible.
- Roll back the database portion on fatal import failure.
- Never import a secret into plain Room storage.

---

## 9. System architecture

### 9.1 Architecture style

Use a pragmatic layered architecture with unidirectional state flow:

```text
Compose UI
   |
ViewModel / Presenter
   |
Use cases
   |
Repositories
   |--------------------------|
Room DAOs                Ktor request engine
   |                          |
Room database            Android / Darwin engine
   |
Secure storage references -> Keystore / Keychain
```

### 9.2 Layer responsibilities

#### Presentation

- Compose screens and components.
- Immutable UI state.
- User intents and one-off effects.
- Navigation.
- Platform-adaptive presentation behavior.

#### Domain

- Request validation.
- Variable resolution.
- Request and response models.
- Use cases.
- Error taxonomy.
- Import and export rules.
- No Android or iOS framework dependencies.

#### Data

- Room entities and DAOs.
- Entity-domain mappers.
- Repositories.
- Ktor implementation.
- Secure storage abstraction.
- File ownership and cleanup.

#### Platform

- Database builders.
- Network engines.
- File picker.
- Share sheet.
- Clipboard.
- Biometrics.
- Keystore or Keychain.

### 9.3 State management

Each feature exposes:

- `UiState`: complete immutable screen state.
- `UiAction`: user or lifecycle event.
- `UiEffect`: navigation, snackbar, share sheet, picker, or other one-time action.

ViewModels expose `StateFlow<UiState>` and accept actions through an explicit function.

### 9.4 Source of truth rule

- Room is the source of truth for persistent screen content.
- A successful write is reflected by observing Room, not by permanently mutating a parallel UI cache.
- Optimistic UI is allowed for responsiveness but must reconcile with Room results.
- Network responses are not automatically authoritative over saved request definitions.
- Secure storage is authoritative only for raw secret bytes; Room remains authoritative for secret identity and metadata.

### 9.5 Concurrency policy

- Use structured concurrency.
- One request execution has one parent job.
- Database and file work never run on the main thread.
- DAO writes are suspend functions.
- DAO observation uses `Flow`.
- Shared mutable state outside Room is minimized.
- Cleanup and import jobs use unique identifiers and durable progress metadata when interruption could corrupt user expectations.

### 9.6 Error boundaries

- Repository errors map to domain errors.
- UI never displays raw stack traces.
- Persistence failure is distinct from request failure.
- A network request may succeed even if history persistence later fails; the UI must show the response and a separate save warning.
- Import validation errors identify the exact item without leaking payload or secret content.

---

## 10. Recommended repository and module structure

Start with a moderate module count. Avoid creating one Gradle module per screen before the product stabilizes.

```text
requestlab/
|-- composeApp/
|   |-- src/commonMain/
|   |   |-- kotlin/app/
|   |   |-- kotlin/navigation/
|   |   |-- kotlin/designsystem/
|   |   |-- kotlin/feature/home/
|   |   |-- kotlin/feature/request/
|   |   |-- kotlin/feature/response/
|   |   |-- kotlin/feature/collections/
|   |   |-- kotlin/feature/history/
|   |   |-- kotlin/feature/environments/
|   |   |-- kotlin/feature/settings/
|   |   |-- kotlin/feature/importexport/
|   |   |-- composeResources/
|   |-- src/androidMain/
|   |-- src/iosMain/
|-- core/
|   |-- model/
|   |-- domain/
|   |-- common/
|-- data/
|   |-- database/
|   |-- network/
|   |-- securestorage/
|   |-- files/
|   |-- repository/
|-- androidApp/
|-- iosApp/
|-- schemas/
|-- docs/
|   |-- architecture/
|   |-- decisions/
|   |-- product/
|   |-- security/
|-- build-logic/
|-- gradle/
|-- settings.gradle.kts
```

### 10.1 Module responsibilities

#### `composeApp`

- Shared Compose UI.
- Navigation.
- ViewModels.
- Platform entry integration hooks.

#### `core:model`

- Stable domain data types.
- Serialization models shared across layers when appropriate.

#### `core:domain`

- Use cases.
- Validation.
- Variable resolver.
- Request snapshot rules.

#### `data:database`

- Room database.
- Entities.
- DAOs.
- Migrations.
- Database builders through platform source sets.

#### `data:network`

- Ktor client configuration.
- Request execution.
- Error mapping.
- Response streaming.

#### `data:securestorage`

- Common secret-store interface.
- Android Keystore implementation.
- iOS Keychain implementation.

#### `data:files`

- App-managed response body files.
- Import and export files.
- Checksums and cleanup.

#### `data:repository`

- Repository implementations combining Room, secure storage, files, and networking.

### 10.2 Dependency direction

```text
composeApp -> core:domain -> core:model
composeApp -> data:repository

data:repository -> data:database
data:repository -> data:network
data:repository -> data:securestorage
data:repository -> data:files

data:* -> core:model
core:* -> no UI or platform app module
```

No data module depends on a feature package.

---

## 11. Technology stack

### 11.1 Required stack

- Kotlin Multiplatform.
- Compose Multiplatform.
- Kotlin coroutines and Flow.
- Kotlin serialization.
- AndroidX Lifecycle ViewModel for multiplatform where compatible with the selected toolchain.
- AndroidX Navigation for Compose Multiplatform or a stable shared navigation solution selected at project initialization.
- Ktor Client.
- Android engine on Android.
- Darwin engine on iOS.
- AndroidX Room for Kotlin Multiplatform.
- Bundled SQLite driver configured through Room unless a documented compatibility reason requires platform drivers.
- KSP for Room code generation.
- A small dependency-injection solution such as Koin, or explicit constructor injection if the team prefers fewer runtime dependencies.
- A logging facade with release redaction.

### 11.2 Version policy

- Pin versions in `libs.versions.toml`.
- Use the current stable versions that are mutually compatible at repository creation.
- Keep Kotlin, Compose Multiplatform, Android Gradle Plugin, KSP, Room, SQLite driver, and Ktor compatibility in one documented matrix.
- Dependency upgrades require Android and iOS builds, common tests, Room schema verification, and smoke tests.
- Do not use dynamic versions.

### 11.3 Library selection constraints

A library must meet all of the following before entering shared production code:

- Supports Android and iOS targets used by the project.
- Has an acceptable maintenance and release history.
- Does not force a desktop target.
- Does not introduce a second persistence framework.
- Has a compatible license.
- Can be tested on iOS simulator and Android CI.
- Does not compromise secret redaction or network safety.

---

## 12. Room database policy

### 12.1 Mandatory database choice

Room is the only structured local database API used by RequestLab.

Allowed:

- Room entities.
- Room DAOs.
- Room transactions.
- Room migrations.
- Room `Flow` queries.
- Room-compatible SQLite driver configuration.
- SQL strings contained in reviewed Room DAO annotations or migrations, because they are part of Room's normal API.

Not allowed:

- SQLDelight.
- Direct SQLite connections in feature code.
- Core Data as a second iOS store.
- Realm as a second store.
- A separate key-value database for persistent feature data.
- Destructive migration fallback in production.
- Storing raw secrets in entity fields.

### 12.2 Database declaration

The database, entities, and DAOs live in shared `commonMain` code.

Illustrative structure:

```kotlin
@Database(
    entities = [
        CollectionEntity::class,
        FolderEntity::class,
        SavedRequestEntity::class,
        DraftEntity::class,
        RequestFieldEntity::class,
        RequestBodyEntity::class,
        MultipartPartEntity::class,
        EnvironmentEntity::class,
        EnvironmentVariableEntity::class,
        HistoryEntryEntity::class,
        ResponseArtifactEntity::class,
        AppSettingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(RequestLabDatabaseConstructor::class)
abstract class RequestLabDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun requestDao(): RequestDao
    abstract fun draftDao(): DraftDao
    abstract fun environmentDao(): EnvironmentDao
    abstract fun historyDao(): HistoryDao
    abstract fun settingsDao(): SettingsDao
}

@Suppress("KotlinNoActualForExpect")
expect object RequestLabDatabaseConstructor :
    RoomDatabaseConstructor<RequestLabDatabase>
```

The exact imports and APIs must follow the Room version selected by the project.

### 12.3 Platform builders

Only database path creation is platform-specific.

#### Android

- Use application context.
- Store the database in the app database directory.
- Use one singleton database instance per process.

#### iOS

- Use an application-owned persistent directory.
- Do not use a temporary directory.
- Ensure backup behavior matches product policy.
- Use one database instance for the app process.

#### Shared configuration

- Configure the selected Room-compatible driver.
- Configure a background coroutine context.
- Register explicit migrations.
- Register callbacks only when supported across required targets.

### 12.4 DAO rules

- Non-reactive DAO operations are suspend functions.
- Observable queries return `Flow`.
- DAO methods return entities or dedicated database projections, not UI models.
- Repositories perform mapping.
- Large multi-entity writes use transactions.
- `@RawQuery` is avoided unless a normal DAO query cannot express the requirement.
- Every query used by a user-facing list has an index review.
- DAO tests cover ordering, filtering, deletion, and cascades.

### 12.5 Entity rules

- Use stable string UUIDs unless a measured reason supports numeric keys.
- Store timestamps in a consistent UTC epoch format.
- Use explicit enum serialization values rather than ordinal positions.
- Avoid storing large unbounded response bodies directly in Room.
- Store request snapshots as normalized child rows where editing and search matter.
- JSON snapshot columns are acceptable for immutable history payloads when schema evolution is versioned and tested.
- Foreign keys and deletion behavior are explicit.
- Important list and lookup fields have indices.

### 12.6 Repository rules

- Features never receive a DAO directly.
- Repositories expose domain models and operations.
- Repositories coordinate Room with secure storage and files.
- Cross-resource operations define rollback or compensation behavior.
- Repository methods document which resource is authoritative.

---

## 13. Room data model

The exact schema may evolve, but the following model is the target for MVP.

## 13.1 `collection`

Fields:

- `id: String` primary key.
- `name: String`.
- `description: String?`.
- `sortOrder: Long`.
- `createdAt: Long`.
- `updatedAt: Long`.

Indices:

- `name` for search support if useful.
- `sortOrder`.

## 13.2 `folder`

Fields:

- `id: String` primary key.
- `collectionId: String` foreign key.
- `parentFolderId: String?` self-reference.
- `name: String`.
- `sortOrder: Long`.
- `createdAt: Long`.
- `updatedAt: Long`.

Rules:

- Cascade when the parent collection is deleted.
- Prevent cycles in repository logic.
- Enforce maximum nesting depth in domain validation.

## 13.3 `saved_request`

Fields:

- `id: String` primary key.
- `collectionId: String?`.
- `folderId: String?`.
- `name: String`.
- `description: String?`.
- `method: String`.
- `urlTemplate: String`.
- `authType: String`.
- `timeoutMillis: Long?`.
- `followRedirects: Boolean?`.
- `isFavorite: Boolean`.
- `sortOrder: Long`.
- `createdAt: Long`.
- `updatedAt: Long`.
- `lastRunAt: Long?`.

Rules:

- A request may be temporarily unfiled.
- Moving a request updates collection and folder in one transaction.

## 13.4 `request_field`

Represents query parameters, headers, form fields, and selected auth fields.

Fields:

- `id: String` primary key.
- `ownerType: String` - saved request or draft.
- `ownerId: String`.
- `fieldType: String` - query, header, form, auth metadata.
- `name: String`.
- `valueTemplate: String`.
- `enabled: Boolean`.
- `isSecret: Boolean`.
- `secureValueRef: String?`.
- `sortOrder: Long`.
- `description: String?`.

Rules:

- If `isSecret` is true, `valueTemplate` contains a placeholder or masked representation, not the raw secret.
- Raw secret bytes are retrieved from secure storage through `secureValueRef`.

## 13.5 `request_body`

Fields:

- `ownerType: String`.
- `ownerId: String` primary key pair.
- `bodyType: String`.
- `contentType: String?`.
- `rawTemplate: String?`.
- `updatedAt: Long`.

Multipart rows are stored separately.

## 13.6 `multipart_part`

Fields:

- `id: String` primary key.
- `ownerType: String`.
- `ownerId: String`.
- `name: String`.
- `kind: String` - text or file.
- `valueTemplate: String?`.
- `fileArtifactId: String?`.
- `contentType: String?`.
- `enabled: Boolean`.
- `isSecret: Boolean`.
- `secureValueRef: String?`.
- `sortOrder: Long`.

## 13.7 `draft`

Fields:

- `id: String` primary key.
- `linkedSavedRequestId: String?`.
- `displayName: String`.
- `method: String`.
- `urlTemplate: String`.
- `authType: String`.
- `selectedEditorSection: String`.
- `activeEnvironmentId: String?`.
- `isFavorite: Boolean`.
- `createdAt: Long`.
- `updatedAt: Long`.
- `lastOpenedAt: Long`.
- `lastAutosaveState: String`.

Associated request fields and body rows use `ownerType = draft`.

## 13.8 `environment`

Fields:

- `id: String` primary key.
- `name: String`.
- `environmentType: String`.
- `isActive: Boolean`.
- `isProtected: Boolean`.
- `confirmBeforeSend: Boolean`.
- `badgeLabel: String?`.
- `createdAt: Long`.
- `updatedAt: Long`.

Rules:

- Exactly zero or one active environment.
- Active-environment changes occur transactionally.

## 13.9 `environment_variable`

Fields:

- `id: String` primary key.
- `environmentId: String` foreign key.
- `key: String`.
- `publicValueTemplate: String?`.
- `isSecret: Boolean`.
- `secureValueRef: String?`.
- `enabled: Boolean`.
- `description: String?`.
- `sortOrder: Long`.
- `createdAt: Long`.
- `updatedAt: Long`.

Constraints:

- Unique key within one environment unless a future scoped-variable model explicitly permits duplicates.
- Secret rows cannot contain raw values.

## 13.10 `history_entry`

Fields:

- `id: String` primary key.
- `sourceRequestId: String?`.
- `sourceDraftId: String?`.
- `method: String`.
- `urlTemplateSnapshot: String`.
- `resolvedHostSnapshot: String?`.
- `requestSnapshotJson: String` - versioned and redacted where required.
- `environmentId: String?`.
- `environmentNameSnapshot: String?`.
- `startedAt: Long`.
- `finishedAt: Long?`.
- `durationMillis: Long?`.
- `resultType: String`.
- `statusCode: Int?`.
- `responseContentType: String?`.
- `responseSizeBytes: Long?`.
- `responseHeadersJson: String?`.
- `responsePreviewText: String?`.
- `responseArtifactId: String?`.
- `errorCode: String?`.
- `safeErrorDetail: String?`.
- `isPinned: Boolean`.
- `note: String?`.

Indices:

- `startedAt`.
- `method`.
- `statusCode`.
- `resultType`.
- `isPinned`.
- `sourceRequestId`.

## 13.11 `response_artifact`

Fields:

- `id: String` primary key.
- `ownerHistoryId: String?`.
- `kind: String`.
- `relativePath: String`.
- `mimeType: String?`.
- `sizeBytes: Long`.
- `checksum: String?`.
- `createdAt: Long`.
- `lastAccessedAt: Long?`.
- `retentionState: String`.

Rules:

- Paths are app-relative, not arbitrary user paths.
- Deletion is coordinated through a repository.
- Orphan scans compare filesystem state to Room references.

## 13.12 `app_setting`

Fields:

- `key: String` primary key.
- `valueJson: String`.
- `updatedAt: Long`.

Settings stored here include theme, timeouts, response limits, retention, and privacy choices.

Highly platform-specific ephemeral preferences may remain platform-side only when they do not affect persistent product behavior.

## 13.13 Optional `search_index`

Do not add a custom search table until standard indexed Room queries are measured and shown insufficient. If full-text search is introduced later, it must remain managed through Room.

---

## 14. Persistence behavior and migrations

### 14.1 Transaction boundaries

Use one Room transaction for:

- Saving a request and all child fields.
- Moving a request between collection and folder.
- Importing one logical collection.
- Switching the active environment.
- Deleting a collection and its owned database rows.
- Finalizing a history record with response metadata.
- Applying retention cleanup row changes.

Filesystem and secure-storage changes cannot always participate in the same database transaction. Use staged operations and compensation:

1. Prepare external resource.
2. Commit Room reference.
3. On Room failure, delete the prepared resource.
4. On external cleanup failure, mark a retry state in Room.

### 14.2 Autosave behavior

- Create the draft record before the user enters substantial content.
- Debounce normal edits, for example 300 to 750 milliseconds.
- Flush immediately on Send, Save, background, and navigation away.
- Show a subtle saving, saved, or failed state.
- Do not block typing while saving.
- Retry transient persistence errors.

### 14.3 Schema versioning

- Export Room schemas to a repository directory.
- Commit every released schema.
- Every schema change includes an explicit migration.
- Migrations are forward-only in production.
- Destructive fallback is prohibited for user data.
- Migration code must preserve secret references and artifact ownership.

### 14.4 Migration test matrix

For every release candidate:

- Create databases from all previously released schemas.
- Insert representative collections, drafts, environments, secret references, history, and artifacts.
- Upgrade to the current schema on Android.
- Upgrade to the current schema on an iOS simulator.
- Verify row counts, relationships, ordering, and representative values.
- Verify no raw secrets appear in the migrated database.
- Verify referenced response files remain reachable.

### 14.5 Backup and restore policy

- Room database and app-managed files are local app data.
- Platform backup behavior must be reviewed for secret references and response bodies.
- Raw Keychain or Keystore secrets are not assumed to restore across devices.
- Workspace export is the supported portable restore mechanism for non-secret data.
- After import, missing secrets are clearly marked and require re-entry.

### 14.6 Data deletion

Clear All Data performs:

1. Cancel active network operations.
2. Close or safely reset repositories.
3. Delete Room rows and database files using supported APIs.
4. Delete app-managed response and import files.
5. Delete RequestLab-owned secure-storage entries.
6. Recreate a clean database.
7. Verify the app launches into the first-run state.

---

## 15. Request execution engine

### 15.1 Engine choice

- Android uses a Ktor engine appropriate for Android.
- iOS uses the Darwin engine.
- Shared code owns request construction and result mapping.
- Engine differences are represented as capabilities, not hidden.

### 15.2 Execution pipeline

```text
Load draft from Room
    -> validate
    -> load active environment from Room
    -> retrieve required secrets from secure storage
    -> resolve variables
    -> show protected-environment confirmation if required
    -> create pending history entry in Room
    -> build Ktor request
    -> execute
    -> stream response
    -> capture bounded preview
    -> optionally write full body artifact
    -> classify response or error
    -> finalize history entry in Room
    -> render response
```

### 15.3 Request validation

Validation levels:

- **Blocking:** malformed URL, missing required file, unresolved protected value, invalid body configuration.
- **Warning:** GET with a body, duplicate content-type headers, blank enabled parameter, unknown cURL flag.
- **Information:** content type inferred, redirect policy, large response storage behavior.

### 15.4 Variable resolution

- Resolve late, immediately before execution.
- Preserve the original template.
- Redact secret values from all debug representations.
- Record only the resolved host and safe metadata in history.
- Resolution returns a structured report containing used keys, missing keys, cycles, and secret participation.

### 15.5 Header behavior

- Preserve duplicate headers when the engine permits.
- Preserve row order where practical.
- Add generated auth headers after user headers according to documented precedence.
- Do not log Authorization, Cookie, Set-Cookie, API-key-like headers, or user-marked secret headers.
- Explain engine-controlled headers in the UI.

### 15.6 Body behavior

- Encode JSON and raw text using a documented charset policy.
- Preserve explicit content type.
- Build form and multipart bodies from ordered fields.
- Stream file parts rather than reading all files into memory.
- Fail before network execution when a selected file is inaccessible.

### 15.7 Timing

Record:

- Start timestamp.
- Finish timestamp.
- Total duration.

DNS, connection, TLS, and server timing breakdowns are P1 unless reliable cross-platform instrumentation is available.

### 15.8 Retry policy

The app does not automatically retry non-idempotent requests in the MVP.

- User-triggered Retry creates a new history entry.
- GET and HEAD may be offered as safe retries after transport failure.
- POST, PUT, PATCH, and DELETE require an explicit user action.
- No hidden automatic retry after timeout.

### 15.9 Background behavior

- A request may continue during normal in-app navigation.
- The app does not promise indefinite execution after the OS suspends or terminates it.
- UI explains when a run was interrupted by app termination.
- Long-running background execution is deferred unless a clear product need justifies platform-specific services.

### 15.10 Mobile networking, cleartext, and local-network permissions

#### HTTPS baseline

- HTTPS is the default and recommended protocol.
- Use normal system trust evaluation.
- Do not pin certificates because RequestLab connects to arbitrary user-selected hosts.
- Do not disable certificate verification in the MVP.

#### Cleartext HTTP policy

A developer API client may need HTTP for local development, emulators, devices on a LAN, and legacy endpoints. The product must treat this as an explicit security case.

- Support HTTPS everywhere.
- Support local-network HTTP only after platform configuration, a clear user warning, and release review.
- Show an `Insecure HTTP` warning before the first cleartext send to a host.
- Explain that traffic may be observed or modified.
- Never include secret headers or query values in the warning.
- Public arbitrary HTTP support is a separate release decision and must not be enabled silently.

#### iOS App Transport Security

- Ktor Darwin uses Apple networking behavior, so App Transport Security applies.
- Prefer the narrowest ATS configuration that supports the accepted product scope.
- Prefer local-network allowances or targeted exceptions over a global arbitrary-load exception.
- Map ATS blocks to a specific, actionable error category.
- Verify the final `Info.plist` and any App Store justification during every release.

#### iOS local-network privacy

- Include `NSLocalNetworkUsageDescription` because users may connect directly to local hosts.
- Show a short pre-permission explanation when the user first attempts a local endpoint.
- Do not trigger the prompt during onboarding without context.
- If access is denied, show a Settings recovery path.
- Test direct IP addresses and `.local` hostnames.

#### Android network security

- Declare the Internet permission.
- Use Android Network Security Configuration deliberately.
- Keep HTTPS as the baseline.
- Map blocked cleartext traffic to a specific error category.
- Do not assume debug-build cleartext behavior is acceptable for release.

#### Android local-network permission

- Isolate local-network permission checks behind a platform adapter.
- Reverify requirements whenever target SDK changes.
- Android 17 enforcement for apps targeting SDK 37 or higher must be included in planning and testing.
- Request `ACCESS_LOCAL_NETWORK` at runtime when the targeted Android version and SDK require it.
- Request permission only when the user attempts a local-network request.
- Explain why direct LAN access is needed.
- Handle denial without crashing and provide a Settings recovery path.

#### Network test matrix

Test at minimum:

- Public HTTPS.
- HTTPS redirect.
- Local HTTP IP address.
- `.local` hostname.
- Invalid DNS.
- Connection refused.
- Slow response and timeout.
- TLS hostname mismatch.
- Expired certificate.
- Large and compressed responses.
- Connection interrupted mid-body.
- Local-network permission denied and later granted.
- Cleartext blocked by platform policy.

---

## 16. Environment variables and secrets

### 16.1 Secret storage abstraction

```kotlin
interface SecretStore {
    suspend fun put(id: String, value: ByteArray)
    suspend fun get(id: String): ByteArray?
    suspend fun delete(id: String)
    suspend fun contains(id: String): Boolean
}
```

Implementations:

- Android: Keystore-backed encrypted storage.
- iOS: Keychain.

### 16.2 Secret metadata in Room

Room stores:

- Variable identity.
- Environment relationship.
- Key name.
- Enabled state.
- Secret flag.
- Opaque secure-storage reference.
- Timestamps.

Room does not store:

- Bearer tokens.
- Passwords.
- API key values.
- Client secrets.
- Secret cookies.
- Any user-marked secret value.

### 16.3 Secret lifecycle

- Create secure value first, then commit its Room reference.
- If Room commit fails, delete the secure value.
- On secret deletion, remove the Room reference and then delete the secure entry, with retry metadata if needed.
- On import without secrets, create missing-secret placeholders.
- On app data wipe, remove all RequestLab-owned secure entries.

### 16.4 Reveal behavior

- Secret fields are masked by default.
- Reveal is temporary.
- Optional biometric verification gates reveal.
- Revealed values are hidden when the app backgrounds.
- Clipboard copy of a secret displays a warning and may auto-clear on Android where practical; iOS behavior must respect platform limitations.

### 16.5 Redaction rules

Always redact:

- Authorization.
- Proxy-Authorization.
- Cookie and Set-Cookie.
- Common API key header names.
- User-marked secret fields.
- Secret environment substitutions.
- Passwords and tokens in generated cURL unless explicitly included.

Redaction should preserve enough shape for debugging, such as prefix and length only when that does not reduce safety.

---

## 17. Import, export, and sharing

### 17.1 RequestLab workspace format

Use a versioned JSON format with:

- `format` identifier.
- `schemaVersion`.
- `exportedAt`.
- `appVersion`.
- Collections.
- Folders.
- Saved requests.
- Environment metadata and non-secret variables.
- Optional history metadata.
- Manifest and checksums if the format becomes an archive.

### 17.2 Default export safety

Default exports exclude:

- Raw secret values.
- Full response body files.
- Sensitive response headers.
- Clipboard history.
- Diagnostic logs.

### 17.3 Conflict policy

Per imported item:

- Keep both with a generated suffix.
- Replace existing.
- Skip.

The preview shows affected counts before any write.

### 17.4 cURL sharing

- Copy as cURL.
- Share redacted cURL through the system share sheet.
- Include a visible note when placeholders replace secrets.
- Do not execute shell substitutions or command chaining from imported cURL.

### 17.5 File ownership

- Imported workspace files are read through platform pickers.
- The app copies only data it must own.
- Temporary import files are deleted after completion.
- Room tracks any persistent app-owned artifact.

---

## 18. Security and privacy

### 18.1 Threat model

Protect against:

- Accidental secret exposure in logs, exports, screenshots, previews, clipboard, and crash reports.
- Sending a request to production by mistake.
- Executing malicious shell content from cURL.
- Rendering active HTML or scripts.
- Unbounded memory use from large responses.
- Path traversal or arbitrary file overwrite during import and export.
- Database downgrade or destructive migration.
- Orphaned secret or response artifacts.

### 18.2 TLS policy

- Validate TLS normally.
- No insecure certificate bypass in MVP.
- Present certificate failures clearly.
- Do not suggest bypassing TLS as the default solution.
- Client certificate support is deferred.

### 18.3 Logging

Release logs may contain:

- Safe event name.
- Operation ID.
- Result category.
- Duration bucket.
- Database migration version.

Release logs must not contain:

- Full URL with query values.
- Request or response body.
- Headers.
- Environment values.
- Secret references that expose user meaning.
- File contents.

### 18.4 Screenshots and app switcher

- Consider a privacy-screen option for app-switcher snapshots.
- Hide temporarily revealed secrets when backgrounded.
- Do not globally block screenshots by default because response sharing is a legitimate workflow.
- Offer a high-security setting later if user demand supports it.

### 18.5 Clipboard

- Read clipboard only after an explicit user action.
- Warn before copying raw secrets.
- Prefer redacted cURL.
- Do not continuously monitor clipboard content.

### 18.6 Import safety

- Enforce file size limits.
- Validate schema and types.
- Reject path traversal.
- Reject executable content as an instruction source.
- Treat imported text as data.
- Show partial failures without applying ambiguous data silently.

### 18.7 Privacy posture

- No account required.
- No network analytics required.
- Core workspace stays on device.
- Any future analytics are opt-in, aggregate, and payload-free.
- A plain-language privacy notice explains local data, exports, secure storage, and optional diagnostics.

---

## 19. Performance and reliability

### 19.1 Performance targets

Use measured targets on agreed reference devices:

- Home screen becomes interactive quickly after cold start.
- Opening a typical saved request feels immediate.
- Typing in URL, headers, and bodies remains responsive during autosave.
- Room list queries return paged or bounded data for large histories.
- JSON formatting never blocks the main thread.
- Scrolling a typical response remains smooth.

Exact millisecond budgets should be established after the first vertical slice and tracked in benchmarks.

### 19.2 Operational defaults

Suggested defaults:

- In-memory response preview: 2 MB.
- Full body disk capture: enabled up to a configurable safe limit.
- History retention: 500 entries.
- Draft retention: 30 days for unpinned empty or abandoned drafts.
- Maximum environment-resolution depth: 10.
- Maximum folder nesting depth: 8.
- Maximum import file size: documented and configurable for testing.

These values are product defaults, not hard architectural limits.

### 19.3 Large response strategy

- Stream network bytes.
- Capture preview up to the configured limit.
- Write full body to an app-owned file when allowed.
- Store metadata in Room.
- Use lazy rendering for tree nodes.
- Avoid building multiple full-size string copies.
- Allow the user to delete stored full bodies independently.

### 19.4 Database performance

- Index history timestamps, status, result type, and source request.
- Index folder and request ordering fields.
- Use projections for list screens rather than loading full bodies or snapshots.
- Paginate history.
- Debounce search.
- Run query-plan review for slow queries.
- Keep Room transactions short and free of network work.

### 19.5 Reliability behaviors

- Drafts survive process death.
- Pending history entries are reconciled on next launch.
- Partial response files are cleaned.
- Failed secure-storage cleanup is retried.
- Import either reports a complete result or a clearly defined partial result.
- Database corruption handling prioritizes non-destructive recovery and export when possible.

### 19.6 Startup reconciliation

On launch:

1. Open Room and run migrations.
2. Find history entries left in pending state.
3. Mark clearly interrupted runs.
4. Find response artifacts marked temporary.
5. Delete safe orphans.
6. Check secure references for missing values without reading all secret content into memory.
7. Continue to Home even if non-critical cleanup fails.

---

## 20. Testing strategy

### 20.1 Test pyramid

- Common unit tests for pure domain logic.
- Room DAO and migration tests on Android and iOS.
- Ktor MockEngine tests for request construction and error mapping.
- Integration tests against a controlled local test server.
- Compose UI tests for shared flows where supported.
- Platform integration tests for file, clipboard, biometric, and secure storage behavior.
- Manual release matrix on representative Android and iOS devices.

### 20.2 Domain unit tests

#### Variable resolver

- Simple public variable.
- Secret variable.
- Nested variable.
- Missing variable.
- Cycle.
- Depth limit.
- Same key in different environments.
- Disabled variable.
- Escaped braces.
- Secret redaction report.

#### Request validator

- Valid and invalid URLs.
- Duplicate headers.
- GET with body warning.
- Missing multipart file.
- Conflicting content type.
- Custom method.
- Protected environment confirmation requirement.

#### cURL parser

- Simple GET.
- Explicit method.
- Multiple headers.
- JSON data.
- Form data.
- Basic auth.
- Quoting and escaped characters.
- Unknown flag.
- Shell substitution treated as literal or rejected.
- Local file reference warning.

#### Import and export

- Round trip without secrets.
- Unsupported version.
- Duplicate IDs.
- Conflict policies.
- Invalid enum.
- Oversized file.
- Path traversal attempt.
- Secret placeholder preservation.

### 20.3 Room DAO tests

- Insert and observe collections.
- Nested folder ordering.
- Move request transaction.
- Draft autosave update.
- Active environment uniqueness.
- Environment variable uniqueness.
- History filters and pagination.
- Pinned retention behavior.
- Cascade delete behavior.
- Response artifact references.
- Search projections.

### 20.4 Room migration tests

For each released version pair:

- Create old schema.
- Insert representative data.
- Run migration.
- Validate schema.
- Verify values and relationships.
- Verify secret columns contain no raw values.
- Verify history snapshot version handling.
- Verify response file references.

Run on:

- Android instrumentation target.
- iOS simulator test target.

### 20.5 Network tests

Use Ktor MockEngine for:

- Header order and duplicates.
- Query encoding.
- Auth generation.
- JSON body.
- Multipart body metadata.
- Timeout mapping.
- Cancellation mapping.
- Redirect behavior.
- Binary response classification.
- Preview truncation.
- Redaction.

Use a controlled integration server for:

- Real TLS success.
- Redirect chain.
- Delayed response.
- Chunked body.
- Large body streaming.
- Gzip response.
- Malformed JSON.
- Duplicate response headers.
- Connection close mid-body.

### 20.6 UI test flows

- First launch to successful GET.
- Paste cURL to editable request.
- Add duplicate headers.
- Change environment.
- Protected environment confirmation.
- Send and cancel.
- JSON search and copy path.
- Save request to a new collection.
- Recover draft after recreation.
- Rerun history snapshot.
- Export and import workspace.
- Enter and reveal secret with biometric option mocked.
- Clear all data.

### 20.7 Accessibility tests

- TalkBack focus order.
- VoiceOver focus order.
- Labels for method, environment, status, and icon-only actions.
- Dynamic text scaling.
- Color contrast.
- Color-independent status meaning.
- Touch target size.
- Keyboard avoidance.
- Reduced motion behavior.

### 20.8 Manual device matrix

At minimum:

- One current Android phone.
- One older supported Android phone.
- One Android device with a small display.
- One current iPhone.
- One older supported iPhone.
- One small iPhone form factor if supported by the deployment target.
- Android light and dark mode.
- iOS light and dark mode.
- Android and iOS with large text.
- Slow and unreliable network profiles.

---

## 21. Observability and diagnostics

### 21.1 Local structured logging

Use categories:

- App lifecycle.
- Room open and migration.
- Repository operations.
- Request lifecycle.
- Import and export.
- File cleanup.
- Secure-storage result category.

Each event should have a safe operation ID and result, never payload content.

### 21.2 Diagnostic report

A user-generated diagnostic report may contain:

- App version.
- Platform and OS version.
- Database schema version.
- Counts of collections, requests, drafts, environments, history entries, and artifacts.
- Storage usage totals.
- Recent safe error codes.
- Migration history.
- Network engine name.
- Feature capability flags.

It must not contain URLs, bodies, headers, variable values, secret values, request names if considered sensitive, or response content.

### 21.3 Crash reporting

If added:

- Opt-in where required by policy.
- Scrub breadcrumbs and custom keys.
- Do not attach database files.
- Do not attach request state.
- Document the vendor and retention policy.

---

## 22. CI/CD and release engineering

### 22.1 Branching

- Trunk-based development with short-lived branches.
- Protected main branch.
- Pull requests required.
- Release tags from main.

### 22.2 Pull request checks

- Formatting and static analysis.
- Common unit tests.
- Android unit tests.
- Room schema export consistency.
- Room database tests available in the PR environment.
- Ktor MockEngine tests.
- Android debug build.
- iOS simulator compile or test on a macOS runner.
- Dependency license check.
- Secret scanning.

### 22.3 Nightly checks

- Full Android instrumentation suite.
- Full iOS simulator test suite.
- Room migration matrix.
- Integration server tests.
- Large-response tests.
- Import fuzz or malformed-input suite.
- Performance smoke benchmarks.

### 22.4 Release candidate checks

- Signed Android App Bundle.
- iOS archive.
- Clean install tests.
- Upgrade from previous public version.
- Database migration tests.
- Export before upgrade and import after upgrade.
- Secret-storage regression test.
- Accessibility smoke test.
- Privacy manifest and store disclosure review.
- Crash symbol and mapping upload if crash reporting exists.

### 22.5 Versioning

- Semantic versioning for app releases where practical.
- Separate Room schema version maintained in code.
- Export format version maintained independently.
- History snapshot format version maintained independently.
- Changelog calls out migrations and data-format changes.

### 22.6 Distribution

- Android: internal testing, closed testing, production.
- iOS: TestFlight internal, TestFlight external, App Store.
- No desktop artifacts.

### 22.7 Signing and secrets

- CI signing credentials are stored in the CI secret manager.
- No signing key or provisioning profile is committed.
- Build logs do not print credentials.
- Release jobs require protected approval.

---

## 23. Implementation milestones

The sequence below is dependency-driven. Team size determines calendar duration.

## Milestone 0 - Product and repository foundation

Deliverables:

- Create Android and iOS KMP project.
- Configure Compose Multiplatform.
- Configure version catalog.
- Add static analysis and formatting.
- Establish common architecture packages.
- Add Android and iOS CI compile jobs.
- Record architecture decisions.
- Add initial design system tokens.

Exit criteria:

- Android and iOS launch a shared placeholder screen.
- Common tests run in CI.
- No desktop target exists.

## Milestone 1 - Room vertical slice

Deliverables:

- Add Room KMP, KSP, and schema export.
- Create database declaration and platform builders.
- Create `DraftEntity` and DAO.
- Create repository and Flow observation.
- Persist one editable URL draft.
- Add Android and iOS database tests.

Exit criteria:

- Typed URL survives process restart on both platforms.
- Room schema version 1 is committed.
- No other persistence framework is present.

## Milestone 2 - Send one request end to end

Deliverables:

- Ktor Android and Darwin engines.
- Basic GET request.
- Send, cancel, loading, success, and failure states.
- Pending and completed history entry in Room.
- Raw response preview.

Exit criteria:

- Same test endpoint works on Android and iOS.
- Cancel is functional.
- Result survives restart in History.

## Milestone 3 - Complete mobile request composer

Deliverables:

- Methods and custom method.
- Query parameter editor.
- Header editor.
- Auth modes.
- Body modes.
- Multipart file integration.
- Validation.
- Debounced Room autosave.
- Resolved request preview.

Exit criteria:

- Representative requests match expected server observations.
- Draft recovery works across app termination.

## Milestone 4 - Response Lab

Deliverables:

- Status summary.
- Header view.
- Raw, pretty JSON, and JSON tree modes.
- Search and copy path.
- Large-response preview and file artifact.
- Share and save response body.

Exit criteria:

- Large response does not cause unbounded memory growth.
- Text and binary behavior is clear.

## Milestone 5 - Collections, folders, favorites, and history

Deliverables:

- Collections and nested folders.
- Save, rename, duplicate, move, reorder, and delete.
- Favorites and recents.
- History filters, notes, pinning, rerun.
- Retention cleanup.
- Room transaction coverage.

Exit criteria:

- All organization data survives restart.
- Deletion and retention remove owned files safely.

## Milestone 6 - Environments and secure secrets

Deliverables:

- Environment CRUD.
- Active environment selector.
- Variable resolver.
- Keystore and Keychain implementations.
- Protected environment confirmation.
- Secret reveal and redaction.

Exit criteria:

- Raw secrets cannot be found in Room database inspection, logs, or default export.
- Production guard flow works on both platforms.

## Milestone 7 - cURL and workspace portability

Deliverables:

- Paste cURL parser and preview.
- Copy and share redacted cURL.
- RequestLab export.
- Import validation and conflict handling.
- Transactional database import.
- Missing-secret placeholders.

Exit criteria:

- Export, clear, import round trip restores non-secret workspace content.
- Malicious or malformed import samples are safely rejected.

## Milestone 8 - Mobile design polish and accessibility

Deliverables:

- Final Android and iOS adaptive behavior.
- Light and dark themes.
- Dynamic type.
- TalkBack and VoiceOver pass.
- Keyboard and safe-area fixes.
- Empty, loading, error, and offline states.
- Haptics and reduced motion.

Exit criteria:

- Design acceptance checklist passes on the manual device matrix.

## Milestone 9 - Release hardening

Deliverables:

- Migration matrix.
- Performance tests.
- Security review.
- Privacy notice.
- Store assets and disclosures.
- Beta feedback fixes.
- Signed release artifacts.

Exit criteria:

- Release definition of done passes.
- No P0 defect remains open.

---

## 24. Prioritized issue backlog

The IDs below can be copied into an issue tracker.

| ID | Priority | Milestone | Issue |
|---|---:|---:|---|
| FND-001 | P0 | 0 | Create Android and iOS KMP project without desktop target |
| FND-002 | P0 | 0 | Configure Compose Multiplatform and shared resources |
| FND-003 | P0 | 0 | Configure CI for Android and iOS simulator builds |
| FND-004 | P0 | 0 | Add formatting, static analysis, and dependency checks |
| DB-001 | P0 | 1 | Add Room KMP, KSP, and schema directory |
| DB-002 | P0 | 1 | Implement shared Room database declaration |
| DB-003 | P0 | 1 | Implement Android database builder |
| DB-004 | P0 | 1 | Implement iOS database builder |
| DB-005 | P0 | 1 | Implement draft entity, DAO, repository, and tests |
| DB-006 | P0 | 1 | Add migration test harness for Android and iOS |
| NET-001 | P0 | 2 | Configure Ktor Android engine |
| NET-002 | P0 | 2 | Configure Ktor Darwin engine |
| NET-003 | P0 | 2 | Implement request execution result model |
| NET-004 | P0 | 2 | Implement send and cancel |
| NET-005 | P0 | 2 | Map transport errors to domain errors |
| HIS-001 | P0 | 2 | Create pending and completed history records |
| REQ-001 | P0 | 3 | Build method and URL composer |
| REQ-002 | P0 | 3 | Build query parameter editor |
| REQ-003 | P0 | 3 | Build header editor |
| REQ-004 | P0 | 3 | Build auth editor |
| REQ-005 | P0 | 3 | Build raw and JSON body editor |
| REQ-006 | P0 | 3 | Build form URL encoded editor |
| REQ-007 | P0 | 3 | Build multipart editor and file picker integration |
| REQ-008 | P0 | 3 | Implement request validation |
| REQ-009 | P0 | 3 | Implement debounced Room autosave and flush rules |
| RESP-001 | P0 | 4 | Build response summary |
| RESP-002 | P0 | 4 | Build response header viewer |
| RESP-003 | P0 | 4 | Build raw response viewer |
| RESP-004 | P0 | 4 | Build pretty JSON formatter |
| RESP-005 | P0 | 4 | Build lazy JSON tree and copy path |
| RESP-006 | P0 | 4 | Implement response search and wrapping |
| RESP-007 | P0 | 4 | Stream large responses to app-managed files |
| RESP-008 | P0 | 4 | Add share and save response integration |
| COL-001 | P0 | 5 | Implement collection CRUD |
| COL-002 | P0 | 5 | Implement nested folder CRUD |
| COL-003 | P0 | 5 | Implement save, move, duplicate, reorder, and delete request |
| COL-004 | P0 | 5 | Implement favorites and recents |
| COL-005 | P0 | 5 | Implement Room-backed collection search |
| HIS-002 | P0 | 5 | Implement history list, filters, notes, and pins |
| HIS-003 | P0 | 5 | Implement rerun from immutable snapshot |
| HIS-004 | P0 | 5 | Implement retention and artifact cleanup |
| ENV-001 | P0 | 6 | Implement environment CRUD and active selection |
| ENV-002 | P0 | 6 | Implement variable resolver |
| ENV-003 | P0 | 6 | Implement Android Keystore secret storage |
| ENV-004 | P0 | 6 | Implement iOS Keychain secret storage |
| ENV-005 | P0 | 6 | Implement protected environment confirmation |
| ENV-006 | P0 | 6 | Implement secret reveal, masking, and redaction |
| CURL-001 | P0 | 7 | Implement cURL tokenizer and parser |
| CURL-002 | P0 | 7 | Build cURL import preview and warnings |
| CURL-003 | P0 | 7 | Generate and share redacted cURL |
| IO-001 | P0 | 7 | Define RequestLab export format |
| IO-002 | P0 | 7 | Implement export through platform file APIs |
| IO-003 | P0 | 7 | Implement import validation and preview |
| IO-004 | P0 | 7 | Implement conflict policies and transactional Room import |
| UI-001 | P0 | 8 | Implement shared light and dark themes |
| UI-002 | P0 | 8 | Adapt navigation and sheets for Android and iOS |
| UI-003 | P0 | 8 | Complete empty, loading, error, and offline states |
| A11Y-001 | P0 | 8 | Complete TalkBack and VoiceOver audit |
| A11Y-002 | P0 | 8 | Complete dynamic type and touch-target audit |
| REL-001 | P0 | 9 | Run Room migration matrix |
| REL-002 | P0 | 9 | Run security and secret-leak review |
| REL-003 | P0 | 9 | Run large-response and import stress tests |
| REL-004 | P0 | 9 | Prepare privacy notice and store disclosures |
| REL-005 | P0 | 9 | Produce signed Android and iOS release candidates |

---

## 25. Definition of done

### 25.1 Feature definition of done

A feature is done when:

- Acceptance criteria are implemented.
- Durable structured state is stored through Room.
- Raw secrets are not stored in Room.
- Loading, empty, error, and retry states are implemented.
- Android and iOS behavior is verified.
- Unit tests cover domain logic.
- DAO or migration tests cover persistence changes.
- Accessibility labels and focus order are present.
- Analytics and logging, if any, are payload-free.
- Documentation and screenshots are updated.
- No new desktop target or desktop-only dependency is introduced.

### 25.2 Milestone definition of done

- All milestone P0 issues are closed.
- CI is green.
- Room schema is committed.
- No destructive migration is used.
- Manual smoke flow passes on Android and iOS.
- Known limitations are documented.

### 25.3 Release definition of done

- Clean install passes.
- Upgrade from previous beta passes.
- Room migration matrix passes.
- Export, wipe, and import round trip passes.
- Secret leak tests pass.
- Accessibility smoke tests pass.
- Large-response tests pass.
- Store disclosures are complete.
- Crash-free beta quality meets the team's threshold.
- No open P0 issue.
- P1 issues are explicitly accepted or deferred.

---

## 26. Risk register

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Room KMP API or tooling changes | Medium | High | Pin compatible versions, maintain a compatibility matrix, upgrade in isolated PRs |
| KSP build instability across iOS targets | Medium | High | Keep CI coverage, use supported Kotlin versions, cache carefully, document clean-build recovery |
| iOS and Android network-engine behavior differs | Medium | High | Capability abstraction, integration tests on both platforms, transparent UI warnings |
| Mobile editor becomes too dense | High | High | Progressive sections, sticky send action, usability testing, avoid desktop tables |
| Large JSON causes memory pressure | Medium | High | Streaming, bounded preview, lazy tree, background formatting |
| Secrets leak through snapshots or logs | Low to Medium | Critical | Central redaction, secure storage, automated scans, security test fixtures |
| Production request sent accidentally | Medium | High | Visible environment badge, protected mode, resolved host confirmation |
| Draft autosave causes typing jank | Medium | Medium | Debounce, background DAO writes, small entities, profiling |
| File picker references expire | Medium | Medium | Copy owned files when required, validate before send, show repair flow |
| Room and filesystem become inconsistent | Medium | High | Artifact state machine, staged writes, orphan reconciliation |
| Import file is malicious or huge | Medium | High | Size limits, schema validation, no code execution, transactional application |
| Shared UI feels non-native on iOS | Medium | Medium | Platform-adaptive navigation and sheets, iOS QA, selective native integration |
| Scope expands toward full Postman parity | High | High | Keep release boundaries, require product decision for new protocols or collaboration |
| Store review questions developer tooling behavior | Low | Medium | Clear privacy policy, no unsafe TLS bypass, transparent file and network use |

---

## 27. Architecture decision records

### ADR-001 - Android and iOS only

Decision: ship phone applications for Android and iOS.  
Reason: focus design, QA, and engineering effort on the requested mobile use case.  
Consequence: no desktop target, desktop UI, or desktop packaging.

### ADR-002 - Shared Compose Multiplatform UI

Decision: share most UI in Compose Multiplatform.  
Reason: consistent product behavior and lower duplicate implementation cost.  
Consequence: platform adaptation must be deliberately designed rather than assuming one visual pattern fits both platforms.

### ADR-003 - Room is mandatory persistence

Decision: use AndroidX Room for Kotlin Multiplatform as the only structured database layer.  
Reason: one shared entity, DAO, migration, and repository model for Android and iOS.  
Consequence: SQLDelight and parallel databases are prohibited.

### ADR-004 - Room is the local source of truth

Decision: persistent screens observe Room-backed repositories.  
Reason: consistency, recovery, and predictable state.  
Consequence: UI caches are temporary and reconcile with Room.

### ADR-005 - Secrets live outside Room

Decision: raw secrets use Android Keystore-backed storage and iOS Keychain.  
Reason: database portability and inspection should not expose credentials.  
Consequence: Room stores opaque references and metadata, requiring cross-resource cleanup logic.

### ADR-006 - Ktor with Android and Darwin engines

Decision: share request orchestration but use platform engines.  
Reason: native networking behavior and KMP compatibility.  
Consequence: capabilities and error details may differ and must be mapped transparently.

### ADR-007 - Immutable history snapshots

Decision: each run creates an immutable snapshot.  
Reason: accurate reproduction and debugging.  
Consequence: rerun creates a new editable draft and new history record.

### ADR-008 - Bounded response memory

Decision: stream responses and cap in-memory preview.  
Reason: mobile memory constraints.  
Consequence: full bodies may use app-managed files tracked by Room.

### ADR-009 - cURL paste is P0

Decision: import cURL in the MVP.  
Reason: it is a high-value mobile entry path from documentation and messages.  
Consequence: parser security and shell non-execution are launch requirements.

### ADR-010 - Protected environments

Decision: environments can require confirmation before send.  
Reason: reduce accidental production calls.  
Consequence: environment state is always visible in request workflows.

### ADR-011 - No insecure TLS switch in MVP

Decision: do not allow certificate validation bypass.  
Reason: avoid normalizing unsafe behavior and reduce store and security risk.  
Consequence: local self-signed workflows require later, carefully designed support.

### ADR-012 - Versioned RequestLab export format

Decision: use a documented native format first.  
Reason: reliable round trips and controlled evolution.  
Consequence: broad third-party import compatibility is phased later.

---

## 28. Post-MVP roadmap

### Release 1.x - Workflow depth

- Cookies.
- Response diff.
- Image preview.
- XML tree.
- Request templates.
- Global and temporary variable scopes.
- HAR export.
- Better history analytics stored locally.

### Release 2.x - Ecosystem compatibility

- Postman import subset.
- OpenAPI import.
- Code snippets.
- Deep link and QR request sharing.
- Optional encrypted secret export.

### Release 3.x - Protocols and identity

- OAuth 2.0 with PKCE.
- OpenID Connect helper.
- GraphQL.
- WebSocket.
- Server-Sent Events.
- Client certificates after security design review.

### Release 4.x - Optional sync and teams

- End-to-end encrypted sync.
- Shared collections.
- Team roles.
- Audit history.
- Conflict resolution.

Desktop remains a separate future product decision and is not implied by this roadmap.

---

## 29. Launch checklist

### Product

- [ ] First request can be sent without onboarding.
- [ ] cURL paste works for documented supported flags.
- [ ] Request editor supports all P0 body and auth types.
- [ ] Favorites, recents, drafts, collections, and history work.
- [ ] Protected environment confirmation works.

### Room and data

- [ ] All structured persistent data uses Room.
- [ ] No SQLDelight dependency exists.
- [ ] No feature uses direct SQLite APIs.
- [ ] Room schemas are committed.
- [ ] Migration tests pass on Android and iOS.
- [ ] No destructive production migration exists.
- [ ] Artifact orphan reconciliation passes.

### Security

- [ ] Raw secrets are absent from Room.
- [ ] Raw secrets are absent from logs and crash reports.
- [ ] Default export excludes secrets.
- [ ] cURL import never executes shell content.
- [ ] TLS verification remains enabled.
- [ ] Production environment warnings are accessible.

### Networking

- [ ] Android engine passes integration tests.
- [ ] Darwin engine passes integration tests.
- [ ] Timeout, cancellation, DNS, connection, and TLS errors are classified.
- [ ] Large responses are streamed.
- [ ] Binary responses are handled safely.

### Android

- [ ] Back navigation is correct.
- [ ] File picker and share intent work.
- [ ] Keystore behavior is verified.
- [ ] TalkBack flow passes.
- [ ] AAB signing and internal distribution pass.

### iOS

- [ ] Safe areas and swipe-back are correct.
- [ ] Document picker and share sheet work.
- [ ] Keychain behavior is verified.
- [ ] VoiceOver flow passes.
- [ ] Archive and TestFlight distribution pass.

### UX and accessibility

- [ ] Light and dark themes pass contrast review.
- [ ] Dynamic text does not hide critical actions.
- [ ] Touch targets meet platform expectations.
- [ ] Keyboard does not cover active fields or Send.
- [ ] Loading, empty, offline, and error states are complete.
- [ ] Status meaning is not communicated by color alone.

### Release operations

- [ ] Privacy policy and store disclosures are complete.
- [ ] Beta feedback is triaged.
- [ ] No P0 issue remains open.
- [ ] Release notes include data-format changes.
- [ ] Rollback and hotfix procedures are documented.

---

## 30. Immediate implementation sequence

Start with this order to reduce architecture risk:

1. Remove every desktop target and desktop-only dependency from the repository.
2. Establish Android and iOS CI builds.
3. Add Room KMP and create schema version 1.
4. Build the platform-specific Room database constructors.
5. Persist and observe one URL draft on both platforms.
6. Add Ktor engines and send one GET request.
7. Store a pending and completed history entry through Room.
8. Add cancellation and error mapping.
9. Build the mobile request composer one section at a time.
10. Add bounded response streaming and JSON viewing.
11. Add collections, favorites, recents, and history.
12. Add environments and secure storage.
13. Add cURL import and redacted export.
14. Add workspace import and export.
15. Complete accessibility, migration, security, and release hardening.

### First demo scenario

The first internal demo should prove the architecture rather than visual completeness:

1. Launch Android or iOS.
2. Enter `https://httpbin.org/get` or the team's controlled test endpoint.
3. Close and reopen the app to prove the URL draft was restored from Room.
4. Tap Send.
5. Display status and raw response.
6. Reopen the completed run from Room-backed History.
7. Repeat on the other platform using the same shared feature code.

### First Room schema slice

Implement only these entities first:

- `DraftEntity`.
- `HistoryEntryEntity`.
- `AppSettingEntity`.

Add the rest through explicit migrations after the request vertical slice is proven.

---

## 31. Official references

Use the latest stable, compatible versions at implementation time.

- Android Developers: Set up Room database for Kotlin Multiplatform.
- Android Developers: Save data in a local database using Room.
- Kotlin Multiplatform documentation: Compose Multiplatform and shared application structure.
- Ktor documentation: Client engines, requests, responses, and testing.
- Apple documentation: Keychain Services, LocalAuthentication, and document pickers.
- Android documentation: Keystore, BiometricPrompt, storage access, and sharing.
- Google Labs: Stitch and DESIGN.md for the separate UI/UX design brief.

---

## Final MVP boundary

The MVP is a **mobile-only Android and iOS REST API client** with a shared KMP codebase, Compose Multiplatform UI, Ktor networking, and **Room as the mandatory local source of truth**. It includes request composition, cURL paste, safe environments, secure secrets, response inspection, collections, drafts, favorites, history, and versioned local import/export. It does not include desktop, cloud sync, teams, scripts, GraphQL, WebSockets, insecure TLS, or a second database technology.
