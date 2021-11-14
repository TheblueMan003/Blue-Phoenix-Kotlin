package utils

import ast.AssignmentType
import ast.Identifier

fun <T> withDefault(arg: List<T>, defaults: List<T?>):List<T>{
    val lst = ArrayList<T>()
    for (i in defaults.indices){
        if (arg.size > i){
            lst.add(arg[i])
        }
        else if (defaults[i] != null){
            lst.add(defaults[i]!!)
        }else{
            return emptyList()
        }
    }
    return lst
}

fun getOperationFunctionName(op: String): Identifier {
    return when(op){
        "+" -> Identifier(listOf("add"))
        "-" -> Identifier(listOf("sub"))
        "*" -> Identifier(listOf("mul"))
        "/" -> Identifier(listOf("div"))
        "%" -> Identifier(listOf("mod"))
        "^" -> Identifier(listOf("pow"))
        else -> throw NotImplementedError()
    }
}
fun getOperationFunctionName(op: AssignmentType): Identifier{
    return when(op){
        AssignmentType.SET -> Identifier(listOf("assign"))
        AssignmentType.ADD -> Identifier(listOf("plusAssign"))
        AssignmentType.SUB -> Identifier(listOf("minusAssign"))
        AssignmentType.MUL -> Identifier(listOf("timesAssign"))
        AssignmentType.DIV -> Identifier(listOf("divAssign"))
        AssignmentType.MOD -> Identifier(listOf("modAssign"))
        AssignmentType.POW -> Identifier(listOf("powAssign"))
    }
}