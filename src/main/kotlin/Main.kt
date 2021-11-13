import analyzer.Context
import parser.TokenStream


fun main(args: Array<String>) {
    val fileContent = getResourceAsText("Main.bp")
    val res = lexer.parse(fileContent, true, true)

    println("Lexer: $res")

    val tree = parser.parse("root", TokenStream(res, 0))

    println("Parser: ${tree.first}")

    val context = Context("root")
    val symTree = analyzer.analyse(tree.first, tree.second.context).first

    println("Tree: $symTree")

    val checkedTree = analyzer.check(symTree, context)

    println("Checked: $checkedTree")

    val simplifiedTree = analyzer.simplify(checkedTree, context)

    println("Simplified: $simplifiedTree")

    val file = codegen.minecraft.genCode(simplifiedTree, "root")

    println("File: $file")
    // Try adding program arguments at Run/Debug configuration
}

fun getResourceAsText(path: String): String {
    return object {}.javaClass.getResource(path)!!.readText()
}