import analyzer.Context
import parser.TokenStream


fun main(args: Array<String>) {
    println("Hello World! 2")
    val res = lexer.parse("int v1 = 0;int v2 = 5;v1+=v2*2;", true, true)

    println("Lexer: $res")

    val tree = parser.parse("", TokenStream(res, 0))

    println("Parser: ${tree.first}")

    val symTree = analyzer.analyseTop(tree.first, Context("root"))

    println("Tree: $symTree")

    val file = codegen.minecraft.genCode(symTree, "root")

    println("File: $file")
    // Try adding program arguments at Run/Debug configuration
}