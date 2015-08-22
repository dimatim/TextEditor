import java.awt.*
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.*
import javax.swing.text
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.StyleConstants

/**
 * Created by Dima on 15-Aug-15.
 */

var frame = JFrame("Text Editor")
val document = EditorDocument()
val editor = MyTextPane()
var currentFile: File? = null

fun main(args: Array<String>) {
    Thread(Runnable {
        run {
            val start = System.currentTimeMillis()
            buildClassMap()
            println("load time = ${System.currentTimeMillis() - start}ms")
        }
    }).start()

    println("building gui")
    buildGUI()
}

fun buildGUI() {
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    var dim = Toolkit.getDefaultToolkit().screenSize;
    frame.minimumSize = Dimension(400, 400)
    frame.preferredSize = Dimension(1024, 768)
    frame.setLocation(dim.width / 2 - frame.preferredSize.width / 2, dim.height / 2 - frame.preferredSize.height / 2);
    frame.contentPane.add(JScrollPane(setupEditorPane()), BorderLayout.CENTER)
    frame.jMenuBar = buildMenuBar()
    frame.pack()
    frame.isVisible = true
}

fun setupEditorPane(): MyTextPane {
    setupKeyBindings()
    editor.background = backgroundColor
    editor.foreground = fontColor
    editor.caretColor = Color.WHITE
    editor.font = Font("Monospaced", Font.PLAIN, 14)
    editor.styledDocument = setupDocument()
    return editor
}

private fun setupDocument(): DefaultStyledDocument {
    generateStyles(document, colorMap)
    return document
}

/**
 * Used to get methods for the current variable when pressing Ctrl+SPACE on a dot (ex: foo.)
 */
fun getVarName(): String {//TODO merge with getCurrentToken
    val offsets = document.getParagraphOffsets(editor.caretPosition)
    val line = document.getText(offsets.first, offsets.second)
    var i = editor.caretPosition - offsets.first - 1
    var value = StringBuilder()
    while (i > 0 && line.get(--i).isLetterOrDigit()) {
        value.append(line.get(i))
    }
    return value.reverse().toString()
}

fun shouldSuggestTypes() : Boolean {
    var i = editor.caretPosition - 1
    while (i >= 0 && document.getText(i, 1).charAt(0).isLetterOrDigit()) {
        i -= 1;
    }
    return document.getText(i + 1, 1).charAt(0).isUpperCase()
}

fun shouldSuggestMethods(): Boolean = editor.caretPosition != 0 && document.getText(editor.caretPosition - 1, 1) == "."

fun setupKeyBindings() {
    editor.inputMap.put(KeyStroke.getKeyStroke("control SPACE"), object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            if (shouldSuggestMethods()) {
                val varName = getVarName()
                if (isValidVar(varName))
                    showPopup(listOf(getClassForVar(varName)))
            } else if (shouldSuggestTypes()) {
                val prefix = document.getCurrentToken()
                val classes = getMatchingClasses(prefix)
                if (classes.isNotEmpty())
                    showPopup(classes)
            }
        }
    })
}

fun generateStyles(pn: text.DefaultStyledDocument, map: Map<String, Color>) {
    for (e in map) {
        val style = pn.addStyle(e.key, null);
        StyleConstants.setForeground(style, e.value);
    }
}

class MyTextPane : JTextPane() {

    override fun getScrollableTracksViewportWidth(): Boolean {
        return getUI().getPreferredSize(this).width <= parent.size.width;
    }
}