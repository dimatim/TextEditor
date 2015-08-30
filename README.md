# TextEditor
Text editor with suggestion capabilities (Uses Kotlin plugin v.0.13.870)

<a href="http://imgur.com/k3dYDTM"><img src="http://i.imgur.com/k3dYDTM.png" title="TextEditor" /></a>

<p>To run from Intellij, modify the Reflection.kt <i>buildClassMap</i> function as follows:</p>
<ol>
<li> Uncomment the following section(IDE code)</li>
<code>val loader = URLClassLoader.newInstance(loadJars(), ClassLoader.getSystemClassLoader())</code><br>
<code>val reflections = Reflections("java", SubTypesScanner(false), loader);</code><br>
<code>val allClasses = reflections.getSubTypesOf(Any::class.java).filter { it.modifiers and ACC_PUBLIC == ACC_PUBLIC && !it.isMemberClass }
</code>
<li> Comment the following section(Jar code)</li>
<code>
val allClasses = ClassAgent.getInstrumentation().allLoadedClasses.filter { it.modifiers and ACC_PUBLIC == ACC_PUBLIC && !it.isMemberClass }
</code>
</ol>
<p>To run from Jar:</p>
<ol>
<li>Modify the Reflection.kt <i>buildClassMap</i> by commenting IDE code and uncommenting Jar code</li>
<li>Make sure to have Agent.jar in the same location as TextEditorGit.jar</li>
<li>Run the following command in the terminal: <code> java -javaagent:Agent.jar -jar TextEditorGit.jar</code>
