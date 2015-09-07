package editor.parser

import editor.*
import editor.backend.resolveType
import javax.swing.text.Utilities

/**
 * Created by Dima on 06-Sep-15.
 */
public fun getActiveExpression(char: Char? = null): Pair<List<Class<*>>, String> {
    val (s, l) = document.getParagraphOffsets(editorPane.caretPosition)
    var text = document.getText(s, l).substring(0, editorPane.caretPosition - s)
    if (char != null)
        text.concat(char.toString())
    var i = text.length - 1
    var result = StringBuilder()
    var prefix = StringBuilder()
    var afterDot = false
    while (text.get(i) != '=' && text.get(i) != ':' && i > 0) {
        if (text.get(i) == '.')
            afterDot = true
        if (afterDot)
            result.append(text.get(i))
        else
            prefix.append(text.get(i))
        --i
    }
    return (resolveType(result.reverse().toString().trim())?:listOf<Class<*>>()) to prefix.reverse().toString().trim()
}

private fun getTokenToLeft(startOffset: Int = 0, lengthOffset: Int = 0): String {
    var start = Utilities.getWordStart(editorPane, editorPane.caretPosition + startOffset)
    var length = Utilities.getWordEnd(editorPane, editorPane.caretPosition + lengthOffset) - start
    var token = document.getText(start, length)
    return (
            if (token == "." && editorPane.caretPosition == start) ""
            else if (token == "" || token == ".") token
            else token.substring(0, editorPane.caretPosition - start)
            ).replace("\n", "")
}

public fun getActiveTokenToLeft(char: Char? = null): Pair<String, Int> {
    var token = getTokenToLeft()
    if (token == "" && editorPane.caretPosition > 0) {
        //get previous token
        token = getTokenToLeft(-1, -1)
        if (token.isNotEmpty() && !token.contains('.') && (token.length() > 1 || token.get(0).isJavaLetter()))//if is valid
            return token to if (token.isNotEmpty() && token.get(0).isUpperCase()) T_TYPE else T_VAR
    }
    if (token == ".") {
        //get preceding token
        token = getTokenToLeft(startOffset = -2)
        return token to T_METHOD
    } else if (token.contains('.')) {
        return token to T_METHOD
    }
    if (char != null)
        token = token.concat(char.toString())
    return token to if (token.isNotEmpty() && token.get(0).isUpperCase()) T_TYPE else T_VAR
}

fun getActiveTokenLength(): Int {
    val token = getActiveTokenToLeft().first
    return if (token.contains('.')) {
        val (varName, prefix) = token.split(".", limit = 2)
        return prefix.length
    } else getActiveTokenToLeft().first.length()
}

public fun getActiveTokenOffset(): Int {
    var start = Utilities.getWordStart(editorPane, editorPane.caretPosition)
    var length = Utilities.getWordEnd(editorPane, editorPane.caretPosition) - start
    var token = document.getText(start, length).substring(0, editorPane.caretPosition - start)
    if (token == "" && editorPane.caretPosition > 0) {
        start = Utilities.getWordStart(editorPane, editorPane.caretPosition - 1)
        return start
    } else if (token.contains('.')) {
        return start + token.indexOfLast { it == '.' }
    }
    return start
}