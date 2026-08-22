# RequestLab Mobile - UI/UX Design Plan for Stitch

> Document version: **1.0**  
> Last updated: **2026-08-18**  
> Product: **Mobile REST API client for developers**  
> Platforms: **Android and iOS phones only**  
> Design tool target: **Google Stitch**  
> Companion design-system file: **DESIGN.md**  
> Desktop: **Do not design**

---

## Table of contents

1. [Design brief](#1-design-brief)
2. [Design objective](#2-design-objective)
3. [Users and mobile context](#3-users-and-mobile-context)
4. [Experience principles](#4-experience-principles)
5. [Brand and visual direction](#5-brand-and-visual-direction)
6. [Android and iOS adaptation strategy](#6-android-and-ios-adaptation-strategy)
7. [Information architecture](#7-information-architecture)
8. [Primary user flows](#8-primary-user-flows)
9. [Screen inventory](#9-screen-inventory)
10. [Detailed screen specifications](#10-detailed-screen-specifications)
11. [Responsive and keyboard behavior](#11-responsive-and-keyboard-behavior)
12. [Component system](#12-component-system)
13. [Content and data-display rules](#13-content-and-data-display-rules)
14. [Color, typography, spacing, and shape](#14-color-typography-spacing-and-shape)
15. [Dark theme](#15-dark-theme)
16. [Motion, feedback, and haptics](#16-motion-feedback-and-haptics)
17. [Accessibility requirements](#17-accessibility-requirements)
18. [Empty, loading, error, and edge states](#18-empty-loading-error-and-edge-states)
19. [Prototype requirements](#19-prototype-requirements)
20. [Stitch generation workflow](#20-stitch-generation-workflow)
21. [Master Stitch prompt](#21-master-stitch-prompt)
22. [Focused Stitch prompts](#22-focused-stitch-prompts)
23. [Stitch critique and iteration prompts](#23-stitch-critique-and-iteration-prompts)
24. [Design review checklist](#24-design-review-checklist)
25. [Developer handoff requirements](#25-developer-handoff-requirements)
26. [Final design acceptance criteria](#26-final-design-acceptance-criteria)

---

## 1. Design brief

Design a production-ready mobile application named **RequestLab**. It is a lightweight REST API client for developers, QA engineers, students, and technical support staff. Users can create HTTP requests, paste cURL commands, switch environments, send requests, inspect formatted responses, save requests into collections, and reopen immutable history entries.

The application is not a desktop client and should never look like a desktop table squeezed onto a phone. The design must be touch-first, keyboard-aware, interruption-safe, and readable on narrow screens.

Create two coordinated platform families:

- Android phone designs using current Material 3 behavior and Android navigation conventions.
- iOS phone designs using familiar iOS navigation, tab, sheet, swipe-back, and destructive-action conventions.

The visual identity must remain recognizably the same across both platforms.

### 1.1 Product personality

RequestLab should feel:

- Precise.
- Fast.
- Calm.
- Technical.
- Trustworthy.
- Capable without being intimidating.
- Dense enough for developers but never cramped.

### 1.2 What the design must avoid

- Do not copy Postman branding, orange palette, layout, or illustrations.
- Do not create a desktop-style three-column interface.
- Do not use tiny spreadsheet cells for headers and parameters.
- Do not hide the active environment.
- Do not communicate method, status, or danger by color alone.
- Do not make the Send action move unpredictably when the keyboard opens.
- Do not show raw secret values in examples.
- Do not overuse gradients, glass effects, neon, or decorative code imagery.
- Do not create a generic consumer app aesthetic that hides technical information.

---

## 2. Design objective

The design should allow a user to complete this core task with minimal friction:

1. Open the app.
2. Start a blank request or paste cURL.
3. Confirm the active environment.
4. Edit the request.
5. Send it.
6. Understand the response.
7. Save or rerun it later.

### 2.1 Primary design outcomes

- A first-time user can locate New Request and Paste cURL immediately.
- A returning user can rerun a favorite request in a few taps.
- The active environment and resolved host are always clear before sending.
- Request editing works comfortably with the software keyboard visible.
- The response viewer prioritizes status and readable JSON.
- Collections, drafts, favorites, recents, and history are visually distinct.
- Android feels like Android and iOS feels like iOS without becoming two unrelated products.

### 2.2 Design success questions

A design direction is successful only when the answer is yes to all of these:

- Can the user send a basic GET request without reading instructions?
- Can the user tell whether they are targeting production?
- Can the user recover an unfinished draft?
- Can the user understand a 401, 404, 500, timeout, and TLS failure?
- Can the user search a JSON response on a phone?
- Can the user reach Send while the keyboard is open?
- Can a screen reader identify methods, status, environment, and icon actions?
- Can developers implement the design with shared Compose Multiplatform components?

---

## 3. Users and mobile context

### 3.1 Primary personas

#### Mobile developer

Needs to test staging endpoints while working on a device, reading documentation, or receiving cURL examples in chat.

Needs:

- Fast cURL paste.
- Environment switching.
- Bearer token support.
- JSON response inspection.
- Saved favorites.

#### QA engineer

Needs to reproduce an API defect and preserve the exact request that was sent.

Needs:

- Immutable history.
- Notes.
- Request rerun.
- Status and timing visibility.
- Shareable redacted request.

#### Student

Needs to understand methods, headers, bodies, and responses without a large professional tool.

Needs:

- Clear labels.
- Helpful empty states.
- Non-technical error explanations with optional detail.
- JSON formatting.

#### Support or operations engineer

Needs to run known health checks or inspect customer endpoints quickly.

Needs:

- Favorites.
- Production guardrails.
- Reliable local collections.
- Searchable history.

### 3.2 Mobile usage conditions

Design for:

- One-handed use.
- Short sessions.
- Interruptions and app switching.
- Pasting from chat or documentation.
- Weak or changing networks.
- Software keyboard covering half the screen.
- Portrait-first use.
- Occasional landscape use for wide JSON or long URLs.
- Sensitive tokens visible in public or shared spaces.

---

## 4. Experience principles

### 4.1 Fast path before organization

A user should not need to create a workspace, collection, or environment before sending a request.

### 4.2 Progressive disclosure

Show the minimum required request controls first:

- Method.
- URL.
- Send.

Place advanced controls behind clear sections:

- Params.
- Headers.
- Auth.
- Body.

### 4.3 Environment confidence

The active environment must be visible on Home and every request screen. A protected environment needs label, icon, and confirmation behavior rather than color alone.

### 4.4 Technical clarity

Use exact HTTP vocabulary where users expect it, but pair it with short helper text when an error or configuration could be confusing.

### 4.5 Draft safety

The interface should reassure the user that edits are saved. Use a subtle `Saving`, `Saved`, or `Save failed` state without interrupting typing.

### 4.6 Response first after send

Once a request completes, prioritize response status and body while preserving a clear way back to the request editor.

### 4.7 Stable actions

Send, Cancel, Save, Search, and environment controls should remain in predictable locations.

### 4.8 Accessible density

Developers can handle information density, but controls still need readable text, clear hierarchy, and platform-appropriate touch targets.

---

## 5. Brand and visual direction

### 5.1 Working brand

Name: **RequestLab**

Suggested tagline for store or onboarding use:

> Build. Send. Inspect.

Do not force the tagline into every screen.

### 5.2 Visual metaphor

The product is a precise mobile workbench, not a science-fiction command center.

Use:

- Clean surfaces.
- Strong information hierarchy.
- Subtle grouping.
- Clear code typography.
- Controlled semantic color.
- Small purposeful motion.

Avoid:

- Heavy chrome.
- Excessive cards.
- Fake terminal decorations.
- Large decorative illustrations inside core work screens.
- Bright method colors across large surfaces.

### 5.3 Logo direction

Create a simple abstract mark that can work as an app icon:

- A request arrow entering a bracket or frame.
- A paired send-and-response path.
- A minimal `R` built from two directional strokes.
- A rounded-square icon with strong silhouette.

Avoid using a paper airplane alone because it reads as messaging. Avoid copying common API-client logos.

### 5.4 Tone of voice

- Direct.
- Helpful.
- Brief.
- Never playful when a production or secret warning is shown.
- Avoid blame.

Examples:

- Good: `Could not reach the host. Check the address or network and try again.`
- Avoid: `Oops! Something went wrong.`
- Good: `This request targets Production: api.example.com.`
- Avoid: `Are you sure?`

---

## 6. Android and iOS adaptation strategy

### 6.1 Shared product identity

Share across platforms:

- Color system.
- Typography hierarchy.
- Code presentation.
- Method and status semantics.
- Environment badges.
- Screen names.
- Information architecture.
- Core component proportions where practical.

### 6.2 Android behavior

Use:

- Material 3 top app bars.
- Material navigation bar.
- Modal bottom sheets for selection and quick editing.
- Android system back behavior.
- Material dialogs for destructive or protected-environment confirmation.
- Android-style snackbars.
- Edge-to-edge layout with correct system-bar contrast.
- Material switches, checkboxes, and text fields.

Avoid:

- iOS-style centered navigation titles everywhere.
- iOS chevron rows without Android context.
- Floating action buttons unless they improve the specific screen; the default RequestLab design does not require a global FAB.

### 6.3 iOS behavior

Use:

- Native-feeling tab bar.
- Navigation stack and interactive swipe-back.
- Large title only where it improves top-level scanning.
- iOS sheets and confirmation dialogs.
- Trailing navigation actions for Save or Done where appropriate.
- Familiar grouped settings rows.
- iOS switches and destructive actions.
- Safe area and home-indicator spacing.

Avoid:

- Material FABs.
- Android-style persistent snackbar placement that conflicts with the tab bar.
- Overly elevated cards.

### 6.4 Shared Compose implementation awareness

Design components so they can be implemented in shared Compose code with platform variants rather than completely separate screen trees.

Recommended approach:

- Shared layout and content model.
- Platform-specific navigation chrome.
- Platform-specific dialog and sheet treatment.
- Platform-aware spacing around system elements.
- Shared custom technical components such as method selector, environment badge, key-value editor, JSON viewer, and response summary.

---

## 7. Information architecture

### 7.1 Top-level destinations

1. **Home**
2. **Collections**
3. **History**
4. **More**

### 7.2 More destination

Contains:

- Environments.
- Import and Export.
- Settings.
- Diagnostics.
- About.

### 7.3 Request workspace hierarchy

```text
Home / Collections / History
    -> Request Workspace
        -> Request editor
        -> Response viewer
        -> Save request
        -> Resolved preview
        -> Run details
```

### 7.4 Environment access

The active environment is available from:

- Home top bar.
- Request workspace top bar.
- Resolved preview.
- More > Environments.

### 7.5 Search

Search entry points:

- Collections search.
- History search and filters.
- Response body search.

Do not place one global search field that ambiguously searches everything in the MVP.

---

## 8. Primary user flows

## 8.1 Flow A - First successful GET

Frames:

1. Home empty.
2. Blank request workspace.
3. Keyboard-visible URL entry.
4. Sending state.
5. Successful JSON response.
6. Save-to-collection sheet.

Prototype actions:

- New Request -> Request Workspace.
- URL tap -> keyboard state.
- Send -> Sending.
- Completion -> Response.
- Save -> Save sheet.

## 8.2 Flow B - Paste cURL

Frames:

1. Home populated.
2. cURL import preview.
3. Import warning state if secrets are detected.
4. Editable request.
5. Response.

Prototype actions:

- Paste cURL -> Preview.
- Import -> Request.
- Warning row -> explanation.
- Send -> Response.

## 8.3 Flow C - Protected production request

Frames:

1. Request using staging.
2. Environment picker.
3. Request using production.
4. Production confirmation.
5. Sending.
6. Response.

The confirmation must display:

- `Production` label.
- Method.
- Resolved host.
- Clear Send Anyway action.
- Cancel action.

## 8.4 Flow D - Rerun history

Frames:

1. History list.
2. History detail.
3. New draft created from snapshot.
4. New response.

The old history entry stays unchanged.

## 8.5 Flow E - Create environment with secret

Frames:

1. Environment list.
2. New environment editor.
3. Add variable row.
4. Mark secret.
5. Save.
6. Environment appears active or selectable.

## 8.6 Flow F - Export and restore

Frames:

1. Import and Export.
2. Export options.
3. Secret exclusion summary.
4. System share or file flow placeholder.
5. Import preview.
6. Conflict resolution.
7. Completion summary.

---

## 9. Screen inventory

Generate matched Android and iOS variants for every P0 screen.

| ID | Screen | Priority | Required states |
|---|---|---:|---|
| S01 | Launch / startup | P0 | normal, migration, recovery failure |
| S02 | Home empty | P0 | first use |
| S03 | Home populated | P0 | favorites, recents, drafts |
| S04 | New request workspace | P0 | blank, edited, saved |
| S05 | Request Params editor | P0 | empty, rows, validation error |
| S06 | Request Headers editor | P0 | rows, duplicate, secret row |
| S07 | Request Auth editor | P0 | none, bearer, basic, API key |
| S08 | Request Body editor | P0 | none, JSON, raw, form, multipart |
| S09 | Resolved request preview | P0 | valid, unresolved variable, redacted secret |
| S10 | Sending state | P0 | sending, canceling |
| S11 | Response JSON | P0 | raw, pretty, tree, search |
| S12 | Response non-JSON | P0 | text, binary summary, truncated |
| S13 | Request failure | P0 | timeout, DNS, TLS, canceled |
| S14 | cURL import preview | P0 | valid, warnings, unsupported flags |
| S15 | Collections list | P0 | empty, populated, search |
| S16 | Collection detail | P0 | folders, requests, reorder mode |
| S17 | Save request sheet | P0 | new collection, select folder, validation |
| S18 | History list | P0 | populated, filters, empty search |
| S19 | History detail | P0 | success, failure, note, pinned |
| S20 | Environment picker | P0 | active, protected, none |
| S21 | Environment list | P0 | empty, populated |
| S22 | Environment editor | P0 | public variable, secret variable, errors |
| S23 | Production confirmation | P0 | protected send |
| S24 | More | P0 | normal |
| S25 | Import and Export | P0 | export, import, completion |
| S26 | Settings | P0 | light, dark, system; limits; clear data |
| S27 | Diagnostics | P0 | summary, generate report |
| S28 | Delete / clear confirmation | P0 | destructive warning |
| S29 | Secret reveal authentication | P0 | biometric success, failure, fallback |
| S30 | Offline / interrupted run recovery | P0 | draft recovered, run interrupted |
| S31 | Local network and insecure HTTP permission flow | P0 | rationale, denied, settings recovery, warning |

### 9.1 Design batch order

Generate in this order:

1. S02, S03, S04, S10, S11.
2. S05, S06, S07, S08, S09.
3. S14, S15, S16, S17.
4. S18, S19, S20, S21, S22, S23.
5. S24, S25, S26, S27, S28, S29, S30, S31.
6. Dark theme variants.
7. Accessibility and large-text variants.

---

## 10. Detailed screen specifications

## 10.1 S01 - Launch and startup

Purpose:

- Give immediate branded feedback while Room opens, migrations run, and recovery checks complete.

Layout:

- Centered RequestLab mark.
- Product name.
- Minimal progress indicator only if startup takes long enough to require feedback.

States:

- Normal launch.
- `Updating local workspace` during migration.
- Recovery problem with Retry and safe diagnostic details.

Rules:

- Do not create a long animated splash.
- Do not show fake progress percentages.
- Avoid exposing database terminology to ordinary users unless an error requires it.

## 10.2 S02 - Home empty

Purpose:

- Make the first request obvious.

Hierarchy:

1. Top bar with RequestLab and active environment control showing `No environment` or `Default`.
2. Primary New Request button.
3. Secondary Paste cURL button.
4. Short explanation: `Create a request from scratch or paste a cURL command.`
5. Optional compact examples, not a long onboarding carousel.
6. Bottom navigation.

Android:

- Material top app bar.
- Filled primary button and outlined secondary button.

IOS:

- Navigation title.
- Prominent primary row or button.
- Secondary action with familiar iOS treatment.

## 10.3 S03 - Home populated

Purpose:

- Help returning users resume quickly.

Sections:

- Quick actions.
- Favorites horizontal or vertical list, whichever remains readable.
- Recent requests.
- Recoverable drafts.

Request list row:

- Method badge with text.
- Request name.
- Host or URL template summary.
- Last run result and relative time.
- Environment label when relevant.
- Overflow action.

Rules:

- Limit Home lists and provide View All.
- Do not create a feed with excessive cards.
- Drafts use a clear `Draft` label and saved timestamp.

## 10.4 S04 - New request workspace

Purpose:

- Compose and send a request on a phone.

Top area:

- Back.
- Request name or `Untitled Request`.
- Autosave state.
- Environment badge.
- Overflow.

Composer:

- Method selector.
- Flexible URL field.
- Send button or icon with text where space permits.

Section navigation:

- Params.
- Headers.
- Auth.
- Body.

Use a horizontally scrollable tab row only if all labels remain obvious. Prefer a segmented or compact tab treatment.

Bottom or secondary actions:

- Preview resolved request.
- Save.

Keyboard behavior:

- Keep Send reachable.
- URL field can grow to multiple lines only within a controlled maximum.
- Provide keyboard Next and Done behavior.

After response:

- Show a Request / Response switch or a collapsible response summary.
- Default to Response after completion.

## 10.5 S05 - Params editor

Purpose:

- Edit ordered query parameters without a desktop grid.

Each row:

- Enabled toggle or checkbox.
- Key field.
- Value field.
- Drag handle or reorder action.
- Row menu.

Mobile row behavior:

- Stack key and value vertically on compact widths.
- Use a focused row editor or bottom sheet for description and secret status if needed.
- Add Parameter action remains reachable.

States:

- Empty instructional state.
- Multiple rows.
- Duplicate keys.
- Blank enabled key warning.
- Variable chip or syntax highlighting inside values if practical.

## 10.6 S06 - Headers editor

Same base pattern as Params with additional behavior:

- Common-header suggestions.
- Secret toggle in row details.
- Duplicate header support.
- Generated authentication header indicator.
- Restricted or engine-managed header explanation.

Secret row presentation:

- Masked value.
- Secret icon and text label.
- Reveal requires explicit action and optional biometric confirmation.

## 10.7 S07 - Auth editor

Auth type selector:

- None.
- Basic.
- Bearer.
- API Key.

Bearer layout:

- Token field.
- Secret toggle or default secret behavior.
- Variable insertion action.

Basic layout:

- Username.
- Password.
- Secret handling explanation.

API key layout:

- Key name.
- Value.
- Add to Header or Query selector.

Rules:

- Preserve user input when switching types and returning.
- Clearly explain that generated auth data appears redacted in preview.

## 10.8 S08 - Body editor

Body type selector:

- None.
- JSON.
- Raw.
- Form.
- Multipart.

JSON:

- Monospace editor.
- Format action.
- Validation status.
- Content type indicator.
- Full-screen editor option.

Raw:

- Content type selector.
- Monospace editor.

Form:

- Key-value rows.

Multipart:

- Text or File part selector.
- File name, size, and missing-file status.
- Content type.

Rules:

- Do not make a complex code editor attempt to mimic desktop IDE chrome.
- Use a full-screen editor for long bodies.
- Keep format and validation actions visible but secondary to Send.

## 10.9 S09 - Resolved request preview

Purpose:

- Let the user verify what will be sent without exposing secrets.

Show:

- Method.
- Resolved host and path.
- Query summary.
- Header summary.
- Body type and size estimate.
- Active environment.
- Warnings.
- Secret values as fixed redaction tokens.

States:

- Ready.
- Missing variable.
- Cycle detected.
- Missing file.
- Protected environment.

Use a bottom sheet on Android and a sheet on iOS unless the content needs a full screen.

## 10.10 S10 - Sending state

Purpose:

- Show that the request is active and can be canceled.

Show:

- Method and host.
- Elapsed time.
- Indeterminate progress.
- Cancel action.
- Environment label.

Rules:

- Do not block access to the request content.
- Prevent duplicate send taps.
- Change Send to Cancel in place when possible.
- Use subtle haptic feedback on send and cancellation.

## 10.11 S11 - Response JSON

Purpose:

- Make JSON response inspection comfortable on a phone.

Top summary:

- Status code and text.
- Duration.
- Size.
- Content type.

Response modes:

- Pretty.
- Tree.
- Raw.

Actions:

- Search.
- Wrap.
- Copy.
- Share or Save.
- More.

Tree behavior:

- Expand and collapse nodes.
- Show array indexes.
- Copy value.
- Copy JSON path.
- Use indentation and type-aware text styling without relying only on color.

Search behavior:

- Search field appears without losing scroll position.
- Match count.
- Previous and next.
- Close search.

Rules:

- Status summary remains visible or quickly recoverable.
- Use monospace for response body.
- Do not use microscopic font to fit more columns.

## 10.12 S12 - Response non-JSON

Text response:

- Raw view.
- Search.
- Wrap.
- Copy.

Binary response:

- File-type icon.
- Content type.
- Size.
- Suggested filename.
- Save and Share.
- No unsafe text preview.

Truncated response:

- Clear banner: `Showing the first 2 MB.`
- Action to save full body when available.
- Action to delete stored full body.

## 10.13 S13 - Request failure

Use a consistent error layout with:

- Error icon.
- Specific title.
- Plain explanation.
- Safe technical detail expandable section.
- Retry.
- Edit Request.

Variants:

- Timeout.
- DNS.
- Connection refused.
- TLS certificate failure.
- Canceled.
- Missing file.
- Persistence warning after successful network response.

Example:

Title: `Could not verify the server certificate`  
Body: `The secure connection could not be established. Check the host and certificate configuration.`

Do not offer an insecure bypass in MVP designs.

## 10.14 S14 - cURL import preview

Purpose:

- Turn pasted cURL into a safe editable request.

Show:

- Parsed method and URL.
- Header count.
- Body type.
- Detected authentication.
- Warnings.
- Suspected secrets with `Store securely` option.
- Unsupported flags.
- Original command in a collapsed section.

Actions:

- Import Request.
- Cancel.
- Edit Source if parsing failed.

Rules:

- State clearly that shell commands are not executed.
- Local file paths require the user to select a file.

## 10.15 S15 - Collections list

Purpose:

- Browse and create collections.

Show:

- Search.
- Collection rows with name, description, request count, and updated time.
- Create Collection.
- Empty state.

Avoid large decorative collection cards. Favor efficient, clear rows.

## 10.16 S16 - Collection detail

Show:

- Collection title and description.
- Breadcrumb or current folder path.
- Folders first, then requests.
- Search within collection.
- Add Folder and New Request.
- Reorder mode.

Request row:

- Method.
- Name.
- URL or host summary.
- Favorite.
- Last run result.

Folder row:

- Folder icon.
- Name.
- Item count.
- Chevron or navigation affordance.

## 10.17 S17 - Save request sheet

Fields:

- Request name.
- Collection selector.
- Folder selector.
- Optional description.

Actions:

- Save.
- New Collection.
- New Folder.
- Cancel.

Validation:

- Name required.
- Show selected location path.
- Preserve entered name if the user creates a folder during the flow.

## 10.18 S18 - History list

Purpose:

- Find and reopen previous runs.

Top controls:

- Search.
- Filters.
- Clear unpinned history.

Row:

- Method.
- Host or safe URL summary.
- Status or error.
- Duration.
- Time.
- Environment.
- Pin indicator.

Filter sheet:

- Date.
- Method.
- Success or failure.
- Status family.
- Environment.
- Pinned only.

Use paging or progressive loading behavior in the prototype if the list is long.

## 10.19 S19 - History detail

Show:

- Immutable label.
- Method and URL template snapshot.
- Safe resolved host.
- Environment snapshot.
- Status or error.
- Duration and size.
- Request headers and body summary with redaction.
- Response headers.
- Stored preview.
- User note.

Actions:

- Rerun as New Draft.
- Pin or Unpin.
- Add Note.
- Share Redacted Request.
- Delete Entry.

Do not present editable request fields inside history detail.

## 10.20 S20 - Environment picker

Show:

- Current environment.
- Environment list.
- Type label.
- Protected indicator.
- Quick link to Manage Environments.
- `No Environment` option if supported.

Selection updates the request preview immediately after dismissal.

## 10.21 S21 - Environment list

Show:

- Active environment.
- Environment type.
- Variable count.
- Protected status.
- Create Environment.
- Reorder if needed.

Swipe actions may be used on iOS if discoverable alternatives also exist. Android uses menus or explicit actions.

## 10.22 S22 - Environment editor

Fields:

- Name.
- Type.
- Protected toggle.
- Confirm before send toggle.
- Variables list.

Variable row:

- Enabled.
- Key.
- Value.
- Secret status.
- Reorder.
- More actions.

Secret value:

- Masked.
- Stored securely helper text.
- Reveal or replace action.

Validation:

- Duplicate key.
- Empty key.
- Variable cycle.
- Missing secure value.

## 10.23 S23 - Production confirmation

High-priority safety screen.

Title:

- `Send request to Production?`

Content:

- Production badge and warning icon.
- Method.
- Resolved host.
- Optional environment description.
- Reminder that this action may affect live data.

Actions:

- Send Anyway - visually strong but not styled as success.
- Cancel - easy to reach and default-safe.

Rules:

- Do not reveal query secrets.
- Do not rely on red alone.
- Avoid generic `OK` and `Cancel` labels when a more specific action is possible.

## 10.24 S24 - More

Rows:

- Environments.
- Import and Export.
- Settings.
- Diagnostics.
- About.

Use platform-native list treatment.

## 10.25 S25 - Import and Export

Sections:

- Export Workspace.
- Export Selected Collections.
- Import Workspace.
- Recent import or export result, if useful.

Export options:

- Include environments without secrets.
- Include history metadata.
- Exclude full response bodies by default.

Import preview:

- Collections count.
- Requests count.
- Environments count.
- Missing secrets count.
- Conflicts.
- Invalid items.

Completion summary:

- Imported.
- Renamed.
- Skipped.
- Failed.

## 10.26 S26 - Settings

Groups:

- Appearance.
- Request defaults.
- Response storage.
- History and drafts.
- Security.
- Data management.

Settings:

- System, Light, Dark.
- Default timeout.
- Follow redirects.
- Response preview limit.
- Full body storage.
- History retention.
- Biometric reveal.
- Clear response cache.
- Clear history.
- Clear all local data.

Use native-feeling settings patterns on each platform.

## 10.27 S27 - Diagnostics

Show safe data only:

- App version.
- Database schema version.
- Collection, request, draft, history, and artifact counts.
- Local storage usage.
- Network engine.
- Recent safe error codes.

Action:

- Generate Redacted Diagnostic Report.

Use explicit text stating what is not included: URLs, headers, bodies, and secret values.

## 10.28 S28 - Destructive confirmation

Use for:

- Delete collection.
- Delete request.
- Clear history.
- Clear all local data.

Content must explain scope and recovery implications.

For Clear All Data:

- State that collections, drafts, environments, history, stored response files, and RequestLab-owned secrets will be removed.
- Require a deliberate destructive action.

## 10.29 S29 - Secret reveal authentication

Show platform authentication flow or a design placeholder for it.

States:

- Authenticate to reveal.
- Success and temporary reveal.
- Failure.
- Cancel.
- Device authentication unavailable.

The app-level screen behind the system prompt should explain what will be revealed and for how long.

## 10.30 S30 - Recovery state

Draft recovered:

- Subtle banner: `Draft restored from your last session.`

Interrupted run:

- History row and detail state: `Interrupted when the app stopped.`
- Actions: Rerun, Edit Request.

Database or artifact cleanup warning:

- Non-blocking, safe language.
- Retry or Diagnostics action.

## 10.31 S31 - Local network and insecure HTTP flow

Purpose:

- Explain why RequestLab needs direct local-network access and why cleartext HTTP is risky without requesting permission out of context.

IOS pre-permission screen or sheet:

- Title: `Allow local API access?`
- Explanation: `RequestLab needs Local Network access when you send requests to devices and development servers on your Wi-Fi network.`
- Continue action that proceeds to the system prompt.
- Not Now action.
- After denial, show `Open Settings` and `Edit Request`.

Android permission rationale:

- Show only when required by the running Android version and target SDK.
- Explain that the permission is used for direct requests to local IP addresses and `.local` hosts.
- Continue to the system permission prompt.
- Provide a Settings recovery path after denial.

Insecure HTTP warning:

- Title: `This request is not encrypted`.
- Show method and safe host only.
- Explain that traffic may be observed or modified.
- Actions: Send Once, Cancel, and an optional remembered choice only if product policy approves it.
- Never show secret headers, tokens, or query values.

Rules:

- Do not request local-network access during onboarding.
- Trigger the rationale only after the user attempts a local endpoint.
- Do not imply that local-network permission makes cleartext HTTP secure.
- Keep permission denial distinct from DNS, timeout, and connection-refused errors.

---

## 11. Responsive and keyboard behavior

### 11.1 Reference frames

Use representative phone frames rather than designing only one device:

- Android compact reference: approximately 412 x 915 dp.
- iOS compact reference: approximately 393 x 852 pt.
- Verify a smaller supported phone width.
- Verify a large phone width.

### 11.2 Portrait

Portrait is the primary orientation.

- Composer remains compact.
- Section tabs stay readable.
- Request fields stack vertically.
- JSON uses horizontal scrolling only when wrap is off.

### 11.3 Landscape

- Keep method, URL, and Send on one row where possible.
- Allow more response width.
- Respect notches and system insets.
- Do not force a desktop split view.

### 11.4 Software keyboard

- Active field scrolls into view.
- Send or Cancel remains accessible.
- Bottom navigation may hide while editing if platform convention and implementation support it.
- Keyboard action moves logically between key and value fields.
- Full-screen body editor has a clear Done action.
- Avoid bottom sheets whose critical actions sit behind the keyboard.

### 11.5 Large text

- Support dynamic text without clipping.
- Allow top-bar titles to truncate safely.
- Method badge and status remain legible.
- Request rows may grow vertically.
- Critical actions remain visible.

---

## 12. Component system

Design all components with light, dark, enabled, disabled, pressed, focused, error, and loading states where relevant.

## 12.1 Navigation components

- Android top app bar.
- iOS navigation bar.
- Android navigation bar.
- iOS tab bar.
- Back action.
- Overflow menu.
- Section tabs.

## 12.2 Request components

- Method selector.
- Method badge.
- URL field.
- Send button.
- Cancel button.
- Environment badge.
- Autosave indicator.
- Key-value row.
- Enabled control.
- Secret indicator.
- Drag handle.
- Body type selector.
- Code editor toolbar.
- Validation banner.
- Resolved-preview row.

## 12.3 Response components

- Status badge.
- Metadata metric.
- Response mode selector.
- JSON tree row.
- Search bar and match counter.
- Truncation banner.
- Binary file summary.
- Copy-path menu.
- Error panel.

## 12.4 Organization components

- Collection row.
- Folder row.
- Saved request row.
- Draft row.
- History row.
- Favorite marker.
- Empty state.
- Filter chip.

## 12.5 Safety components

- Protected environment badge.
- Production confirmation panel.
- Local-network permission rationale.
- Insecure HTTP warning.
- Secret field.
- Biometric reveal row.
- Redaction token.
- Destructive confirmation.

## 12.6 Feedback components

- Snackbar or iOS equivalent.
- Inline save state.
- Inline validation.
- Progress indicator.
- Retry panel.
- Completion summary.

### 12.7 Component naming for handoff

Use consistent component names such as:

- `RLMethodBadge`
- `RLEnvironmentBadge`
- `RLRequestFieldRow`
- `RLSendButton`
- `RLStatusSummary`
- `RLJsonTreeRow`
- `RLHistoryRow`
- `RLSecretField`
- `RLValidationBanner`

The `RL` prefix is optional in code but useful in design handoff.

---

## 13. Content and data-display rules

### 13.1 HTTP methods

Always display text labels. Color is supplementary.

Suggested semantic treatment:

- GET: teal accent.
- POST: indigo accent.
- PUT: amber accent.
- PATCH: violet accent.
- DELETE: red accent.
- HEAD and OPTIONS: neutral blue-gray.
- Custom: neutral outline.

### 13.2 Status codes

- 2xx: success icon and label.
- 3xx: redirect icon and label.
- 4xx: client-error label.
- 5xx: server-error label.
- Transport failure: distinct from an HTTP status.

Never label all non-2xx responses simply `Failed`; preserve the actual status.

### 13.3 URLs

- Use monospace only where it improves scanning.
- Prefer host emphasis and path de-emphasis in list rows.
- Truncate the middle or path thoughtfully.
- Never expose secret query values in previews.

### 13.4 Timestamps

- Lists use relative time plus accessible full timestamp.
- Detail screens use absolute local timestamp.
- History ordering is newest first by default.

### 13.5 Durations and sizes

- Use consistent units.
- Avoid false precision.
- Examples: `284 ms`, `1.7 s`, `842 KB`, `2.3 MB`.

### 13.6 JSON

- Code uses a platform monospace font.
- Keys, strings, numbers, booleans, and null may have distinct styling, but type is not indicated by color alone.
- Tree rows show disclosure state and type icon or text when needed.
- Long strings can wrap or expand on demand.

### 13.7 Secrets

Use a stable redaction token such as:

- `••••••••`
- `Secret value`
- `{{api_token}}`

Do not use realistic token examples in final design files.

---

## 14. Color, typography, spacing, and shape

The companion `DESIGN.md` contains the normative tokens. This section explains the intent.

### 14.1 Color intent

- Indigo is the primary action and identity color.
- Teal is a supporting technical accent.
- Green is reserved for success.
- Amber is reserved for warnings and PUT-method accents.
- Red is reserved for destructive actions, production risk, DELETE, and serious errors.
- Neutral slate surfaces keep code and structured data readable.

### 14.2 Typography

Use platform system UI fonts for interface text:

- Android: Roboto or system default.
- iOS: San Francisco or system default.
- Shared implementation: system family.

Use platform monospace for:

- URLs when appropriate.
- Headers and values.
- Request bodies.
- Response bodies.
- JSON paths.

Hierarchy:

- Display: rare, top-level first-use moments.
- Title large: top-level screen titles.
- Title medium: section and detail titles.
- Body large: important content.
- Body medium: normal content.
- Label large: buttons and tabs.
- Label small: metadata.
- Code medium: editor and response.

### 14.3 Spacing

Use a 4-point base scale:

- 4: micro gap.
- 8: related inline items.
- 12: compact component padding.
- 16: standard screen padding.
- 24: section separation.
- 32: major separation.

### 14.4 Shapes

- Inputs and primary buttons: medium rounded corners.
- Method and environment badges: pill or compact rounded shape.
- Cards: use sparingly with moderate radius.
- Code surfaces: slightly smaller radius to feel precise.
- Do not make every container a floating rounded card.

### 14.5 Elevation

- Keep elevation subtle.
- Use separators and surface contrast before shadows.
- Android may use low Material elevation.
- iOS primarily uses grouped surfaces and separators.
- Sticky Send and floating search controls may use modest depth when required.

---

## 15. Dark theme

Dark theme is equal in importance to light theme because the target audience frequently uses developer tools in dark mode.

### 15.1 Requirements

- Dark backgrounds are deep blue-black rather than pure black everywhere.
- Code surfaces remain distinguishable from main surfaces.
- Primary actions stay high contrast.
- Method and status colors are adjusted for dark backgrounds.
- Borders and dividers remain visible without becoming bright.
- Red production warnings remain readable and not neon.
- Syntax colors meet contrast requirements for normal text where used as essential information.

### 15.2 Dark-theme review screens

At minimum review:

- Home populated.
- Request workspace with keyboard.
- JSON response tree.
- History list.
- Environment editor with secret field.
- Production confirmation.
- Local-network permission rationale and insecure HTTP warning.
- Settings.
- Error state.

---

## 16. Motion, feedback, and haptics

### 16.1 Motion principles

- Fast and functional.
- No decorative page transitions that delay work.
- Preserve context when switching Request and Response.
- Respect reduced-motion settings.

### 16.2 Suggested motion

- Method menu opens with platform standard motion.
- Request editor section transition uses a short fade or slide.
- Response summary appears with subtle emphasis.
- JSON tree expands quickly without dramatic spring effects.
- Search highlights update without moving the entire layout.
- Save state crossfades between Saving and Saved.

### 16.3 Haptics

Use restrained platform haptics for:

- Request send.
- Request cancellation.
- Successful save.
- Validation failure.
- Protected production confirmation.
- Destructive action.

Do not trigger haptics for every row edit or navigation tap.

---

## 17. Accessibility requirements

### 17.1 Touch targets

- Meet platform minimum target guidance.
- Icon-only actions need sufficient hit areas.
- Drag handles have an accessible alternative such as Move Up and Move Down.

### 17.2 Screen readers

Labels must communicate:

- `GET method` rather than only `GET` if context is unclear.
- `Production environment, protected`.
- `Status 404, Not Found`.
- `Secret value, hidden`.
- `Header enabled`.
- `JSON object, 6 items, collapsed`.

### 17.3 Focus order

- Follow visual order.
- After Send, move focus to response summary when appropriate.
- Opening search moves focus to the search field.
- Closing a sheet returns focus to the launching control.
- Error banners are announced.

### 17.4 Color

- Do not use color alone for method, status, secret, protected environment, warning, or validation state.
- Provide text, icon, shape, or label.

### 17.5 Text scaling

- Support platform text scaling.
- Avoid fixed-height text containers.
- Allow rows to grow.
- Preserve critical buttons.

### 17.6 Motion and flashing

- Respect reduced motion.
- No flashing status or repeated pulse.
- Progress is calm and non-distracting.

### 17.7 Code accessibility

- Provide raw text copy.
- JSON tree labels include key, value type, and expansion state.
- Search results are announced with match position.

---

## 18. Empty, loading, error, and edge states

Design these states explicitly rather than leaving them to implementation.

### 18.1 Empty states

- Home first use.
- No favorites.
- No drafts.
- No collections.
- Empty collection.
- No history.
- No search results.
- No environments.
- Empty response body.

Each empty state contains one clear next action where appropriate.

### 18.2 Loading states

- Database migration.
- Home data loading.
- Request sending.
- Response formatting.
- Import validation.
- Import application.
- Export preparation.
- History paging.

Avoid skeletons for very small, quickly loaded local lists unless they improve perceived stability.

### 18.3 Validation states

- Invalid URL.
- Missing variable.
- Variable cycle.
- Empty header name.
- Missing multipart file.
- Invalid JSON.
- Duplicate environment key.
- Invalid import file.

### 18.4 Network errors

- Timeout.
- DNS.
- Connection refused.
- TLS.
- No network.
- Connection lost mid-response.
- User canceled.

### 18.5 Persistence errors

- Draft save failed.
- History save failed after response.
- Database open or migration failure.
- Response artifact write failed.
- Secure secret save failed.

### 18.6 Edge cases

- Extremely long URL.
- Hundreds of headers or parameters.
- Very large JSON.
- Duplicate headers.
- Empty 204 response.
- Redirect chain.
- Unknown content type.
- Invalid UTF-8 or binary body.
- History item with missing response file.
- Imported environment missing secret values.

---

## 19. Prototype requirements

Create a clickable prototype for both Android and iOS.

### 19.1 Required prototype flows

- First successful GET.
- Paste cURL and send.
- Switch from staging to protected production and confirm.
- Save request to a new collection.
- Search JSON and copy path.
- Rerun a history entry.
- Create an environment with a secret variable.
- Export workspace without secrets.
- Attempt a local HTTP endpoint, review permission rationale, and handle denial or warning.

### 19.2 Interaction requirements

- Bottom navigation works.
- Back navigation works according to platform convention.
- Request section tabs work.
- Method selector works.
- Environment picker works.
- Send changes to Cancel, then Response.
- Response modes work.
- Search opens and closes.
- Save sheet works.
- Destructive and production confirmations work.

### 19.3 Prototype data

Use realistic but safe sample data:

- Hosts: `api.example.dev`, `staging.example.dev`, `api.example.com`.
- Endpoint: `/v1/users/42`.
- Headers: `Accept: application/json`, `X-Request-ID: {{request_id}}`.
- Secret placeholder: `{{api_token}}`.
- Do not use a realistic token value.

Sample success response:

```json
{
  "id": 42,
  "name": "Ada Lovelace",
  "role": "developer",
  "active": true,
  "projects": [
    { "id": "p-100", "name": "Mobile API" },
    { "id": "p-101", "name": "Auth Service" }
  ]
}
```

Sample error response:

```json
{
  "error": "unauthorized",
  "message": "A valid access token is required."
}
```

---

## 20. Stitch generation workflow

Use Stitch in controlled stages instead of asking for every screen in one generation.

### Stage 1 - Establish visual directions

Provide:

- This design plan.
- The companion `DESIGN.md`.
- The master prompt.

Ask Stitch for three clearly different visual directions while keeping the same information architecture:

1. Precision Minimal.
2. Technical Editorial.
3. Calm Instrument Panel.

Select one direction before generating every screen.

### Stage 2 - Generate the core vertical slice

Generate matched Android and iOS frames for:

- Home empty.
- Home populated.
- Request workspace.
- Sending.
- JSON response.

Evaluate hierarchy, environment visibility, keyboard behavior, and platform identity.

### Stage 3 - Generate request editing

Generate:

- Params.
- Headers.
- Auth.
- Body.
- Resolved preview.

Evaluate touch density and long-content behavior.

### Stage 4 - Generate organization and safety

Generate:

- Collections.
- History.
- Environments.
- Production confirmation.
- Import and Export.
- Settings.

### Stage 5 - Generate all states

Generate:

- Empty.
- Loading.
- Validation.
- Network error.
- Persistence error.
- Large response.
- Secret reveal.
- Recovery.

### Stage 6 - Dark theme and accessibility

- Apply dark tokens.
- Generate large-text variants.
- Audit contrast.
- Audit labels and focus sequence descriptions.

### Stage 7 - Prototype

Connect required flows and run them from start to finish on both platform sets.

### Stage 8 - Handoff cleanup

- Normalize component names.
- Remove duplicate component variants.
- Annotate spacing and behavior.
- Export design rules.
- Freeze accepted screens and mark experimental ones.

---

## 21. Master Stitch prompt

Copy the prompt below into Stitch together with this file and `DESIGN.md`.

```text
Design a production-ready mobile app called RequestLab. It is a lightweight, local-first REST API client for developers, QA engineers, students, and technical support staff.

Create coordinated Android and iOS phone designs only. Do not create desktop, tablet-first, web, or marketing-site screens. Create an Android set that follows current Material 3 behavior and an iOS set that follows familiar iOS tab, navigation, sheet, swipe-back, and destructive-action conventions. Keep the same RequestLab brand and information architecture across both platforms.

The app must feel precise, fast, calm, technical, and trustworthy. It should look like a professional mobile workbench, not a futuristic dashboard and not a desktop interface squeezed onto a phone. Do not copy Postman branding, orange color dominance, logo, or screen layout.

Use the supplied DESIGN.md as the visual-system source of truth. Design light and dark themes. Use platform system fonts for interface text and platform monospace for URLs, headers, request bodies, response bodies, and JSON paths. Preserve accessible contrast and do not communicate method, HTTP status, production danger, validation, or secret state by color alone.

Top-level navigation has four destinations: Home, Collections, History, and More. More contains Environments, Import and Export, Settings, Diagnostics, and About. The active environment must appear clearly on Home and every request screen. Environments may be marked Local, Development, Staging, Production, or Custom. Protected environments show a label and icon, and can require confirmation before Send.

The Home screen must have immediate New Request and Paste cURL actions, followed by Favorites, Recent Requests, and Recoverable Drafts. A first-time user must be able to send a GET request without creating an account, collection, workspace, or environment.

The Request Workspace must be mobile-first. Include a top bar with request name, autosave state, environment badge, and overflow. Include a method selector, flexible URL field, and stable Send action that remains reachable when the software keyboard is open. Include request sections for Params, Headers, Auth, and Body. Params and Headers use touch-friendly key-value rows rather than tiny spreadsheet cells. Body supports None, JSON, Raw, Form, and Multipart. Include a Resolved Request Preview that redacts secrets.

After Send, prioritize the response. Show status code, status meaning, duration, size, and content type. Provide Pretty JSON, Tree, and Raw modes. Include response search, line wrap, copy, share or save, and copy JSON path. Design a clear truncated-response state and a binary-response summary. Design specific timeout, DNS, connection, TLS, cancellation, and persistence-warning states. Do not offer an insecure TLS bypass.

Collections support folders and saved requests. History rows show method, safe host summary, status or error, duration, time, environment, and pin state. History detail is immutable and includes Rerun as New Draft. Environments support public and secret variables. Secret values are masked and can require biometric authentication to reveal. Never place realistic secret tokens in sample content.

Create matched Android and iOS versions for these core screens first: Home Empty, Home Populated, New Request, Params Editor, Headers Editor, Auth Editor, JSON Body Editor, Resolved Preview, Sending, JSON Response, Request Error, cURL Import Preview, Collections, Collection Detail, Save Request, History, History Detail, Environment Picker, Environment List, Environment Editor, Production Confirmation, More, Import and Export, Settings, Diagnostics, Destructive Confirmation, Recovery, and the Local Network plus Insecure HTTP flow.

Use realistic safe sample data such as api.example.dev, staging.example.dev, api.example.com, /v1/users/42, and {{api_token}}. Do not use real credentials.

Generate component variants and annotate interaction behavior. Keep controls implementation-friendly for shared Compose Multiplatform UI with platform-specific navigation chrome and dialogs. First present three distinct visual directions for the five-screen core vertical slice: Home Empty, Home Populated, Request Workspace, Sending, and JSON Response. Name the directions Precision Minimal, Technical Editorial, and Calm Instrument Panel.
```

---

## 22. Focused Stitch prompts

Use these after selecting the visual direction.

## 22.1 Core navigation and Home

```text
Using the accepted RequestLab design system and visual direction, generate matched Android and iOS phone screens for Home Empty and Home Populated. Include New Request, Paste cURL, active environment, Favorites, Recent Requests, Recoverable Drafts, and the four-destination bottom navigation. Keep the layout efficient and avoid excessive cards. Show a clear first-use path and a returning-user path. Add light and dark variants and explain platform-specific differences.
```

## 22.2 Request workspace

```text
Generate matched Android and iOS RequestLab Request Workspace screens. Include request name, autosave state, active environment, method selector, URL field, stable Send action, and sections for Params, Headers, Auth, and Body. Show a compact phone width and a keyboard-visible state. The Send action must remain accessible. Do not use desktop tables. Create blank, edited, validation-warning, and saved-request states.
```

## 22.3 Params and headers

```text
Design RequestLab mobile editors for query parameters and headers. Use ordered touch-friendly key-value rows with enabled state, duplicate-key support, reorder access, variable placeholders, and row menus. Headers also support secret values and generated-auth indicators. Generate compact and large-text variants for Android and iOS. Ensure every drag action has an accessible non-drag alternative.
```

## 22.4 Auth and body

```text
Design RequestLab Auth and Body editors for Android and iOS. Auth supports None, Basic, Bearer, and API Key in header or query. Body supports None, JSON, Raw, Form URL Encoded, and Multipart. Include secure-value treatment, JSON validation, format action, full-screen body editing, file-part selection, and missing-file error. Keep the experience mobile-first and implementation-friendly in Compose Multiplatform.
```

## 22.5 Response Lab

```text
Design RequestLab response inspection for Android and iOS. Show status, status meaning, duration, size, content type, Pretty JSON, Tree, and Raw modes. Include search with match count and next/previous, wrap toggle, copy value, copy JSON path, share or save, truncated preview, binary response summary, empty 204 response, and malformed JSON. Use readable monospace text and lazy-looking tree rows without tiny typography.
```

## 22.6 Errors and recovery

```text
Generate RequestLab error and recovery states for timeout, DNS failure, connection refused, TLS certificate failure, cancellation, missing multipart file, history-save failure after a successful response, restored draft, interrupted run, and missing stored response file. Each error needs a specific title, plain explanation, safe technical detail, and relevant Retry or Edit action. Do not show generic Oops language or insecure TLS bypass.
```

## 22.7 Collections and save flow

```text
Design RequestLab Collections List, Collection Detail, folder navigation, request rows, and Save Request flow for Android and iOS. Support collection search, nested folders, new folder, new collection, request move, reorder mode, favorite, duplicate, and delete. Use efficient list rows rather than large decorative cards. The Save flow must preserve the request name while creating a new folder or collection.
```

## 22.8 History

```text
Design RequestLab History List and immutable History Detail for Android and iOS. Rows show method, safe host, status or transport error, duration, time, environment, and pin state. Include search and filters for date, method, success or failure, status family, environment, and pinned only. History Detail includes request snapshot, redacted headers and body summary, response metadata, note, pin, share redacted request, delete, and Rerun as New Draft.
```

## 22.9 Environments and production guard

```text
Design RequestLab Environment Picker, Environment List, Environment Editor, secret variable row, and protected Production confirmation for Android and iOS. Environment types are Local, Development, Staging, Production, and Custom. The active environment is obvious. Protected state uses text and icon as well as color. Production confirmation shows method and resolved host without secret query values and uses specific Send Anyway and Cancel actions.
```

## 22.10 More, settings, and data

```text
Design RequestLab More, Import and Export, Settings, Diagnostics, and destructive Clear All Data confirmation for Android and iOS. Settings include appearance, request defaults, response preview and storage limits, history retention, biometric secret reveal, clear response cache, clear history, and clear all local data. Diagnostics explains that URLs, headers, bodies, and secrets are excluded. Use native-feeling grouped settings patterns on each platform.
```

## 22.11 Dark theme

```text
Apply the RequestLab dark theme to the accepted screens. Use deep blue-black backgrounds, distinct code surfaces, accessible indigo primary actions, restrained semantic colors, visible dividers, and readable syntax styling. Review Home, Request with keyboard, JSON tree, History, Environment Editor, Production confirmation, Settings, and Error. Avoid pure-black flattening and neon accents.
```

## 22.12 Accessibility audit

```text
Audit the accepted RequestLab Android and iOS designs for WCAG contrast, platform text scaling, touch-target size, screen-reader labeling, focus order, reduced motion, keyboard avoidance, and color-independent status meaning. Generate corrected large-text variants for Home, Request Workspace, JSON Response, Environment Editor, and Production Confirmation. Annotate the accessibility label for every icon-only control.
```

## 22.13 Local network and insecure HTTP

```text
Design the RequestLab local-network permission and insecure HTTP flow for Android and iOS. Show a contextual rationale only after the user attempts a local IP address or .local hostname. Include the platform system-permission transition, denial state, Open Settings recovery, and a separate warning that cleartext HTTP is not encrypted. Show only the method and safe host. Never reveal tokens, headers, or secret query values. Do not request permission during onboarding.
```

---

## 23. Stitch critique and iteration prompts

Use these to improve the generated work.

### 23.1 Reduce desktop influence

```text
Critique these RequestLab screens specifically for desktop-interface patterns that do not belong on phones. Replace tiny grids, overly dense horizontal toolbars, multi-column panes, hover-dependent controls, and small click targets with touch-first mobile patterns while preserving developer-level information.
```

### 23.2 Improve one-handed use

```text
Review the RequestLab Request Workspace for one-handed use and keyboard visibility. Identify actions that are too high, unstable, hidden by the keyboard, or difficult to reach. Revise the design so Send, Cancel, section switching, and add-row actions remain predictable without sacrificing platform conventions.
```

### 23.3 Improve environment safety

```text
Review every RequestLab request-related screen and make the active environment and resolved host clearer. Ensure Production and protected environments are obvious through label, icon, and layout, not color alone. Reduce accidental Send risk without adding friction to Local, Development, or Staging requests.
```

### 23.4 Improve information hierarchy

```text
Critique the hierarchy of method, URL, environment, Send, response status, duration, size, and response body. Make the primary task obvious within two seconds. Remove decoration that competes with technical content and reduce unnecessary card nesting.
```

### 23.5 Improve code readability

```text
Review the RequestLab JSON, URL, header, and body presentation. Improve monospace size, line height, indentation, wrapping, selection, search highlights, type differentiation, and long-string handling. Ensure the design remains readable on a small phone and with dark mode.
```

### 23.6 Improve platform authenticity

```text
Compare the Android and iOS RequestLab variants. Keep the same brand, content, and component logic, but correct any controls that feel imported from the other platform. Preserve implementation feasibility in shared Compose Multiplatform code by limiting differences to navigation chrome, sheets, dialogs, system behaviors, and a small number of platform variants.
```

### 23.7 Simplify components

```text
Audit the RequestLab component set for duplicates and one-off variants. Consolidate method badges, environment badges, request rows, key-value rows, status summaries, validation banners, and list rows into a coherent reusable system. Document required states and remove purely decorative variants.
```

### 23.8 Accessibility correction

```text
Identify all RequestLab places where color alone communicates HTTP method, status, production, destructive action, validation, or secret state. Add text, icons, labels, shape, or accessible descriptions. Correct low-contrast text and undersized controls without reducing information density unnecessarily.
```

---

## 24. Design review checklist

### Navigation

- [ ] Android back behavior is clear.
- [ ] iOS swipe-back behavior is preserved.
- [ ] Four top-level destinations are consistent.
- [ ] Environment access is easy from Home and Request.
- [ ] Deep screens have a clear return path.

### Home

- [ ] New Request is the strongest action.
- [ ] Paste cURL is immediately visible.
- [ ] Favorites, recents, and drafts are distinct.
- [ ] First-use empty state is concise.

### Request editor

- [ ] Method, URL, and Send dominate the hierarchy.
- [ ] Send remains reachable with the keyboard.
- [ ] Params and Headers are not desktop grids.
- [ ] Auth and Body use progressive disclosure.
- [ ] Autosave state is visible but quiet.
- [ ] Resolved preview redacts secrets.

### Environment safety

- [ ] Active environment appears on every request screen.
- [ ] Production uses text and icon, not color alone.
- [ ] Confirmation includes method and resolved host.
- [ ] Cancel is the safe default.

### Response

- [ ] Status, duration, size, and content type are immediately visible.
- [ ] Pretty, Tree, and Raw modes are clear.
- [ ] Search is usable without losing position.
- [ ] JSON path copy is discoverable.
- [ ] Large and binary states are designed.
- [ ] Errors are specific and actionable.

### Organization

- [ ] Collections use efficient list rows.
- [ ] Folder path is understandable.
- [ ] Save flow preserves input.
- [ ] History is visibly immutable.
- [ ] Rerun creates a new draft.

### Secrets

- [ ] No realistic secret values appear.
- [ ] Secret state is labeled.
- [ ] Reveal is explicit and temporary.
- [ ] Export explains secret exclusion.

### Android

- [ ] Material 3 patterns are used appropriately.
- [ ] System bars and back behavior are correct.
- [ ] Sheets and dialogs feel Android-native.
- [ ] No iOS-only control pattern is copied without reason.

### iOS

- [ ] Tab and navigation behavior feels familiar.
- [ ] Safe areas and home indicator are respected.
- [ ] Sheets and destructive actions feel iOS-native.
- [ ] No Material FAB or Android snackbar pattern appears without reason.

### Accessibility

- [ ] Text contrast passes.
- [ ] Touch targets pass.
- [ ] Dynamic text works.
- [ ] Focus order is documented.
- [ ] Icon actions have labels.
- [ ] Reduced motion is supported.

---

## 25. Developer handoff requirements

Each accepted screen should include:

- Platform designation.
- Frame size.
- Light or dark theme.
- Component names.
- Spacing annotations.
- Text style token.
- Color token.
- Shape token.
- Elevation treatment.
- Interaction notes.
- Loading, error, disabled, and pressed states.
- Keyboard behavior.
- Scroll behavior.
- Safe-area behavior.
- Accessibility label for icon-only controls.
- Screen-reader reading order.
- Dynamic-text expectation.

### 25.1 Component handoff

For every reusable component, provide:

- Purpose.
- Anatomy.
- Variants.
- States.
- Content rules.
- Minimum and maximum size behavior.
- Android differences.
- iOS differences.
- Accessibility behavior.

### 25.2 Design-to-code mapping

Map design components to likely shared Compose components:

| Design component | Compose implementation concept |
|---|---|
| Method badge | Shared composable with semantic method style |
| Environment badge | Shared composable with protected and active variants |
| Key-value editor row | Shared composable with compact and expanded layout |
| Send / Cancel | Shared action model with platform button treatment |
| Request section tabs | Shared tab content with adaptive platform styling |
| JSON tree row | Shared lazy tree node composable |
| Response summary | Shared status and metrics composable |
| History row | Shared list item with platform container treatment |
| Confirmation | Shared content model rendered by platform dialog or sheet |
| Secret reveal | Shared field state plus platform biometric integration |

### 25.3 Asset handoff

Provide:

- App icon source.
- Monochrome Android icon variant if required.
- iOS app icon set.
- Vector icons or exact system-symbol references.
- Empty-state illustration only if accepted and genuinely useful.
- No font files unless separately licensed and deliberately selected; the implementation defaults to platform system fonts.

---

## 26. Final design acceptance criteria

The design package is complete when:

- Android and iOS P0 screen families are present.
- No desktop screens are present.
- Light and dark themes are complete.
- The active environment is visible throughout request flows.
- Production confirmation is explicit and accessible.
- The keyboard-visible request state is designed.
- Params and Headers are touch-friendly.
- JSON response search, tree, raw, and copy-path interactions are designed.
- cURL import preview includes security warnings.
- Collections, history, environments, settings, diagnostics, and import/export are complete.
- Empty, loading, validation, network error, persistence error, local-network permission, insecure HTTP, large response, and recovery states are complete.
- Required prototype flows work on both platform sets.
- Components are consolidated and named.
- Dynamic text and accessibility corrections are incorporated.
- The accepted design is consistent with the companion `DESIGN.md`.
- The design can be implemented with shared Compose Multiplatform UI plus focused Android and iOS adaptations.

---

## Final instruction to Stitch

Design RequestLab as a **phone-native developer tool**, not a miniaturized desktop API client. Prioritize the first request, active environment, stable Send action, readable response, recoverable drafts, and safe secret behavior. Produce coordinated but platform-authentic Android and iOS experiences, and use the companion `DESIGN.md` as the visual source of truth.
