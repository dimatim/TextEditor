package editor.parser

import editor.*
import java.lang.reflect.GenericDeclaration
import javax.swing.text.Utilities

/**
 * Created by Dima on 06-Sep-15.
 */
val emptySuggestion = Pair<List<GenericDeclaration>, String>(listOf(), "")

public fun getActiveExpression(char: Char? = null): Pair<List<GenericDeclaration>, String> {
    val (s, l) = document.getParagraphOffsets(editorPane.caretPosition)
    var text = document.getText(s, l).substring(0, editorPane.caretPosition - s)
    if (char != null)
        text.concat(char.toString())
    return if (text.isBlank()) emptySuggestion else extractInfo(text)
}

public fun getActiveTokenOffset(): Int {//TODO replace this
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