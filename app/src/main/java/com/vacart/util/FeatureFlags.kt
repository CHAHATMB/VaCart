package com.vacart.util

object FeatureFlags {
    /**
     * Feature Flag: Enables Bottom Sheet UI for Train Search & Journey Date selection.
     * When true: Tapping input fields opens bottom sheets for searching and selecting options.
     * When false: Uses standard inline Dropdown menus.
     */
    var isBottomSheetSearchEnabled: Boolean = true

    /**
     * Feature Flag: Enables the Chat tab in bottom navigation.
     * When true: Chat tab is shown in the bottom bar.
     * When false: Chat tab is hidden.
     */
    var isChatEnabled: Boolean = false
}

