import java.awt.Color

/**
 * Created by Dima on 22-Aug-15.
 */
//colors
val backgroundColor = Color.decode("#242424")
val fontColor = Color.decode("#D1D1D1")

//highlight patterns & styles
val H_STRINGS = "strings"
val H_KEYWORDS = "keywords"
val H_DEFAULT = "default"
val H_ERROR = "error"

val patternMap = linkedMapOf(
        H_DEFAULT to """.*""",
        H_STRINGS to """"[^"\n]*"""",
        H_KEYWORDS to """\b(private|public|fun|var|val|class|override|for|while|if|true|false|super|import|return|null)\b""",//TODO generate dynamically if possible
        H_ERROR to """\w+ *: *\w+"""
)

val colorMap = mapOf(
        H_DEFAULT to fontColor,
        H_STRINGS to Color.GREEN,
        H_KEYWORDS to Color.ORANGE,
        H_ERROR to Color.RED
)

