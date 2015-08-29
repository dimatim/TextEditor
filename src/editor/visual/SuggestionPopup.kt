package editor.visual

import document
import editor.backend.getClassForVar
import editor.backend.getMatchingClasses
import editor.backend.isValidVar
import editorPane
import frame
import getVarName
import shouldSuggestMethods
import shouldSuggestTypes
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

private var popupShown = false

var suggestionComplete: Boolean = false
    get() = if (popupShown) {
        popupShown = false
        true
    } else false

private var addSpace: String = ""
    get() = if (shouldSuggestTypes()) " " else ""

val popupKeyListener: KeyListener = object : KeyListener {
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
            popupShown = true
        }
    }

    override fun keyTyped(e: KeyEvent?) {

    }

    override fun keyReleased(e: KeyEvent?) {
    }
}

private val listModel = DefaultListModel<String>()
private var listSet = listOf("")
private var list = JList(listModel)

private fun getClassList(): List<Class<*>> {
    val prefix = document.getCurrentToken()
    val classes = getMatchingClasses(prefix)
    return classes
}

private fun updateList() {
    val clsList: List<Class<*>> = if (shouldSuggestMethods()) {
        val varName = getVarName()
        if (isValidVar(varName))
            listOf(getClassForVar(getVarName()))
        else
            listOf<Class<*>>()
    } else if (shouldSuggestTypes()) {
        getClassList()
    } else listOf<Class<*>>()
    val (dataSet, listArr) = if (clsList.size() == 1) getData(clsList.get(0)) else getData(clsList)
    listModel.clear()
    listArr forEach { listModel.addElement(it) }
    listSet = dataSet
    list.selectedIndex = 0
    list.ensureIndexIsVisible(0)
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

fun showPopup() {
    if (dialog == null)
        dialog = setupDialog()
    updateList()
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