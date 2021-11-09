import parser.TokenStream

fun main(args: Array<String>) {
    println("Hello World! 2")
    val res = lexer.parse("int value = test",true,true)

    println("Lexer: $res")

    val tree = parser.parse("", TokenStream(res,0))
    // Try adding program arguments at Run/Debug configuration
    println("Parser: $tree")
}
