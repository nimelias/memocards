package com.zatiki.memocards.navigation

sealed class Routes(val route: String) {
    data object DeckList : Routes("decks")
    data object Stats : Routes("stats")
    data object Search : Routes("search")
    data object DeckDetail : Routes("deck/{deckId}") {
        fun create(deckId: Long) = "deck/$deckId"
    }
    data object NoteEditor : Routes("note-editor/{deckId}") {
        fun create(deckId: Long) = "note-editor/$deckId"
    }
    data object Review : Routes("review/{deckId}/{advanceDays}/{queueFilter}") {
        fun create(
            deckId: Long,
            advanceDays: Int = 0,
            queueFilter: String = "all",
        ) = "review/$deckId/$advanceDays/$queueFilter"
    }
    data object Settings : Routes("settings")
    data object BookReader : Routes("book/{bookId}") {
        fun create(bookId: Long) = "book/$bookId"
    }

    companion object {
        val tabRoutes = setOf(
            DeckList.route,
            Stats.route,
            Search.route,
            Settings.route,
        )
    }
}
