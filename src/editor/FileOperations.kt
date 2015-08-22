
import java.io.FileWriter
import java.nio.charset.Charset
import javax.swing.JFileChooser

/**
 * Created by Dima on 16-Aug-15.
 */
fun newFile() {
    document.remove(0, document.length)
    currentFile = null
}

fun openFile() {
    val chooser = JFileChooser()
    val returnVal = chooser.showOpenDialog(null);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
        currentFile = chooser.selectedFile;
        document.remove(0, document.length)
        val content = currentFile?.readText(Charset.defaultCharset())?.replaceAll(System.getProperty("line.separator"), "\n")
        document.insertString(0, content, null)
    } else {
        println("Open command cancelled by user.");
    }
}

fun saveFile() {
    val content = document.getText(0, document.length)
    if (currentFile != null) {
        writeToFile(content)
    } else {
        val chooser = JFileChooser()
        val returnVal = chooser.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentFile = chooser.selectedFile;
            writeToFile(content)
        } else {
            println("Save command cancelled by user.");
        }
    }
}

private fun writeToFile(content : String) {
    val fooWriter = FileWriter(currentFile, false);
    fooWriter.write(content);
    fooWriter.close();
}