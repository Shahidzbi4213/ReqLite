import Foundation
import SwiftUI
import Shared

public typealias ReqLiteEnvironment = Shared.Environment

enum ExecutionState {
    case idle
    case loading
    case success(HistoryEntry, String)
    case error(String)
}

@MainActor
class WorkspaceViewModel: ObservableObject {
    @Published var method: String = "GET"
    @Published var url: String = "https://jsonplaceholder.typicode.com/todos/1"
    @Published var environments: [ReqLiteEnvironment] = []
    @Published var activeEnvironment: ReqLiteEnvironment? = nil
    @Published var executionState: ExecutionState = .idle
    @Published var showProtectedWarning: Bool = false
    @Published var selectedTab: RequestTab = .params
    @Published var responseTab: ResponseTab = .pretty

    struct KeyValueItem: Identifiable, Equatable {
        let id = UUID()
        var key: String
        var value: String
        var isEnabled: Bool = true
    }

    @Published var queryParams: [KeyValueItem] = [
        KeyValueItem(key: "userId", value: "1")
    ]
    @Published var headers: [KeyValueItem] = [
        KeyValueItem(key: "Accept", value: "application/json")
    ]
    @Published var authType: String = "None"
    @Published var authToken: String = ""
    @Published var requestBody: String = "{\n  \"title\": \"foo\",\n  \"completed\": false\n}"

    enum RequestTab: String, CaseIterable, Identifiable, CustomStringConvertible {
        case params = "Params"
        case headers = "Headers"
        case auth = "Auth"
        case body = "Body"
        var id: String { rawValue }
        var description: String { rawValue }
    }

    enum ResponseTab: String, CaseIterable, Identifiable, CustomStringConvertible {
        case pretty = "Pretty"
        case raw = "Raw"
        case headers = "Headers"
        var id: String { rawValue }
        var description: String { rawValue }
    }

    let availableMethods = ["GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"]

    @Published var historyEntries: [HistoryEntry] = []
    @Published var showingHistorySheet: Bool = false

    private var adapter: IosWorkspaceAdapter? = nil

    init() {
        self.adapter = IosWorkspaceAdapter()
        observeEnvironments()
        observeHistory()
    }

    deinit {
        adapter?.close()
    }

    private func observeEnvironments() {
        adapter?.observeEnvironments { [weak self] envList in
            Task { @MainActor in
                self?.environments = envList
                if let current = self?.activeEnvironment {
                    self?.activeEnvironment = envList.first(where: { $0.id == current.id })
                }
            }
        }
    }

    private func observeHistory() {
        adapter?.observeHistory { [weak self] entries in
            Task { @MainActor in
                self?.historyEntries = entries
            }
        }
    }

    func loadHistoryEntry(_ entry: HistoryEntry) {
        self.method = entry.requestMethod.name
        self.url = entry.requestUrl
        self.showingHistorySheet = false
    }

    func deleteHistoryEntry(_ id: String) {
        adapter?.deleteHistoryEntry(id: id)
    }

    func clearAllHistory() {
        adapter?.clearHistory()
    }

    func selectEnvironment(_ env: ReqLiteEnvironment?) {
        self.activeEnvironment = env
    }

    var isCurrentEnvironmentProtected: Bool {
        guard let env = activeEnvironment else { return false }
        let lower = env.name.lowercased()
        return lower.contains("prod") || lower.contains("live") || lower.contains("main")
    }

    func onSendClicked() {
        if isCurrentEnvironmentProtected {
            showProtectedWarning = true
        } else {
            executeRequest()
        }
    }

    func confirmProtectedExecution() {
        showProtectedWarning = false
        executeRequest()
    }

    func dismissProtectedWarning() {
        showProtectedWarning = false
    }

    private func executeRequest() {
        guard let adapter = adapter else { return }
        
        self.executionState = .loading

        var compiledHeaders: [RequestField] = []
        for h in headers where h.isEnabled && !h.key.trimmingCharacters(in: .whitespaces).isEmpty {
            compiledHeaders.append(
                RequestField(
                    id: UUID().uuidString,
                    key: h.key,
                    value: h.value,
                    isEnabled: true,
                    description: nil
                )
            )
        }

        if authType == "Bearer Token" && !authToken.isEmpty {
            compiledHeaders.append(
                RequestField(
                    id: UUID().uuidString,
                    key: "Authorization",
                    value: "Bearer \(authToken)",
                    isEnabled: true,
                    description: nil
                )
            )
        } else if authType == "Basic Auth" && !authToken.isEmpty {
            let base64Auth = Data(authToken.utf8).base64EncodedString()
            compiledHeaders.append(
                RequestField(
                    id: UUID().uuidString,
                    key: "Authorization",
                    value: "Basic \(base64Auth)",
                    isEnabled: true,
                    description: nil
                )
            )
        } else if authType == "API Key" && !authToken.isEmpty {
            let parts = authToken.split(separator: ":", maxSplits: 1).map(String.init)
            if parts.count == 2 {
                compiledHeaders.append(
                    RequestField(
                        id: UUID().uuidString,
                        key: parts[0].trimmingCharacters(in: .whitespaces),
                        value: parts[1].trimmingCharacters(in: .whitespaces),
                        isEnabled: true,
                        description: nil
                    )
                )
            }
        }

        var compiledParams: [RequestField] = []
        for p in queryParams where p.isEnabled && !p.key.trimmingCharacters(in: .whitespaces).isEmpty {
            compiledParams.append(
                RequestField(
                    id: UUID().uuidString,
                    key: p.key,
                    value: p.value,
                    isEnabled: true,
                    description: nil
                )
            )
        }

        let bodyToSend: String? = (method == "POST" || method == "PUT" || method == "PATCH") ? requestBody : nil

        adapter.createDraftWithDetails(
            methodName: method,
            url: url,
            headers: compiledHeaders,
            queryParams: compiledParams,
            bodyContent: bodyToSend
        ) { [weak self] draftId in
            Task { @MainActor in
                guard let self = self else { return }
                adapter.executeRequest(
                    draftId: draftId,
                    environmentId: self.activeEnvironment?.id
                ) { state in
                    Task { @MainActor in
                        if state is IosExecutionState.Loading {
                            self.executionState = .loading
                        } else if let success = state as? IosExecutionState.Success {
                            self.executionState = .success(success.result, success.responseBody)
                        } else if let err = state as? IosExecutionState.Error {
                            self.executionState = .error(err.message)
                        } else {
                            self.executionState = .idle
                        }
                    }
                }
            }
        }
    }

    func cancelExecution() {
        adapter?.cancelExecution { [weak self] _ in
            Task { @MainActor in
                self?.executionState = .idle
            }
        }
    }
}
