import SwiftUI
import Shared

struct EnvironmentBadgeView: View {
    let activeEnvironment: ReqLiteEnvironment?
    let environments: [ReqLiteEnvironment]
    let onSelect: (ReqLiteEnvironment?) -> Void

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
                            .foregroundColor(.orange)
                        Text(env.name)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.orange)
                        Text("PROTECTED")
                            .font(.system(size: 9, weight: .bold))
                            .padding(.horizontal, 4)
                            .padding(.vertical, 2)
                            .background(Color.orange.opacity(0.2))
                            .cornerRadius(4)
                            .foregroundColor(.orange)
                    } else {
                        Image(systemName: "globe")
                            .foregroundColor(.blue)
                        Text(env.name)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(.primary)
                        Text("(\(env.variables.count))")
                            .font(.system(size: 11))
                            .foregroundColor(.secondary)
                    }
                } else {
                    Image(systemName: "slash.circle")
                        .foregroundColor(.secondary)
                    Text("No Environment")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }

                Image(systemName: "chevron.up.chevron.down")
                    .font(.system(size: 10))
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(8)
        }
    }
}
