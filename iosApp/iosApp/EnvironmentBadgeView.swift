import SwiftUI
import Shared

struct EnvironmentBadgeView: View {
    let activeEnvironment: ReqLiteEnvironment?
    let environments: [ReqLiteEnvironment]
    let onSelect: (ReqLiteEnvironment?) -> Void

    @SwiftUI.Environment(\.colorScheme) private var colorScheme: ColorScheme

    var isProtected: Bool {
        guard let env = activeEnvironment else { return false }
        let lower = env.name.lowercased()
        return lower.contains("prod") || lower.contains("live") || lower.contains("main")
    }

    var body: some View {
        Menu {
            Section("Environments") {
                Button(action: { onSelect(nil) }) {
                    HStack {
                        Text("No Environment")
                        if activeEnvironment == nil {
                            Image(systemName: "checkmark")
                        }
                    }
                }

                ForEach(environments, id: \.id) { env in
                    Button(action: { onSelect(env) }) {
                        HStack {
                            Text(env.name)
                            if activeEnvironment?.id == env.id {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                }
            }
        } label: {
            HStack(spacing: ReqTokens.Spacing.xs) {
                if let env = activeEnvironment {
                    if isProtected {
                        Image(systemName: "shield.fill")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.orange)
                        Text(env.name)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.orange)
                            .lineLimit(1)
                        Text("PROD")
                            .font(.system(size: 9, weight: .heavy))
                            .padding(.horizontal, 4)
                            .padding(.vertical, 1.5)
                            .background(
                                Capsule()
                                    .fill(Color.orange.opacity(colorScheme == .dark ? 0.35 : 0.18))
                            )
                            .foregroundColor(.orange)
                    } else {
                        Image(systemName: "globe")
                            .font(.system(size: 12))
                            .foregroundColor(.accentColor)
                        Text(env.name)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(.primary)
                            .lineLimit(1)
                        Text("(\(env.variables.count))")
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundColor(.secondary)
                    }
                } else {
                    Image(systemName: "slash.circle")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                    Text("No Environment")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }

                Image(systemName: "chevron.up.chevron.down")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundColor(.secondary.opacity(0.8))
                
            }
            .padding(.horizontal, ReqTokens.Spacing.sm)
            .frame(height: ReqTokens.Height.compact)
            .reqGlass(isProtected ? .prominent(tint: .orange) : .quiet, cornerRadius: ReqTokens.Radius.pill)
        }
    }
}
