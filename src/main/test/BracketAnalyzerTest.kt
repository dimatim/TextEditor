import editor.parser.getOpenBraces
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Created by Dima on 24-Sep-15.
 */
class BracketAnalyzerTest {
    @Test fun testBrackets() {
        assertEquals(getOpenBraces("{{{}}{"), 2)
    }
}