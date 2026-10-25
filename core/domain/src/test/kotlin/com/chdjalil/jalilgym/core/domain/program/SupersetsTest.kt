package com.chdjalil.jalilgym.core.domain.program

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SupersetsTest {

    private val ids = listOf("a", "b", "c", "d")

    @Test
    fun `linking two exercises groups them under the first id`() {
        assertThat(toggleSupersetLink(ids, listOf(null, null, null, null), 1)).containsExactly(null, "b", "b", null).inOrder()
    }

    @Test
    fun `linking extends a run into a tri-set`() {
        val groups = toggleSupersetLink(ids, listOf("a", "a", null, null), 1)
        assertThat(groups).containsExactly("a", "a", "a", null).inOrder()
        assertThat(groups.isLinkedToNext(1)).isTrue()
        assertThat(groups.isLinkedToNext(2)).isFalse()
    }

    @Test
    fun `unlinking in the middle splits the run`() {
        assertThat(toggleSupersetLink(ids, listOf("a", "a", "a", "a"), 1)).containsExactly("a", "a", "c", "c").inOrder()
        assertThat(toggleSupersetLink(ids, listOf("a", "a", "a", null), 0)).containsExactly(null, "b", "b", null).inOrder()
    }

    @Test
    fun `out of range index is ignored`() {
        val groups = listOf<String?>(null, null, null, null)
        assertThat(toggleSupersetLink(ids, groups, 3)).isEqualTo(groups)
        assertThat(toggleSupersetLink(ids, groups, -1)).isEqualTo(groups)
    }
}
