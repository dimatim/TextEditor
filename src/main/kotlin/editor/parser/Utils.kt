package editor.parser

import editor.document
import editor.editorPane
import editor.keywords
import java.lang.reflect.GenericDeclaration
import javax.swing.text.Utilities

/**
 * Created by Dima on 06-Sep-15.
 */
val emptySuggestion = Pair<List<GenericDeclaration>, String>(listOf(), "")

val NONE = 0
val CLASS = 1
val METHOD = 2

fun getMatchingKeywords(): Pair<List<String>, Array<String>> {
    val token = getActiveToken()
    val kw = keywords filter { it.startsWith(token) }
    return kw to kw.toTypedArray()
}

fun getCaretContext(): Int {
    val s = document.getText(0, editorPane.caretPosition)
    var list = extractEnclosingContext(s)
    return Math.min(list.size(), METHOD)
}

fun getActiveToken(): String {
    val (s, l) = document.getParagraphOffsets(editorPane.caretPosition)
    return document.getText(s, l).substring(getActiveTokenOffset() - s, editorPane.caretPosition - s)
}

fun getActiveExpression(char: Char? = null): Pair<List<GenericDeclaration>, String> {
    val (s, l) = document.getParagraphOffsets(editorPane.caretPosition)
    var text = document.getText(s, l).substring(0, editorPane.caretPosition - s)
    if (char != null)
        text.concat(char.toString())
    return if (text.isBlank()) emptySuggestion else extractInfo(text)
}

fun getActiveTokenOffset(): Int {
    //TODO replace this
    var start = Utilities.getWordStart(editorPane, editorPane.caretPosition)
    var length = Utilities.getWordEnd(editorPane, editorPane.caretPosition) - start
    var token = document.getText(start, length).substring(0, editorPane.caretPosition - start)
    if (token == "") {
        if (editorPane.caretPosition > 0) {
            start = Utilities.getWordStart(editorPane, editorPane.caretPosition - 1)
            return start
        } else return -1
    } else if (token.contains('.')) {
        return start + token.indexOfLast { it == '.' }
    }
    return start
}