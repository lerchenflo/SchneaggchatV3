package org.lerchenflo.schneaggchatv3mp.games.presentation.undercover

data class UndercoverWordPair(
    val civilianWord: String,
    val undercoverWord: String
)

val UNDERCOVER_WORD_PAIRS: List<UndercoverWordPair> = listOf(
    UndercoverWordPair(civilianWord = "Beach", undercoverWord = "Island"),
    UndercoverWordPair(civilianWord = "Coffee", undercoverWord = "Tea"),
    UndercoverWordPair(civilianWord = "Cat", undercoverWord = "Dog"),
    UndercoverWordPair(civilianWord = "Pizza", undercoverWord = "Burger"),
    UndercoverWordPair(civilianWord = "Football", undercoverWord = "Basketball"),
    UndercoverWordPair(civilianWord = "Doctor", undercoverWord = "Nurse")
)
