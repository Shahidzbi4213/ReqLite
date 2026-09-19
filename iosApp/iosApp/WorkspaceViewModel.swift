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

    enum RequestTab: String, CaseIterable, Identifiable {
        case params = "Params"
        case headers = "Headers"
        case auth = "Auth"
        case body = "Body"
        var id: String { rawValue }
    }

    enum ResponseTab: String, CaseIterable, Identifiable {
        case pretty = "Pretty"
        case raw = "Raw"
        case headers = "Headers"
        var id: String { rawValue }
    }

    let availableMethods = ["GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"]

    private var adapter: IosWorkspaceAdapter? = nil

    init() {
        self.adapter = IosWorkspaceAdapter()
        observeEnvironments()
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
        adapter.createQuickDraft(methodName: method, url: url) { [weak self] draftId in
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
