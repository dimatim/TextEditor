package editor

import editor.backend.extractClassInfo
import editor.backend.isValidType
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.Utilities

/**
 * Created by Dima on 22-Aug-15.
 */
class EditorDocument : DefaultStyledDocument() {

    override fun insertString(offset: Int, str: String?, a: AttributeSet?) {
        super.insertString(offset, str, a)
        if (str?.contains('\n') ?: false) {
            applyStyles(offset, str ?: "")
            extractClassInfo(getText(offset, str?.length() ?: 0))
        } else {
            val offsets = getParagraphOffsets(offset)
            applyStyles(offsets.first, offsets.second)
            extractClassInfo(getText(offsets.first, offsets.second))
        }
    }

    override fun remove(offset: Int, len: Int) {
        super.remove(offset, len)
        val offsets = getParagraphOffsets(offset)
        applyStyles(offsets.first, offsets.second)
    }

    private fun applyStyles(offset: Int, str: String) {
        applyStyles(offset, str.length())
    }

    public fun getParagraphOffsets(offset: Int): Pair<Int, Int> {
        val paragraphElement = getParagraphElement(offset)
        var start = paragraphElement?.startOffset ?: 0
        val length = (paragraphElement?.endOffset ?: 0) - start
        return start to length
    }

    private fun applyStyles(start: Int, length: Int) {
        var pattern: Pattern
        var matcher: Matcher
        val content = try {
            getText(start, length)
        } catch (e: BadLocationException) {
            println("exception: start = $start end = $length"); ""
        }

        fun mark(regex: String, type: String): Unit {
            pattern = Pattern.compile(regex)
            matcher = pattern.matcher(content)
            while (matcher.find()) {
                var off = 0;
                if (type.equals(H_ERROR)) {
                    val result = matcher.group().split(":")
                    if (!isValidType(result[1].trim()))
                        off = result[0].length() + 1
                    else continue
                } else if (type.equals(H_FUN_NAME)) {
                    off = 4
                }
                setCharacterAttributes(start + matcher.start() + off, matcher.end() - matcher.start() - off, getStyle(type), false)
            }
        }
        patternMap forEach { mark(it.value, it.key) }
    }

    private fun getToken(startOffset: Int = 0, lengthOffset: Int = 0): String {
        var start = Utilities.getWordStart(editorPane, editorPane.caretPosition + startOffset)
        var length = Utilities.getWordEnd(editorPane, editorPane.caretPosition + lengthOffset) - start
        var token = document.getText(start, length)
        return (
                if (token == "." && editorPane.caretPosition == start) ""
                else if (token == "" || token == ".") token
                else token.substring(0, editorPane.caretPosition - start)
                ).replace("\n", "")
    }

    public fun getActiveToken(char: Char? = null): Pair<String, Int> {
        var token = getToken()
        if (token == "" && editorPane.caretPosition > 0) {
            //get previous token
            token = getToken(-1, -1)
            if (token.isNotEmpty() && !token.contains('.') && (token.length() > 1 || token.get(0).isJavaLetter()))//if is valid
                return token to if (token.isNotEmpty() && token.get(0).isUpperCase()) T_TYPE else T_VAR
        }
        if (token == ".") {
            //get preceding token
            token = getToken(startOffset = -2)
            return token to T_METHOD
        } else if (token.contains('.')) {
            return token to T_METHOD
        }
        if (char != null)
            token = token.concat(char.toString())
        return token to if (token.isNotEmpty() && token.get(0).isUpperCase()) T_TYPE else T_VAR
    }

    fun getActiveTokenLength(): Int {
        val token = getActiveToken().first
        return if (token.contains('.')) {
            val (varName, prefix) = token.split(".", limit = 2)
            return prefix.length
        } else getActiveToken().first.length()
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

    var caretPos = 0
}

