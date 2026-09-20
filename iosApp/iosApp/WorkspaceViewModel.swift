import Foundation
import SwiftUI
import Shared

public typealias ReqLiteEnvironment = Shared.Environment
public typealias ReqLiteCollection = Shared.Collection
public typealias ReqLiteRequest = Shared.Request

enum ExecutionState {
    case idle
    case loading
    case success(HistoryEntry, String, [String: String])
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

    @Published var collections: [ReqLiteCollection] = []
    @Published var requests: [ReqLiteRequest] = []
    @Published var expandedCollectionIds: Set<String> = []
    @Published var showingSaveCollectionSheet: Bool = false
    @Published var showingNewCollectionAlert: Bool = false

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
        observeCollections()
        observeRequests()
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

    private func observeCollections() {
        adapter?.observeCollections { [weak self] list in
            Task { @MainActor in
                self?.collections = list
            }
        }
    }

    private func observeRequests() {
        adapter?.observeRequests { [weak self] list in
            Task { @MainActor in
                self?.requests = list
            }
        }
    }

    func requestsForCollection(_ collectionId: String) -> [ReqLiteRequest] {
        return requests.filter { $0.collectionId == collectionId }
    }

    func createCollection(name: String, description: String? = nil, completion: ((ReqLiteCollection) -> Void)? = nil) {
        adapter?.createCollection(name: name, description: description) { col in
            Task { @MainActor in
                completion?(col)
            }
        }
    }

    func deleteCollection(_ id: String) {
        adapter?.deleteCollection(id: id)
    }

    func deleteRequest(_ id: String) {
        adapter?.deleteRequest(id: id)
    }

    func saveCurrentRequestToCollection(collectionId: String, name: String) {
        guard let adapter = self.adapter else { return }
        let headersList = headers.map { item in
            adapter.createRequestField(key: item.key, value: item.value, isEnabled: item.isEnabled)
        }
        let queryParamsList = queryParams.map { item in
            adapter.createRequestField(key: item.key, value: item.value, isEnabled: item.isEnabled)
        }
        let body = requestBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : requestBody

        adapter.saveRequestToCollection(
            collectionId: collectionId,
            name: name,
            methodName: method,
            url: url,
            headers: headersList,
            queryParams: queryParamsList,
            bodyContent: body
        ) { _ in
            Task { @MainActor in
                self.showingSaveCollectionSheet = false
            }
        }
    }

    func loadSavedRequest(_ req: ReqLiteRequest) {
        self.method = req.method.name
        self.url = req.url
        self.queryParams = req.queryParams.map { field in
            KeyValueItem(key: field.key, value: field.value, isEnabled: field.isEnabled)
        }
        self.headers = req.headers.map { field in
            KeyValueItem(key: field.key, value: field.value, isEnabled: field.isEnabled)
        }
        if let adapter = self.adapter {
            self.requestBody = adapter.extractRequestBodyText(request: req)
        } else {
            self.requestBody = ""
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
                            self.executionState = .success(success.result, success.responseBody, success.responseHeaders)
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

    @discardableResult
    func applySmartPayload(_ input: String) -> Bool {
        guard let adapter = adapter else { return false }
        let result = adapter.parseSmartPayload(rawInput: input)
        
        switch result.type {
        case "config":
            self.url = result.url
            self.method = result.method
            if !result.queryParams.isEmpty {
                self.queryParams = result.queryParams.map { KeyValueItem(key: $0.key, value: $0.value, isEnabled: $0.isEnabled) }
            }
            if !result.headers.isEmpty {
                self.headers = result.headers.map { KeyValueItem(key: $0.key, value: $0.value, isEnabled: $0.isEnabled) }
            }
            if let body = result.bodyContent {
                self.requestBody = body
                self.selectedTab = .body
            }
            if let token = result.bearerToken {
                self.authType = "Bearer Token"
                self.authToken = token
            }
            return true
            
        case "auth":
            if let token = result.bearerToken {
                self.authType = "Bearer Token"
                self.authToken = token
                self.selectedTab = .auth
                return true
            }
            return false
            
        case "url":
            self.url = result.url
            return true
            
        default:
            return false
        }
    }

    @discardableResult
    func applyCurl(_ curlCommand: String) -> Bool {
        return applySmartPayload(curlCommand)
    }

    func cancelExecution() {
        adapter?.cancelExecution { [weak self] _ in
            Task { @MainActor in
                self?.executionState = .idle
            }
        }
    }
}
