package com.towkirhs.handy.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplyGuardTest {
    @Test
    fun keepsAPlainCorrection() {
        assertEquals("I went home.", ReplyGuard.accept("i has went home", "I went home."))
    }

    @Test
    fun stripsChattyPreambles() {
        assertEquals(
            "I went home.",
            ReplyGuard.accept("i has went home", "Here is the corrected text: I went home.")
        )
        assertEquals("I went home.", ReplyGuard.accept("i has went home", "Sure! Corrected: I went home."))
    }

    @Test
    fun keepsAPreambleTheSpeakerActuallySaid() {
        val said = "Here is what I think: we should go."
        assertEquals(said, ReplyGuard.accept(said, said))
    }

    @Test
    fun stripsCodeFencesAndWrappingQuotes() {
        assertEquals("I went home.", ReplyGuard.accept("i went home", "```\nI went home.\n```"))
        assertEquals("I went home.", ReplyGuard.accept("i went home", "\"I went home.\""))
    }

    @Test
    fun keepsQuotesWhenTheSpeakerQuotedToo() {
        assertEquals("\"I went home.\"", ReplyGuard.accept("\"i went home\"", "\"I went home.\""))
    }

    @Test
    fun rejectsEmptyReplies() {
        assertNull(ReplyGuard.accept("i went home", "   "))
    }

    @Test
    fun rejectsAnAnswerInsteadOfACorrection() {
        val answer = "The capital of France is Paris. ".repeat(6).trim()
        assertNull(ReplyGuard.accept("what is the capital of france", answer))
    }

    @Test
    fun rejectsAReplyThatDroppedMostOfTheText() {
        assertNull(ReplyGuard.accept("so we went to the store and bought some milk and eggs", "Ok."))
    }
}
