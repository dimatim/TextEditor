package editor.backend

import CONFIG_PATH
import currentFile
import document
import java.io.File
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
        val content = currentFile?.readText(Charset.defaultCharset())?.replace(System.getProperty("line.separator"), "\n")
        document.insertString(0, content, null)
    } else {
        println("Open command cancelled by user.");
    }
}

fun chooseDir(): String {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    val returnVal = chooser.showOpenDialog(null);
    var path = ""
    if (returnVal == JFileChooser.APPROVE_OPTION) {
        path = chooser.selectedFile.path;
    } else {
        println("Open command cancelled by user.");
    }
    return path
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

fun savePath(path: String) {
    val file = File(CONFIG_PATH)
    if (!file.exists()) {
        file.parentFile.mkdir()
        file.createNewFile()
    }
    file.writeText("$path\\jre\\lib\n$path\\jre\\lib\\ext")
}

private fun writeToFile(content: String) {
    val fooWriter = FileWriter(currentFile, false);
    fooWriter.write(content);
    fooWriter.close();
}