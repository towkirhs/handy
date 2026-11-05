package com.towkirhs.handy.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VocabTest {
    @Test
    fun learnsEachChangedStretch() {
        assertEquals(
            listOf("ultapalt a" to "Ulta palta", "onk" to "onek"),
            Vocab.diffFixes("Ultapalt a chinta ashe onk", "Ulta palta chinta ashe onek")
        )
    }

    @Test
    fun neighbouringChangesAreOneFix() {
        assertEquals(
            listOf("ace sundae" to "ase sondhay"),
            Vocab.diffFixes("kal tuition ace sundae", "kal tuition ase sondhay")
        )
    }

    @Test
    fun ignoresCaseAndPunctuation() {
        assertEquals(emptyList<Pair<String, String>>(), Vocab.diffFixes("Hello, world.", "hello world"))
    }

    @Test
    fun skipsLongRewrites() {
        assertEquals(emptyList<Pair<String, String>>(), Vocab.diffFixes("one two three four five", "a b c d e"))
    }
}
