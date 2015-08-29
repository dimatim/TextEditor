import java.awt.Color

/**
 * Created by Dima on 22-Aug-15.
 */
//config
val CONFIG_PATH = "config/config.txt"

//colors
val backgroundColor = Color.decode("#242424")
val fontColor = Color.decode("#D1D1D1")
val keywordColor = Color.decode("#E68A00")
val funColor = Color.decode("#FFCC00")
val numberColor = Color.decode("#6699FF")
val stringColor = Color.decode("#338950")

//highlight patterns & styles
val H_STRINGS = "strings"
val H_KEYWORDS = "keywords"
val H_DEFAULT = "default"
val H_ERROR = "error"
val H_PUNCTUATION = "punctuation"
val H_NUMBERS = "numbers"
val H_FUN_NAME = "fun name"

private val keywords = arrayOf(
        "private", "protected", "internal", "public",
        "for", "in", "when", "while", "do", "if", "else",
        "fun", "var", "val",
        "class", "object", "abstract",
        "override",
        "true", "false", "null",
        "super", "this",
        "import",
        "return"
)

val patternMap = linkedMapOf(
        H_DEFAULT to """.*""",
        H_PUNCTUATION to """[;,]""",
        H_NUMBERS to """[0-9]+""",
        H_FUN_NAME to """fun +\w+""",
        H_KEYWORDS to """\b(${keywords.join("|")})\b""", //TODO generate dynamically if possible
        H_ERROR to """\w+ *: *\w+""",
        H_STRINGS to """"[^"\n]*""""
)

val colorMap = mapOf(
        H_DEFAULT to fontColor,
        H_PUNCTUATION to keywordColor,
        H_NUMBERS to numberColor,
        H_FUN_NAME to funColor,
        H_STRINGS to stringColor,
        H_KEYWORDS to keywordColor,
        H_ERROR to Color.RED
)

