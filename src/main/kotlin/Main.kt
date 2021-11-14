import analyzer.Context
import analyzer.runAnalyse
import analyzer.runChecker
import analyzer.runSimplifier
import parser.TokenStream


fun main(args: Array<String>) {
    val fileContent = getResourceAsText("Main.bp")
    val res = lexer.parse(fileContent)

    println("Lexer: $res")

    val tree = parser.parse("root", TokenStream(res, 0))

    println("Parser: ${tree.first}")

    val context = Context("root")
    val symTree = runAnalyse(tree.first, tree.second.context)

    println("Tree: $symTree")

    val checkedTree = runChecker(symTree, context)

    println("Checked: $checkedTree")

    val simplifiedTree = runSimplifier(checkedTree, context){s,c -> runChecker(runAnalyse(s,c), c)}

    println("Simplified: $simplifiedTree")

    val file = codegen.minecraft.genCode(simplifiedTree, "root")

    println("File: $file")
    // Try adding program arguments at Run/Debug configuration
}

fun getResourceAsText(path: String): String {
    return object {}.javaClass.getResource(path)!!.readText()
}