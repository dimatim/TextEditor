import java.awt.*
import java.awt.event.*
import java.io.File
import java.util.HashMap
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.text
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.StyleConstants

/**
 * Created by Dima on 15-Aug-15.
 */

val backgroundColor = Color.decode("#242424")
val fontColor = Color.decode("#D1D1D1")

var frame = JFrame("Text Editor")
val document = MyDocument()
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
    setupKeyBindings()
    editor.background = backgroundColor
    editor.foreground = fontColor
    editor.caretColor = Color.WHITE
    editor.font = Font("Monospaced", Font.PLAIN, 14)
    editor.styledDocument = setupDocument()
    return editor
}

private fun setupDocument(): DefaultStyledDocument {
    generateStyles(document, mapOf("strings" to Color.GREEN, "keywords" to Color.ORANGE, "default" to fontColor))
    return document
}

/**
 * Used to get methods for the current variable when pressing Ctrl+SPACE on a dot (ex: foo.)
 */
fun getVarName(): String {
    val offsets = document.getParagraphOffsets(editor.caretPosition)
    val line = document.getText(offsets.first, offsets.second)
    var i = editor.caretPosition - offsets.first - 1
    var value = StringBuilder()
    while (i > 0 && line.get(--i).isLetterOrDigit()) {
        value.append(line.get(i))
    }
    return value.reverse().toString()
}

fun shouldSuggestMethods() :Boolean = document.getText(editor.caretPosition - 1, 1) == "."

fun setupKeyBindings() {
    editor.inputMap.put(KeyStroke.getKeyStroke("control SPACE"), object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            if (shouldSuggestMethods()) {
                val varName = getVarName()
                if (isValidVar(varName))
                    showPopup(getClassForVar(varName)!!)
            }
        }
    })
}

fun showPopup(c: Class<*>) {
    val rectangle = editor.modelToView(editor.caretPosition)
    val dialog = JDialog()
    dialog.layout = BorderLayout();
    dialog.isUndecorated = true;
    dialog.add(buildContextualMenu(dialog, c))
    dialog.location = Point(frame.locationOnScreen.x + rectangle.x, frame.locationOnScreen.y + rectangle.y + 75)  //FIXME needs universal coords
    dialog.isVisible = true
    dialog.pack()
}

fun buildContextualMenu(dialog: JDialog, c: Class<*>): JScrollPane {
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