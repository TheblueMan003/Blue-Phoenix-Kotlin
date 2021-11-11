package codegen.minecraft

import analyzer.data.LinkedVariableAssignment
import parser.*
import utils.OutputFile

fun genCode(statement: Statement, name: String):OutputFile{
    val output = OutputFile(name)
    genCode(statement, output)
    return output
}

fun genCode(stm: Statement, outputFile: OutputFile){
    when(stm){
        is Block -> {
            stm.statements.map { it -> genCode(it, outputFile) }
        }
        is LinkedVariableAssignment -> {
            genCodeVariableAssignment(stm.variable, stm.expr, outputFile)
        }
        else ->{

        }
    }
}

fun genCodeVariableAssignment(variable: Variable, expr: Expression, outputFile: OutputFile){
    when(expr){
        is IntLitExpr -> {
            setVariableValue(variable, expr.value)
        }
        is BinaryExpr -> {

        }
    }
}