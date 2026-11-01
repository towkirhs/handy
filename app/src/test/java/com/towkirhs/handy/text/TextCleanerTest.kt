package com.towkirhs.handy.text

import org.junit.Assert.assertEquals
import org.junit.Test

class TextCleanerTest {
    @Test
    fun dropsNonSpeechTags() {
        assertEquals("", TextCleaner.clean(" [BLANK_AUDIO] "))
        assertEquals("Hello there.", TextCleaner.clean("[MUSIC] Hello there. (laughter)"))
    }

    @Test
    fun removesFillersAndStutters() {
        assertEquals(
            "So I think we should go.",
            TextCleaner.clean(" Um, so I I think we should, uh, go.")
        )
        assertEquals("I want to go home.", TextCleaner.clean("I want to to go home."))
        assertEquals("Okay.", TextCleaner.clean("Hmm, okay."))
    }

    @Test
    fun dropsLeadInPhrasesAtSentenceStart() {
        assertEquals("We should go.", TextCleaner.clean("I mean, we should go."))
    }

    @Test
    fun scratchThatRemovesPreviousSentence() {
        assertEquals(
            "Let's meet at 4.",
            TextCleaner.clean("Let's meet at 3. Scratch that. Let's meet at 4.")
        )
    }

    @Test
    fun newLineCommand() {
        assertEquals(
            "Send the email.\nThanks, John.",
            TextCleaner.clean("Send the email. New line. Thanks, John.")
        )
    }

    @Test
    fun fixesCapitalsButNotAbbreviations() {
        assertEquals("This is a test. I think so.", TextCleaner.clean("this is a test. i think so."))
        assertEquals("I'm here.", TextCleaner.clean("i'm here."))
        assertEquals("Use tools, e.g. the hammer.", TextCleaner.clean("Use tools, e.g. the hammer."))
    }
}
