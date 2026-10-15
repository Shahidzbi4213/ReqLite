import Foundation
import SwiftUI
import Shared

public typealias ReqLiteEnvironment = Shared.Environment
public typealias ReqLiteCollection = Shared.Collection
public typealias ReqLiteRequest = Shared.Request

enum ExecutionState {
    case idle
    case loading
    case success(HistoryEntry, String, [String: String], [IosTestResult], [String])
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
    @Published var showingImportPostmanSheet: Bool = false
    @Published var showingExportSheet: Bool = false
    @Published var selectedCollectionForExport: ReqLiteCollection? = nil
    @Published var activeRequestId: String? = nil
    @Published var showingCodeSnippetSheet: Bool = false

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

    // MARK: - Script State
    @Published var preRequestScript: String = ""
    @Published var testScript: String = ""
    @Published var testResults: [IosTestResult] = []
    @Published var scriptLogs: [String] = []

    // MARK: - GraphQL State
    @Published var isGraphQL: Bool = false
    @Published var graphQLVariables: String = "{}"
    @Published var graphQLSchema: GraphQLSchema? = nil
    @Published var isLoadingSchema: Bool = false
    @Published var showingSchemaExplorer: Bool = false
    @Published var schemaSearchQuery: String = ""
    @Published var schemaErrorMessage: String? = nil

    enum RequestTab: String, CaseIterable, Identifiable, CustomStringConvertible {
        case params = "Params"
        case headers = "Headers"
        case auth = "Auth"
        case body = "Body"
        case preRequest = "Pre-request"
        case tests = "Tests"
        var id: String { rawValue }
        var description: String { rawValue }
    }

    enum ResponseTab: String, CaseIterable, Identifiable, CustomStringConvertible {
        case pretty = "Pretty"
        case raw = "Raw"
        case headers = "Headers"
        case tests = "Tests"
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
        let preScript = preRequestScript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : preRequestScript
        let tScript = testScript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : testScript

        adapter.saveRequestToCollection(
            collectionId: collectionId,
            name: name,
            methodName: method,
            url: url,
            headers: headersList,
            queryParams: queryParamsList,
            bodyContent: body,
            isGraphQL: isGraphQL,
            variablesContent: isGraphQL ? graphQLVariables : nil,
            preRequestScript: preScript,
            testScript: tScript
        ) { [weak self] req in
            Task { @MainActor in
                self?.activeRequestId = req.id
                self?.showingSaveCollectionSheet = false
            }
        }
    }

    func saveCurrentRequest() {
        guard let reqId = activeRequestId else {
            showingSaveCollectionSheet = true
            return
        }
        guard let adapter = adapter else { return }

        var headersList: [RequestField] = []
        for h in headers where !h.key.trimmingCharacters(in: .whitespaces).isEmpty {
            headersList.append(adapter.createRequestField(key: h.key, value: h.value, isEnabled: h.isEnabled))
        }

        var queryParamsList: [RequestField] = []
        for p in queryParams where !p.key.trimmingCharacters(in: .whitespaces).isEmpty {
            queryParamsList.append(adapter.createRequestField(key: p.key, value: p.value, isEnabled: p.isEnabled))
        }

        let body = requestBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : requestBody
        let preScript = preRequestScript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : preRequestScript
        let tScript = testScript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : testScript

        adapter.updateRequest(
            requestId: reqId,
            name: nil,
            methodName: method,
            url: url,
            headers: headersList,
            queryParams: queryParamsList,
            bodyContent: body,
            isGraphQL: isGraphQL,
            variablesContent: isGraphQL ? graphQLVariables : nil,
            preRequestScript: preScript,
            testScript: tScript
        ) { _ in }
    }

    func loadSavedRequest(_ req: ReqLiteRequest) {
        self.activeRequestId = req.id
        self.method = req.method.name
        self.url = req.url
        self.queryParams = req.queryParams.map { field in
            KeyValueItem(key: field.key, value: field.value, isEnabled: field.isEnabled)
        }
        self.headers = req.headers.map { field in
            KeyValueItem(key: field.key, value: field.value, isEnabled: field.isEnabled)
        }
        self.preRequestScript = req.preRequestScript ?? ""
        self.testScript = req.testScript ?? ""
        if let adapter = self.adapter {
            self.isGraphQL = adapter.isGraphQLRequest(request: req)
            if self.isGraphQL {
                self.graphQLVariables = adapter.extractGraphQLVariables(request: req)
            } else {
                self.graphQLVariables = "{}"
            }
            self.requestBody = adapter.extractRequestBodyText(request: req)
        } else {
            self.isGraphQL = false
            self.graphQLVariables = "{}"
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

        let bodyToSend: String? = isGraphQL ? requestBody : ((method == "POST" || method == "PUT" || method == "PATCH") ? requestBody : nil)
        let preScript = preRequestScript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : preRequestScript
        let tScript = testScript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : testScript

        adapter.createDraftWithDetails(
            methodName: isGraphQL ? "POST" : method,
            url: url,
            headers: compiledHeaders,
            queryParams: compiledParams,
            bodyContent: bodyToSend,
            isGraphQL: isGraphQL,
            variablesContent: isGraphQL ? graphQLVariables : nil,
            preRequestScript: preScript,
            testScript: tScript
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
                            self.testResults = success.testResults
                            self.scriptLogs = success.scriptLogs
                            self.executionState = .success(
                                success.result,
                                success.responseBody,
                                success.responseHeaders,
                                success.testResults,
                                success.scriptLogs
                            )
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

    func previewPostmanCollection(_ json: String, completion: @escaping (String?, Int, Int, String?) -> Void) {
        guard let adapter = adapter else {
            completion(nil, 0, 0, "Adapter unavailable")
            return
        }
        adapter.previewPostmanCollection(
            jsonContent: json,
            onSuccess: { name, reqs, folders in
                completion(name, Int(truncating: reqs), Int(truncating: folders), nil)
            },
            onError: { errorMsg in
                completion(nil, 0, 0, errorMsg)
            }
        )
    }

    func importPostmanCollection(_ json: String, completion: @escaping (Bool, String) -> Void) {
        guard let adapter = adapter else {
            completion(false, "Adapter unavailable")
            return
        }
        adapter.importPostmanCollection(
            jsonContent: json,
            onSuccess: { name, reqs in
                Task { @MainActor in
                    completion(true, "Successfully imported \(reqs) requests into \(name)")
                }
            },
            onError: { errorMsg in
                Task { @MainActor in
                    completion(false, errorMsg)
                }
            }
        )
    }

    func previewUniversal(_ json: String, completion: @escaping (String?, String?, Int, Int, String?) -> Void) {
        guard let adapter = adapter else {
            completion(nil, nil, 0, 0, "Adapter unavailable")
            return
        }
        adapter.previewUniversal(
            jsonContent: json,
            onSuccess: { format, name, reqs, folders in
                completion(format, name, Int(truncating: reqs), Int(truncating: folders), nil)
            },
            onError: { errorMsg in
                completion(nil, nil, 0, 0, errorMsg)
            }
        )
    }

    func importUniversal(_ json: String, completion: @escaping (Bool, String) -> Void) {
        guard let adapter = adapter else {
            completion(false, "Adapter unavailable")
            return
        }
        adapter.importUniversal(
            jsonContent: json,
            onSuccess: { cols, reqs, envs in
                Task { @MainActor in
                    var msg = "Imported \(reqs) request\(Int(truncating: reqs) == 1 ? "" : "s")"
                    if Int(truncating: cols) > 0 {
                        msg += " across \(cols) collection\(Int(truncating: cols) == 1 ? "" : "s")"
                    }
                    if Int(truncating: envs) > 0 {
                        msg += " and \(envs) environment\(Int(truncating: envs) == 1 ? "" : "s")"
                    }
                    completion(true, msg)
                }
            },
            onError: { errorMsg in
                Task { @MainActor in
                    completion(false, errorMsg)
                }
            }
        )
    }

    func previewInsomniaCollection(_ json: String, completion: @escaping (String?, Int, Int, String?) -> Void) {
        guard let adapter = adapter else {
            completion(nil, 0, 0, "Adapter unavailable")
            return
        }
        adapter.previewInsomniaCollection(
            jsonContent: json,
            onSuccess: { name, reqs, folders in
                completion(name, Int(truncating: reqs), Int(truncating: folders), nil)
            },
            onError: { errorMsg in
                completion(nil, 0, 0, errorMsg)
            }
        )
    }

    func importInsomniaCollection(_ json: String, completion: @escaping (Bool, String) -> Void) {
        guard let adapter = adapter else {
            completion(false, "Adapter unavailable")
            return
        }
        adapter.importInsomniaCollection(
            jsonContent: json,
            onSuccess: { name, reqs in
                Task { @MainActor in
                    completion(true, "Successfully imported \(reqs) requests into \(name)")
                }
            },
            onError: { errorMsg in
                Task { @MainActor in
                    completion(false, errorMsg)
                }
            }
        )
    }

    func exportWorkspace(
        includeSecrets: Bool = false,
        collectionId: String? = nil,
        completion: @escaping (Result<String, Error>) -> Void
    ) {
        guard let adapter = adapter else {
            completion(.failure(NSError(domain: "ReqLite", code: -1, userInfo: [NSLocalizedDescriptionKey: "Adapter unavailable"])))
            return
        }
        if let collectionId = collectionId {
            adapter.exportCollection(
                collectionId: collectionId,
                onSuccess: { json in
                    Task { @MainActor in
                        completion(.success(json))
                    }
                },
                onError: { errorMsg in
                    Task { @MainActor in
                        completion(.failure(NSError(domain: "ReqLite", code: -1, userInfo: [NSLocalizedDescriptionKey: errorMsg])))
                    }
                }
            )
        } else {
            adapter.exportWorkspace(
                collectionIds: nil,
                environmentIds: nil,
                includeSecrets: includeSecrets,
                onSuccess: { json in
                    Task { @MainActor in
                        completion(.success(json))
                    }
                },
                onError: { errorMsg in
                    Task { @MainActor in
                        completion(.failure(NSError(domain: "ReqLite", code: -1, userInfo: [NSLocalizedDescriptionKey: errorMsg])))
                    }
                }
            )
        }
    }

    // MARK: - GraphQL Operations

    func setGraphQLMode(_ enabled: Bool) {
        self.isGraphQL = enabled
        if enabled {
            self.method = "POST"
            if self.requestBody.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || self.requestBody.contains("\"title\":") {
                self.requestBody = "query {\n  \n}"
            }
            if self.graphQLSchema == nil {
                fetchGraphQLSchema()
            }
        }
    }

    func fetchGraphQLSchema() {
        guard let adapter = adapter, !url.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        isLoadingSchema = true
        schemaErrorMessage = nil

        var compiledHeaders: [RequestField] = []
        for h in headers where h.isEnabled && !h.key.trimmingCharacters(in: .whitespaces).isEmpty {
            compiledHeaders.append(adapter.createRequestField(key: h.key, value: h.value, isEnabled: true))
        }

        adapter.introspectGraphQL(
            url: url,
            environmentId: activeEnvironment?.id,
            headers: compiledHeaders
        ) { [weak self] schema in
            Task { @MainActor in
                self?.graphQLSchema = schema
                self?.isLoadingSchema = false
            }
        } onError: { [weak self] errorMsg in
            Task { @MainActor in
                self?.schemaErrorMessage = errorMsg
                self?.isLoadingSchema = false
            }
        }
    }

    func formatGraphQLQuery() {
        guard let adapter = adapter else { return }
        requestBody = adapter.formatGraphQL(query: requestBody)
    }

    func getGraphQLCompletions(cursorPosition: Int) -> [GraphQLCompletionItem] {
        guard let adapter = adapter else { return [] }
        return adapter.getGraphQLCompletions(query: requestBody, cursorPosition: Int32(cursorPosition), schema: graphQLSchema)
    }

    func explorerCategories(searchQuery: String) -> [SchemaExplorerCategory] {
        guard let adapter = adapter, let schema = graphQLSchema else { return [] }
        return adapter.filterGraphQLSchema(schema: schema, searchQuery: searchQuery)
    }

    func generateGraphQLSnippet(item: SchemaExplorerItem) -> String? {
        guard let adapter = adapter, let schema = graphQLSchema else { return nil }
        return adapter.generateGraphQLSnippet(item: item, schema: schema)
    }

    // MARK: - Script Operations

    func getPreRequestSnippets() -> [IosScriptSnippet] {
        return adapter?.getPreRequestSnippets() ?? []
    }

    func getTestSnippets() -> [IosScriptSnippet] {
        return adapter?.getTestSnippets() ?? []
    }

    func insertPreRequestSnippet(_ code: String) {
        if preRequestScript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            preRequestScript = code
        } else {
            preRequestScript += "\n\n" + code
        }
    }

    func insertTestSnippet(_ code: String) {
        if testScript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            testScript = code
        } else {
            testScript += "\n\n" + code
        }
    }

    func getSupportedSnippetLanguages() -> [IosCodeSnippetLanguage] {
        return adapter?.getSupportedSnippetLanguages() ?? []
    }

    func generateCodeSnippet(languageId: String) -> String {
        guard let adapter = adapter else { return "" }

        var compiledHeaders: [RequestField] = []
        for h in headers where h.isEnabled && !h.key.isEmpty {
            compiledHeaders.append(adapter.createRequestField(key: h.key, value: h.value, isEnabled: true))
        }

        var compiledParams: [RequestField] = []
        for p in queryParams where p.isEnabled && !p.key.isEmpty {
            compiledParams.append(adapter.createRequestField(key: p.key, value: p.value, isEnabled: true))
        }

        let bearer = (authType == "Bearer" && !authToken.isEmpty) ? authToken : nil
        let contentType = isGraphQL ? "application/json" : "application/json"

        return adapter.generateSnippet(
            method: method,
            url: url,
            headers: compiledHeaders,
            queryParams: compiledParams,
            bodyContent: requestBody,
            contentType: contentType,
            bearerToken: bearer,
            basicUsername: nil,
            basicPassword: nil,
            languageId: languageId
        )
    }
}

