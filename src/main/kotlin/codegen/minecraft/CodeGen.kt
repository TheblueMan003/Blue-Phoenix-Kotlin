package codegen.minecraft

import ast.*
import utils.OutputFile

fun genCode(statement: Statement, name: String):List<OutputFile>{
    val sbi = ScoreboardInitializer()
    val output = OutputFile(name)
    return genCode(statement, output, sbi) + output
}

fun genCode(stm: Statement, outputFile: OutputFile, sbi: ScoreboardInitializer): List<OutputFile>{
    val files = ArrayList<OutputFile>()
    val mpt = emptyList<OutputFile>()
    files.addAll(when(stm){
        is Block -> {
            stm.statements.map { genCode(it, outputFile, sbi) }.flatten()
        }
        is Sequence -> {
            stm.statements.map { genCode(it, outputFile, sbi) }.flatten()
        }
        is LinkedVariableAssignment -> {
            outputFile.add(setVariableExpression(stm.variable, stm.expr, stm.op, sbi){genCode(it, outputFile, sbi)})
            mpt
        }
        is RawFunctionCall -> {
            outputFile.add("function ${stm.function.name}")
            mpt
        }
        is FunctionBody -> {
            if (stm.function.isUsed()){
                val file = OutputFile(stm.function.name.toString())
                files.add(file)
                genCode(stm.body, file, sbi)
            } else {
                mpt
            }
        }
        else ->{
            mpt
        }
    })
    return files
}