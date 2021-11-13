package codegen.minecraft

import utils.OutputFile

fun callBlock(file: OutputFile): String{
    return "function ${file.name.replaceFirst('.',':').replace(".","/")}"
}
fun callBlock(file: ast.Function): String{
    return "function ${file.name.toString().replaceFirst('.',':').replace(".","/")}"
}