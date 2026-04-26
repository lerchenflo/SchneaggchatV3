package org.lerchenflo.schneaggchatv3mp.games.presentation.morse

val MORSE_CODES: Map<Char, String> = mapOf(
    'A' to ".-",   'B' to "-...", 'C' to "-.-.", 'D' to "-..",
    'E' to ".",    'F' to "..-.", 'G' to "--.",  'H' to "....",
    'I' to "..",   'J' to ".---", 'K' to "-.-",  'L' to ".-..",
    'M' to "--",   'N' to "-.",   'O' to "---",  'P' to ".--.",
    'Q' to "--.-", 'R' to ".-.",  'S' to "...",  'T' to "-",
    'U' to "..-",  'V' to "...-", 'W' to ".--",  'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
    '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
    '8' to "---..", '9' to "----."
)

private val codeToChar: Map<String, Char> = MORSE_CODES.entries.associate { it.value to it.key }

fun charForCode(code: String): Char? = codeToChar[code]

data class MorseTreeNode(
    val code: String,
    val char: Char?,
    val dot: MorseTreeNode?,  // left  = dot  (.)
    val dash: MorseTreeNode?  // right = dash (-)
)

private fun buildNode(prefix: String, depth: Int): MorseTreeNode? {
    if (depth > 5) return null
    val char = codeToChar[prefix]
    val dotChild = buildNode("$prefix.", depth + 1)
    val dashChild = buildNode("$prefix-", depth + 1)
    if (depth > 0 && char == null && dotChild == null && dashChild == null) return null
    return MorseTreeNode(prefix, char, dotChild, dashChild)
}

val MORSE_TREE: MorseTreeNode = buildNode("", 0)!!
