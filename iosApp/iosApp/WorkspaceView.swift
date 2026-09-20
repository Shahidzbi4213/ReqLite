import SwiftUI
import Shared

struct WorkspaceView: View {
    @StateObject private var viewModel = WorkspaceViewModel()
    @State private var showingQRScanner = false
    @State private var showingCurlSheet = false
    @State private var selectedItem: String? = "workspace"

    var body: some View {
        NavigationSplitView {
            List(selection: $selectedItem) {
                NavigationLink(value: "workspace") {
                    Label("New Request", systemImage: "plus.circle")
                        .font(.system(size: 15, weight: .medium))
                        .padding(.vertical, ReqTokens.Spacing.xs)
                }

                Section("Workspaces") {
                    Button {
                        viewModel.showingHistorySheet = true
                    } label: {
                        HStack {
                            Label("History", systemImage: "clock")
                                .foregroundColor(.primary)
                            Spacer()
                            if !viewModel.historyEntries.isEmpty {
                                Text("\(viewModel.historyEntries.count)")
                                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Color.accentColor)
                                    .clipShape(Capsule())
                            }
                        }
                    }
                    .buttonStyle(.plain)
                    .padding(.vertical, ReqTokens.Spacing.xxs)

                    HStack {
                        Label("Collections", systemImage: "folder")
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                    .padding(.vertical, ReqTokens.Spacing.xxs)
                }

                if !viewModel.historyEntries.isEmpty {
                    Section("Recent History") {
                        ForEach(viewModel.historyEntries.prefix(5), id: \.id) { entry in
                            Button {
                                viewModel.loadHistoryEntry(entry)
                            } label: {
                                VStack(alignment: .leading, spacing: 2) {
                                    HStack {
                                        Text(entry.requestMethod.name)
                                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                                            .foregroundColor(ReqTokens.MethodColor.color(for: entry.requestMethod.name))
                                        Spacer()
                                        if let code = entry.statusCode?.intValue, code > 0 {
                                            Text("\(code)")
                                                .font(.system(size: 10, weight: .semibold, design: .monospaced))
                                                .foregroundColor(code < 400 ? Color.green : Color.red)
                                        }
                                    }
                                    Text(entry.requestUrl)
                                        .font(.system(size: 12, design: .monospaced))
                                        .foregroundColor(.secondary)
                                        .lineLimit(1)
                                        .truncationMode(.middle)
                                }
                                .padding(.vertical, 2)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .navigationTitle("ReqLite")
        } detail: {
            NavigationStack {
                ZStack {
                    // Fluid Liquid Glass Ambient Backdrop
                    ReqAmbientBackdrop()
                        .ignoresSafeArea()

                    ScrollView {
                        VStack(spacing: ReqTokens.Spacing.md) {
                            // MARK: - Layer 2: Unified Request Composer
                            ReqRequestComposer(
                                method: $viewModel.method,
                                url: $viewModel.url,
                                availableMethods: viewModel.availableMethods,
                                onCommit: {
                                    viewModel.onSendClicked()
                                }
                            )
                            .padding(.horizontal, ReqTokens.Spacing.md)
                            .padding(.top, ReqTokens.Spacing.sm)

                            // MARK: - Layer 2: Request Tabs (Liquid Glass Segmented Control)
                            ReqGlassSegmentedControl(
                                items: WorkspaceViewModel.RequestTab.allCases,
                                selection: $viewModel.selectedTab
                            )
                            .padding(.horizontal, ReqTokens.Spacing.md)

                            // MARK: - Layer 1: Request Tab Content (Luminous Frosted Glass Card)
                            Group {
                                switch viewModel.selectedTab {
                                case .params:
                                    ReqParamsTabView(params: $viewModel.queryParams)
                                case .headers:
                                    ReqHeadersTabView(headers: $viewModel.headers)
                                case .auth:
                                    ReqAuthTabView(authType: $viewModel.authType, authToken: $viewModel.authToken)
                                case .body:
                                    ReqBodyTabView(bodyText: $viewModel.requestBody)
                                }
                            }
                            .padding(.horizontal, ReqTokens.Spacing.md)

                            // MARK: - Response Area (Liquid Glass Layer)
                            ResponseStateView(
                                state: viewModel.executionState,
                                responseTab: $viewModel.responseTab
                            )
                            .padding(.horizontal, ReqTokens.Spacing.md)
                            .padding(.bottom, ReqTokens.Spacing.xl * 2)
                        }
                        .frame(maxWidth: 800)
                        .frame(maxWidth: .infinity)
                    }
                    .scrollContentBackground(.hidden)
                }
                .navigationTitle("ReqLite")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button {
                            viewModel.showingHistorySheet = true
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "clock.arrow.circlepath")
                                    .font(.system(size: 14, weight: .semibold))
                                if !viewModel.historyEntries.isEmpty {
                                    Text("\(viewModel.historyEntries.count)")
                                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                                }
                            }
                            .foregroundColor(.primary)
                            .padding(.horizontal, ReqTokens.Spacing.xs)
                            .padding(.vertical, 4)
                            .reqGlass(.quiet, cornerRadius: ReqTokens.Radius.pill)
                        }
                        .buttonStyle(.plain)
                    }

                    // Environment Badge in navigation bar
                    ToolbarItem(placement: .navigationBarTrailing) {
                        EnvironmentBadgeView(
                            activeEnvironment: viewModel.activeEnvironment,
                            environments: viewModel.environments,
                            onSelect: { env in
                                viewModel.selectEnvironment(env)
                            }
                        )
                    }
                }
                .safeAreaInset(edge: .bottom) {
                    // Floating Liquid Glass Action Bar
                    ReqBottomActionBar(
                        executionState: viewModel.executionState,
                        onQRScan: {
                            showingQRScanner = true
                        },
                        onPasteCurl: {
                            showingCurlSheet = true
                        },
                        onSend: {
                            viewModel.onSendClicked()
                        },
                        onCancel: {
                            viewModel.cancelExecution()
                        }
                    )
                    .padding(.horizontal, ReqTokens.Spacing.md)
                    .padding(.bottom, ReqTokens.Spacing.xs)
                }
                .navigationDestination(isPresented: Binding(
                    get: {
                        if case .success = viewModel.executionState { return true }
                        return false
                    },
                    set: { isPresenting in
                        if !isPresenting {
                            viewModel.executionState = .idle
                        }
                    }
                )) {
                    if case .success(let history, let responseBody, let responseHeaders) = viewModel.executionState {
                        FullScreenResponseView(
                            history: history,
                            responseBody: responseBody,
                            responseHeaders: responseHeaders,
                            responseTab: $viewModel.responseTab
                        )
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
                            set: { scannedCode in
                                if let code = scannedCode {
                                    viewModel.applySmartPayload(code)
                                }
                            }
                        ))
                    }
                }
                .sheet(isPresented: $showingCurlSheet) {
                    ReqPasteCurlSheet(isPresented: $showingCurlSheet) { curl in
                        viewModel.applyCurl(curl)
                    }
                }
                .sheet(isPresented: $viewModel.showingHistorySheet) {
                    ReqHistorySheetView(
                        entries: viewModel.historyEntries,
                        onSelect: { entry in
                            viewModel.loadHistoryEntry(entry)
                        },
                        onDelete: { id in
                            viewModel.deleteHistoryEntry(id)
                        },
                        onClearAll: {
                            viewModel.clearAllHistory()
                        }
                    )
                }
            }
        }
    }
}

// MARK: - Paste cURL Sheet
struct ReqPasteCurlSheet: View {
    @Binding var isPresented: Bool
    let onImport: (String) -> Void
    @State private var curlText: String = ""

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: ReqTokens.Spacing.sm) {
                Text("Paste a raw cURL command to automatically populate the URL, method, headers, auth token, and request body.")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                    .padding(.horizontal, ReqTokens.Spacing.md)
                    .padding(.top, ReqTokens.Spacing.sm)

                TextEditor(text: $curlText)
                    .font(.system(size: 13, design: .monospaced))
                    .padding(ReqTokens.Spacing.xs)
                    .background(Color(uiColor: .secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: ReqTokens.Radius.control, style: .continuous))
                    .padding(.horizontal, ReqTokens.Spacing.md)
                    .frame(minHeight: 200)

                HStack {
                    Button(action: {
                        if let clip = UIPasteboard.general.string {
                            curlText = clip
                        }
                    }) {
                        Label("Paste Clipboard", systemImage: "doc.on.clipboard")
                            .font(.system(size: 13, weight: .medium))
                    }
                    .buttonStyle(.bordered)

                    Spacer()

                    if !curlText.isEmpty {
                        Button("Clear") {
                            curlText = ""
                        }
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal, ReqTokens.Spacing.md)

                Spacer()
            }
            .navigationTitle("Import cURL")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        isPresented = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Import") {
                        onImport(curlText)
                        isPresented = false
                    }
                    .fontWeight(.bold)
                    .disabled(curlText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .onAppear {
                if let clip = UIPasteboard.general.string, clip.trimmingCharacters(in: .whitespaces).lowercased().hasPrefix("curl") {
                    curlText = clip
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
