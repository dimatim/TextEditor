import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

/**
 * Created by Dima on 22-Aug-15.
 */
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