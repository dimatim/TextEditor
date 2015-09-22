package editor.parser

import editor.backend.*
import java.lang.reflect.GenericDeclaration
import java.util.*

/**
 * Created by Dima on 12-Sep-15.
 */
class Node(val value: String) {
    val children: ArrayList<Node> = arrayListOf()

    val simpleName: String
        get() = if (value.contains('(')) value.substring(0, value.indexOfFirst { it == '(' }) else value
}

fun extractInfo(s: String): Pair<List<GenericDeclaration>, String> {
    val n = Node("big boss")
    val inter = reverseProcess(s)
    val ind = inter.size() - 1
    populate(n, inter, 1, ind)
    return getSuggestions(n)
}

fun extractType(s: String): Class<*>? {
    val n = Node("big boss")
    val aux = "$s."
    val inter = reverseProcess(aux)
    val ind = inter.size() - 1
    populate(n, inter, 1, ind)
    return getClassForNode(n)
}

private val stopChars = arrayOf('=', '<', '>', '&', '|', '%', ';', '*', '/', '+', '-', '!', '<', '>', ':', '^')

private fun populate(node: Node, intervals: ArrayList<Pair<Int, String>>, currentLevel: Int, currentIndex: Int): Int {
    var activeNode = node
    var index = currentIndex
    while (index >= 0) {
        if (currentLevel - intervals.get(index).first > 0)
            return index
        else if (intervals.get(index).first == currentLevel + 1) {
            index = populate(activeNode, intervals, currentLevel + 1, index)
        } else if (intervals.get(index).first == currentLevel) {
            activeNode = Node(intervals.get(index).second)
            node.children.add(activeNode)
            index--
        }
    }
    return 0
}

private fun reverseProcess(string: String): ArrayList<Pair<Int, String>> {
    val stack = Stack<Int>()
    val intervals: ArrayList<Pair<Int, String>> = arrayListOf()
    stack.push(string.length())
    for (i in string.indices.reversed()) {
        if (stack.size() == 1 && stopChars.contains(string.get(i))) {
            intervals.add(stack.size() to string.substring(i + 1, stack.pop()).trim())
            break
        } else if (string.get(i) == '.') {
            intervals.add(stack.size() to string.substring(i + 1, stack.pop()).trim())
            stack.push(i)
        } else if (string.get(i) == ',') {
            intervals.add(stack.size() to string.substring(i + 1, stack.pop()).trim())
            if (stack.empty()) break else stack.push(i)
        } else if (string.get(i) == ')') {
            if (i == string.length() - 1) {
                intervals.add(stack.size() to string.substring(i + 1, stack.pop()).trim())
                break
            } else stack.push(i)
        } else if (string.get(i) == '(') {
            intervals.add(stack.size() to string.substring(i + 1, stack.pop()).trim())
            if (stack.empty()) break
        }
    }
    if (stack.isNotEmpty())
        intervals.add(stack.size() to string.substring(0, stack.pop()).trim())
    return intervals
}

private fun getSuggestions(node: Node): Pair<List<GenericDeclaration>, String> {
    var cls: Class<*>? = getClassForNode(node)
    if (cls != null) {
        return (cls.methods filter {it.name.startsWith(node.children.last().simpleName)}).toList() to node.children.last().simpleName
    } else if (node.children.first().simpleName.isBlank())
        return emptySuggestion
    else {
        val token = node.children.get(0).simpleName.split(' ').last()
        return getMatchingClasses(token) to token
    }
}

private fun getClassForNode(node: Node): Class<*>? {
    var cls: Class<*>? = null
    for (n in node.children.dropLast(1)) {
        if (cls == null) {
            if (isValidVar(n.simpleName))
                cls = getClassForVar(n.simpleName)
            else if (isValidType(n.simpleName))
                cls = getClassForType(n.simpleName)
            if (cls == null)
                break
        } else {
            cls = (cls.methods firstOrNull { it.name == n.simpleName })?.returnType
            if (cls == null)
                break
        }
    }
    return cls
}