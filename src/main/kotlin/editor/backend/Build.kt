package editor.backend

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
/**
 * Created by Dima on 16-Aug-15.
 */
private fun printLines(name : String, ins : InputStream) {
    val input = BufferedReader(InputStreamReader(ins));
    var line = input.readLine()
    while (line != null) {
        System.out.println("$name $line");
        line = input.readLine()
    }
}

private fun runProcessAndWait(command : String) {
    val pro = Runtime.getRuntime().exec(command);
    printLines("$command stdout:", pro.inputStream)
    printLines("$command stderr:", pro.errorStream);
    pro.waitFor();
    println(command + " exitValue() " + pro.exitValue());
}

private fun runProcess(command : String) {
    Runtime.getRuntime().exec(command);
}

fun buildRestart() {
    saveFile()
    runProcessAndWait("cmd /c kotlinc-jvm src/main.kotlin.editor -include-runtime -d Editor.jar");
    runProcess("java -jar Editor.jar");
    System.exit(0)
}