import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import java.util.HashMap
import java.util.regex.Pattern
import kotlin.reflect.jvm.java

/**
 * Created by Dima on 19-Aug-15.
 */
val variableMap = HashMap<String, Class<*>?>()
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
    val pattern = Pattern.compile("""\w+ *: *\w+""")
    val matcher = pattern.matcher(string)
    while (matcher.find()) {
        val result = matcher.group().split(":")
            variableMap.put(result[0].trim(), classMap.get(result[1].trim()))
    }
}

fun buildClassMap() {
    val reflections = Reflections("java", SubTypesScanner(false));
    val allClasses = reflections.getSubTypesOf(Any::class.java).filter { it.modifiers and ACC_PUBLIC == ACC_PUBLIC && !it.isMemberClass }
    for (c in allClasses)
        classMap.put(c.simpleName, c)
}

fun getClassForVar(variable: String): Class<*> = variableMap.get(variable)?: Any::class.java

fun getMatchingClasses(prefix : String) : List<Class<*>> = classMap.filterKeys { it.startsWith(prefix) } map { it.getValue()}

fun isValidVar(varName : String) : Boolean = variableMap.containsKey(varName)

fun isValidType(varName : String) : Boolean = classMap.containsKey(varName)