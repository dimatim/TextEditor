package editor.parser

/**
 * Created by Dima on 21-Sep-15.
 */


fun getOpenBraces(s:String): Int {
    var braceCount = 0
    var index = 0
    while (index < s.length()) {
        if (s.get(index) == '{') {
            braceCount++
        } else if (s.get(index) == '}')
            if (braceCount > 0) braceCount--
        index++
    }
    return braceCount
}

fun extractEnclosingContext(s: String): List<String> {
    var braceCount = 0
    val tokens = arrayListOf<String>()
    var index = s.length() - 1
    while (index >= 0) {
        if (s.get(index) == '{') {
            if (braceCount == 0) {
                tokens.add(getToken(s, index))
            } else
                braceCount--
        } else if (s.get(index) == '}')
            braceCount++
        index--
    }
    return tokens
}

private fun getToken(s: String, ind: Int): String {
    var builder = StringBuilder()
    var isParams = false
    var isSpace = true
    var chr = ind - 1
    while (chr >= 0) {
        if (s.get(chr) != ' ') {
            isSpace = false
            if (s.get(chr) == ')')
                isParams = true
            else if (s.get(chr) == '(') {
                isParams = false
                isSpace = s.get(chr - 1) == ' '
            } else if (s.get(chr) == ';')
                if (!isParams && !isSpace)
                    break
        } else if (s.get(chr) == ' ')
            if (!isParams && !isSpace)
                break
        builder.append(s.get(chr--))
    }
    return builder.reverse().toString().replace("\n", "")
}