package codegen.minecraft

import ast.*
import utils.OutputFile

fun genCode(statement: Statement, name: String):List<OutputFile>{
    val sbi = ScoreboardInitializer()
    val outputFiles = ArrayList<OutputFile>()
    val output = OutputFile(name)
    output.add(genCode(statement, outputFiles, sbi))
    outputFiles.add(output)
    return outputFiles
}

fun genCode(stm: Statement, outputFiles: ArrayList<OutputFile>, sbi: ScoreboardInitializer, forceUnique: Boolean = false): List<String>{
    return when(stm){
        is Block -> {
            val ret = stm.statements.map { genCode(it, outputFiles, sbi) }.flatten()
            createBlock("\$block.${ret.hashCode()}", ret, outputFiles)
        }
        is Sequence -> {
            val ret = stm.statements.map { genCode(it, outputFiles, sbi) }.flatten()
            if (forceUnique){
                createBlock("\$block.${ret.hashCode()}", ret, outputFiles)
            } else {
              ret
            }
        }
        is LinkedVariableAssignment -> {
            val ret = setVariableExpression(stm.variable, stm.expr, stm.op, sbi) { genCode(it, outputFiles, sbi) }
            return if (!forceUnique || ret.size < 2) {
                ret
            }else{
                createBlock("\$block.${ret.hashCode()}", ret, outputFiles)
            }
        }
        is If -> {
            listOf(genIf(stm){genCode(it, outputFiles, sbi, true)})
        }
        is RawFunctionCall -> {
            listOf("function ${stm.function.name}")
        }
        is FunctionBody -> {
            if (stm.function.isUsed()){
                createBlock(stm.function.name.toString(), genCode(stm.body, outputFiles, sbi), outputFiles)
                emptyList()
            } else {
                emptyList()
            }
        }
        else ->{
            emptyList()
        }
    }
}

fun createBlock(name: String, lines: List<String>, outputFiles: ArrayList<OutputFile>):List<String>{
    val file = OutputFile(name)
    outputFiles.add(file)
    file.add(lines)
    return listOf(callBlock(file))
}