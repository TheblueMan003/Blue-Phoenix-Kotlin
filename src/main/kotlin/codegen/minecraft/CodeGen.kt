package codegen.minecraft

import analyzer.data.LinkedVariableAssignment
import analyzer.data.RawFunctionCall
import parser.*
import utils.OutputFile

fun genCode(statement: Statement, name: String):List<OutputFile>{
    val sbi = ScoreboardInitializer()
    val output = OutputFile(name)
    return genCode(statement, output, sbi) + output
}

fun genCode(stm: Statement, outputFile: OutputFile, sbi: ScoreboardInitializer): List<OutputFile>{
    when(stm){
        is Block -> {
            stm.statements.map { genCode(it, outputFile, sbi) }
        }
        is Sequence -> {
            stm.statements.map { genCode(it, outputFile, sbi) }
        }
        is LinkedVariableAssignment -> {
            outputFile.add(setVariableExpression(stm.variable, stm.expr, stm.op, sbi){genCode(it, outputFile, sbi)})
        }
        is RawFunctionCall -> {
            outputFile.add("function ${stm.function.name}")
        }
        else ->{

        }
    }
    return emptyList()
}