with open("androidApp/src/main/kotlin/com/learn/reqlite/ui/response/JsonTreeView.kt", "r") as f:
    content = f.read()

# the problem is that we duplicated from the beginning of JsonTreeView up to the first Column
# We can just read the original from the file before the rewrite.
# Actually we can just write it out correctly now.
