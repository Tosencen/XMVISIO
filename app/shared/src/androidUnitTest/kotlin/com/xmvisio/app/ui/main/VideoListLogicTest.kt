package com.xmvisio.app.ui.main

import com.xmvisio.app.data.Folder
import com.xmvisio.app.data.VideoInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoListLogicTest {

    private fun video(
        id: Long,
        name: String = "v$id.mp4",
        path: String = "/storage/emulated/0/Movies/v$id.mp4",
        duration: Long = 1000 * id,
        size: Long = 1024 * id,
        dateModified: Long = id
    ) = VideoInfo(id = id, uri = "content://media/video/$id", name = name, duration = duration, size = size, dateModified = dateModified, path = path)

    // ===== pathParent / pathName =====

    @Test
    fun `pathParent - normal path returns parent`() {
        assertEquals("/storage/emulated/0/Movies", pathParent("/storage/emulated/0/Movies/a.mp4"))
    }

    @Test
    fun `pathParent - root file returns root slash`() {
        // 与 java.io.File("/a.mp4").parent == "/" 保持一致
        assertEquals("/", pathParent("/a.mp4"))
    }

    @Test
    fun `pathParent - no slash returns null`() {
        assertNull(pathParent("a.mp4"))
    }

    @Test
    fun `pathName - returns last segment`() {
        assertEquals("a.mp4", pathName("/storage/emulated/0/Movies/a.mp4"))
        assertEquals("a.mp4", pathName("a.mp4"))
    }

    // ===== computeSortedVideos =====

    @Test
    fun `sort by name ascending`() {
        val videos = listOf(video(1, "b.mp4"), video(2, "a.mp4"), video(3, "c.mp4"))
        val sorted = computeSortedVideos(videos, sortBy = 0, sortAscending = true)
        assertEquals(listOf(2L, 1L, 3L), sorted.map { it.id })
    }

    @Test
    fun `sort by name descending`() {
        val videos = listOf(video(1, "b.mp4"), video(2, "a.mp4"), video(3, "c.mp4"))
        val sorted = computeSortedVideos(videos, sortBy = 0, sortAscending = false)
        assertEquals(listOf(3L, 1L, 2L), sorted.map { it.id })
    }

    @Test
    fun `sort by date modified descending by default`() {
        val videos = listOf(video(1), video(2), video(3))
        val sorted = computeSortedVideos(videos, sortBy = 1, sortAscending = false)
        assertEquals(listOf(3L, 2L, 1L), sorted.map { it.id })
    }

    @Test
    fun `sort by size`() {
        val videos = listOf(video(1), video(2), video(3))
        val sorted = computeSortedVideos(videos, sortBy = 2, sortAscending = true)
        assertEquals(listOf(1L, 2L, 3L), sorted.map { it.id })
    }

    @Test
    fun `sort by unknown key falls back to date`() {
        val videos = listOf(video(1), video(2))
        val sorted = computeSortedVideos(videos, sortBy = 99, sortAscending = true)
        assertEquals(listOf(1L, 2L), sorted.map { it.id })
    }

    // ===== computeFolderHierarchy =====

    @Test
    fun `folder hierarchy groups videos by parent directory`() {
        val videos = listOf(
            video(1, path = "/sdcard/Movies/a.mp4"),
            video(2, path = "/sdcard/Movies/b.mp4"),
            video(3, path = "/sdcard/DCIM/c.mp4"),
        )
        val folders = computeFolderHierarchy(videos)
        assertEquals(setOf("/sdcard/Movies", "/sdcard/DCIM"), folders.map { it.path }.toSet())
        val movies = folders.first { it.path == "/sdcard/Movies" }
        assertEquals("Movies", movies.name)
        assertEquals(2, movies.mediaCount)
        assertEquals("/sdcard", movies.parentPath)
    }

    @Test
    fun `folder hierarchy computes aggregate stats`() {
        val videos = listOf(
            video(1, path = "/sdcard/Movies/a.mp4", duration = 1000, size = 100),
            video(2, path = "/sdcard/Movies/b.mp4", duration = 2000, size = 200),
        )
        val folders = computeFolderHierarchy(videos)
        val movies = folders.single()
        assertEquals(2, movies.mediaCount)
        assertEquals(3000, movies.totalDuration)
        assertEquals(300, movies.totalSize)
        assertEquals(2, movies.dateModified)
    }

    @Test
    fun `folder hierarchy ignores empty path videos`() {
        val folders = computeFolderHierarchy(listOf(video(1, path = "")))
        assertEquals(0, folders.size)
    }

    // ===== computeFolderTree =====

    private fun folder(path: String, parent: String?) = Folder(
        name = path.substringAfterLast('/'),
        path = path,
        parentPath = parent,
        mediaCount = 1
    )

    @Test
    fun `folder tree at root only shows top-level folders`() {
        val folders = listOf(
            folder("/sdcard/Movies", "/sdcard"),
            folder("/sdcard/DCIM", "/sdcard"),
            folder("/sdcard/Movies/sub", "/sdcard/Movies"),
        )
        val tree = computeFolderTree(folders, null)
        assertEquals(setOf("/sdcard/Movies", "/sdcard/DCIM"), tree.map { it.path }.toSet())
    }

    @Test
    fun `folder tree inside a folder shows direct children only`() {
        val folders = listOf(
            folder("/sdcard/Movies", "/sdcard"),
            folder("/sdcard/Movies/sub", "/sdcard/Movies"),
            folder("/sdcard/Movies/other", "/sdcard/Movies"),
        )
        val tree = computeFolderTree(folders, "/sdcard/Movies")
        assertEquals(setOf("/sdcard/Movies/sub", "/sdcard/Movies/other"), tree.map { it.path }.toSet())
    }

    // ===== computeFolderVideos =====

    @Test
    fun `folder videos - view mode all returns everything`() {
        val videos = listOf(video(1), video(2))
        val result = computeFolderVideos(videos, viewMode = 2, currentFolderPath = null, folderPathSet = emptySet())
        assertEquals(2, result.size)
    }

    @Test
    fun `folder videos - inside folder filters by parent`() {
        val videos = listOf(
            video(1, path = "/sdcard/Movies/a.mp4"),
            video(2, path = "/sdcard/DCIM/b.mp4"),
        )
        val result = computeFolderVideos(videos, viewMode = 0, currentFolderPath = "/sdcard/Movies", folderPathSet = emptySet())
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `folder videos - at root excludes videos inside known folders`() {
        val videos = listOf(
            video(1, path = "/sdcard/Movies/a.mp4"),
            video(2, path = "/sdcard/root_video.mp4"),
        )
        val folderPathSet = setOf("/sdcard/Movies")
        val result = computeFolderVideos(videos, viewMode = 0, currentFolderPath = null, folderPathSet = folderPathSet)
        assertEquals(listOf(2L), result.map { it.id })
    }

    // ===== computeBreadcrumbPath =====

    @Test
    fun `breadcrumb builds chain from root to current`() {
        val folders = listOf(
            folder("/sdcard", null),
            folder("/sdcard/Movies", "/sdcard"),
            folder("/sdcard/Movies/sub", "/sdcard/Movies"),
        )
        val breadcrumb = computeBreadcrumbPath(folders, "/sdcard/Movies/sub")
        assertEquals(listOf("/sdcard/Movies", "/sdcard/Movies/sub"), breadcrumb)
    }

    @Test
    fun `breadcrumb stops at storage root`() {
        val folders = listOf(
            folder("/storage/emulated/0", null),
            folder("/storage/emulated/0/Movies", "/storage/emulated/0"),
        )
        val breadcrumb = computeBreadcrumbPath(folders, "/storage/emulated/0/Movies")
        assertEquals(listOf("/storage/emulated/0/Movies"), breadcrumb)
    }

    @Test
    fun `breadcrumb returns empty when not in a folder`() {
        assertEquals(emptyList(), computeBreadcrumbPath(emptyList(), null))
    }
}
