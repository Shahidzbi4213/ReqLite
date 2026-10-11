import SwiftUI
import Shared

// MARK: - ReqTokens (Centralized Design Tokens)

enum ReqTokens {
    enum Spacing {
        static let xxs: CGFloat = 4
        static let xs: CGFloat = 8
        static let sm: CGFloat = 12
        static let md: CGFloat = 16
        static let lg: CGFloat = 20
        static let xl: CGFloat = 24
        static let xxl: CGFloat = 32
    }

    enum Radius {
        static let micro: CGFloat = 6
        static let small: CGFloat = 8
        static let control: CGFloat = 12
        static let card: CGFloat = 16
        static let prominent: CGFloat = 20
        static let pill: CGFloat = 999
    }

    enum Height {
        static let compact: CGFloat = 36
        static let control: CGFloat = 44  // Minimum standard iOS touch target
        static let prominent: CGFloat = 50
    }

    enum MethodColor {
        static func color(for method: String) -> Color {
            switch method.uppercased() {
            case "GET":
                return Color(red: 0.18, green: 0.80, blue: 0.44) // Clean vibrant emerald green
            case "POST":
                return Color(red: 0.12, green: 0.53, blue: 0.98) // Apple system blue
            case "PUT":
                return Color(red: 1.0, green: 0.58, blue: 0.0)  // System amber/orange
            case "DELETE":
                return Color(red: 1.0, green: 0.27, blue: 0.23) // System red
            case "PATCH":
                return Color(red: 0.69, green: 0.32, blue: 0.87) // System purple
            case "HEAD":
                return Color(red: 0.40, green: 0.73, blue: 0.85) // Cyan
            case "OPTIONS":
                return Color(red: 0.55, green: 0.58, blue: 0.62) // Slate
            default:
                return Color.secondary
            }
        }
    }
}

// MARK: - Liquid Glass Ambient Backdrop

/// Dynamic ambient fluid light field that provides optical refraction for Liquid Glass components
struct ReqAmbientBackdrop: View {
    @SwiftUI.Environment(\.colorScheme) private var colorScheme: ColorScheme

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                // Base background
                Color(uiColor: colorScheme == .dark ? .systemBackground : .systemGroupedBackground)

                // Fluid chromatic glow orbs (Cyan, Azure, Violet)
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                Color(red: 0.15, green: 0.55, blue: 1.0).opacity(colorScheme == .dark ? 0.38 : 0.22),
                                Color(red: 0.15, green: 0.55, blue: 1.0).opacity(0.0)
                            ],
                            center: .center,
                            startRadius: 20,
                            endRadius: proxy.size.width * 0.5
                        )
                    )
                    .frame(width: proxy.size.width * 0.95, height: proxy.size.width * 0.95)
                    .offset(x: -proxy.size.width * 0.2, y: -proxy.size.height * 0.25)
                    .blur(radius: 55)

                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                Color(red: 0.70, green: 0.35, blue: 0.95).opacity(colorScheme == .dark ? 0.32 : 0.18),
                                Color(red: 0.70, green: 0.35, blue: 0.95).opacity(0.0)
                            ],
                            center: .center,
                            startRadius: 20,
                            endRadius: proxy.size.width * 0.5
                        )
                    )
                    .frame(width: proxy.size.width * 0.90, height: proxy.size.width * 0.90)
                    .offset(x: proxy.size.width * 0.25, y: proxy.size.height * 0.05)
                    .blur(radius: 65)

                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                Color(red: 0.10, green: 0.85, blue: 0.75).opacity(colorScheme == .dark ? 0.28 : 0.15),
                                Color(red: 0.10, green: 0.85, blue: 0.75).opacity(0.0)
                            ],
                            center: .center,
                            startRadius: 20,
                            endRadius: proxy.size.width * 0.45
                        )
                    )
                    .frame(width: proxy.size.width * 0.80, height: proxy.size.width * 0.80)
                    .offset(x: -proxy.size.width * 0.15, y: proxy.size.height * 0.35)
                    .blur(radius: 55)
            }
        }
    }
}

// MARK: - Liquid Glass Style Variants

enum ReqGlassVariant {
    /// Standard functional controls (URL input shell, method selector, toolbars)
    case standard
    /// Prominent primary actions (Send button, execute)
    case prominent(tint: Color)
    /// Subtle/quiet secondary actions (QR scanner, icon buttons, utility chips)
    case quiet
    /// Elevated navigation elements (Environment badge, floating pills)
    case elevated
    /// Content layer (luminous frosted glass surface)
    case content
}

// MARK: - Liquid Glass View Modifier

struct ReqGlassModifier: ViewModifier {
    @SwiftUI.Environment(\.colorScheme) private var colorScheme: ColorScheme
    @SwiftUI.Environment(\.accessibilityReduceTransparency) private var reduceTransparency: Bool
    @SwiftUI.Environment(\.colorSchemeContrast) private var contrast: ColorSchemeContrast

    let variant: ReqGlassVariant
    let cornerRadius: CGFloat

    func body(content: Content) -> some View {
        if reduceTransparency {
            // High-accessibility fallback: strictly opaque semantic backgrounds
            content
                .background(opaqueBackgroundColor, in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .stroke(contrast == .increased ? Color.primary : Color.secondary.opacity(0.3), lineWidth: contrast == .increased ? 1.5 : 1)
                )
        } else {
            switch variant {
            case .content:
                // Content Layer: Translucent optical glass container with subtle wash and specular rim
                content
                    .background(
                        ZStack {
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .fill(.ultraThinMaterial)
                            
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .fill(colorScheme == .dark ? Color.white.opacity(0.04) : Color.white.opacity(0.55))
                            
                            // Top specular sheen
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(colorScheme == .dark ? 0.15 : 0.45),
                                    Color.white.opacity(0.0)
                                ],
                                startPoint: .top,
                                endPoint: .center
                            )
                            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                        }
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .strokeBorder(specularBorder, lineWidth: 1.0)
                    )
                    .shadow(
                        color: colorScheme == .dark ? Color.black.opacity(0.35) : Color.black.opacity(0.06),
                        radius: 12,
                        x: 0,
                        y: 4
                    )

            case .standard:
                // Standard Functional Layer: Optical Liquid Glass with specular rim highlight and depth shadow
                content
                    .background(
                        ZStack {
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .fill(.ultraThinMaterial)
                            
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .fill(colorScheme == .dark ? Color.white.opacity(0.06) : Color.white.opacity(0.60))
                            
                            // Top subtle light sheen
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(colorScheme == .dark ? 0.20 : 0.50),
                                    Color.white.opacity(0.0)
                                ],
                                startPoint: .top,
                                endPoint: .center
                            )
                            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                        }
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .strokeBorder(specularBorder, lineWidth: 1.0)
                    )
                    .shadow(
                        color: colorScheme == .dark ? Color.black.opacity(0.40) : Color.black.opacity(0.07),
                        radius: 14,
                        x: 0,
                        y: 6
                    )

            case .prominent(let tint):
                // Prominent Action Layer: Tinted liquid glass with internal optical glow and specular sheen
                content
                    .background(
                        ZStack {
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .fill(.regularMaterial)
                            
                            // Dynamic chromatic tinting
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .fill(tint.opacity(colorScheme == .dark ? 0.32 : 0.20))
                            
                            // Top specular reflection gradient
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .fill(
                                    LinearGradient(
                                        colors: [
                                            Color.white.opacity(colorScheme == .dark ? 0.35 : 0.65),
                                            Color.white.opacity(0.0)
                                        ],
                                        startPoint: .top,
                                        endPoint: .center
                                    )
                                )
                        }
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .strokeBorder(
                                LinearGradient(
                                    colors: [
                                        Color.white.opacity(colorScheme == .dark ? 0.55 : 0.85),
                                        tint.opacity(0.5),
                                        tint.opacity(0.15)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1.25
                            )
                    )
                    .shadow(
                        color: tint.opacity(colorScheme == .dark ? 0.40 : 0.25),
                        radius: 12,
                        x: 0,
                        y: 4
                    )

            case .quiet:
                // Quiet Functional Layer: Minimal translucent glass
                content
                    .background(
                        ZStack {
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .fill(.ultraThinMaterial)
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .fill(colorScheme == .dark ? Color.white.opacity(0.04) : Color.white.opacity(0.40))
                        }
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .strokeBorder(specularBorder, lineWidth: 0.75)
                    )
                    .shadow(
                        color: colorScheme == .dark ? Color.black.opacity(0.25) : Color.black.opacity(0.04),
                        radius: 8,
                        x: 0,
                        y: 3
                    )

            case .elevated:
                // Elevated Transient Layer: Refined depth with multi-layered material and prominent rim
                content
                    .background(
                        ZStack {
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .fill(.regularMaterial)
                            
                            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                                .fill(colorScheme == .dark ? Color.white.opacity(0.08) : Color.white.opacity(0.65))
                            
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(colorScheme == .dark ? 0.28 : 0.65),
                                    Color.white.opacity(0.0)
                                ],
                                startPoint: .top,
                                endPoint: .center
                            )
                            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                        }
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .strokeBorder(specularBorder, lineWidth: 1.25)
                    )
                    .shadow(
                        color: colorScheme == .dark ? Color.black.opacity(0.50) : Color.black.opacity(0.12),
                        radius: 20,
                        x: 0,
                        y: 10
                    )
            }
        }
    }

    private var opaqueBackgroundColor: Color {
        switch variant {
        case .prominent(let tint):
            return tint.opacity(0.15)
        case .content:
            return Color(uiColor: .secondarySystemGroupedBackground)
        default:
            return Color(uiColor: .secondarySystemBackground)
        }
    }

    private var specularBorder: LinearGradient {
        if colorScheme == .dark {
            return LinearGradient(
                colors: [
                    Color.white.opacity(0.45),
                    Color.white.opacity(0.15),
                    Color.white.opacity(0.03)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        } else {
            return LinearGradient(
                colors: [
                    Color.white.opacity(0.95),
                    Color.white.opacity(0.45),
                    Color.black.opacity(0.08)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }
}

extension View {
    func reqGlass(
        _ variant: ReqGlassVariant = .standard,
        cornerRadius: CGFloat = ReqTokens.Radius.card
    ) -> some View {
        self.modifier(ReqGlassModifier(variant: variant, cornerRadius: cornerRadius))
    }
}

// MARK: - Interactive Button Style with Liquid Spring Dynamics

struct ReqGlassButtonStyle: ButtonStyle {
    @SwiftUI.Environment(\.accessibilityReduceMotion) private var reduceMotion: Bool
    let variant: ReqGlassVariant
    let cornerRadius: CGFloat

    init(
        variant: ReqGlassVariant = .standard,
        cornerRadius: CGFloat = ReqTokens.Radius.control
    ) {
        self.variant = variant
        self.cornerRadius = cornerRadius
    }

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .reqGlass(variant, cornerRadius: cornerRadius)
            .scaleEffect(
                reduceMotion ? 1.0 : (configuration.isPressed ? 0.96 : 1.0)
            )
            .opacity(configuration.isPressed ? 0.88 : 1.0)
            .animation(
                reduceMotion ? nil : .spring(response: 0.28, dampingFraction: 0.7, blendDuration: 0),
                value: configuration.isPressed
            )
    }
}

// MARK: - Centralized Reusable Components

/// Reusable Liquid Glass icon button with proper optical alignment and touch target
struct ReqGlassIconButton: View {
    let icon: String
    let size: CGFloat
    let action: () -> Void

    init(
        icon: String,
        size: CGFloat = ReqTokens.Height.control,
        action: @escaping () -> Void
    ) {
        self.icon = icon
        self.size = size
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(.primary)
                .frame(width: size, height: size)
        }
        .buttonStyle(ReqGlassButtonStyle(variant: .quiet, cornerRadius: ReqTokens.Radius.control))
    }
}

/// Cohesive Request Composer uniting HTTP Method selector and URL text field into one harmonious control
struct ReqRequestComposer: View {
    @Binding var method: String
    @Binding var url: String
    let availableMethods: [String]
    let onCommit: () -> Void

    @FocusState private var isUrlFocused: Bool
    @SwiftUI.Environment(\.colorScheme) private var colorScheme: ColorScheme

    init(
        method: Binding<String>,
        url: Binding<String>,
        availableMethods: [String],
        onCommit: @escaping () -> Void = {}
    ) {
        self._method = method
        self._url = url
        self.availableMethods = availableMethods
        self.onCommit = onCommit
    }

    var body: some View {
        HStack(spacing: ReqTokens.Spacing.xs) {
            // Integrated Method Selector Menu
            Menu {
                ForEach(availableMethods, id: \.self) { m in
                    Button(action: {
                        withAnimation(.spring(response: 0.25, dampingFraction: 0.75)) {
                            method = m
                        }
                    }) {
                        HStack {
                            Text(m)
                                .font(.system(.body, design: .monospaced))
                            if method == m {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                }
            } label: {
                HStack(spacing: ReqTokens.Spacing.xxs) {
                    Text(method)
                        .font(.system(size: 14, weight: .bold, design: .monospaced))
                        .foregroundColor(ReqTokens.MethodColor.color(for: method))
                    Image(systemName: "chevron.down")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, ReqTokens.Spacing.sm)
                .frame(height: ReqTokens.Height.compact)
                .background(
                    Capsule(style: .continuous)
                        .fill(ReqTokens.MethodColor.color(for: method).opacity(colorScheme == .dark ? 0.22 : 0.12))
                )
                .overlay(
                    Capsule(style: .continuous)
                        .strokeBorder(ReqTokens.MethodColor.color(for: method).opacity(0.3), lineWidth: 0.75)
                )
            }
            .accessibilityLabel("HTTP Method \(method)")

            // Subtle Vertical Divider
            Rectangle()
                .fill(Color.primary.opacity(0.12))
                .frame(width: 1, height: 22)

            // High-legibility URL text field (Clean content layer inside control shell)
            TextField("https://api.example.com/v1/resource", text: $url)
                .font(.system(size: 14, weight: .regular, design: .monospaced))
                .textContentType(.URL)
                .keyboardType(.URL)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)
                .focused($isUrlFocused)
                .submitLabel(.go)
                .onSubmit(onCommit)
                .frame(maxWidth: .infinity, alignment: .leading)

            // Quick Clear Button when editing
            if !url.isEmpty {
                Button(action: { url = "" }) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary.opacity(0.7))
                }
                .buttonStyle(.plain)
                .padding(.trailing, ReqTokens.Spacing.xxs)
                .accessibilityLabel("Clear URL")
            }
        }
        .padding(.horizontal, ReqTokens.Spacing.sm)
        .frame(height: ReqTokens.Height.prominent)
        .reqGlass(.standard, cornerRadius: ReqTokens.Radius.card)
        .overlay(
            RoundedRectangle(cornerRadius: ReqTokens.Radius.card, style: .continuous)
                .strokeBorder(isUrlFocused ? Color.accentColor.opacity(0.65) : Color.clear, lineWidth: 1.5)
                .animation(.easeInOut(duration: 0.2), value: isUrlFocused)
        )
    }
}

/// Liquid Glass segmented control with fluid glass active indicator
struct ReqGlassSegmentedControl<T: Hashable & Identifiable & CustomStringConvertible>: View {
    let items: [T]
    @Binding var selection: T
    @Namespace private var segmentNamespace
    @SwiftUI.Environment(\.colorScheme) private var colorScheme: ColorScheme
    @SwiftUI.Environment(\.accessibilityReduceTransparency) private var reduceTransparency: Bool

    init(items: [T], selection: Binding<T>) {
        self.items = items
        self._selection = selection
    }

    var body: some View {
        HStack(spacing: 4) {
            ForEach(items) { item in
                let isSelected = selection == item
                Button {
                    withAnimation(.spring(response: 0.32, dampingFraction: 0.76)) {
                        selection = item
                    }
                } label: {
                    Text(item.description)
                        .font(.system(size: 13, weight: isSelected ? .bold : .medium))
                        .foregroundColor(isSelected ? .primary : .secondary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 32)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .background {
                    if isSelected {
                        RoundedRectangle(cornerRadius: ReqTokens.Radius.small, style: .continuous)
                            .fill(
                                reduceTransparency
                                    ? Color(uiColor: .systemBackground)
                                    : (colorScheme == .dark ? Color.white.opacity(0.24) : Color.white.opacity(0.92))
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: ReqTokens.Radius.small, style: .continuous)
                                    .strokeBorder(
                                        colorScheme == .dark
                                            ? Color.white.opacity(0.40)
                                            : Color.white.opacity(0.95),
                                        lineWidth: 1.0
                                    )
                            )
                            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.35 : 0.08), radius: 6, x: 0, y: 3)
                            .matchedGeometryEffect(id: "ActiveSegmentThumb", in: segmentNamespace)
                    }
                }
            }
        }
        .padding(4)
        .reqGlass(.quiet, cornerRadius: ReqTokens.Radius.control)
    }
}

/// Quiet Signal Developer Empty State (Liquid Glass HUD Card)
struct ReqEmptyStateView: View {
    @SwiftUI.Environment(\.colorScheme) private var colorScheme: ColorScheme
    let icon: String
    let title: String
    let subtitle: String

    init(
        icon: String = "paperplane",
        title: String = "Ready to Execute",
        subtitle: String = "Configure request details above and tap Send"
    ) {
        self.icon = icon
        self.title = title
        self.subtitle = subtitle
    }

    var body: some View {
        VStack(spacing: ReqTokens.Spacing.sm) {
            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                Color.blue.opacity(colorScheme == .dark ? 0.35 : 0.18),
                                Color.blue.opacity(0.0)
                            ],
                            center: .center,
                            startRadius: 10,
                            endRadius: 36
                        )
                    )
                    .frame(width: 72, height: 72)
                    .blur(radius: 6)

                Image(systemName: icon)
                    .font(.system(size: 32, weight: .light))
                    .foregroundColor(.accentColor)
            }
            .padding(.bottom, ReqTokens.Spacing.xxs)

            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.primary.opacity(0.95))

            Text(subtitle)
                .font(.system(size: 13, weight: .regular))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 260)
        }
        .padding(.vertical, ReqTokens.Spacing.xl)
        .padding(.horizontal, ReqTokens.Spacing.lg)
        .frame(maxWidth: .infinity)
        .reqGlass(.content, cornerRadius: ReqTokens.Radius.card)
    }
}

// MARK: - Reusable Liquid Glass Button

struct ReqGlassButton: View {
    enum Variant {
        case primary(tint: Color = .blue)
        case secondary
        case destructive
    }

    let title: String
    let icon: String?
    let variant: Variant
    let isLoading: Bool
    let isEnabled: Bool
    let height: CGFloat
    let action: () -> Void

    init(
        title: String,
        icon: String? = nil,
        variant: Variant = .primary(tint: .blue),
        isLoading: Bool = false,
        isEnabled: Bool = true,
        height: CGFloat = ReqTokens.Height.control,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.variant = variant
        self.isLoading = isLoading
        self.isEnabled = isEnabled
        self.height = height
        self.action = action
    }

    private var glassVariant: ReqGlassVariant {
        switch variant {
        case .primary(let tint):
            return .prominent(tint: tint)
        case .secondary:
            return .quiet
        case .destructive:
            return .prominent(tint: Color.red)
        }
    }

    private var foregroundColor: Color {
        switch variant {
        case .primary:
            return .blue
        case .secondary:
            return .primary
        case .destructive:
            return .red
        }
    }

    var body: some View {
        Button(action: {
            if isEnabled && !isLoading {
                action()
            }
        }) {
            HStack(spacing: ReqTokens.Spacing.xs) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: foregroundColor))
                        .scaleEffect(0.85)
                } else if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 14, weight: .semibold))
                }

                Text(title)
                    .font(.system(size: 14, weight: .bold))
            }
            .foregroundColor(foregroundColor)
            .padding(.horizontal, ReqTokens.Spacing.md)
            .frame(height: height)
        }
        .buttonStyle(ReqGlassButtonStyle(variant: glassVariant, cornerRadius: ReqTokens.Radius.control))
        .disabled(!isEnabled || isLoading)
        .opacity(isEnabled ? 1.0 : 0.5)
    }
}

// MARK: - Cohesive Bottom Action Bar

struct ReqBottomActionBar: View {
    let executionState: ExecutionState
    let onQRScan: () -> Void
    var onPasteCurl: (() -> Void)? = nil
    let onSend: () -> Void
    let onCancel: () -> Void

    init(
        executionState: ExecutionState,
        onQRScan: @escaping () -> Void,
        onPasteCurl: (() -> Void)? = nil,
        onSend: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.executionState = executionState
        self.onQRScan = onQRScan
        self.onPasteCurl = onPasteCurl
        self.onSend = onSend
        self.onCancel = onCancel
    }

    private var isLoading: Bool {
        if case .loading = executionState { return true }
        return false
    }

    var body: some View {
        HStack(spacing: ReqTokens.Spacing.xs) {
            // Secondary Action: QR Scanner (Subtle, quiet glass)
            ReqGlassIconButton(icon: "qrcode.viewfinder", size: ReqTokens.Height.control) {
                onQRScan()
            }
            .accessibilityLabel("Scan URL QR Code")

            if let onPasteCurl = onPasteCurl {
                ReqGlassIconButton(icon: "terminal", size: ReqTokens.Height.control) {
                    onPasteCurl()
                }
                .accessibilityLabel("Paste cURL")
            }

            Spacer()

            // Primary Action: Send or Cancel (Prominent liquid glass)
            if isLoading {
                ReqGlassButton(
                    title: "Cancel",
                    icon: nil,
                    variant: .destructive,
                    isLoading: true,
                    height: ReqTokens.Height.control,
                    action: onCancel
                )
                .accessibilityLabel("Cancel request")
            } else {
                ReqGlassButton(
                    title: "Send",
                    icon: "paperplane.fill",
                    variant: .primary(tint: Color.blue),
                    isLoading: false,
                    height: ReqTokens.Height.control,
                    action: onSend
                )
                .accessibilityLabel("Send HTTP request")
            }
        }
        .padding(.horizontal, ReqTokens.Spacing.md)
        .padding(.vertical, ReqTokens.Spacing.xs)
        .reqGlass(.elevated, cornerRadius: ReqTokens.Radius.pill)
    }
}

// MARK: - Response Summary Bar

struct ReqResponseSummaryBar: View {
    let statusCode: Int
    let statusText: String
    let durationMs: Int64
    @Binding var responseTab: WorkspaceViewModel.ResponseTab

    init(
        statusCode: Int,
        statusText: String,
        durationMs: Int64,
        responseTab: Binding<WorkspaceViewModel.ResponseTab>
    ) {
        self.statusCode = statusCode
        self.statusText = statusText
        self.durationMs = durationMs
        self._responseTab = responseTab
    }

    private var statusColor: Color {
        switch statusCode {
        case 200..<300: return ReqTokens.MethodColor.color(for: "GET")
        case 300..<400: return ReqTokens.MethodColor.color(for: "POST")
        case 400..<500: return ReqTokens.MethodColor.color(for: "PUT")
        case 500..<600: return ReqTokens.MethodColor.color(for: "DELETE")
        default: return .secondary
        }
    }

    var body: some View {
        HStack(spacing: ReqTokens.Spacing.sm) {
            // Status Code Badge
            HStack(spacing: ReqTokens.Spacing.xxs) {
                Circle()
                    .fill(statusColor)
                    .frame(width: 7, height: 7)
                Text(statusCode > 0 ? "\(statusCode) \(statusText)" : "No Status")
                    .font(.system(size: 13, weight: .bold, design: .monospaced))
                    .foregroundColor(statusColor)
            }
            .padding(.horizontal, ReqTokens.Spacing.xs)
            .frame(height: 28)
            .background(
                Capsule()
                    .fill(statusColor.opacity(0.12))
            )
            .overlay(
                Capsule()
                    .strokeBorder(statusColor.opacity(0.25), lineWidth: 0.5)
            )

            // Duration badge
            HStack(spacing: 3) {
                Image(systemName: "clock")
                    .font(.system(size: 10))
                Text("\(durationMs) ms")
                    .font(.system(size: 12, weight: .medium, design: .monospaced))
            }
            .foregroundColor(.secondary)

            Spacer()

            // Response tab picker
            ReqGlassSegmentedControl(
                items: WorkspaceViewModel.ResponseTab.allCases,
                selection: $responseTab
            )
            .frame(width: 210)
        }
        .padding(.horizontal, ReqTokens.Spacing.md)
        .padding(.vertical, ReqTokens.Spacing.xs)
        .reqGlass(.standard, cornerRadius: ReqTokens.Radius.card)
    }
}

// MARK: - Request Tab Content Layer (Strictly Readable, No Bubble Overkill)

struct ReqTabContentContainer<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: ReqTokens.Spacing.sm) {
            content
        }
        .padding(ReqTokens.Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .reqGlass(.content, cornerRadius: ReqTokens.Radius.card)
    }
}

// MARK: - Tab Configuration Views (Content Layer)

struct ReqParamsTabView: View {
    @Binding var params: [WorkspaceViewModel.KeyValueItem]

    var body: some View {
        VStack(alignment: .leading, spacing: ReqTokens.Spacing.xs) {
            if params.isEmpty {
                Text("No query parameters configured")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                    .padding(.vertical, ReqTokens.Spacing.xs)
            } else {
                ForEach($params) { $item in
                    HStack(spacing: ReqTokens.Spacing.xs) {
                        Button {
                            item.isEnabled.toggle()
                        } label: {
                            Image(systemName: item.isEnabled ? "checkmark.circle.fill" : "circle")
                                .foregroundColor(item.isEnabled ? .accentColor : .secondary)
                                .font(.system(size: 16))
                        }
                        .buttonStyle(.plain)

                        TextField("Key", text: $item.key)
                            .font(.system(size: 13, design: .monospaced))
                            .padding(.horizontal, ReqTokens.Spacing.xs)
                            .frame(height: 32)
                            .background(
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .fill(Color.primary.opacity(0.04))
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.75)
                            )

                        TextField("Value", text: $item.value)
                            .font(.system(size: 13, design: .monospaced))
                            .padding(.horizontal, ReqTokens.Spacing.xs)
                            .frame(height: 32)
                            .background(
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .fill(Color.primary.opacity(0.04))
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.75)
                            )

                        Button {
                            params.removeAll { $0.id == item.id }
                        } label: {
                            Image(systemName: "minus.circle")
                                .foregroundColor(.secondary)
                                .font(.system(size: 14))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            Button {
                withAnimation(.spring(response: 0.25, dampingFraction: 0.75)) {
                    params.append(WorkspaceViewModel.KeyValueItem(key: "", value: ""))
                }
            } label: {
                HStack(spacing: ReqTokens.Spacing.xxs) {
                    Image(systemName: "plus")
                        .font(.system(size: 12, weight: .bold))
                    Text("Add Parameter")
                        .font(.system(size: 13, weight: .medium))
                }
                .foregroundColor(.accentColor)
                .padding(.horizontal, ReqTokens.Spacing.sm)
                .frame(height: 28)
            }
            .buttonStyle(ReqGlassButtonStyle(variant: .quiet, cornerRadius: ReqTokens.Radius.small))
            .padding(.top, ReqTokens.Spacing.xxs)
        }
        .padding(ReqTokens.Spacing.sm)
        .reqGlass(.content, cornerRadius: ReqTokens.Radius.control)
    }
}

struct ReqHeadersTabView: View {
    @Binding var headers: [WorkspaceViewModel.KeyValueItem]

    var body: some View {
        VStack(alignment: .leading, spacing: ReqTokens.Spacing.xs) {
            if headers.isEmpty {
                Text("No headers configured")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                    .padding(.vertical, ReqTokens.Spacing.xs)
            } else {
                ForEach($headers) { $item in
                    HStack(spacing: ReqTokens.Spacing.xs) {
                        Button {
                            item.isEnabled.toggle()
                        } label: {
                            Image(systemName: item.isEnabled ? "checkmark.circle.fill" : "circle")
                                .foregroundColor(item.isEnabled ? .accentColor : .secondary)
                                .font(.system(size: 16))
                        }
                        .buttonStyle(.plain)

                        TextField("Header", text: $item.key)
                            .font(.system(size: 13, design: .monospaced))
                            .padding(.horizontal, ReqTokens.Spacing.xs)
                            .frame(height: 32)
                            .background(
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .fill(Color.primary.opacity(0.04))
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.75)
                            )

                        TextField("Value", text: $item.value)
                            .font(.system(size: 13, design: .monospaced))
                            .padding(.horizontal, ReqTokens.Spacing.xs)
                            .frame(height: 32)
                            .background(
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .fill(Color.primary.opacity(0.04))
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.75)
                            )

                        Button {
                            headers.removeAll { $0.id == item.id }
                        } label: {
                            Image(systemName: "minus.circle")
                                .foregroundColor(.secondary)
                                .font(.system(size: 14))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            Button {
                withAnimation(.spring(response: 0.25, dampingFraction: 0.75)) {
                    headers.append(WorkspaceViewModel.KeyValueItem(key: "", value: ""))
                }
            } label: {
                HStack(spacing: ReqTokens.Spacing.xxs) {
                    Image(systemName: "plus")
                        .font(.system(size: 12, weight: .bold))
                    Text("Add Header")
                        .font(.system(size: 13, weight: .medium))
                }
                .foregroundColor(.accentColor)
                .padding(.horizontal, ReqTokens.Spacing.sm)
                .frame(height: 28)
            }
            .buttonStyle(ReqGlassButtonStyle(variant: .quiet, cornerRadius: ReqTokens.Radius.small))
            .padding(.top, ReqTokens.Spacing.xxs)
        }
        .padding(ReqTokens.Spacing.sm)
        .reqGlass(.content, cornerRadius: ReqTokens.Radius.control)
    }
}

struct ReqAuthTabView: View {
    @Binding var authType: String
    @Binding var authToken: String
    @SwiftUI.Environment(\.colorScheme) private var colorScheme: ColorScheme
    let authOptions = ["None", "Bearer Token", "Basic Auth", "API Key"]

    var body: some View {
        VStack(alignment: .leading, spacing: ReqTokens.Spacing.sm) {
            // Liquid Glass Auth Type Segmented Bar
            HStack(spacing: 4) {
                ForEach(authOptions, id: \.self) { opt in
                    let isSelected = authType == opt
                    Button {
                        withAnimation(.spring(response: 0.28, dampingFraction: 0.76)) {
                            authType = opt
                        }
                    } label: {
                        Text(opt)
                            .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                            .foregroundColor(isSelected ? .primary : .secondary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                            .frame(maxWidth: .infinity)
                            .frame(height: 30)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .background {
                        if isSelected {
                            RoundedRectangle(cornerRadius: 6, style: .continuous)
                                .fill(colorScheme == .dark ? Color.white.opacity(0.24) : Color.white.opacity(0.92))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                                        .strokeBorder(colorScheme == .dark ? Color.white.opacity(0.40) : Color.white.opacity(0.95), lineWidth: 1.0)
                                )
                                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.35 : 0.08), radius: 4, x: 0, y: 2)
                        }
                    }
                }
            }
            .padding(3)
            .reqGlass(.quiet, cornerRadius: 8)

            if authType == "Bearer Token" {
                VStack(alignment: .leading, spacing: ReqTokens.Spacing.xxs) {
                    Text("Token")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.secondary)
                    TextField("Enter Bearer Token", text: $authToken)
                        .font(.system(size: 13, design: .monospaced))
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled(true)
                        .padding(.horizontal, ReqTokens.Spacing.xs)
                        .frame(height: 36)
                        .background(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .fill(Color.primary.opacity(0.04))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.75)
                        )
                }
            } else if authType == "Basic Auth" {
                VStack(alignment: .leading, spacing: ReqTokens.Spacing.xxs) {
                    Text("Credentials")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.secondary)
                    TextField("username:password", text: $authToken)
                        .font(.system(size: 13, design: .monospaced))
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled(true)
                        .padding(.horizontal, ReqTokens.Spacing.xs)
                        .frame(height: 36)
                        .background(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .fill(Color.primary.opacity(0.04))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.75)
                        )
                }
            } else if authType == "API Key" {
                VStack(alignment: .leading, spacing: ReqTokens.Spacing.xxs) {
                    Text("API Key / Value")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.secondary)
                    TextField("X-API-Key: value", text: $authToken)
                        .font(.system(size: 13, design: .monospaced))
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled(true)
                        .padding(.horizontal, ReqTokens.Spacing.xs)
                        .frame(height: 36)
                        .background(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .fill(Color.primary.opacity(0.04))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.75)
                        )
                }
            } else {
                Text("This request does not use any authorization headers.")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                    .padding(.vertical, ReqTokens.Spacing.xxs)
            }
        }
        .padding(ReqTokens.Spacing.sm)
        .reqGlass(.content, cornerRadius: ReqTokens.Radius.control)
    }
}

// MARK: - GraphQL & Body Types

enum ReqGraphQLSubTab: String, CaseIterable, Identifiable, CustomStringConvertible {
    case query = "Query"
    case variables = "Variables"
    var id: String { rawValue }
    var description: String { rawValue }
}

struct ReqBodyTabView: View {
    @Binding var bodyText: String
    var isGraphQL: Binding<Bool>? = nil
    var variablesText: Binding<String>? = nil
    var schema: GraphQLSchema? = nil
    var isLoadingSchema: Bool = false
    var onFetchSchema: (() -> Void)? = nil
    var onFormatGraphQL: (() -> Void)? = nil
    var onOpenExplorer: (() -> Void)? = nil
    var getCompletions: ((Int) -> [GraphQLCompletionItem])? = nil

    @State private var subTab: ReqGraphQLSubTab = .query
    @SwiftUI.Environment(\.colorScheme) private var colorScheme: ColorScheme

    private var inGraphQLMode: Bool {
        isGraphQL?.wrappedValue ?? false
    }

    var body: some View {
        VStack(alignment: .leading, spacing: ReqTokens.Spacing.xs) {
            // Mode Selector Bar
            HStack(spacing: ReqTokens.Spacing.xs) {
                // Segmented control between JSON and GraphQL
                HStack(spacing: 2) {
                    Button {
                        isGraphQL?.wrappedValue = false
                    } label: {
                        Text("JSON")
                            .font(.system(size: 11, weight: !inGraphQLMode ? .bold : .medium))
                            .foregroundColor(!inGraphQLMode ? .primary : .secondary)
                            .padding(.horizontal, 10)
                            .frame(height: 26)
                            .background {
                                if !inGraphQLMode {
                                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                                        .fill(colorScheme == .dark ? Color.white.opacity(0.18) : Color.white)
                                        .shadow(color: Color.black.opacity(0.06), radius: 2, y: 1)
                                }
                            }
                    }
                    .buttonStyle(.plain)

                    Button {
                        isGraphQL?.wrappedValue = true
                        if bodyText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || bodyText.contains("\"title\":") {
                            bodyText = "query {\n  \n}"
                        }
                        if schema == nil {
                            onFetchSchema?()
                        }
                    } label: {
                        Text("GraphQL")
                            .font(.system(size: 11, weight: inGraphQLMode ? .bold : .medium))
                            .foregroundColor(inGraphQLMode ? .primary : .secondary)
                            .padding(.horizontal, 10)
                            .frame(height: 26)
                            .background {
                                if inGraphQLMode {
                                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                                        .fill(colorScheme == .dark ? Color.white.opacity(0.18) : Color.white)
                                        .shadow(color: Color.black.opacity(0.06), radius: 2, y: 1)
                                }
                            }
                    }
                    .buttonStyle(.plain)
                }
                .padding(2)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.primary.opacity(0.05))
                )

                if inGraphQLMode {
                    // Sub-tab switcher: Query vs Variables
                    Picker("Tab", selection: $subTab) {
                        ForEach(ReqGraphQLSubTab.allCases) { tab in
                            Text(tab.rawValue).tag(tab)
                        }
                    }
                    .pickerStyle(.segmented)
                    .frame(maxWidth: 160)
                }

                Spacer()

                // Actions
                if inGraphQLMode {
                    if subTab == .query {
                        Button {
                            onFormatGraphQL?()
                        } label: {
                            HStack(spacing: 3) {
                                Image(systemName: "wand.and.stars")
                                    .font(.system(size: 10))
                                Text("Prettify")
                                    .font(.system(size: 11, weight: .medium))
                            }
                            .foregroundColor(.accentColor)
                            .padding(.horizontal, ReqTokens.Spacing.xs)
                            .frame(height: 26)
                        }
                        .buttonStyle(ReqGlassButtonStyle(variant: .quiet, cornerRadius: 6))

                        if isLoadingSchema {
                            HStack(spacing: 4) {
                                ProgressView()
                                    .controlSize(.mini)
                                Text("Introspecting...")
                                    .font(.system(size: 10, weight: .medium))
                                    .foregroundColor(.secondary)
                            }
                            .padding(.horizontal, ReqTokens.Spacing.xs)
                            .frame(height: 26)
                        } else if schema != nil {
                            Button {
                                onOpenExplorer?()
                            } label: {
                                HStack(spacing: 4) {
                                    Circle()
                                        .fill(Color(red: 0.18, green: 0.80, blue: 0.44))
                                        .frame(width: 6, height: 6)
                                    Image(systemName: "sparkles")
                                        .font(.system(size: 10))
                                    Text("Explorer")
                                        .font(.system(size: 11, weight: .medium))
                                }
                                .foregroundColor(.primary)
                                .padding(.horizontal, ReqTokens.Spacing.xs)
                                .frame(height: 26)
                            }
                            .buttonStyle(ReqGlassButtonStyle(variant: .quiet, cornerRadius: 6))
                        } else {
                            Button {
                                onFetchSchema?()
                            } label: {
                                HStack(spacing: 3) {
                                    Image(systemName: "arrow.triangle.2.circlepath")
                                        .font(.system(size: 10))
                                    Text("Introspect")
                                        .font(.system(size: 11, weight: .medium))
                                }
                                .foregroundColor(.secondary)
                                .padding(.horizontal, ReqTokens.Spacing.xs)
                                .frame(height: 26)
                            }
                            .buttonStyle(ReqGlassButtonStyle(variant: .quiet, cornerRadius: 6))
                        }
                    }
                } else {
                    Button {
                        if let data = bodyText.data(using: .utf8),
                           let obj = try? JSONSerialization.jsonObject(with: data, options: []),
                           let pretty = try? JSONSerialization.data(withJSONObject: obj, options: [.prettyPrinted, .sortedKeys]),
                           let str = String(data: pretty, encoding: .utf8) {
                            bodyText = str
                        }
                    } label: {
                        Text("Format")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(.accentColor)
                            .padding(.horizontal, ReqTokens.Spacing.xs)
                            .frame(height: 26)
                    }
                    .buttonStyle(ReqGlassButtonStyle(variant: .quiet, cornerRadius: 6))

                    Button {
                        bodyText = ""
                    } label: {
                        Text("Clear")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(.secondary)
                            .padding(.horizontal, ReqTokens.Spacing.xs)
                            .frame(height: 26)
                    }
                    .buttonStyle(ReqGlassButtonStyle(variant: .quiet, cornerRadius: 6))
                }
            }

            // GraphQL Autocomplete Chips Bar
            if inGraphQLMode && subTab == .query {
                let completions = getActiveCompletions()
                if !completions.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(completions.prefix(12), id: \.label) { item in
                                Button {
                                    insertCompletion(item)
                                } label: {
                                    HStack(spacing: 4) {
                                        Image(systemName: iconForKind(item.kind))
                                            .font(.system(size: 9))
                                            .foregroundColor(colorForKind(item.kind))

                                        Text(item.label)
                                            .font(.system(size: 11, weight: .semibold, design: .monospaced))
                                            .foregroundColor(.primary)

                                        if !item.detail.isEmpty {
                                            Text(item.detail)
                                                .font(.system(size: 9, design: .monospaced))
                                                .foregroundColor(.secondary)
                                        }
                                    }
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(
                                        Capsule()
                                            .fill(colorScheme == .dark ? Color.white.opacity(0.08) : Color.black.opacity(0.04))
                                    )
                                    .overlay(
                                        Capsule()
                                            .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.5)
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.vertical, 2)
                    }
                }
            }

            // Editor Area
            if inGraphQLMode && subTab == .variables {
                VStack(alignment: .leading, spacing: 4) {
                    Text("JSON Variables (e.g. { \"id\": \"123\" })")
                        .font(.system(size: 10, weight: .medium, design: .monospaced))
                        .foregroundColor(.secondary)

                    TextEditor(text: Binding(
                        get: { variablesText?.wrappedValue ?? "{}" },
                        set: { variablesText?.wrappedValue = $0 }
                    ))
                    .font(.system(size: 13, design: .monospaced))
                    .autocorrectionDisabled(true)
                    .textInputAutocapitalization(.never)
                    .frame(minHeight: 110, maxHeight: 180)
                    .padding(4)
                    .scrollContentBackground(.hidden)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color.primary.opacity(0.04))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.75)
                    )
                }
            } else {
                TextEditor(text: $bodyText)
                    .font(.system(size: 13, design: .monospaced))
                    .autocorrectionDisabled(true)
                    .textInputAutocapitalization(.never)
                    .frame(minHeight: 110, maxHeight: 180)
                    .padding(4)
                    .scrollContentBackground(.hidden)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color.primary.opacity(0.04))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.75)
                    )
            }
        }
        .padding(ReqTokens.Spacing.sm)
        .reqGlass(.content, cornerRadius: ReqTokens.Radius.control)
    }

    private func getActiveCompletions() -> [GraphQLCompletionItem] {
        if let getCompletions = getCompletions {
            let cursor = bodyText.count
            let items = getCompletions(cursor)
            if !items.isEmpty {
                return items
            }
        }
        return [
            GraphQLCompletionItem(label: "query", insertText: "query {\n  \n}", detail: "Operation", documentation: nil, kind: CompletionKind.keyword),
            GraphQLCompletionItem(label: "mutation", insertText: "mutation {\n  \n}", detail: "Operation", documentation: nil, kind: CompletionKind.keyword),
            GraphQLCompletionItem(label: "__typename", insertText: "__typename", detail: "Field", documentation: nil, kind: CompletionKind.field)
        ]
    }

    private func insertCompletion(_ item: GraphQLCompletionItem) {
        if bodyText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            bodyText = item.insertText
        } else if !bodyText.contains(item.insertText) {
            if bodyText.contains("{") && bodyText.contains("}") {
                if let lastBrace = bodyText.lastIndex(of: "}") {
                    let prefix = bodyText[..<lastBrace]
                    let suffix = bodyText[lastBrace...]
                    bodyText = "\(prefix)  \(item.insertText)\n\(suffix)"
                } else {
                    bodyText += "\n\(item.insertText)"
                }
            } else {
                bodyText += " \(item.insertText)"
            }
        }
        onFormatGraphQL?()
    }

    private func iconForKind(_ kind: CompletionKind) -> String {
        switch kind {
        case .keyword: return "key"
        case .field: return "square.stack.3d.down.forward"
        case .argument: return "slider.horizontal.3"
        case .type: return "tag"
        case .directive: return "at"
        default: return "sparkles"
        }
    }

    private func colorForKind(_ kind: CompletionKind) -> Color {
        switch kind {
        case .keyword: return Color(red: 0.69, green: 0.32, blue: 0.87) // Purple
        case .field: return Color(red: 0.18, green: 0.80, blue: 0.44) // Emerald
        case .argument: return Color(red: 0.12, green: 0.53, blue: 0.98) // Blue
        case .type: return Color(red: 1.0, green: 0.58, blue: 0.0) // Orange
        default: return Color.secondary
        }
    }
}

// MARK: - Liquid Glass GraphQL Schema Explorer Sheet

struct ReqGraphQLSchemaExplorerSheet: View {
    let schema: GraphQLSchema
    let onInsertSnippet: (String) -> Void
    let onDismiss: () -> Void

    @State private var searchQuery: String = ""
    @State private var selectedTab: String = "All"
    @SwiftUI.Environment(\.colorScheme) private var colorScheme: ColorScheme

    private var categories: [SchemaExplorerCategory] {
        GraphQLSchemaExplorer.shared.filter(schema: schema, searchQuery: searchQuery)
    }

    private var availableTabs: [String] {
        let titles = categories.map { $0.title }
        return ["All"] + titles
    }

    private var displayedCategories: [SchemaExplorerCategory] {
        if selectedTab == "All" {
            return categories
        }
        return categories.filter { $0.title == selectedTab }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                ReqAmbientBackdrop()

                VStack(spacing: ReqTokens.Spacing.sm) {
                    // Search Bar
                    HStack(spacing: ReqTokens.Spacing.xs) {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.secondary)
                            .font(.system(size: 14))

                        TextField("Search queries, mutations, types...", text: $searchQuery)
                            .font(.system(size: 14))
                            .autocorrectionDisabled(true)
                            .textInputAutocapitalization(.never)

                        if !searchQuery.isEmpty {
                            Button {
                                searchQuery = ""
                            } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.secondary)
                                    .font(.system(size: 14))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, ReqTokens.Spacing.sm)
                    .padding(.vertical, 8)
                    .background(
                        RoundedRectangle(cornerRadius: ReqTokens.Radius.control, style: .continuous)
                            .fill(Color.primary.opacity(0.04))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: ReqTokens.Radius.control, style: .continuous)
                            .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.75)
                    )
                    .padding(.horizontal, ReqTokens.Spacing.md)

                    // Category Filter Pills
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(availableTabs, id: \.self) { tab in
                                let isSelected = selectedTab == tab
                                Button {
                                    selectedTab = tab
                                } label: {
                                    Text(tab)
                                        .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                                        .foregroundColor(isSelected ? .white : .primary)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(
                                            Capsule()
                                                .fill(isSelected ? Color.accentColor : Color.primary.opacity(0.05))
                                        )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, ReqTokens.Spacing.md)
                    }

                    // Content List
                    if displayedCategories.isEmpty {
                        VStack(spacing: 12) {
                            Spacer()
                            Image(systemName: "doc.text.magnifyingglass")
                                .font(.system(size: 36))
                                .foregroundColor(.secondary)
                            Text("No matching schema items")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(.secondary)
                            Spacer()
                        }
                    } else {
                        ScrollView {
                            LazyVStack(alignment: .leading, spacing: ReqTokens.Spacing.md) {
                                ForEach(displayedCategories, id: \.title) { cat in
                                    VStack(alignment: .leading, spacing: ReqTokens.Spacing.xs) {
                                        Text(cat.title.uppercased())
                                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                                            .foregroundColor(.secondary)
                                            .padding(.horizontal, ReqTokens.Spacing.xs)

                                        ForEach(cat.items, id: \.id) { item in
                                            SchemaItemCard(
                                                item: item,
                                                schema: schema,
                                                onInsert: {
                                                    if let snippet = generateSnippet(for: item) {
                                                        onInsertSnippet(snippet)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            .padding(.horizontal, ReqTokens.Spacing.md)
                            .padding(.bottom, ReqTokens.Spacing.xl)
                        }
                    }
                }
            }
            .navigationTitle("Schema Explorer")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") {
                        onDismiss()
                    }
                }
            }
        }
    }

    private func generateSnippet(for item: SchemaExplorerItem) -> String? {
        guard let field = item.rawField else { return nil }
        let op: String
        if item.kind == .mutation {
            op = "mutation"
        } else if item.kind == .subscription {
            op = "subscription"
        } else {
            op = "query"
        }
        return GraphQLSchemaExplorer.shared.generateSnippet(field: field, schema: schema, operation: op)
    }
}

// MARK: - Schema Item Card

struct SchemaItemCard: View {
    let item: SchemaExplorerItem
    let schema: GraphQLSchema
    let onInsert: () -> Void
    @SwiftUI.Environment(\.colorScheme) private var colorScheme: ColorScheme

    private var kindColor: Color {
        switch item.kind {
        case .query: return Color(red: 0.18, green: 0.80, blue: 0.44)
        case .mutation: return Color(red: 0.12, green: 0.53, blue: 0.98)
        case .subscription: return Color(red: 0.69, green: 0.32, blue: 0.87)
        case .objectType, .interfaceType, .unionType: return Color(red: 1.0, green: 0.58, blue: 0.0)
        case .inputType: return Color(red: 0.95, green: 0.35, blue: 0.55)
        default: return Color(red: 0.40, green: 0.73, blue: 0.85)
        }
    }

    private var kindLabel: String {
        switch item.kind {
        case .query: return "QUERY"
        case .mutation: return "MUTATION"
        case .subscription: return "SUBSCRIPTION"
        case .objectType: return "OBJECT"
        case .inputType: return "INPUT"
        case .enumType: return "ENUM"
        case .scalarType: return "SCALAR"
        case .interfaceType: return "INTERFACE"
        case .unionType: return "UNION"
        default: return "TYPE"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            // Header: Kind Badge + Name + Deprecation + Insert Button
            HStack(spacing: 6) {
                Text(kindLabel)
                    .font(.system(size: 9, weight: .bold, design: .monospaced))
                    .foregroundColor(kindColor)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(kindColor.opacity(0.12))
                    .clipShape(Capsule())

                Text(item.name)
                    .font(.system(size: 13, weight: .bold, design: .monospaced))
                    .foregroundColor(.primary)

                if item.isDeprecated {
                    HStack(spacing: 2) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 9))
                        Text("DEPRECATED")
                            .font(.system(size: 9, weight: .bold))
                    }
                    .foregroundColor(Color(red: 1.0, green: 0.58, blue: 0.0))
                    .padding(.horizontal, 5)
                    .padding(.vertical, 2)
                    .background(Color(red: 1.0, green: 0.58, blue: 0.0).opacity(0.12))
                    .clipShape(Capsule())
                }

                Spacer()

                if item.rawField != nil {
                    Button {
                        onInsert()
                    } label: {
                        HStack(spacing: 3) {
                            Image(systemName: "plus.circle.fill")
                                .font(.system(size: 11))
                            Text("Insert")
                                .font(.system(size: 11, weight: .semibold))
                        }
                        .foregroundColor(.accentColor)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.accentColor.opacity(0.10))
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }

            // Return Type / Detail
            if !item.detail.isEmpty {
                Text(item.detail)
                    .font(.system(size: 12, weight: .medium, design: .monospaced))
                    .foregroundColor(.secondary)
            }

            // Description
            if let desc = item.description_, !desc.isEmpty {
                Text(desc)
                    .font(.system(size: 11))
                    .foregroundColor(.secondary)
                    .lineLimit(3)
            }
        }
        .padding(ReqTokens.Spacing.sm)
        .reqGlass(.content, cornerRadius: ReqTokens.Radius.control)
    }
}

// MARK: - Liquid Glass History Components

struct ReqHistoryRowView: View {
    let entry: HistoryEntry
    let onSelect: () -> Void
    let onDelete: () -> Void

    private var methodStr: String {
        entry.requestMethod.name
    }

    private var methodColor: Color {
        ReqTokens.MethodColor.color(for: methodStr)
    }

    private var statusCode: Int {
        entry.statusCode?.intValue ?? 0
    }

    private var statusColor: Color {
        if statusCode >= 200 && statusCode < 300 {
            return Color(red: 0.18, green: 0.80, blue: 0.44)
        } else if statusCode >= 400 {
            return Color.red
        } else if statusCode >= 300 {
            return Color.orange
        } else {
            return Color.secondary
        }
    }

    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: ReqTokens.Spacing.sm) {
                // Method pill
                Text(methodStr)
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(methodColor)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(methodColor.opacity(0.12))
                    .clipShape(Capsule())

                // URL & Error/Time info
                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.requestUrl)
                        .font(.system(size: 13, weight: .medium, design: .monospaced))
                        .foregroundColor(.primary)
                        .lineLimit(1)
                        .truncationMode(.middle)

                    HStack(spacing: ReqTokens.Spacing.xs) {
                        if let duration = entry.durationMs?.int64Value {
                            Text("\(duration) ms")
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundColor(.secondary)
                        }

                        if let errorMsg = entry.errorMessage, !errorMsg.isEmpty {
                            Text(errorMsg)
                                .font(.system(size: 11))
                                .foregroundColor(.red)
                                .lineLimit(1)
                        }
                    }
                }

                Spacer()

                // Status code chip
                if statusCode > 0 {
                    Text("\(statusCode)")
                        .font(.system(size: 12, weight: .bold, design: .monospaced))
                        .foregroundColor(statusColor)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(statusColor.opacity(0.12))
                        .clipShape(Capsule())
                }

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.secondary.opacity(0.5))
            }
            .padding(ReqTokens.Spacing.sm)
            .reqGlass(.content, cornerRadius: ReqTokens.Radius.card)
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button(role: .destructive, action: onDelete) {
                Label("Delete Entry", systemImage: "trash")
            }
        }
    }
}

struct ReqHistorySheetView: View {
    let entries: [HistoryEntry]
    let onSelect: (HistoryEntry) -> Void
    let onDelete: (String) -> Void
    let onClearAll: () -> Void
    @SwiftUI.Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ZStack {
                ReqAmbientBackdrop()
                    .ignoresSafeArea()

                if entries.isEmpty {
                    ReqEmptyStateView(
                        icon: "clock.arrow.circlepath",
                        title: "No History Yet",
                        subtitle: "Executed requests will be automatically saved to Room database here."
                    )
                    .padding(ReqTokens.Spacing.xl)
                } else {
                    ScrollView {
                        LazyVStack(spacing: ReqTokens.Spacing.xs) {
                            ForEach(entries, id: \.id) { entry in
                                ReqHistoryRowView(
                                    entry: entry,
                                    onSelect: {
                                        onSelect(entry)
                                        dismiss()
                                    },
                                    onDelete: {
                                        onDelete(entry.id)
                                    }
                                )
                            }
                        }
                        .padding(ReqTokens.Spacing.md)
                    }
                }
            }
            .navigationTitle("Request History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    if !entries.isEmpty {
                        Button(role: .destructive, action: onClearAll) {
                            Text("Clear")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(.red)
                        }
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .font(.system(size: 14, weight: .bold))
                }
            }
        }
    }
}

