import SwiftUI
import Shared

struct ResponseStateView: View {
    let state: ExecutionState
    @Binding var responseTab: WorkspaceViewModel.ResponseTab
    @State private var searchQuery: String = ""
    @State private var filteredText: String? = nil
    @State private var isFiltering: Bool = false

    var body: some View {
        VStack(spacing: ReqTokens.Spacing.sm) {
            switch state {
            case .idle:
                ReqEmptyStateView(
                    icon: "paperplane",
                    title: "Ready to Execute",
                    subtitle: "Enter a URL and tap Send to execute"
                )

            case .loading:
                VStack(spacing: ReqTokens.Spacing.md) {
                    ProgressView()
                        .scaleEffect(1.2)
                    Text("Executing request...")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(ReqTokens.Spacing.xl)

            case .error(let message):
                VStack(spacing: ReqTokens.Spacing.sm) {
                    HStack(alignment: .top, spacing: ReqTokens.Spacing.xs) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 14))
                            .foregroundColor(.red)
                        Text(message)
                            .font(.system(size: 13, weight: .regular, design: .monospaced))
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(ReqTokens.Spacing.md)
                    .background(
                        RoundedRectangle(cornerRadius: ReqTokens.Radius.control, style: .continuous)
                            .fill(Color.red.opacity(0.08))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: ReqTokens.Radius.control, style: .continuous)
                            .strokeBorder(Color.red.opacity(0.2), lineWidth: 1)
                    )
                }
                .padding(ReqTokens.Spacing.md)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)

            case .success(let history, let responseBody, let responseHeaders, let testResults, let scriptLogs):
                let code = history.statusCode?.intValue ?? 0
                let duration = history.durationMs?.int64Value ?? 0

                VStack(spacing: ReqTokens.Spacing.xs) {
                    // Refined Liquid Glass Summary Bar
                    ReqResponseSummaryBar(
                        statusCode: code,
                        statusText: statusText(for: code),
                        durationMs: duration,
                        testResults: testResults,
                        responseTab: $responseTab
                    )
                    .padding(.horizontal, ReqTokens.Spacing.md)

                    // Search input if viewing body
                    if responseTab == .pretty || responseTab == .raw {
                        HStack(spacing: ReqTokens.Spacing.xs) {
                            Image(systemName: "magnifyingglass")
                                .font(.system(size: 13))
                                .foregroundColor(.secondary)

                            TextField("Search response...", text: $searchQuery)
                                .font(.system(size: 13, design: .monospaced))
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled(true)

                            if !searchQuery.isEmpty {
                                Button(action: { searchQuery = "" }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.system(size: 13))
                                        .foregroundColor(.secondary)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, ReqTokens.Spacing.sm)
                        .frame(height: 36)
                        .background(
                            RoundedRectangle(cornerRadius: ReqTokens.Radius.small, style: .continuous)
                                .fill(Color(uiColor: .secondarySystemBackground))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: ReqTokens.Radius.small, style: .continuous)
                                .strokeBorder(Color.primary.opacity(0.06), lineWidth: 0.5)
                        )
                        .padding(.horizontal, ReqTokens.Spacing.md)
                        .padding(.top, ReqTokens.Spacing.xxs)
                    }

                    // Content Layer: Clean, high-readability response
                    ScrollView {
                        VStack(alignment: .leading, spacing: 0) {
                            switch responseTab {
                            case .pretty, .raw:
                                if let errorMsg = history.errorMessage, !errorMsg.isEmpty {
                                    Text(errorMsg)
                                        .font(.system(size: 13, design: .monospaced))
                                        .foregroundColor(.red)
                                        .padding(ReqTokens.Spacing.md)
                                } else {
                                    let actualText = responseTab == .pretty ? ResponseStateView.prettyJson(responseBody) : responseBody
                                    if isFiltering {
                                        ProgressView()
                                            .padding(ReqTokens.Spacing.md)
                                    } else {
                                        Text(searchQuery.isEmpty ? actualText : (filteredText ?? actualText))
                                            .font(.system(size: 13, design: .monospaced))
                                            .foregroundColor(.primary)
                                            .padding(ReqTokens.Spacing.md)
                                            .textSelection(.enabled)
                                    }
                                }

                            case .headers:
                                VStack(alignment: .leading, spacing: ReqTokens.Spacing.sm) {
                                    if !responseHeaders.isEmpty {
                                        Text("Response Headers (\(responseHeaders.count))")
                                            .font(.system(size: 12, weight: .bold))
                                            .foregroundColor(.primary)
                                            .padding(.bottom, 2)

                                        ForEach(Array(responseHeaders.keys.sorted()), id: \.self) { key in
                                            HStack(alignment: .top, spacing: ReqTokens.Spacing.xs) {
                                                Text(key)
                                                    .font(.system(size: 12, weight: .bold, design: .monospaced))
                                                    .foregroundColor(.primary)
                                                Text(":")
                                                    .foregroundColor(.secondary)
                                                Text(responseHeaders[key] ?? "")
                                                    .font(.system(size: 12, design: .monospaced))
                                                    .foregroundColor(.secondary)
                                            }
                                            .textSelection(.enabled)
                                        }

                                        Divider()
                                            .padding(.vertical, 4)
                                    }

                                    Text("Request Info")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(.primary)
                                        .padding(.bottom, 2)

                                    HStack(alignment: .top, spacing: ReqTokens.Spacing.xs) {
                                        Text("Request ID")
                                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                                            .foregroundColor(.primary)
                                        Text(":")
                                            .foregroundColor(.secondary)
                                        Text(history.requestId ?? "None")
                                            .font(.system(size: 12, design: .monospaced))
                                            .foregroundColor(.secondary)
                                    }

                                    HStack(alignment: .top, spacing: ReqTokens.Spacing.xs) {
                                        Text("Target URL")
                                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                                            .foregroundColor(.primary)
                                        Text(":")
                                            .foregroundColor(.secondary)
                                        Text(history.requestUrl)
                                            .font(.system(size: 12, design: .monospaced))
                                            .foregroundColor(.secondary)
                                    }
                                }
                                .padding(ReqTokens.Spacing.md)
                                .frame(maxWidth: .infinity, alignment: .leading)

                            case .tests:
                                ReqTestsResultView(testResults: testResults, scriptLogs: scriptLogs)
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .task(id: searchQuery) {
                    guard !searchQuery.isEmpty else {
                        filteredText = nil
                        return
                    }
                    isFiltering = true
                    defer { isFiltering = false }

                    let query = searchQuery
                    let text = responseBody

                    let result = await Task.detached { () -> String in
                        if let data = text.data(using: .utf8),
                           let jsonObject = try? JSONSerialization.jsonObject(with: data, options: []),
                           let filteredData = Self.filterJSON(jsonObject, query: query),
                           let prettyString = String(data: filteredData, encoding: .utf8) {
                            return prettyString
                        } else {
                            return text.components(separatedBy: .newlines)
                                .filter { $0.localizedCaseInsensitiveContains(query) }
                                .joined(separator: "\n")
                        }
                    }.value

                    if !Task.isCancelled {
                        filteredText = result
                    }
                }
            }
        }
    }

    private func statusText(for code: Int) -> String {
        switch code {
        case 200: return "OK"
        case 201: return "Created"
        case 204: return "No Content"
        case 400: return "Bad Request"
        case 401: return "Unauthorized"
        case 403: return "Forbidden"
        case 404: return "Not Found"
        case 500: return "Internal Server Error"
        default: return ""
        }
    }

    nonisolated static func filterJSON(_ json: Any, query: String) -> Data? {
        let q = query.lowercased()
        func filterNode(_ node: Any) -> Any? {
            if let dict = node as? [String: Any] {
                var newDict = [String: Any]()
                for (k, v) in dict {
                    if k.lowercased().contains(q) {
                        newDict[k] = v
                    } else if let filteredV = filterNode(v) {
                        newDict[k] = filteredV
                    }
                }
                return newDict.isEmpty ? nil : newDict
            } else if let arr = node as? [Any] {
                let newArr = arr.compactMap { filterNode($0) }
                return newArr.isEmpty ? nil : newArr
            } else if let str = node as? String {
                return str.lowercased().contains(q) ? str : nil
            } else if let num = node as? NSNumber {
                return num.stringValue.lowercased().contains(q) ? num : nil
            } else {
                return nil
            }
        }

        guard let filtered = filterNode(json) else { return try? JSONSerialization.data(withJSONObject: [:], options: .prettyPrinted) }
        return try? JSONSerialization.data(withJSONObject: filtered, options: [.prettyPrinted, .sortedKeys])
    }
}

// MARK: - Full Screen Response Inspector

struct FullScreenResponseView: View {
    let history: HistoryEntry
    let responseBody: String
    var responseHeaders: [String: String] = [:]
    var testResults: [IosTestResult] = []
    var scriptLogs: [String] = []
    @Binding var responseTab: WorkspaceViewModel.ResponseTab

    @State private var searchQuery: String = ""
    @State private var filteredText: String? = nil
    @State private var isFiltering: Bool = false

    var body: some View {
        let code = history.statusCode?.intValue ?? 0
        let duration = history.durationMs?.int64Value ?? 0

        VStack(spacing: ReqTokens.Spacing.xs) {
            // Liquid Glass Summary Bar
            ReqResponseSummaryBar(
                statusCode: code,
                statusText: statusText(for: code),
                durationMs: duration,
                testResults: testResults,
                responseTab: $responseTab
            )
            .padding(.horizontal, ReqTokens.Spacing.md)
            .padding(.top, ReqTokens.Spacing.xs)

            // Search Bar
            if responseTab == .pretty || responseTab == .raw {
                HStack(spacing: ReqTokens.Spacing.xs) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)

                    TextField("Search response...", text: $searchQuery)
                        .font(.system(size: 13, design: .monospaced))
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled(true)

                    if !searchQuery.isEmpty {
                        Button(action: { searchQuery = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .font(.system(size: 13))
                                .foregroundColor(.secondary)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, ReqTokens.Spacing.sm)
                .frame(height: 36)
                .background(
                    RoundedRectangle(cornerRadius: ReqTokens.Radius.small, style: .continuous)
                        .fill(Color(uiColor: .secondarySystemBackground))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: ReqTokens.Radius.small, style: .continuous)
                        .strokeBorder(Color.primary.opacity(0.06), lineWidth: 0.5)
                )
                .padding(.horizontal, ReqTokens.Spacing.md)
            }

            // Content Layer: Clean Code Display
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    switch responseTab {
                    case .pretty, .raw:
                        if let errorMsg = history.errorMessage, !errorMsg.isEmpty {
                            Text(errorMsg)
                                .font(.system(size: 13, design: .monospaced))
                                .foregroundColor(.red)
                                .padding(ReqTokens.Spacing.md)
                        } else {
                            let actualText = responseTab == .pretty ? ResponseStateView.prettyJson(responseBody) : responseBody
                            if isFiltering {
                                ProgressView()
                                    .padding(ReqTokens.Spacing.md)
                            } else {
                                Text(searchQuery.isEmpty ? actualText : (filteredText ?? actualText))
                                    .font(.system(size: 13, design: .monospaced))
                                    .foregroundColor(.primary)
                                    .padding(ReqTokens.Spacing.md)
                                    .textSelection(.enabled)
                            }
                        }

                    case .headers:
                        VStack(alignment: .leading, spacing: ReqTokens.Spacing.sm) {
                            if !responseHeaders.isEmpty {
                                Text("Response Headers (\(responseHeaders.count))")
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(.primary)
                                    .padding(.bottom, 2)

                                ForEach(Array(responseHeaders.keys.sorted()), id: \.self) { key in
                                    HStack(alignment: .top, spacing: ReqTokens.Spacing.xs) {
                                        Text(key)
                                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                                            .foregroundColor(.primary)
                                        Text(":")
                                            .foregroundColor(.secondary)
                                        Text(responseHeaders[key] ?? "")
                                            .font(.system(size: 12, design: .monospaced))
                                            .foregroundColor(.secondary)
                                    }
                                    .textSelection(.enabled)
                                }

                                Divider()
                                    .padding(.vertical, 4)
                            }

                            Text("Request Info")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.primary)
                                .padding(.bottom, 2)

                            HStack(alignment: .top, spacing: ReqTokens.Spacing.xs) {
                                Text("Request ID")
                                    .font(.system(size: 12, weight: .bold, design: .monospaced))
                                    .foregroundColor(.primary)
                                Text(":")
                                    .foregroundColor(.secondary)
                                Text(history.requestId ?? "None")
                                    .font(.system(size: 12, design: .monospaced))
                                    .foregroundColor(.secondary)
                            }

                            HStack(alignment: .top, spacing: ReqTokens.Spacing.xs) {
                                Text("Target URL")
                                    .font(.system(size: 12, weight: .bold, design: .monospaced))
                                    .foregroundColor(.primary)
                                Text(":")
                                    .foregroundColor(.secondary)
                                Text(history.requestUrl)
                                    .font(.system(size: 12, design: .monospaced))
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(ReqTokens.Spacing.md)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    case .tests:
                        ReqTestsResultView(testResults: testResults, scriptLogs: scriptLogs)
                    }
                }
            }
        }
        .navigationTitle("Response")
        .navigationBarTitleDisplayMode(.inline)
        .task(id: searchQuery) {
            guard !searchQuery.isEmpty else {
                filteredText = nil
                return
            }
            isFiltering = true
            defer { isFiltering = false }

            let query = searchQuery
            let text = responseBody

            let result = await Task.detached { () -> String in
                if let data = text.data(using: .utf8),
                   let jsonObject = try? JSONSerialization.jsonObject(with: data, options: []),
                   let filteredData = ResponseStateView.filterJSON(jsonObject, query: query),
                   let prettyString = String(data: filteredData, encoding: .utf8) {
                    return prettyString
                } else {
                    return text.components(separatedBy: .newlines)
                        .filter { $0.localizedCaseInsensitiveContains(query) }
                        .joined(separator: "\n")
                }
            }.value

            if !Task.isCancelled {
                filteredText = result
            }
        }
    }

    private func statusText(for code: Int) -> String {
        switch code {
        case 200: return "OK"
        case 201: return "Created"
        case 204: return "No Content"
        case 400: return "Bad Request"
        case 401: return "Unauthorized"
        case 403: return "Forbidden"
        case 404: return "Not Found"
        case 500: return "Internal Server Error"
        default: return ""
        }
    }
}

// MARK: - Test Assertions Results View

struct ReqTestsResultView: View {
    let testResults: [IosTestResult]
    let scriptLogs: [String]
    @SwiftUI.Environment(\.colorScheme) private var colorScheme: ColorScheme

    private var passedCount: Int {
        testResults.filter { $0.passed }.count
    }

    private var failedCount: Int {
        testResults.filter { !$0.passed }.count
    }

    var body: some View {
        VStack(alignment: .leading, spacing: ReqTokens.Spacing.sm) {
            if testResults.isEmpty {
                ReqEmptyStateView(
                    icon: "checkmark.shield",
                    title: "No Tests Executed",
                    subtitle: "Add assertions in the Tests tab using pm.test() or reqlite.test() to automatically validate responses."
                )
                .padding(.vertical, ReqTokens.Spacing.lg)
            } else {
                // Header Scoreboard Card
                HStack(spacing: ReqTokens.Spacing.sm) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Test Results")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.primary)
                        Text("\(testResults.count) total assertion\(testResults.count == 1 ? "" : "s") executed")
                            .font(.system(size: 11))
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    HStack(spacing: 6) {
                        HStack(spacing: 3) {
                            Circle()
                                .fill(Color.green)
                                .frame(width: 6, height: 6)
                            Text("\(passedCount) Passed")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(.green)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.green.opacity(0.12))
                        .clipShape(Capsule())

                        if failedCount > 0 {
                            HStack(spacing: 3) {
                                Circle()
                                    .fill(Color.red)
                                    .frame(width: 6, height: 6)
                                Text("\(failedCount) Failed")
                                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                                    .foregroundColor(.red)
                            }
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.red.opacity(0.12))
                            .clipShape(Capsule())
                        }
                    }
                }
                .padding(ReqTokens.Spacing.sm)
                .background(Color(uiColor: .secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: ReqTokens.Radius.control, style: .continuous))

                // Test Assertions List
                VStack(spacing: ReqTokens.Spacing.xs) {
                    ForEach(Array(testResults.enumerated()), id: \.offset) { _, result in
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 8) {
                                Image(systemName: result.passed ? "checkmark.circle.fill" : "xmark.circle.fill")
                                    .font(.system(size: 14))
                                    .foregroundColor(result.passed ? .green : .red)

                                Text(result.name)
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(.primary)

                                Spacer()

                                if result.durationMs > 0 {
                                    Text("\(result.durationMs)ms")
                                        .font(.system(size: 10, design: .monospaced))
                                        .foregroundColor(.secondary)
                                }

                                Text(result.passed ? "PASS" : "FAIL")
                                    .font(.system(size: 9, weight: .heavy, design: .monospaced))
                                    .foregroundColor(result.passed ? .green : .red)
                                    .padding(.horizontal, 5)
                                    .padding(.vertical, 2)
                                    .background(result.passed ? Color.green.opacity(0.12) : Color.red.opacity(0.12))
                                    .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                            }

                            if let error = result.errorMessage, !error.isEmpty {
                                Text(error)
                                    .font(.system(size: 11, design: .monospaced))
                                    .foregroundColor(.red)
                                    .padding(.leading, 22)
                                    .padding(.top, 2)
                            }
                        }
                        .padding(ReqTokens.Spacing.sm)
                        .background(
                            RoundedRectangle(cornerRadius: ReqTokens.Radius.control, style: .continuous)
                                .fill(result.passed ? Color.green.opacity(0.04) : Color.red.opacity(0.06))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: ReqTokens.Radius.control, style: .continuous)
                                .strokeBorder(result.passed ? Color.green.opacity(0.18) : Color.red.opacity(0.25), lineWidth: 0.75)
                        )
                    }
                }
            }

            // Script Console Logs
            if !scriptLogs.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("CONSOLE LOGS (\(scriptLogs.count))")
                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                        .foregroundColor(.secondary)
                        .padding(.top, ReqTokens.Spacing.xs)

                    VStack(alignment: .leading, spacing: 3) {
                        ForEach(Array(scriptLogs.enumerated()), id: \.offset) { _, log in
                            Text(log)
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundColor(.primary)
                                .textSelection(.enabled)
                        }
                    }
                    .padding(ReqTokens.Spacing.sm)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(uiColor: .tertiarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: ReqTokens.Radius.control, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ReqTokens.Radius.control, style: .continuous)
                            .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.5)
                    )
                }
            }
        }
        .padding(ReqTokens.Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

extension ResponseStateView {
    static func prettyJson(_ raw: String) -> String {
        guard raw.count < 1_000_000 else { return raw }
        guard let data = raw.data(using: .utf8),
              let jsonObject = try? JSONSerialization.jsonObject(with: data, options: []),
              let prettyData = try? JSONSerialization.data(withJSONObject: jsonObject, options: [.prettyPrinted, .sortedKeys]),
              let prettyString = String(data: prettyData, encoding: .utf8) else {
            return raw
        }
        return prettyString
    }
}
