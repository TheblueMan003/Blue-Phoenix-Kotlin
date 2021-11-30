package codegen.minecraft

import ast.*
import utils.OutputFile

val builtInFunction = mapOf<String, Any>(
    Pair("__to_raw_json__", ::mcJavaToRawJson)
)

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
            listOf(genIf(stm, sbi){genCode(it, outputFiles, sbi, true)})
        }
        is RawFunctionCall -> {
            listOf("function ${stm.function.name}")
        }
        is FunctionBody -> {
            if (stm.function.isUsed() && !stm.function.modifier.lazy && !stm.function.modifier.inline){
                createBlock(stm.function.name.toString(), genCode(stm.body, outputFiles, sbi), outputFiles)
                emptyList()
            } else {
                emptyList()
            }
        }
        is Empty -> emptyList()
        is RawCommand -> listOf(stm.cmd)
        is RawCommandArg -> {
            var str = stm.cmd
            val prepare = ArrayList<String>()
            stm.args.mapIndexed{ id, it -> Pair(id, it) }
                .reversed()
                .map {
                    val eval = expressionToString(it.second, sbi)
                    prepare.addAll(eval.first)
                    str = str.replace("\$${it.first}", eval.second)
                }

            listOf(str)
        }
        else ->{
            throw NotImplementedError("$stm")
        }
    }
}

fun createBlock(name: String, lines: List<String>, outputFiles: ArrayList<OutputFile>):List<String>{
    val file = OutputFile(name)
    outputFiles.add(file)
    file.add(lines)
    return listOf(callBlock(file))
}

fun expressionToString(expr: Expression, sbi: ScoreboardInitializer): Pair<List<String>, String>{
    return when(expr){
        is BuildInFunctionCall -> {
            (builtInFunction[expr.function.toString()] as ((List<Expression>,ScoreboardInitializer)->Pair<List<String>,String>))(expr.expr, sbi)
        }
        else -> Pair(emptyList() ,analyzer.expressionToString(expr))
    }
}