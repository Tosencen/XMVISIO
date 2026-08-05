package com.xmvisio.app.ui.main

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaListLogicTest {

    // ===== matchesMediaQuery =====

    @Test
    fun `blank query matches everything`() {
        assertTrue(matchesMediaQuery("任意标题", null, "  "))
        assertTrue(matchesMediaQuery("anything", null, ""))
    }

    @Test
    fun `matches title case-insensitively`() {
        assertTrue(matchesMediaQuery("Hello World", null, "hello"))
        assertTrue(matchesMediaQuery("hello world", null, "WORLD"))
    }

    @Test
    fun `matches artist`() {
        assertTrue(matchesMediaQuery("Song", "Artist Name", "artist"))
    }

    @Test
    fun `no match returns false`() {
        assertFalse(matchesMediaQuery("Song Title", "Artist", "zzz"))
    }

    @Test
    fun `null artist does not crash`() {
        assertFalse(matchesMediaQuery("Song Title", null, "artist"))
    }

    // ===== applyCustomOrderById =====

    private data class Item(val id: Long, val name: String)

    private val base = listOf(Item(1, "a"), Item(2, "b"), Item(3, "c"), Item(4, "d"))

    @Test
    fun `null custom order returns base list unchanged`() {
        assertEquals(base, applyCustomOrderById(base, null) { it.id })
    }

    @Test
    fun `empty custom order keeps original order`() {
        assertEquals(base, applyCustomOrderById(base, emptyList()) { it.id })
    }

    @Test
    fun `custom order reorders matching items`() {
        val ordered = applyCustomOrderById(base, listOf(3L, 1L)) { it.id }
        assertEquals(listOf(3L, 1L, 2L, 4L), ordered.map { it.id })
    }

    @Test
    fun `custom order ignores ids not in base list`() {
        val ordered = applyCustomOrderById(base, listOf(99L, 2L)) { it.id }
        assertEquals(listOf(2L, 1L, 3L, 4L), ordered.map { it.id })
    }
}
