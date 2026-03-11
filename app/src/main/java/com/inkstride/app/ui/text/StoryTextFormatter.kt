package com.inkstride.app.ui.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle

object StoryTextFormatter {
    private const val START_TAG = "<i>"
    private const val END_TAG = "</i>"

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