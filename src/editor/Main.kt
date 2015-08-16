import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.text
import javax.swing.text.*

/**
 * Created by Dima on 15-Aug-15.
 */

val backgroundColor = Color.decode("#242424")
val fontColor = Color.decode("#D1D1D1")

var frame = JFrame("Text Editor")
val document = MyDocument()
var currentFile: File? = null

fun main(args: Array<String>) {
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

fun buildMenuBar(): JMenuBar {
    val bar = JMenuBar()
    bar.add(setupMenu("File", mapOf("New" to ::newFile, "Open" to ::openFile, "Save" to ::saveFile)))
    bar.add(setupMenu("Build", mapOf("Build & Restart" to ::buildRestart)))
    return bar
}

fun setupMenu(name: String, map: Map<String, () -> Unit>): JMenu {
    val menu = JMenu(name)
    for (e in map)
        menu.add(setupButton(e.key, e.value))
    return menu
}

fun setupButton(name: String, func: () -> Unit): JMenuItem {
    val button = JMenuItem(name)
    button.preferredSize = Dimension(200, 25)
    button.addActionListener({ e: ActionEvent ->
        run {
            if (e.source == button) {
                func()
            }
        }
    })
    return button
}

fun setupEditorPane(): MyTextPane {
    val editorPane = MyTextPane()
    setupKeyBindings(editorPane)
    editorPane.background = backgroundColor
    editorPane.foreground = fontColor
    editorPane.caretColor = Color.WHITE
    editorPane.font = Font("Monospaced", Font.PLAIN, 14)
    editorPane.styledDocument = setupDocument()
    return editorPane
}

private fun setupDocument(): DefaultStyledDocument {
    generateStyles(document, mapOf("strings" to Color.GREEN, "keywords" to Color.ORANGE, "default" to fontColor))
    return document
}

fun setupKeyBindings(editor: MyTextPane) {
    editor.inputMap.put(KeyStroke.getKeyStroke("control SPACE"), object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            showPopup(editor, editor.modelToView(editor.caretPosition))
        }
    })
}

fun showPopup(comp: Component, rectangle: Rectangle) {
    val dialog = JDialog()
    dialog.layout = BorderLayout();
    dialog.isUndecorated = true;
    dialog.add(buildContextualMenu(dialog, rectangle.javaClass))
    dialog.location = Point(rectangle.x, rectangle.y + 25)  //FIXME needs universal coords
    dialog.isVisible = true
    dialog.pack()
    /*val popup = buildContextualMenu(rectangle.javaClass)
    popup.show(comp, rectangle.x, rectangle.y + 25)*/
}

fun<T> buildContextualMenu(dialog: JDialog, c: Class<T>): JScrollPane {
    val list = JList((c.methods map  { it.name }).toTypedArray())
    list.selectionMode = ListSelectionModel.SINGLE_SELECTION;
    list.layoutOrientation = JList.VERTICAL;
    val listScroller = JScrollPane(list);
    listScroller.preferredSize = Dimension(250, 200);
    list.addKeyListener(object : KeyListener {
        override fun keyTyped(e: KeyEvent?) {

        }

        override fun keyPressed(e: KeyEvent?) {
            if (e?.keyCode == KeyEvent.VK_ESCAPE) dialog.isVisible = false
        }

        override fun keyReleased(e: KeyEvent?) {
        }
    })
    return listScroller
}

fun generateStyles(pn: text.DefaultStyledDocument, map: Map<String, Color>) {
    for (e in map) {
        val style = pn.addStyle(e.key, null);
        StyleConstants.setForeground(style, e.value);
    }
}

private fun printFontList() {
    var fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames;

    for (i in fonts) {
        println(i);
    }
}

class MyDocument : DefaultStyledDocument() {

    override fun insertString(offset: Int, str: String?, a: AttributeSet?) {
        super.insertString(offset, str, a)
        if (str?.contains('\n') ?: false)
            applyStyles(offset, str ?: "")
        else
            applyStyles(getParagraphElement(offset))
    }

    override fun remove(offs: Int, len: Int) {
        super.remove(offs, len)
        applyStyles(getParagraphElement(offs))
    }

    private fun applyStyles(offset: Int, str: String) {
        applyStyles(offset, str.length())
    }

    private fun applyStyles(paragraphElement: Element?) {
        var start = paragraphElement?.startOffset ?: 0
        val length = (paragraphElement?.endOffset ?: 0) - start
        applyStyles(start, length)
    }

    private fun applyStyles(start: Int, length: Int) {
        val content = try {
            getText(start, length)
        } catch (e: BadLocationException) {
            println("exception: start = $start end = $length"); ""
        }

        setCharacterAttributes(start, length, getStyle("default"), false)
        var pattern = Pattern.compile(""""[^"\n]*"""")
        var matcher = pattern.matcher(content)
        fun mark(type: String): Unit = setCharacterAttributes(start + matcher.start(), matcher.end() - matcher.start(), getStyle(type), false)
        while (matcher.find()) {
            mark("strings")
        }
        pattern = Pattern.compile("""\b(private|public|fun|var|val|class|override|for|while|if|true|false|super|import|return|null)\b""")
        matcher = pattern.matcher(content)
        while (matcher.find()) {
            mark("keywords")
        }
    }

}

class MyTextPane : JTextPane() {

    override fun getScrollableTracksViewportWidth(): Boolean {
        return getUI().getPreferredSize(this).width <= parent.size.width;
    }
}