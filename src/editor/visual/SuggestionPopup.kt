package editor.visual

import editor.*
import editor.backend.getClassForVar
import editor.backend.getMatchingClasses
import editor.backend.isValidVar
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

private var popupVisible : Boolean
    set(visible) = run{
        dialog?.isVisible = visible
        editorPane.actionMap.get("caret-down").isEnabled =  !visible
        editorPane.actionMap.get("caret-up").isEnabled =  !visible
    }
    get() = dialog?.isVisible?:false

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
                            val (varName, prefix) = currentField.first.split(".", limit = 2)
                            if (isValidVar(varName))
                                getData(getClassForVar(varName), prefix)
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
        if (e?.keyCode == KeyEvent.VK_ESCAPE) popupVisible = false
        else if (e?.keyCode == KeyEvent.VK_UP && popupVisible) {
            list.selectedIndex = if (list.selectedIndex > 0) (list.selectedIndex - 1) else (list.model.size - 1)
            list.ensureIndexIsVisible(list.selectedIndex)
        } else if (e?.keyCode == KeyEvent.VK_DOWN && popupVisible) {
            list.selectedIndex = if (list.selectedIndex >= list.model.size - 1) 0 else (list.selectedIndex + 1)
            list.ensureIndexIsVisible(list.selectedIndex)
        } else if (e?.keyCode == KeyEvent.VK_ENTER && popupVisible) {
            val completionMatch = listSet.get(list.selectedIndex)
            document.insertString(editorPane.caretPosition, completionMatch.substring(document.getActiveTokenLength()) /*+ addSpace*/, null)
            popupVisible = false
        }
        caretPos = editorPane.caretPosition
    }

    override fun keyTyped(e: KeyEvent?) {
        if (popupVisible && e?.keyChar?.isLetterOrDigit() ?: false) {
            updatePopup(e?.keyChar)
        }
    }

    override fun keyReleased(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_CONTROL,
            KeyEvent.VK_SPACE,
            KeyEvent.VK_UP,
            KeyEvent.VK_DOWN -> {
                if (popupVisible) editorPane.caretPosition = caretPos
            }
            KeyEvent.VK_LEFT,
            KeyEvent.VK_RIGHT,
            KeyEvent.VK_BACK_SPACE-> {
                if (popupVisible) updatePopup()
                var (start, length) = document.getParagraphOffsets(editorPane.caretPosition)
                document.caretPos = editorPane.caretPosition - start

            }
            else -> {
                if (popupVisible && e?.keyChar?.isLetterOrDigit() ?: false) updatePopup()
            }
        }
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
    popupVisible = cl.first.isNotEmpty()
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
    currentField = document.getActiveToken(char)
    updateList()
}

fun showPopup() {
    if (dialog == null)
        dialog = setupDialog()
    popupShown = true
    updatePopup()
    val rectangle = editorPane.modelToView(document.getActiveTokenOffset() + 1)
    dialog?.location = Point(frame.locationOnScreen.x + rectangle.x, frame.locationOnScreen.y + rectangle.y + 75)  //FIXME needs universal coords
    dialog?.pack()
}

private fun mapMethods(c: Class<*>): HashMap<Pair<String, String>, List<String>> {
    val data = HashMap<Pair<String, String>, List<String>>()
    c.methods forEach { data.put(Pair(it.name, it.returnType.simpleName), it.parameterTypes map { it.simpleName }) }
    return data
}

private fun getData(c: Class<*>, prefix: String = ""): Pair<List<String>, Array<String>> {
    val data = mapMethods(c)
    val dataset = data.toList() map { it.first.first } sortedBy  { it } filter {it.startsWith(prefix)}
    val methodList = (data filter {it.key.first.startsWith(prefix)} map  { it.key.first + it.value.join(separator = ",", prefix = "(", postfix = ") : ") + it.key.second } sortedBy { it }).toTypedArray()
    return dataset to methodList
}

private fun getData(c: List<Class<*>>): Pair<List<String>, Array<String>> {
    val dataset = c map { it.simpleName } sortedBy { it }
    return dataset to dataset.toTypedArray()
}

fun shouldSuggestTypes(): Boolean {
    var i = editorPane.caretPosition - 1
    while (i >= 0 && document.getText(i, 1).charAt(0).isLetterOrDigit()) {
        i -= 1;
    }
    return document.getText(i + 1, 1).charAt(0).isUpperCase()
}

fun shouldSuggestMethods(): Boolean = editorPane.caretPosition != 0 && document.getText(editorPane.caretPosition - 1, 1) == "."
