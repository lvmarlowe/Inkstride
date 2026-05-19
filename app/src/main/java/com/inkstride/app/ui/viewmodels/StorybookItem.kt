package com.inkstride.app.ui.viewmodels

import com.inkstride.app.data.repositories.StorybookSegment

/**
 * StorybookItem: Row model for the Storybook LazyColumn.
 * Segments and the new-memories divider share one ordered list so the divider
 * sits at a stable index and LazyColumn can key items correctly.
 */
sealed class StorybookItem {
    data class Segment(val segment: StorybookSegment, val isNew: Boolean) : StorybookItem()
    data object NewDivider : StorybookItem()
}