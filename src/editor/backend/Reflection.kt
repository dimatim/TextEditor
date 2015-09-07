package editor.backend

import editor.CONFIG_PATH
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

fun extractClassInfo(string: String) {
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
        variableMap.put(result[0].trim(), resolveType(result[1].trim())?.get(0))
    }
    //variableMap.forEach { println("${it.getKey()} - ${it.getValue()}") }
}

fun resolveType(string: String): List<Class<*>>? {
    val split = string.split(".")
    if (split.size >= 2) {
        var cclass: Class<*> = Unit.javaClass
        for (i in split.indices) {
            if (i > 0)
                if (i == 1) {
                    if (split[i].contains('(')) {
                        cclass = resolveMethodType(getClassForVar(split[0]), split[1])
                    } else if (split[i].isEmpty())
                        cclass = getClassForVar(split[0])
                } else {
                    if (split[i].contains('(')) {
                        cclass = resolveMethodType(cclass, split[i])
                    }
                }
        }
        return listOf(cclass)
    } else if (!string.contains('.') && string.isNotEmpty()) return getMatchingClasses(string.trim())
    return null
}

fun resolveMethodType(cls: Class<*>, string: String): Class<*> {
    val bracketIndex = string.indexOfFirst { it == '(' }
    val name = string.substring(0, bracketIndex)
    return (cls.methods first { it.name == name }).returnType
    //println("name = $name paramString = $paramString")
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

fun getClassForVar(variable: String): Class<*> = variableMap.get(variable) ?: Any::class.java

fun getMatchingClasses(prefix: String): List<Class<*>> = classMap.filterKeys { it.startsWith(prefix) } map { it.getValue() }

fun isValidVar(varName: String): Boolean = variableMap.containsKey(varName)

fun isValidType(varName: String): Boolean = classMap.containsKey(varName)