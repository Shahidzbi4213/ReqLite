version: "2.0"
name: "RequestLab - Quiet Signal"
description: "A restrained, mobile-first design system for a professional REST API client on Android and iOS. Warm neutral surfaces, ink-first hierarchy, one mineral-green accent, crisp keylines, compact geometry, and edge-to-edge technical content."
design_language:
  name: "Quiet Signal"
  keywords:
    - "minimal"
    - "editorial"
    - "technical"
    - "calm"
    - "precise"
    - "phone-native"
  signature:
    - "Warm paper-like canvas instead of cool gray or purple-tinted surfaces"
    - "Ink-black primary actions with mineral-green focus and active states"
    - "Small square environment markers instead of colorful pills"
    - "Numbered monospace section labels such as 01 PARAMS and 02 HEADERS"
    - "Response status presented as a typographic strip with a slim semantic rail"
    - "Code and JSON presented edge-to-edge with a line-number gutter"
  anti_patterns:
    - "Purple, violet, or blue-purple brand palettes"
    - "Gradients, glow, glassmorphism, blur-heavy chrome, or neon decoration"
    - "Large rounded cards nested inside other rounded cards"
    - "Colorful pill badges for every HTTP method and state"
    - "Blob illustrations, fake terminal art, floating 3D objects, or generic AI concept imagery"
    - "Oversized hero text inside product screens"
    - "Desktop dashboards compressed into a phone frame"
colors:
  canvas: "#F3F2ED"
  on-canvas: "#151715"
  surface: "#FBFAF7"
  on-surface: "#151715"
  surface-raised: "#FFFFFF"
  surface-sunken: "#EAE9E3"
  selected-surface: "#E3ECE7"
  text-primary: "#151715"
  text-secondary: "#5E635E"
  text-tertiary: "#646963"
  border: "#D2D4CD"
  border-strong: "#858B84"
  primary: "#151715"
  on-primary: "#FFFFFF"
  accent: "#0B6B53"
  on-accent: "#FFFFFF"
  accent-soft: "#DCEAE4"
  on-accent-soft: "#164A3A"
  focus-ring: "#0B6B53"
  code-surface: "#ECEBE5"
  code-gutter: "#E1E0D9"
  code-on-surface: "#1B1E1B"
  syntax-key: "#245E86"
  syntax-string: "#26704C"
  syntax-number: "#8A4B19"
  syntax-literal: "#6D5700"
  syntax-muted: "#646963"
  success: "#16754A"
  on-success: "#FFFFFF"
  success-soft: "#DCEDE3"
  on-success-soft: "#174B32"
  warning: "#8B5C00"
  on-warning: "#FFFFFF"
  warning-soft: "#F4E8C9"
  on-warning-soft: "#5A3A00"
  error: "#B42318"
  on-error: "#FFFFFF"
  error-soft: "#F7E2DE"
  on-error-soft: "#6E1A12"
  info: "#2F5F8F"
  on-info: "#FFFFFF"
  info-soft: "#DFEAF4"
  on-info-soft: "#244A70"
  protected: "#8F2620"
  protected-soft: "#F2DEDA"
  on-protected-soft: "#681B16"
  method-default: "#343934"
  method-delete: "#9D2A21"
  dark-canvas: "#10120F"
  dark-on-canvas: "#F1F2ED"
  dark-surface: "#161915"
  dark-on-surface: "#F1F2ED"
  dark-surface-raised: "#1C201B"
  dark-surface-sunken: "#0B0D0B"
  dark-selected-surface: "#1A3128"
  dark-text-primary: "#F1F2ED"
  dark-text-secondary: "#A7ADA5"
  dark-text-tertiary: "#80867F"
  dark-border: "#30352E"
  dark-border-strong: "#676E65"
  dark-primary: "#F1F2ED"
  dark-on-primary: "#111310"
  dark-accent: "#79C7A8"
  dark-on-accent: "#0D2A20"
  dark-accent-soft: "#173D30"
  dark-on-accent-soft: "#BCE6D4"
  dark-focus-ring: "#79C7A8"
  dark-code-surface: "#0C0E0C"
  dark-code-gutter: "#131612"
  dark-code-on-surface: "#E9EAE5"
  dark-syntax-key: "#83B7D8"
  dark-syntax-string: "#82C8A0"
  dark-syntax-number: "#E0A46E"
  dark-syntax-literal: "#D9C873"
  dark-syntax-muted: "#92988F"
  dark-success: "#72C792"
  dark-success-soft: "#153622"
  dark-warning: "#E4B45D"
  dark-warning-soft: "#3D2D0E"
  dark-error: "#FF8C82"
  dark-error-soft: "#431B18"
  dark-info: "#7FA6CF"
  dark-info-soft: "#172B3E"
  dark-protected: "#FF9A91"
  dark-protected-soft: "#451D1A"
typography:
  display:
    fontFamily: "System UI"
    fontSize: "30px"
    fontWeight: 600
    lineHeight: "36px"
    letterSpacing: "-0.5px"
  title-lg:
    fontFamily: "System UI"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: "30px"
    letterSpacing: "-0.3px"
  title-md:
    fontFamily: "System UI"
    fontSize: "20px"
    fontWeight: 600
    lineHeight: "26px"
    letterSpacing: "-0.1px"
  title-sm:
    fontFamily: "System UI"
    fontSize: "17px"
    fontWeight: 600
    lineHeight: "22px"
  body-lg:
    fontFamily: "System UI"
    fontSize: "17px"
    fontWeight: 400
    lineHeight: "24px"
  body-md:
    fontFamily: "System UI"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: "22px"
  body-sm:
    fontFamily: "System UI"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: "19px"
  label-lg:
    fontFamily: "System UI"
    fontSize: "15px"
    fontWeight: 600
    lineHeight: "20px"
  label-md:
    fontFamily: "System UI"
    fontSize: "13px"
    fontWeight: 600
    lineHeight: "18px"
  label-sm:
    fontFamily: "System UI"
    fontSize: "11px"
    fontWeight: 600
    lineHeight: "16px"
    letterSpacing: "0.4px"
  index-label:
    fontFamily: "ui-monospace"
    fontSize: "11px"
    fontWeight: 600
    lineHeight: "16px"
    letterSpacing: "0.9px"
  code-lg:
    fontFamily: "ui-monospace"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: "23px"
  code-md:
    fontFamily: "ui-monospace"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "21px"
  code-sm:
    fontFamily: "ui-monospace"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: "18px"
  metric:
    fontFamily: "ui-monospace"
    fontSize: "22px"
    fontWeight: 600
    lineHeight: "26px"
    letterSpacing: "-0.3px"
rounded:
  none: "0px"
  xs: "3px"
  sm: "6px"
  md: "10px"
  lg: "14px"
  full: "999px"
spacing:
  none: "0px"
  xxs: "2px"
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  xxl: "32px"
  xxxl: "48px"
borders:
  hairline: "1px"
  strong: "2px"
  trace: "2px"
  status-rail: "3px"
layout:
  screen-padding: "16px"
  compact-screen-padding: "12px"
  section-gap: "24px"
  row-height: "56px"
  dense-row-height: "48px"
  control-height: "48px"
  compact-control-height: "40px"
  bottom-action-height: "56px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.sm}"
    height: "48px"
    paddingHorizontal: "16px"
  button-secondary:
    backgroundColor: "{colors.surface-raised}"
    textColor: "{colors.text-primary}"
    borderColor: "{colors.border-strong}"
    borderWidth: "{borders.hairline}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.sm}"
    height: "48px"
    paddingHorizontal: "16px"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.text-primary}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.xs}"
    height: "44px"
    paddingHorizontal: "12px"
  button-destructive:
    backgroundColor: "{colors.error}"
    textColor: "{colors.on-error}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.sm}"
    height: "48px"
    paddingHorizontal: "16px"
  input-default:
    backgroundColor: "{colors.surface-raised}"
    textColor: "{colors.text-primary}"
    borderColor: "{colors.border-strong}"
    borderWidth: "{borders.hairline}"
    typography: "{typography.body-md}"
    rounded: "{rounded.sm}"
    height: "48px"
    paddingHorizontal: "12px"
  input-focused:
    backgroundColor: "{colors.surface-raised}"
    textColor: "{colors.text-primary}"
    borderColor: "{colors.accent}"
    borderWidth: "{borders.strong}"
    typography: "{typography.body-md}"
    rounded: "{rounded.sm}"
    height: "48px"
    paddingHorizontal: "11px"
  method-control:
    backgroundColor: "{colors.surface-raised}"
    textColor: "{colors.method-default}"
    borderColor: "{colors.border-strong}"
    borderWidth: "{borders.hairline}"
    typography: "{typography.index-label}"
    rounded: "{rounded.sm}"
    height: "48px"
    paddingHorizontal: "10px"
  environment-locator:
    backgroundColor: "transparent"
    textColor: "{colors.text-secondary}"
    markerColor: "{colors.accent}"
    typography: "{typography.index-label}"
    rounded: "{rounded.none}"
    height: "28px"
    paddingHorizontal: "8px"
  environment-protected:
    backgroundColor: "transparent"
    textColor: "{colors.protected}"
    markerColor: "{colors.protected}"
    typography: "{typography.index-label}"
    rounded: "{rounded.none}"
    height: "28px"
    paddingHorizontal: "8px"
  section-index:
    backgroundColor: "transparent"
    textColor: "{colors.text-tertiary}"
    typography: "{typography.index-label}"
    rounded: "{rounded.none}"
  section-tab-active:
    backgroundColor: "transparent"
    textColor: "{colors.text-primary}"
    indicatorColor: "{colors.accent}"
    indicatorHeight: "2px"
    typography: "{typography.label-md}"
    rounded: "{rounded.none}"
    height: "44px"
  section-tab-inactive:
    backgroundColor: "transparent"
    textColor: "{colors.text-secondary}"
    typography: "{typography.label-md}"
    rounded: "{rounded.none}"
    height: "44px"
  key-value-row:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-primary}"
    dividerColor: "{colors.border}"
    dividerWidth: "{borders.hairline}"
    typography: "{typography.body-md}"
    rounded: "{rounded.none}"
    minHeight: "64px"
    paddingVertical: "10px"
  code-panel:
    backgroundColor: "{colors.code-surface}"
    textColor: "{colors.code-on-surface}"
    borderColor: "{colors.border}"
    borderWidth: "{borders.hairline}"
    typography: "{typography.code-md}"
    rounded: "{rounded.xs}"
    padding: "0px"
  code-gutter:
    backgroundColor: "{colors.code-gutter}"
    textColor: "{colors.text-tertiary}"
    typography: "{typography.code-sm}"
    rounded: "{rounded.none}"
    width: "44px"
  status-strip:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-primary}"
    borderColor: "{colors.border}"
    borderWidth: "{borders.hairline}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.none}"
    minHeight: "72px"
    padding: "12px"
  status-code:
    backgroundColor: "transparent"
    textColor: "{colors.text-primary}"
    typography: "{typography.metric}"
    rounded: "{rounded.none}"
  validation-warning:
    backgroundColor: "{colors.warning-soft}"
    textColor: "{colors.on-warning-soft}"
    railColor: "{colors.warning}"
    railWidth: "{borders.status-rail}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.xs}"
    padding: "12px"
  validation-error:
    backgroundColor: "{colors.error-soft}"
    textColor: "{colors.on-error-soft}"
    railColor: "{colors.error}"
    railWidth: "{borders.status-rail}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.xs}"
    padding: "12px"
  list-row:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-primary}"
    dividerColor: "{colors.border}"
    dividerWidth: "{borders.hairline}"
    typography: "{typography.body-md}"
    rounded: "{rounded.none}"
    minHeight: "60px"
    paddingHorizontal: "16px"
  bottom-navigation:
    backgroundColor: "{colors.surface-raised}"
    textColor: "{colors.text-secondary}"
    selectedTextColor: "{colors.text-primary}"
    selectedIndicatorColor: "{colors.accent}"
    borderColor: "{colors.border}"
    borderWidth: "{borders.hairline}"
    rounded: "{rounded.none}"
    height: "64px"
---

# RequestLab Design System

## 1. Direction: Quiet Signal

RequestLab should feel like a compact technical instrument made for daily use. It is not a colorful productivity dashboard, a futuristic console, or a portfolio concept. The interface is intentionally quiet so the request, target environment, and response become the visual focus.

The visual identity is created through restraint rather than decoration:

- Warm neutral canvas instead of blue-gray or purple-tinted backgrounds.
- Ink-black primary actions instead of a bright brand-colored button on every screen.
- Mineral green used only for focus, active navigation, selection, and the request trace.
- Thin keylines and surface changes instead of shadows and card stacks.
- Compact square geometry with 3px to 10px radii.
- Monospace index labels for technical structure.
- Edge-to-edge code presentation with a visible gutter.
- Typography-led status summaries instead of colored pills.

The result should feel authored, utilitarian, and recognizable even when all branding is removed.

## 2. Non-negotiable rules

- Never use purple, violet, lavender, or blue-purple as the brand direction.
- Never use gradients in product UI.
- Never use glassmorphism, glowing borders, blurred floating panels, or neon accents.
- Never turn every section into a card.
- Never use a rainbow of HTTP method pills.
- Never use generic abstract illustrations, 3D shapes, code rain, or fake terminal decoration.
- Never enlarge corner radii merely to make the UI feel friendly.
- Never create separate visual identities for Android and iOS.
- Never sacrifice information density for decorative whitespace.
- Never communicate status, danger, or environment through color alone.

## 3. Visual signature

### 3.1 Environment locator

The active environment is shown as a compact locator, not a filled pill. Use a 6px square marker, a short uppercase monospace label, and optional host text.

Examples:

- Green square + `STAGING`
- Neutral square + `LOCAL`
- Shield icon + `PRODUCTION` in protected red

The marker, label, and protection icon must all remain visible without relying on color.

### 3.2 Indexed request sections

Use small monospace section labels to create a consistent technical rhythm:

- `01 PARAMS`
- `02 HEADERS`
- `03 AUTH`
- `04 BODY`

These labels are structural, not decorative. They use tertiary text color and generous letter spacing. The active section uses a 2px mineral-green underline rather than a filled segmented pill.

### 3.3 Request trace

On Request and Response screens, a subtle 2px vertical or horizontal mineral-green trace may connect the request composer to the response status. It should be used once per screen and never become a decorative network diagram.

### 3.4 Response status strip

Do not present `200 OK` as a rounded badge. Use a horizontal status strip with:

- A 3px semantic rail.
- A large monospace status code.
- The status phrase in normal text.
- Duration, size, and content type aligned as compact metrics.

Transport failures use the same structure but show a named failure such as `DNS` or `TLS` instead of inventing an HTTP code.

### 3.5 Code surface

Code editors and response viewers are working surfaces, not cards. They may extend to the screen edge below a toolbar. Use:

- A quiet inset background.
- A 44px line-number or tree gutter when relevant.
- 1px separators.
- Minimal 3px corner radius only when the panel is inset.
- No shadow.
- No decorative terminal header dots.

## 4. Color system

### 4.1 Neutral-first palette

Approximately 85 to 90 percent of every screen should use neutral colors. The warm canvas prevents the product from feeling like another generic cool-gray dashboard. The surface hierarchy is intentionally subtle:

- Canvas: app background.
- Surface: grouped content.
- Raised surface: inputs, sheets, and controls that need separation.
- Sunken surface: code, read-only previews, and recessed technical areas.

### 4.2 Brand accent

Mineral green is the single brand accent. Use it for:

- Focus rings.
- Active section underline.
- Selected navigation marker.
- Environment locator marker.
- Request trace.
- Small progress indicators.

Do not use it as a large decorative background. Primary buttons remain ink-black in light mode so the accent keeps meaning.

### 4.3 Semantic colors

Semantic color is limited to small rails, icons, text, and focused banners:

- Success: completed request or successful operation.
- Warning: validation or potentially unsafe configuration.
- Error: destructive action, transport failure, or invalid state.
- Protected: Production and protected environments.
- Info: neutral technical explanation.

Large red or green filled cards are not part of the system.

### 4.4 HTTP methods

HTTP methods are primarily distinguished by text. Use the same neutral method control for GET, POST, PUT, PATCH, HEAD, and OPTIONS. DELETE may use a restrained error-colored text or 2px marker because it is destructive. This avoids the familiar rainbow-method look.

## 5. Typography

Use the platform system font for interface copy and the platform monospace family for technical content. Do not introduce a decorative brand font during the first release.

Typography should feel editorial rather than promotional:

- Use weight 600 for titles and actions; avoid unnecessary 700 and 800 weights.
- Use large titles sparingly.
- Use monospace for status codes, methods, URLs, JSON paths, timing, sizes, and index labels.
- Use uppercase only for short technical labels.
- Keep code at 14px or larger for primary reading surfaces.
- Prefer wrapping or full-screen editing over shrinking text.

## 6. Geometry and spacing

### 6.1 Shape

- Inputs and buttons: 6px radius.
- Sheets and major modal surfaces: up to 14px where platform conventions require it.
- Code panels: 0px to 3px radius.
- List rows: square, separated by keylines.
- Tags and environment locators: square or 3px radius, never default pills.
- Fully rounded shapes are reserved for native switches, tiny progress dots, and accessibility-required controls.

### 6.2 Spacing

Use a 4-point grid. The standard screen inset is 16px. Preserve compact, readable density:

- 4px: micro alignment.
- 8px: related inline items.
- 12px: compact component padding.
- 16px: screen padding and standard control padding.
- 24px: section separation.
- 32px: major structural break.

Do not add empty vertical space merely to make a screen look premium.

## 7. Depth and surface behavior

- Prefer 1px borders, dividers, and tonal shifts over shadows.
- Use no shadow on cards, code panels, status strips, or list rows.
- Android may use low elevation only for a scrolling app bar, bottom sheet, or sticky action required by Material behavior.
- iOS may use native sheet depth and navigation-bar material, but content itself remains flat.
- Never stack a rounded card inside another rounded card.

## 8. Component grammar

### 8.1 Primary actions

The primary button is ink-black with white text in light mode. In dark mode it uses the light primary token with dark text, or the dark accent for the Send action when stronger differentiation is needed. Buttons use 6px corners and no gradient.

Send occupies a stable position. During execution, Cancel replaces Send in the same footprint.

### 8.2 Secondary actions

Secondary actions use a white or raised surface with a 1px strong border. Tertiary actions are text or icon actions without a container until pressed.

### 8.3 Method control

The method selector is compact, monospace, and outlined. It joins visually with the URL field without becoming a fully fused desktop-style address bar. Method text is always visible.

### 8.4 Navigation

The selected bottom-navigation destination uses label weight plus a short mineral-green line or dot. Do not place the selected icon inside a large colored bubble. Platform navigation dimensions and behavior remain native.

### 8.5 Key-value rows

Params, headers, form data, and environment variables use full-width rows separated by keylines. On narrow widths, key and value stack. Row actions appear in a trailing menu rather than a permanent toolbar of icons.

### 8.6 Banners

Warnings and errors use a pale semantic background with a 3px left rail. They are rectangular with a 3px radius and concise copy. Avoid oversized alert cards.

### 8.7 Empty states

Empty states use typography, one small line icon, and a direct action. Do not use a large illustration unless usability testing demonstrates a need.

## 9. Light theme

The light theme is warm, not beige. Surfaces must still look clean and technical. The canvas is slightly darker than the main surface, and inputs use the raised surface to remain visible without shadow.

Review the palette in context before changing it. Do not cool the neutrals toward blue-gray and do not tint them toward lavender.

## 10. Dark theme

The dark theme uses neutral green-black rather than blue-black. It should feel like the same product after dark, not a neon terminal theme.

- Use `#10120F` as the canvas, not pure black.
- Keep surfaces neutral; avoid navy and purple undertones.
- Use mint-green accent only for focus and active states.
- Keep code surface darker than the main surface with a visible gutter.
- Keep semantic colors softened and small.
- Do not use glow, bloom, luminous borders, or saturated syntax colors.

## 11. Platform adaptation

### Android

- Follow Material 3 interaction behavior, navigation, back handling, sheets, snackbars, and edge-to-edge requirements.
- Apply RequestLab colors, compact shapes, keylines, and component grammar rather than default large Material pills.
- Use tonal elevation only where Material behavior genuinely requires it.

### iOS

- Follow iOS navigation bars, tab bars, sheets, swipe-back, menus, switches, and destructive actions.
- Preserve RequestLab's warm neutral palette, square technical labels, and status strips.
- Avoid making the iOS version look like an Android screen with iOS icons.

Shared screens should clearly belong to the same product while native chrome and interactions remain platform-authentic.

## 12. Accessibility

- Maintain at least 4.5:1 contrast for normal text and 3:1 for large text and essential non-text UI.
- Pair every semantic color with text, iconography, or structure.
- Keep Android touch targets at least 48dp and iOS targets at least 44pt.
- Support Dynamic Type, Android font scaling, screen readers, reduced motion, and high-contrast review.
- Do not use small uppercase text for instructions or error explanations.
- Preserve focus visibility with a 2px focus ring and additional state cues.

## 13. Do

- Let URLs, request bodies, and responses dominate the working screens.
- Use warm neutrals, ink, and one mineral-green accent.
- Use keylines and spacing to group content.
- Use numbered section labels consistently.
- Keep the active environment visible without a filled pill.
- Present status as a strip with a semantic rail.
- Make code surfaces feel like serious editing and inspection tools.
- Keep the system feasible in shared Compose Multiplatform code.

## 14. Do not

- Do not use purple anywhere in the brand palette.
- Do not use gradients, glass, glow, or floating decorative shapes.
- Do not use multiple accent colors to make the design look interesting.
- Do not create a card for every list, section, action, or metric.
- Do not use pill-shaped methods, environments, statuses, and filters by default.
- Do not use fake terminal chrome or decorative code snippets.
- Do not add large illustrations to Home, History, Collections, or errors.
- Do not copy Postman, Insomnia, or generic AI dashboard patterns.
- Do not let platform-native adaptation override the shared RequestLab identity.
