import ast.Identifier
import compiler.Compiler
import compiler.ResourceGetter


fun main(args: Array<String>) {
    val getter = ResourceGetter()
    val compiler = Compiler(listOf(getter.get("compiler_test.Main"), getter.get("compiler_test.Main2")), getter)
    println(compiler.compile())
}

fun getResourceAsText(path: String): String {
    return object {}.javaClass.getResource(path)!!.readText()
}