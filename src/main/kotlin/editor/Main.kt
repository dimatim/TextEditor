package editor

import editor.backend.*
import editor.parser.getNewLineOffset
import editor.visual.*
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
val editorPane = MyTextPane()
var currentFile: File? = null

fun main(args: Array<String>) {
    if (File(CONFIG_PATH).exists()) {
        startEditor()
    } else {
        val path = System.getenv("JAVA_HOME")
        if (path != null) {
            savePath(path)
            startEditor()
        } else
            showPathDialog()
    }
}

fun startEditor() {
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
    var dim = Toolkit.getDefaultToolkit().screenSize
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
    editorPane.background = backgroundColor
    editorPane.foreground = fontColor
    editorPane.caretColor = Color.WHITE
    editorPane.font = Font("Monospaced", Font.PLAIN, 14)
    editorPane.styledDocument = setupDocument()
    return editorPane
}

private fun setupDocument(): DefaultStyledDocument {
    generateStyles(document, colorMap)
    return document
}

fun javaSyntax() {
    document.setLanguage(JAVA)
}

fun kotlinSyntax() {
    document.setLanguage(KOTLIN)
}

fun setupKeyBindings() {
    fun setBehavior(keyStroke: KeyStroke, func: () -> Unit) {
        editorPane.inputMap.put(keyStroke, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                func()
            }
        })
    }

    setBehavior(KeyStroke.getKeyStroke("control SPACE"), ::showPopup)
    setBehavior(KeyStroke.getKeyStroke('}'), ::adjustCaret)
    setBehavior(KeyStroke.getKeyStroke("ENTER"), ::adjustNewline)
    setBehavior(KeyStroke.getKeyStroke('('), {
        document.insertString(editorPane.caretPosition, "()", null)
        --editorPane.caretPosition
    })
    editorPane.addKeyListener(popupKeyListener)
}

fun adjustCaret() {
    val (s, l) = document.getParagraphOffsets(editorPane.caretPosition)
    val sequence = if (document.getText(s, l).isBlank()) {
        document.remove(s, l - 1)
        getNewLineOffset("}") + "}"
    } else "}"
    document.insertString(editorPane.caretPosition, sequence, null)
}

fun adjustNewline() {
    /*val (s, l) = document.getParagraphOffsets(editorPane.caretPosition)
    val nextChar = document.getText(editorPane.caretPosition, l - editorPane.caretPosition)*/
    val nextChar = document.getText(editorPane.caretPosition, 1)//TODO paragraph string to the right of caret
    val sequence = if (suggestionComplete) "" else ("\n" + getNewLineOffset(nextChar))
    document.insertString(editorPane.caretPosition, sequence, null)
}

fun generateStyles(pn: text.DefaultStyledDocument, map: Map<String, Color>) {
    for (e in map) {
        val style = pn.addStyle(e.key, null)
        StyleConstants.setForeground(style, e.value)
    }
}

class MyTextPane : JTextPane() {

    override fun getScrollableTracksViewportWidth(): Boolean {
        return getUI().getPreferredSize(this).width <= parent.size.width;
    }
}