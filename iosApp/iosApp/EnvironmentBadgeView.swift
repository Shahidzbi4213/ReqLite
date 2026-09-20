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
            HStack(spacing: 6) {
                if let env = activeEnvironment {
                    if isProtected {
                        Image(systemName: "shield.fill")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.orange)

                        Text(env.name)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.orange)
                            .lineLimit(1)

                        Text("PROD")
                            .font(.system(size: 8, weight: .heavy))
                            .padding(.horizontal, 4)
                            .padding(.vertical, 1.5)
                            .background(
                                Capsule()
                                    .fill(Color.orange.opacity(colorScheme == .dark ? 0.35 : 0.18))
                            )
                            .foregroundColor(.orange)
                    } else {
                        Circle()
                            .fill(Color.green)
                            .frame(width: 6, height: 6)

                        Text(env.name)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.primary)
                            .lineLimit(1)
                    }
                } else {
                    Image(systemName: "globe")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.secondary)

                    Text("No Environment")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }

                Image(systemName: "chevron.down")
                    .font(.system(size: 8, weight: .bold))
                    .foregroundColor(isProtected ? .orange.opacity(0.8) : .secondary.opacity(0.6))
            }
            .padding(.horizontal, 10)
            .frame(height: 28)
            .background(
                isProtected
                    ? Color.orange.opacity(colorScheme == .dark ? 0.22 : 0.12)
                    : Color(uiColor: .secondarySystemFill)
            )
            .overlay(
                Capsule(style: .continuous)
                    .strokeBorder(
                        isProtected
                            ? Color.orange.opacity(0.35)
                            : Color.primary.opacity(0.08),
                        lineWidth: 0.5
                    )
            )
            .clipShape(Capsule(style: .continuous))
        }
    }
}
