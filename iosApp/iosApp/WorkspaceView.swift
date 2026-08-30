import SwiftUI
import Shared

struct WorkspaceView: View {
    @StateObject private var viewModel = WorkspaceViewModel()
    @State private var showingQRScanner = false

    var body: some View {
        NavigationSplitView {
            List {
                Text("History / Collections")
                    .foregroundColor(.secondary)
            }
            .navigationTitle("ReqLite")
        } detail: {
            VStack(spacing: 0) {
                // Request Header (Method, URL, Send)
                VStack(spacing: 8) {
                    HStack(spacing: 8) {
                        // Method Selector
                        Menu {
                            ForEach(viewModel.availableMethods, id: \.self) { m in
                                Button(action: { viewModel.method = m }) {
                                    Text(m)
                                }
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Text(viewModel.method)
                                    .font(.system(size: 14, weight: .bold, design: .monospaced))
                                    .foregroundColor(methodColor(for: viewModel.method))
                                Image(systemName: "chevron.down")
                                    .font(.system(size: 10))
                                    .foregroundColor(.secondary)
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 8)
                            .background(methodColor(for: viewModel.method).opacity(0.12))
                            .cornerRadius(8)
                        }

                        // URL Field
                        TextField("Enter request URL", text: $viewModel.url)
                            .font(.system(size: 14, design: .monospaced))
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 8)
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(8)

                    }
                    .padding(.horizontal)
                    .padding(.top, 8)

                    // Request Tabs
                    Picker("Request Tab", selection: $viewModel.selectedTab) {
                        ForEach(WorkspaceViewModel.RequestTab.allCases) { tab in
                            Text(tab.rawValue).tag(tab)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)
                    .padding(.bottom, 8)
                }
                .background(Color(UIColor.systemBackground))

                Divider()

                // Response Area
                ResponseStateView(
                    state: viewModel.executionState,
                    responseTab: $viewModel.responseTab
                )
            }
            .frame(maxWidth: 800)
            .navigationTitle("ReqLite")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItemGroup(placement: .bottomBar) {
                    EnvironmentBadgeView(
                        activeEnvironment: viewModel.activeEnvironment,
                        environments: viewModel.environments,
                        onSelect: { viewModel.selectEnvironment($0) }
                    )
                    
                    if #available(iOS 16.0, *) {
                        Button(action: { showingQRScanner = true }) {
                            Image(systemName: "qrcode.viewfinder")
                        }
                    }
                    
                    Spacer()
                    
                    if case .loading = viewModel.executionState {
                        Button(action: { viewModel.cancelExecution() }) {
                            HStack(spacing: 4) {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                                    .scaleEffect(0.8)
                                Text("Cancel")
                                    .font(.system(size: 13, weight: .semibold))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color.red)
                            .cornerRadius(8)
                        }
                    } else {
                        Button(action: { viewModel.onSendClicked() }) {
                            HStack(spacing: 4) {
                                Image(systemName: "paperplane.fill")
                                    .font(.system(size: 12))
                                Text("Send")
                                    .font(.system(size: 13, weight: .semibold))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(Color.blue)
                            .cornerRadius(8)
                        }
                    }
                }
            }
            .alert("Protected Environment", isPresented: $viewModel.showProtectedWarning) {
                Button("Confirm & Send", role: .destructive) {
                    viewModel.confirmProtectedExecution()
                }
                Button("Cancel", role: .cancel) {
                    viewModel.dismissProtectedWarning()
                }
            } message: {
                Text("You are about to execute a \(viewModel.method) request against the protected '\(viewModel.activeEnvironment?.name ?? "")' environment. This action may modify live production data.")
            }
            .sheet(isPresented: $showingQRScanner) {
                if #available(iOS 16.0, *) {
                    QRScannerView(scannedCode: Binding(
                        get: { nil },
                        set: { newUrl in
                            if let newUrl = newUrl {
                                viewModel.url = newUrl
                                viewModel.method = "GET"
                            }
                        }
                    ))
                }
            }
        }
    }

    private func methodColor(for method: String) -> Color {
        switch method.uppercased() {
        case "GET": return .green
        case "POST": return .blue
        case "PUT": return .orange
        case "DELETE": return .red
        case "PATCH": return .purple
        default: return .secondary
        }
    }
}
