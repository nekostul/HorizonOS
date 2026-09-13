package ru.nekostul.horizonos.ui.files

import java.io.File

/** Result of a file operation that can return partial/failure info. */
data class OpResult(val ok: Boolean, val message: String = "")

/** How the current directory listing is ordered. */
enum class FileSortMode { NAME, SIZE, DATE }

/**
 * Performs all file-system operations. When running as root it falls back to
 * shell commands for paths that are not directly writable; otherwise it uses
 * plain java.io.File access (which is fine for user-scoped and removable
 * storage directories).
 */
object FileOperations {

    fun list(parent: File): List<FileEntry> {
        val children = parent.listFiles() ?: return emptyList()
        return children.map { it.toEntry() }.sortedWith(FileSorting.byName)
    }

    fun listRoot(path: String): List<FileEntry> =
        if (RootHelper.isRootAvailable()) RootHelper.listRoot(path) else list(File(path))

    fun listWithHidden(parent: File): List<FileEntry> {
        val children = parent.listFiles() ?: return emptyList()
        return children.map { it.toEntry() }
    }

    suspend fun copy(src: File, dstDir: File): OpResult = io {
        val target = File(dstDir, src.name)
        if (target.exists()) return@io OpResult(false, "already exists")
        val ok = if (src.isDirectory) copyDir(src, target) else runCatching { src.copyTo(target, overwrite = false) }.isSuccess
        if (ok) OpResult(true)
        else OpResult(rootRun("cp -r \"${src.absolutePath}\" \"${target.absolutePath}\" "))
    }

    private fun copyDir(src: File, dst: File): Boolean = run {
        if (!dst.mkdirs()) return@run false
        src.listFiles()?.all { child ->
            if (child.isDirectory) copyDir(child, File(dst, child.name))
            else child.copyTo(File(dst, child.name), overwrite = false).let { true }
        } ?: true
    }

    suspend fun move(src: File, dstDir: File): OpResult = io {
        val target = File(dstDir, src.name)
        if (target.exists()) return@io OpResult(false, "already exists")
        val direct = src.renameTo(target) ||
            (copyBlocking(src, dstDir) && deleteBlocking(src))
        if (direct) OpResult(true)
        else OpResult(rootRun("mv \"${src.absolutePath}\" \"${target.absolutePath}\" "))
    }

    private fun copyBlocking(src: File, dstDir: File): Boolean {
        val target = File(dstDir, src.name)
        if (target.exists()) return false
        return if (src.isDirectory) copyDir(src, target) else runCatching { src.copyTo(target, overwrite = false) }.isSuccess
    }

    private fun deleteBlocking(file: File): Boolean =
        if (file.isDirectory) deleteDir(file) else file.delete()

    suspend fun delete(file: File): OpResult = io {
        val direct = if (file.isDirectory) deleteDir(file) else file.delete()
        if (direct) OpResult(true)
        else OpResult(rootRun("rm -rf \"${file.absolutePath}\" "))
    }

    private fun deleteDir(dir: File): Boolean {
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) deleteDir(child) else child.delete()
        }
        return dir.delete()
    }

    suspend fun rename(file: File, newName: String): OpResult = io {
        val target = File(file.parent ?: "", newName)
        if (file.renameTo(target)) OpResult(true)
        else OpResult(rootRun("mv \"${file.absolutePath}\" \"${target.absolutePath}\" "))
    }

    suspend fun createFolder(parent: File, name: String): OpResult = io {
        if (File(parent, name).mkdir()) OpResult(true)
        else OpResult(rootRun("mkdir -p \"${File(parent, name).absolutePath}\" "))
    }

    suspend fun createFile(parent: File, name: String): OpResult = io {
        if (File(parent, name).createNewFile()) OpResult(true)
        else OpResult(rootRun("touch \"${File(parent, name).absolutePath}\" "))
    }

    /** Runs a root command string; returns true when it succeeded. */
    private fun rootRun(command: String): Boolean {
        if (!RootHelper.isRootAvailable()) return false
        val (code, _) = RootHelper.runRoot(command)
        return code == 0
    }

    suspend fun exists(path: String): Boolean = io { File(path).exists() }

    /** Total size of a subtree; stops early for huge folders. */
    suspend fun sizeOf(file: File, limitFiles: Int = 200_000): Long = io {
        var total = 0L
        var count = 0
        fun walk(f: File) {
            if (count > limitFiles) return
            if (f.isFile) {
                total += f.length()
                count++
            } else {
                f.listFiles()?.forEach { walk(it) }
            }
        }
        walk(file)
        total
    }

    suspend fun itemCount(file: File, limit: Int = 50_000): Int = io {
        if (file.isFile) return@io 1
        var c = 0
        fun walk(f: File) {
            if (c > limit) return
            f.listFiles()?.forEach { child ->
                c++
                if (child.isDirectory) walk(child)
            }
        }
        walk(file)
        c
    }

    private suspend fun <T> io(block: () -> T): T =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { block() }
}

object FileSorting {
    val byName: Comparator<FileEntry> = Comparator { a, b ->
        if (a.isDirectory != b.isDirectory) if (a.isDirectory) -1 else 1 else a.name.lowercase().compareTo(b.name.lowercase())
    }
    val bySize: Comparator<FileEntry> = Comparator { a, b ->
        if (a.isDirectory != b.isDirectory) if (a.isDirectory) -1 else 1 else (b.size.compareTo(a.size))
    }
    val byDate: Comparator<FileEntry> = Comparator { a, b ->
        if (a.isDirectory != b.isDirectory) if (a.isDirectory) -1 else 1 else (b.lastModified.compareTo(a.lastModified))
    }
}
