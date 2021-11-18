package codegen.minecraft

import data_struct.Function
import utils.OutputFile

fun callBlock(file: OutputFile): String{
    return "function ${file.name.replaceFirst('.',':').replace(".","/")}"
}
fun callBlock(file: Function): String{
    return "function ${file.name.toString().replaceFirst('.',':').replace(".","/")}"
}