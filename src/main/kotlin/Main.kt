import compiler.Compiler
import compiler.ResourceGetter


fun main(args: Array<String>) {
    val getter = ResourceGetter()
    val compiler = Compiler(listOf(getter.get("Main")), getter)
    println(compiler.compile())
}

fun getResourceAsText(path: String): String {
    return object {}.javaClass.getResource(path)!!.readText()
}