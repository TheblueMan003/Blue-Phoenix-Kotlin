import lexer.Lexer

fun main(args: Array<String>) {
    println("Hello World!")
    val lexer = Lexer()
    val res = lexer.parse("int value = test",true,true)
    // Try adding program arguments at Run/Debug configuration
    println("Program arguments: $res")
}
