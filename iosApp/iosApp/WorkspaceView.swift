import SwiftUI
import Shared

struct WorkspaceView: View {
    @StateObject private var viewModel = WorkspaceViewModel()
    @State private var showingQRScanner = false
    @State private var showingCurlSheet = false
    @State private var newCollectionName = ""
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
                }

                Section("Collections") {
                    if viewModel.collections.isEmpty {
                        Text("No collections yet")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                            .padding(.vertical, ReqTokens.Spacing.xxs)
                    } else {
                        ForEach(viewModel.collections, id: \.id) { collection in
                            DisclosureGroup(
                                isExpanded: Binding(
                                    get: { viewModel.expandedCollectionIds.contains(collection.id) },
                                    set: { isExpanded in
                                        if isExpanded {
                                            viewModel.expandedCollectionIds.insert(collection.id)
                                        } else {
                                            viewModel.expandedCollectionIds.remove(collection.id)
                                        }
                                    }
                                )
                            ) {
                                let colRequests = viewModel.requestsForCollection(collection.id)
                                if colRequests.isEmpty {
                                    Text("No saved requests")
                                        .font(.system(size: 11))
                                        .foregroundColor(.secondary)
                                        .padding(.leading, 8)
                                        .padding(.vertical, 2)
                                } else {
                                    ForEach(colRequests, id: \.id) { req in
                                        Button {
                                            viewModel.loadSavedRequest(req)
                                        } label: {
                                            HStack(spacing: 6) {
                                                Text(req.method.name)
                                                    .font(.system(size: 9, weight: .bold, design: .monospaced))
                                                    .foregroundColor(ReqTokens.MethodColor.color(for: req.method.name))
                                                    .frame(width: 34, alignment: .leading)
                                                Text(req.name.isEmpty ? req.url : req.name)
                                                    .font(.system(size: 12))
                                                    .lineLimit(1)
                                                    .truncationMode(.middle)
                                                Spacer()
                                            }
                                            .padding(.vertical, 2)
                                        }
                                        .buttonStyle(.plain)
                                        .contextMenu {
                                            Button(role: .destructive) {
                                                viewModel.deleteRequest(req.id)
                                            } label: {
                                                Label("Delete Request", systemImage: "trash")
                                            }
                                        }
                                    }
                                }
                            } label: {
                                HStack {
                                    Label(collection.name, systemImage: "folder.fill")
                                        .font(.system(size: 13, weight: .medium))
                                    Spacer()
                                    let count = viewModel.requestsForCollection(collection.id).count
                                    if count > 0 {
                                        Text("\(count)")
                                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                                            .foregroundColor(.secondary)
                                            .padding(.horizontal, 5)
                                            .padding(.vertical, 1)
                                            .background(Color.secondary.opacity(0.15))
                                            .clipShape(Capsule())
                                    }
                                }
                            }
                            .contextMenu {
                                Button(role: .destructive) {
                                    viewModel.deleteCollection(collection.id)
                                } label: {
                                    Label("Delete Collection", systemImage: "trash")
                                }
                            }
                        }
                    }

                    HStack {
                        Button {
                            viewModel.showingNewCollectionAlert = true
                        } label: {
                            Label("New Collection", systemImage: "plus")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(Color.accentColor)
                        }
                        .buttonStyle(.plain)

                        Spacer()

                        Button {
                            viewModel.showingImportPostmanSheet = true
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "square.and.arrow.down")
                                    .font(.system(size: 12, weight: .medium))
                                Text("Import")
                                    .font(.system(size: 12, weight: .medium))
                            }
                            .foregroundColor(Color.accentColor)
                        }
                        .buttonStyle(.plain)
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
                .navigationTitle("")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .principal) {
                        EnvironmentBadgeView(
                            activeEnvironment: viewModel.activeEnvironment,
                            environments: viewModel.environments,
                            onSelect: { env in
                                viewModel.selectEnvironment(env)
                            }
                        )
                    }

                    ToolbarItemGroup(placement: .navigationBarTrailing) {
                        Button {
                            viewModel.showingHistorySheet = true
                        } label: {
                            Image(systemName: "clock.arrow.circlepath")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(.primary)
                                .frame(width: 32, height: 32)
                                .background(Color(uiColor: .secondarySystemFill))
                                .clipShape(Circle())
                                .overlay(
                                    Circle()
                                        .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.5)
                                )
                                .overlay(alignment: .topTrailing) {
                                    if !viewModel.historyEntries.isEmpty {
                                        Circle()
                                            .fill(Color.accentColor)
                                            .frame(width: 7, height: 7)
                                            .offset(x: 1, y: -1)
                                    }
                                }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Request History")

                        Button {
                            viewModel.showingSaveCollectionSheet = true
                        } label: {
                            Image(systemName: "folder.badge.plus")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(.primary)
                                .frame(width: 32, height: 32)
                                .background(Color(uiColor: .secondarySystemFill))
                                .clipShape(Circle())
                                .overlay(
                                    Circle()
                                        .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.5)
                                )
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Save to Collection")
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
                .sheet(isPresented: $viewModel.showingSaveCollectionSheet) {
                    ReqSaveToCollectionSheet(
                        isPresented: $viewModel.showingSaveCollectionSheet,
                        collections: viewModel.collections,
                        initialRequestName: viewModel.url,
                        onSave: { colId, reqName in
                            viewModel.saveCurrentRequestToCollection(collectionId: colId, name: reqName)
                        },
                        onCreateCollection: { name, completion in
                            viewModel.createCollection(name: name) { newCol in
                                completion(newCol.id)
                            }
                        }
                    )
                }
                .sheet(isPresented: $viewModel.showingImportPostmanSheet) {
                    ReqImportPostmanSheet(
                        isPresented: $viewModel.showingImportPostmanSheet,
                        onPreview: { json, completion in
                            viewModel.previewPostmanCollection(json, completion: completion)
                        },
                        onImport: { json, completion in
                            viewModel.importPostmanCollection(json, completion: completion)
                        }
                    )
                }
                .alert("New Collection", isPresented: $viewModel.showingNewCollectionAlert) {
                    TextField("Collection Name", text: $newCollectionName)
                    Button("Create") {
                        let trimmed = newCollectionName.trimmingCharacters(in: .whitespacesAndNewlines)
                        if !trimmed.isEmpty {
                            viewModel.createCollection(name: trimmed)
                            newCollectionName = ""
                        }
                    }
                    Button("Cancel", role: .cancel) {
                        newCollectionName = ""
                    }
                } message: {
                    Text("Enter a name for your new collection.")
                }
            }
        }
    }
}

// MARK: - Save to Collection Sheet
struct ReqSaveToCollectionSheet: View {
    @Binding var isPresented: Bool
    let collections: [ReqLiteCollection]
    let initialRequestName: String
    let onSave: (String, String) -> Void
    let onCreateCollection: (String, @escaping (String) -> Void) -> Void

    @State private var requestName: String = ""
    @State private var selectedCollectionId: String = ""
    @State private var isCreatingNew: Bool = false
    @State private var newCollectionName: String = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Request Details") {
                    TextField("Request Name", text: $requestName)
                        .font(.system(size: 14))
                }

                Section("Target Collection") {
                    if collections.isEmpty || isCreatingNew {
                        VStack(alignment: .leading, spacing: 6) {
                            TextField("New Collection Name", text: $newCollectionName)
                                .font(.system(size: 14))

                            if !collections.isEmpty {
                                Button("Choose existing collection") {
                                    isCreatingNew = false
                                }
                                .font(.system(size: 12))
                                .foregroundColor(Color.accentColor)
                            }
                        }
                    } else {
                        Picker("Collection", selection: $selectedCollectionId) {
                            ForEach(collections, id: \.id) { col in
                                Text(col.name).tag(col.id)
                            }
                        }
                        .pickerStyle(.menu)

                        Button {
                            isCreatingNew = true
                        } label: {
                            Label("Create New Collection", systemImage: "plus")
                                .font(.system(size: 13))
                                .foregroundColor(Color.accentColor)
                        }
                    }
                }
            }
            .navigationTitle("Save Request")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        isPresented = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let finalName = requestName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                            ? initialRequestName
                            : requestName.trimmingCharacters(in: .whitespacesAndNewlines)

                        if isCreatingNew || collections.isEmpty {
                            let newName = newCollectionName.trimmingCharacters(in: .whitespacesAndNewlines)
                            if !newName.isEmpty {
                                onCreateCollection(newName) { newId in
                                    onSave(newId, finalName)
                                    isPresented = false
                                }
                            }
                        } else {
                            if !selectedCollectionId.isEmpty {
                                onSave(selectedCollectionId, finalName)
                                isPresented = false
                            }
                        }
                    }
                    .fontWeight(.bold)
                    .disabled(
                        (isCreatingNew || collections.isEmpty)
                            ? newCollectionName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                            : selectedCollectionId.isEmpty
                    )
                }
            }
            .onAppear {
                requestName = initialRequestName
                if let first = collections.first {
                    selectedCollectionId = first.id
                } else {
                    isCreatingNew = true
                }
            }
        }
        .presentationDetents([.medium, .large])
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

// MARK: - Postman Collection Import Sheet
struct ReqImportPostmanSheet: View {
    @Binding var isPresented: Bool
    let onPreview: (String, @escaping (String?, Int, Int, String?) -> Void) -> Void
    let onImport: (String, @escaping (Bool, String) -> Void) -> Void

    @State private var jsonText: String = ""
    @State private var previewName: String? = nil
    @State private var previewRequestsCount: Int = 0
    @State private var previewFoldersCount: Int = 0
    @State private var previewError: String? = nil
    @State private var isImporting: Bool = false
    @State private var errorMessage: String? = nil

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: ReqTokens.Spacing.sm) {
                    Text("Paste a Postman Collection (v2.0 or v2.1 JSON) to import its requests, nested folders, headers, and parameters.")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                        .padding(.horizontal, ReqTokens.Spacing.md)
                        .padding(.top, ReqTokens.Spacing.sm)

                    TextEditor(text: $jsonText)
                        .font(.system(size: 13, design: .monospaced))
                        .padding(ReqTokens.Spacing.xs)
                        .background(Color(uiColor: .secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: ReqTokens.Radius.control, style: .continuous))
                        .padding(.horizontal, ReqTokens.Spacing.md)
                        .frame(minHeight: 180, maxHeight: 260)
                        .onChange(of: jsonText) { newValue in
                            runPreview(newValue)
                        }

                    HStack {
                        Button(action: {
                            if let clip = UIPasteboard.general.string {
                                jsonText = clip
                            }
                        }) {
                            Label("Paste Clipboard", systemImage: "doc.on.clipboard")
                                .font(.system(size: 13, weight: .medium))
                        }
                        .buttonStyle(.bordered)

                        Spacer()

                        if !jsonText.isEmpty {
                            Button("Clear") {
                                jsonText = ""
                            }
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)
                        }
                    }
                    .padding(.horizontal, ReqTokens.Spacing.md)

                    // Preview Card or Error
                    if let name = previewName {
                        VStack(alignment: .leading, spacing: ReqTokens.Spacing.xs) {
                            HStack(spacing: 6) {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                                    .font(.system(size: 16))
                                Text("Valid Postman Collection")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(.primary)
                            }
                            Text(name)
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(.primary)

                            HStack(spacing: 8) {
                                HStack(spacing: 4) {
                                    Image(systemName: "arrow.up.right.circle")
                                        .font(.system(size: 11))
                                    Text("\(previewRequestsCount) requests")
                                        .font(.system(size: 12, weight: .medium))
                                }
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.accentColor.opacity(0.12))
                                .foregroundColor(Color.accentColor)
                                .clipShape(Capsule())

                                if previewFoldersCount > 0 {
                                    HStack(spacing: 4) {
                                        Image(systemName: "folder")
                                            .font(.system(size: 11))
                                        Text("\(previewFoldersCount) folders")
                                            .font(.system(size: 12, weight: .medium))
                                    }
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.secondary.opacity(0.12))
                                    .foregroundColor(.secondary)
                                    .clipShape(Capsule())
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(ReqTokens.Spacing.sm)
                        .background(Color.green.opacity(0.08))
                        .overlay(
                            RoundedRectangle(cornerRadius: ReqTokens.Radius.control)
                                .stroke(Color.green.opacity(0.25), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: ReqTokens.Radius.control))
                        .padding(.horizontal, ReqTokens.Spacing.md)
                        .padding(.top, ReqTokens.Spacing.xs)
                    } else if let error = previewError, !jsonText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 6) {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(.orange)
                                    .font(.system(size: 14))
                                Text("Unrecognized or Invalid Collection")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(.orange)
                            }
                            Text(error)
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(ReqTokens.Spacing.sm)
                        .background(Color.orange.opacity(0.08))
                        .overlay(
                            RoundedRectangle(cornerRadius: ReqTokens.Radius.control)
                                .stroke(Color.orange.opacity(0.25), lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: ReqTokens.Radius.control))
                        .padding(.horizontal, ReqTokens.Spacing.md)
                        .padding(.top, ReqTokens.Spacing.xs)
                    }

                    if let err = errorMessage {
                        Text(err)
                            .font(.system(size: 12))
                            .foregroundColor(.red)
                            .padding(.horizontal, ReqTokens.Spacing.md)
                    }

                    Spacer(minLength: ReqTokens.Spacing.xl)
                }
            }
            .navigationTitle("Import Postman")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        isPresented = false
                    }
                    .disabled(isImporting)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isImporting {
                        ProgressView()
                            .progressViewStyle(.circular)
                    } else {
                        Button("Import") {
                            startImport()
                        }
                        .fontWeight(.bold)
                        .disabled(previewName == nil)
                    }
                }
            }
            .onAppear {
                if let clip = UIPasteboard.general.string, clip.contains("\"_postman_id\"") || clip.contains("\"info\"") {
                    jsonText = clip
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func runPreview(_ text: String) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            previewName = nil
            previewRequestsCount = 0
            previewFoldersCount = 0
            previewError = nil
            errorMessage = nil
            return
        }

        onPreview(trimmed) { name, reqs, folders, error in
            Task { @MainActor in
                if let error = error {
                    self.previewName = nil
                    self.previewRequestsCount = 0
                    self.previewFoldersCount = 0
                    self.previewError = error
                } else {
                    self.previewName = name
                    self.previewRequestsCount = reqs
                    self.previewFoldersCount = folders
                    self.previewError = nil
                }
            }
        }
    }

    private func startImport() {
        let trimmed = jsonText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        isImporting = true
        errorMessage = nil

        onImport(trimmed) { success, message in
            Task { @MainActor in
                isImporting = false
                if success {
                    isPresented = false
                } else {
                    errorMessage = message
                }
            }
        }
    }
}

