import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.*
import javax.swing.JDialog
import javax.swing.JList
import javax.swing.JScrollPane
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent

/**
 * Created by Dima on 22-Aug-15.
 */
fun showPopup(clsList: List<Class<*>>) {
    val rectangle = editor.modelToView(editor.caretPosition)
    val dialog = JDialog()
    dialog.layout = BorderLayout();
    dialog.isUndecorated = true;
    dialog.add(buildContextualMenu(dialog, clsList))
    dialog.location = Point(frame.locationOnScreen.x + rectangle.x, frame.locationOnScreen.y + rectangle.y + 75)  //FIXME needs universal coords
    dialog.isVisible = true
    dialog.pack()
}

private fun mapMethods(c: Class<*>): HashMap<Pair<String, String>, List<String>> {
    val data = HashMap<Pair<String, String>, List<String>>()
    c.methods forEach { data.put(Pair(it.name, it.returnType.simpleName), it.parameterTypes map { it.simpleName }) }
    return data
}

private fun getData(c: Class<*>) : Pair<List<String>, Array<String>> {
    val data = mapMethods(c)
    val dataset = data.toList() map { it.first.first } sortBy { it }
    val methodList = (data map  { it.key.first + it.value.join(separator = ",", prefix = "(", postfix = ") : ") + it.key.second } sortBy { it }).toTypedArray()
    return dataset to methodList
}

private fun getData(c: List<Class<*>>) : Pair<List<String>, Array<String>> {
    val dataset = c map {it.simpleName} sortBy {it}
    return dataset to dataset.toTypedArray()
}

fun buildContextualMenu(dialog: JDialog, clsList : List<Class<*>>): JScrollPane {
    val (dataset, listArr) = if (clsList.size() == 1) getData(clsList.get(0)) else getData(clsList)
    val list = JList(listArr)
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
                document.insertString(editor.caretPosition, methodCandidate.substring(document.getCurrentToken().length()), null)
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
    list.addListSelectionListener({ e: ListSelectionEvent? -> methodCandidate = dataset.get(e?.lastIndex!!) })
    return listScroller
}