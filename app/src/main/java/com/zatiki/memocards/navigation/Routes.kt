package com.zatiki.memocards.navigation

sealed class Routes(val route: String) {
    data object DeckList : Routes("decks")
    data object DeckDetail : Routes("deck/{deckId}") {
        fun create(deckId: Long) = "deck/$deckId"
    }
    data object NoteEditor : Routes("note-editor/{deckId}") {
        fun create(deckId: Long) = "note-editor/$deckId"
    }
    data object Review : Routes("review/{deckId}") {
        fun create(deckId: Long) = "review/$deckId"
    }
    data object Settings : Routes("settings")
}
