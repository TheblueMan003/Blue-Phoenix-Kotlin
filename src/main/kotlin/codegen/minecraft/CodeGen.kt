package codegen.minecraft

import analyzer.data.LinkedVariableAssignment
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
        is LinkedVariableAssignment -> {
            outputFile.add(setVariableExpression(stm.variable, stm.expr, stm.op, sbi))
        }
        else ->{

        }
    }
    return emptyList()
}