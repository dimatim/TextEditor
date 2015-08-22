import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultStyledDocument

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
                if (type.equals("error")) {
                    val result = matcher.group().split(":")
                    if (!isValidType(result[1].trim()))
                        off = result[0].length() + 1
                    else continue
                }
                setCharacterAttributes(start + matcher.start() + off, matcher.end() - matcher.start() - off, getStyle(type), false)
            }
        }
        patternMap forEach { mark(it.value, it.key) }
    }

    fun getCurrentToken(): String {
        val offsets = document.getParagraphOffsets(editor.caretPosition)
        val line = document.getText(offsets.first, offsets.second)
        var i = editor.caretPosition - offsets.first - 1
        var value = StringBuilder()
        while (i >= 0 && line.get(i).isLetterOrDigit()) {
            value.append(line.get(i))
            i -= 1
        }
        return value.reverse().toString()
    }
}

