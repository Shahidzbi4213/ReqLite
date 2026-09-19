import SwiftUI
import Shared

struct ResponseStateView: View {
    let state: ExecutionState
    @Binding var responseTab: WorkspaceViewModel.ResponseTab
    @State private var searchQuery: String = ""
    @State private var filteredText: String? = nil
    @State private var isFiltering: Bool = false

    var body: some View {
        VStack(spacing: 0) {
            switch state {
            case .idle:
                VStack(spacing: 12) {
                    Image(systemName: "paperplane")
                        .font(.system(size: 36))
                        .foregroundColor(.secondary.opacity(0.6))
                    Text("Enter a URL and tap Send to execute")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding()

            case .loading:
                VStack(spacing: 14) {
                    ProgressView()
                        .scaleEffect(1.2)
                    Text("Executing request...")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding()

            case .error(let message):
                VStack(spacing: 10) {
                    HStack(alignment: .top, spacing: 8) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                        Text(message)
                            .font(.system(size: 13, design: .monospaced))
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding()
                    .background(Color.red.opacity(0.1))
                    .cornerRadius(8)
                }
                .padding()
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)

            case .success(let history, let responseBody):
                let code = history.statusCode?.intValue ?? 0
                let duration = history.durationMs?.int64Value ?? 0

                VStack(spacing: 0) {
                    // Status bar
                    HStack(spacing: 12) {
                        // Status Code Chip
                        HStack(spacing: 4) {
                            Circle()
                                .fill(statusColor(for: code))
                                .frame(width: 8, height: 8)
                            Text(code > 0 ? "\(code) \(statusText(for: code))" : "No Status")
                                .font(.system(size: 13, weight: .bold, design: .monospaced))
                                .foregroundColor(statusColor(for: code))
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(statusColor(for: code).opacity(0.12))
                        .cornerRadius(6)

                        // Duration Chip
                        HStack(spacing: 4) {
                            Image(systemName: "clock")
                                .font(.system(size: 11))
                            Text("\(duration) ms")
                                .font(.system(size: 12, design: .monospaced))
                        }
                        .foregroundColor(.secondary)

                        Spacer()

                        // Response Tab Picker
                        Picker("Response Tab", selection: $responseTab) {
                            ForEach(WorkspaceViewModel.ResponseTab.allCases) { tab in
                                Text(tab.rawValue).tag(tab)
                            }
                        }
                        .pickerStyle(.segmented)
                        .frame(width: 200)
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                    .background(Color(UIColor.secondarySystemBackground))

                    Divider()
                    
                    if responseTab == .pretty || responseTab == .raw {
                        TextField("Search response...", text: $searchQuery)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .padding(.horizontal)
                            .padding(.top, 8)
                            .disableAutocorrection(true)
                            .autocapitalization(.none)
                    }

                    // Response Content
                    ScrollView {
                        VStack(alignment: .leading) {
                            switch responseTab {
                            case .pretty, .raw:
                                if let errorMsg = history.errorMessage, !errorMsg.isEmpty {
                                    Text(errorMsg)
                                        .font(.system(size: 12, design: .monospaced))
                                        .foregroundColor(.red)
                                        .padding()
                                } else {
                                    let actualText = responseBody
                                    
                                    if isFiltering {
                                        ProgressView()
                                            .padding()
                                    } else {
                                        Text(searchQuery.isEmpty ? actualText : (filteredText ?? actualText))
                                            .font(.system(size: 13, design: .monospaced))
                                            .foregroundColor(.primary)
                                            .padding()
                                            .textSelection(.enabled)
                                    }
                                }

                            case .headers:
                                VStack(alignment: .leading, spacing: 6) {
                                    HStack(alignment: .top) {
                                        Text("Request ID")
                                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                                            .foregroundColor(.primary)
                                        Text(":")
                                            .foregroundColor(.secondary)
                                        Text(history.requestId ?? "")
                                            .font(.system(size: 12, design: .monospaced))
                                            .foregroundColor(.secondary)
                                    }
                                    HStack(alignment: .top) {
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
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                    }
                    .background(Color(UIColor.systemBackground))
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

    private func statusColor(for code: Int) -> Color {
        switch code {
        case 200..<300: return .green
        case 300..<400: return .blue
        case 400..<500: return .orange
        case 500..<600: return .red
        default: return .secondary
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

    private static func filterJSON(_ json: Any, query: String) -> Data? {
        let q = query.lowercased()
        func filterNode(_ node: Any) -> Any? {
            if let dict = node as? [String: Any] {
                var newDict = [String: Any]()
                for (k, v) in dict {
                    if k.lowercased().contains(q) {
                        newDict[k] = v // keep whole subtree if key matches
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
import SwiftUI
import Shared

struct FullScreenResponseView: View {
    let history: HistoryEntry
    let responseBody: String
    @Binding var responseTab: WorkspaceViewModel.ResponseTab
    
    @State private var searchQuery: String = ""
    @State private var filteredText: String? = nil
    @State private var isFiltering: Bool = false
    
    var body: some View {
        VStack(spacing: 0) {
            // Summary Bar
            HStack(spacing: 16) {
                // Status Code
                let code = history.statusCode?.intValue ?? 0
                HStack(spacing: 6) {
                    Circle()
                        .fill(statusColor(for: code))
                        .frame(width: 8, height: 8)
                    Text("\(code) \(statusText(for: code))")
                        .font(.system(size: 13, weight: .bold, design: .monospaced))
                        .foregroundColor(statusColor(for: code))
                }

                // Duration
                let duration = history.durationMs?.int64Value ?? 0
                HStack(spacing: 4) {
                    Image(systemName: "clock")
                        .font(.system(size: 11))
                    Text("\(duration) ms")
                        .font(.system(size: 12, design: .monospaced))
                }
                .foregroundColor(.secondary)

                Spacer()

                // Response Tab Picker
                Picker("Response Tab", selection: $responseTab) {
                    ForEach(WorkspaceViewModel.ResponseTab.allCases) { tab in
                        Text(tab.rawValue).tag(tab)
                    }
                }
                .pickerStyle(.segmented)
                .frame(width: 200)
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
            .background(Color(UIColor.secondarySystemBackground))

            Divider()
            
            if responseTab == .pretty || responseTab == .raw {
                TextField("Search response...", text: $searchQuery)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)
                    .padding(.top, 8)
                    .disableAutocorrection(true)
                    .autocapitalization(.none)
            }

            // Response Content
            ScrollView {
                VStack(alignment: .leading) {
                    switch responseTab {
                    case .pretty, .raw:
                        if let errorMsg = history.errorMessage, !errorMsg.isEmpty {
                            Text(errorMsg)
                                .font(.system(size: 12, design: .monospaced))
                                .foregroundColor(.red)
                                .padding()
                        } else {
                            let actualText = responseBody
                            
                            if isFiltering {
                                ProgressView()
                                    .padding()
                            } else {
                                Text(searchQuery.isEmpty ? actualText : (filteredText ?? actualText))
                                    .font(.system(size: 13, design: .monospaced))
                                    .foregroundColor(.primary)
                                    .padding()
                                    .textSelection(.enabled)
                            }
                        }

                    case .headers:
                        VStack(alignment: .leading, spacing: 6) {
                            HStack(alignment: .top) {
                                Text("Request ID")
                                    .font(.system(size: 12, weight: .bold, design: .monospaced))
                                    .foregroundColor(.primary)
                                Text(":")
                                    .foregroundColor(.secondary)
                                Text(history.requestId ?? "")
                                    .font(.system(size: 12, design: .monospaced))
                                    .foregroundColor(.secondary)
                            }
                            HStack(alignment: .top) {
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
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
            .background(Color(UIColor.systemBackground))
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

    private func statusColor(for code: Int) -> Color {
        switch code {
        case 200..<300: return .green
        case 300..<400: return .blue
        case 400..<500: return .orange
        case 500..<600: return .red
        default: return .secondary
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

    private static func filterJSON(_ json: Any, query: String) -> Data? {
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
