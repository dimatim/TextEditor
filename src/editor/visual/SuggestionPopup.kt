package editor.visual

import editor.backend.*
import editor.*
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.*
import javax.swing.*

/**
 * Created by Dima on 22-Aug-15.
 */
private var dialog: JDialog? = null

var popupShown = false

var suggestionComplete: Boolean = false
    get() = if (popupShown) {
        popupShown = false
        backup = Pair(listOf(), arrayOf())
        true
    } else false

private var backup: Pair<List<String>, Array<String>> = Pair(listOf(), arrayOf())

var currentField: Pair<String, Int> = ("" to 1)

private var currentClassList: Pair<List<String>, Array<String>> = backup
    get() = if (!popupShown) {
        backup = Pair(listOf(), arrayOf())
        backup
    } else {
        backup =
                if (currentField.first.isNotBlank()) {
                    when (currentField.second) {
                        T_METHOD -> {
                            if (isValidVar(currentField.first))
                                getData(getClassForVar(currentField.first))
                            else
                                Pair(listOf(), arrayOf())
                        }
                        T_TYPE -> getData(getMatchingClasses(currentField.first))
                        T_VAR -> Pair<List<String>, Array<String>>(listOf(), arrayOf())//TODO var suggestion
                        else -> Pair<List<String>, Array<String>>(listOf(), arrayOf())
                    }
                } else Pair<List<String>, Array<String>>(listOf(), arrayOf())
        backup
    }

private var addSpace: String = ""
    get() = if (shouldSuggestTypes()) " " else ""

val popupKeyListener: KeyListener = object : KeyListener {
    var caretPos: Int = 0

    override fun keyPressed(e: KeyEvent?) {
        if (e?.keyCode == KeyEvent.VK_ESCAPE) dialog?.isVisible = false
        else if (e?.keyCode == KeyEvent.VK_UP && dialog?.isVisible ?: false) {
            list.selectedIndex = if (list.selectedIndex > 0) (list.selectedIndex - 1) else (list.model.size - 1)
            list.ensureIndexIsVisible(list.selectedIndex)
        } else if (e?.keyCode == KeyEvent.VK_DOWN && dialog?.isVisible ?: false) {
            list.selectedIndex = if (list.selectedIndex > list.model.size - 1) 0 else (list.selectedIndex + 1)
            list.ensureIndexIsVisible(list.selectedIndex)
        } else if (e?.keyCode == KeyEvent.VK_ENTER && dialog?.isVisible ?: false) {
            val completionMatch = listSet.get(list.selectedIndex)
            document.insertString(editorPane.caretPosition, completionMatch.substring(document.getCurrentToken().length()) /*+ addSpace*/, null)
            dialog?.isVisible = false
        }
        caretPos = editorPane.caretPosition
    }

    override fun keyTyped(e: KeyEvent?) {
        if (dialog?.isVisible ?: false && e?.keyChar?.isLetterOrDigit() ?: false) {
            updatePopup(e?.keyChar)
        }
    }

    override fun keyReleased(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_CONTROL,
            KeyEvent.VK_SPACE,
            KeyEvent.VK_UP,
            KeyEvent.VK_DOWN -> {
                if (dialog?.isVisible ?: false ) editorPane.caretPosition = caretPos
            }
            KeyEvent.VK_LEFT,
            KeyEvent.VK_RIGHT -> {
                if (dialog?.isVisible ?: false ) updatePopup()
            }
            else -> {
                if (dialog?.isVisible ?: false && e?.keyChar?.isLetterOrDigit() ?: false) updatePopup()
            }
        }
        println("caret position = ${editorPane.caretPosition}, ${document.getParagraphOffsets(editorPane.caretPosition)}")
    }
}

private val listModel = DefaultListModel<String>()
private var listSet = listOf("")
private var list = JList(listModel)

private fun updateList() {
    val cl = currentClassList
    if (cl.first.isNotEmpty()) {
        val (dataSet, listArr) = cl
        listModel.clear()
        listArr forEach { listModel.addElement(it) }
        listSet = dataSet
        list.selectedIndex = 0
        list.ensureIndexIsVisible(0)
    }
    dialog?.isVisible = cl.first.isNotEmpty()
}

private fun setupDialog(): JDialog {
    val dialog = JDialog()
    dialog.layout = BorderLayout();
    dialog.isUndecorated = true;
    dialog.isFocusable = false
    dialog.isAutoRequestFocus = false
    dialog.add(buildContextualMenu(dialog))
    return dialog
}

private fun buildContextualMenu(dialog: JDialog): JScrollPane {
    list.selectionMode = ListSelectionModel.SINGLE_SELECTION;
    list.layoutOrientation = JList.VERTICAL;
    val listScroller = JScrollPane(list);
    listScroller.maximumSize = Dimension(450, 200);
    list.addKeyListener(popupKeyListener)
    list.addFocusListener(object : FocusListener {
        override fun focusLost(e: FocusEvent?) {
            dialog.isVisible = false
        }

        override fun focusGained(e: FocusEvent?) {
        }

    })
    return listScroller
}

fun updatePopup(char: Char? = null) {
    currentField = getActiveToken(char)
    updateList()
}

fun showPopup() {
    if (dialog == null)
        dialog = setupDialog()
    popupShown = true
    updatePopup()
    val rectangle = editorPane.modelToView(editorPane.caretPosition)
    dialog?.location = Point(frame.locationOnScreen.x + rectangle.x, frame.locationOnScreen.y + rectangle.y + 75)  //FIXME needs universal coords
    dialog?.isVisible = true
    dialog?.pack()
}

private fun mapMethods(c: Class<*>): HashMap<Pair<String, String>, List<String>> {
    val data = HashMap<Pair<String, String>, List<String>>()
    c.methods forEach { data.put(Pair(it.name, it.returnType.simpleName), it.parameterTypes map { it.simpleName }) }
    return data
}

private fun getData(c: Class<*>): Pair<List<String>, Array<String>> {
    val data = mapMethods(c)
    val dataset = data.toList() map { it.first.first } sortedBy  { it }
    val methodList = (data map  { it.key.first + it.value.join(separator = ",", prefix = "(", postfix = ") : ") + it.key.second } sortedBy { it }).toTypedArray()
    return dataset to methodList
}

private fun getData(c: List<Class<*>>): Pair<List<String>, Array<String>> {
    val dataset = c map { it.simpleName } sortedBy { it }
    return dataset to dataset.toTypedArray()
}

fun getActiveToken(char: Char?): Pair<String, Int> {
    //TODO merge with getCurrentToken
    val offsets = document.getParagraphOffsets(editorPane.caretPosition)
    var line = document.getText(offsets.first, offsets.second).replace("\n", "")
    var i = editorPane.caretPosition - offsets.first - 1 + if (char != null) {
        line = line.concat(char.toString()); 1
    } else 0
    var value = StringBuilder()
    val method = if (line.get(i).equals('.')) {
        --i; true
    } else false
    while (i >= 0 && line.get(i).isLetterOrDigit()) {
        value.append(line.get(i--))
    }
    val result = value.reverse().toString()
    return Pair(result, if (method) T_METHOD else if (result.isNotEmpty() && result.get(0).isUpperCase()) T_TYPE else T_VAR)
}

fun shouldSuggestTypes(): Boolean {
    var i = editorPane.caretPosition - 1
    while (i >= 0 && document.getText(i, 1).charAt(0).isLetterOrDigit()) {
        i -= 1;
    }
    return document.getText(i + 1, 1).charAt(0).isUpperCase()
}

fun shouldSuggestMethods(): Boolean = editorPane.caretPosition != 0 && document.getText(editorPane.caretPosition - 1, 1) == "."
