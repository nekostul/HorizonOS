package ru.nekostul.horizonos.ui.files

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Recursive, cancellable search over a readable subtree. Always bounded in
 * depth and result count so huge trees never freeze the device.
 */
object FileSearch {

    data class SearchHit(
        val entry: FileEntry,
        val parent: String
    )

    suspend fun search(
        root: File,
        query: String,
        maxResults: Int = 200,
        maxDepth: Int = 8
    ): List<SearchHit> = withContext(Dispatchers.IO) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return@withContext emptyList()
        val out = mutableListOf<SearchHit>()
        var visited = 0
        var tooMany = false

        fun walk(dir: File, depth: Int) {
            if (tooMany || out.size >= maxResults) { tooMany = true; return }
            val children = dir.listFiles() ?: return
            if (children.size > 5000) return // avoid pathological dirs
            children.forEach { child ->
                if (tooMany) return
                visited++
                val nm = child.name
                if (nm.lowercase().contains(needle)) {
                    out.add(SearchHit(child.toEntry(), dir.absolutePath))
                }
                if (child.isDirectory && depth < maxDepth && nm !in setOf(".", "..", "cache", ".cache")) {
                    walk(child, depth + 1)
                }
            }
        }

        walk(root, 0)
        out
    }
}
