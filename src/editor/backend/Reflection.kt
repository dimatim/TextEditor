package editor.backend

import editor.CONFIG_PATH
import editor.JAVA
import editor.KOTLIN
import editor.SYNTAX
import editor.parser.extractInfo
import editor.parser.extractType
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.regex.Pattern

/**
 * Created by Dima on 19-Aug-15.
 */
val variableMap = HashMap<String, Class<*>?>()
val uVariableMap = HashMap<String, Class<*>?>()
val classMap = HashMap<String, Class<*>>()

val ACC_PUBLIC = 0x0001//     Declared public; may be accessed from outside its package.
val ACC_FINAL = 0x0010//	Declared final; no subclasses allowed.
val ACC_SUPER = 0x0020//	Treat superclass methods specially when invoked by the invokespecial instruction.
val ACC_INTERFACE = 0x0200//	Is an interface, not a class.
val ACC_ABSTRACT = 0x0400//	Declared abstract; must not be instantiated.
val ACC_SYNTHETIC = 0x1000//	Declared synthetic; not present in the source code.
val ACC_ANNOTATION = 0x2000//	Declared as an annotation type.
val ACC_ENUM = 0x4000//	Declared as an enum type.

fun clearClassInfo(): Unit = variableMap.clear()

fun extractClassInfo(string: String) {
    when (SYNTAX) {
        JAVA -> extractJClassInfo(string)
        KOTLIN -> extractKClassInfo(string)
    }
}

fun extractJClassInfo(string: String) {
    val pattern = Pattern.compile("""\w+ *\w+ *=""")
    val matcher = pattern.matcher(string)
    while (matcher.find()) {
        val temp = matcher.group().dropLast(1).trim()
        val result = temp.split(" ")
        variableMap.put(result[result.size - 1].trim(), classMap.get(result[0].trim()))
    }
}

fun extractKClassInfo(string: String) {
    val pattern = Pattern.compile("""\w+ *: *[A-Z]\w+""")
    val matcher = pattern.matcher(string)
    while (matcher.find()) {
        val result = matcher.group().split(":")
        variableMap.put(result[0].trim(), classMap.get(result[1].trim()))
    }
    extractPartialClassInfo(string)
}

fun extractPartialClassInfo(string: String) {
    val pattern = Pattern.compile("""\w+ *= *\w+.*""")
    val matcher = pattern.matcher(string)
    while (matcher.find()) {
        val result = matcher.group().split("=")
        variableMap.put(result[0].trim(), extractType(result[1].trim()))
    }
}

fun buildClassMap() {
    //for IDE testing
    val loader = URLClassLoader.newInstance(loadJars(), ClassLoader.getSystemClassLoader())
    val reflections = Reflections("java", SubTypesScanner(false), loader);
    val allClasses = reflections.getSubTypesOf(Any::class.java).filter { it.modifiers and ACC_PUBLIC == ACC_PUBLIC && !it.isMemberClass }

    //for Jar testing
    //val allClasses = ClassAgent.getInstrumentation().allLoadedClasses.filter { it.modifiers and ACC_PUBLIC == ACC_PUBLIC && !it.isMemberClass }
    println("found ${allClasses.size()} classes")
    for (c in allClasses)
        classMap.put(c.simpleName, c)
}

private fun loadJars(): Array<URL> {
    val folder = File(CONFIG_PATH)
    var matchingFiles: Array<File>
    val urls = arrayListOf<URL>()
    for (l in folder.readLines("UTF-8")) {
        matchingFiles = File(l).listFiles({ file: File, name: String -> name.contains(".jar") })//TODO problematic - returns null?
        for (f in matchingFiles) {
            urls.add(URL("file:" + f.canonicalPath))
            println("file:" + f.canonicalPath)
        }
    }
    println("loaded ${urls.size()} libs")
    return urls.toTypedArray()
}

fun getClassForVar(variable: String): Class<*>? = variableMap.get(variable)

fun getClassForType(type: String): Class<*>? = classMap.get(type)

fun getMatchingClasses(prefix: String): List<Class<*>> = classMap.filterKeys { it.startsWith(prefix) } map { it.getValue() }

fun isValidVar(varName: String): Boolean = variableMap.containsKey(varName)

fun isValidType(varName: String): Boolean = classMap.containsKey(varName)