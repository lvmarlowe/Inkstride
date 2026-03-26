package com.inkstride.app.ui.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle

/**
 * StoryTextFormatter: Parses inline italic markup in story text for Compose display.
 * Converts paired i tags to italic spans so story content can use emphasis without a rich text engine.
 */
object StoryTextFormatter {

    // Opening tag marking the start of an italic span in story text.
    private const val START_TAG = "<i>"

    // Closing tag marking the end of an italic span in story text.
    private const val END_TAG = "</i>"

    /**
     * parseItalicMarkup: Returns an AnnotatedString with italic spans applied from i tag pairs.
     * Appends remaining text as-is when an opening or closing tag is missing to avoid data loss.
     */
    fun parseItalicMarkup(text: String): AnnotatedString {
        return buildAnnotatedString {
            var cursor = 0
            while (cursor < text.length) {
                val italicStart = text.indexOf(START_TAG, cursor)
                if (italicStart == -1) {
                    append(text.substring(cursor))
                    break
                }

                append(text.substring(cursor, italicStart))
                val contentStart = italicStart + START_TAG.length
                val italicEnd = text.indexOf(END_TAG, contentStart)

                if (italicEnd == -1) {
                    append(text.substring(italicStart))
                    break
                }

                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(text.substring(contentStart, italicEnd))
                pop()

                cursor = italicEnd + END_TAG.length
            }
        }
    }
}