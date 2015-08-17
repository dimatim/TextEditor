import java.awt.*
import java.awt.event.*
import java.io.File
import java.util.*
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
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

fun showPopup(comp: MyTextPane, rectangle: Rectangle) {
    val dialog = JDialog()
    dialog.layout = BorderLayout();
    dialog.isUndecorated = true;
    dialog.add(buildContextualMenu(comp, dialog, rectangle.javaClass))
    dialog.location = Point(frame.locationOnScreen.x + rectangle.x, frame.locationOnScreen.y + rectangle.y + 75)  //FIXME needs universal coords
    dialog.isVisible = true
    dialog.pack()
}

fun<T> buildContextualMenu(editor: MyTextPane, dialog: JDialog, c: Class<T>): JScrollPane {
    val data = HashMap<Pair<String, String>, List<String>>()
    c.methods forEach { data.put(Pair(it.name, it.returnType.simpleName), it.parameterTypes map { it.simpleName }) }
    val dataset = data.toList() map { it.first } sortBy { it.first }
    val list = JList((data map  { it.key.first + it.value.join(separator = ",", prefix = "(", postfix = ") : ") + it.key.second } sortBy { it }).toTypedArray())
    list.selectionMode = ListSelectionModel.SINGLE_SELECTION;
    list.layoutOrientation = JList.VERTICAL;
    val listScroller = JScrollPane(list);
    listScroller.maximumSize = Dimension(450, 200);
    var methodCandidate: String
    list.addKeyListener(object : KeyListener {
        override fun keyTyped(e: KeyEvent?) {

        }

        override fun keyPressed(e: KeyEvent?) {
            if (e?.keyCode == KeyEvent.VK_ESCAPE) dialog.isVisible = false
            if (e?.keyCode == KeyEvent.VK_ENTER) {
                document.insertString(editor.caretPosition, methodCandidate, null)
                dialog.isVisible = false
            }

        }

        override fun keyReleased(e: KeyEvent?) {
        }
    })
    list.addFocusListener(object : FocusListener {
        override fun focusLost(e: FocusEvent?) {
            dialog.isVisible = false
        }

        override fun focusGained(e: FocusEvent?) {
        }

    })
    list.addListSelectionListener({ e: ListSelectionEvent? -> methodCandidate = dataset.get(e?.lastIndex!!).first })
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