package editor.visual

import editor.backend.*
import editor.startEditor
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.ActionEvent
import javax.swing.*

/**
 * Created by Dima on 25-Aug-15.
 */

fun showPathDialog() {
    val panel = JDialog()
    panel.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    panel.title = "Path to JDK"
    panel.preferredSize = Dimension(500, 150)
    panel.isResizable = false
    val label = JLabel("Specify the path to JDK")
    val editText = JTextField()
    editText.preferredSize = Dimension(400, 25)
    val button = JButton("...")
    button.addActionListener({ e: ActionEvent -> editText.text = chooseDir() })
    val ok = JButton("OK")
    ok.addActionListener({ e: ActionEvent -> if (editText.text != null) {savePath(editText.text); panel.dispose(); startEditor()} })
    val cancel = JButton("Cancel")
    cancel.addActionListener({ e: ActionEvent -> panel.dispose() })
    var dim = Toolkit.getDefaultToolkit().screenSize;
    panel.setLocation(dim.width / 2 - panel.preferredSize.width / 2, dim.height / 2 - panel.preferredSize.height / 2);
    val springLayout = SpringLayout()
    panel.layout = springLayout
    panel.add(label)
    panel.add(editText)
    panel.add(button)
    panel.add(ok)
    panel.add(cancel)
    springLayout.putConstraint(SpringLayout.NORTH, label, 15, SpringLayout.NORTH, panel)
    springLayout.putConstraint(SpringLayout.WEST, label, 25, SpringLayout.WEST, panel)
    springLayout.putConstraint(SpringLayout.NORTH, editText, 5, SpringLayout.SOUTH, label)
    springLayout.putConstraint(SpringLayout.WEST, editText, 25, SpringLayout.WEST, panel)
    springLayout.putConstraint(SpringLayout.WEST, button, 5, SpringLayout.EAST, editText)
    springLayout.putConstraint(SpringLayout.NORTH, button, 5, SpringLayout.SOUTH, label)
    springLayout.putConstraint(SpringLayout.NORTH, ok, 15, SpringLayout.SOUTH, button)
    springLayout.putConstraint(SpringLayout.WEST, ok, 340, SpringLayout.WEST, panel)
    springLayout.putConstraint(SpringLayout.NORTH, cancel, 15, SpringLayout.SOUTH, button)
    springLayout.putConstraint(SpringLayout.WEST, cancel, 10, SpringLayout.EAST, ok)
    panel.isVisible = true
    panel.pack()
}