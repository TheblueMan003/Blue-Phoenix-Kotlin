package analyzer

import analyzer.data.*
import parser.*
import parser.Function


data class InvalidTypeException(val type: DataType): Exception()

fun check(stm: Statement): Statement{
    return when(stm){
        is If -> {
            stm
        }
        is IfElse-> {
            stm
        }
        is Block->{
            Block(stm.statements.map { check(it) })
        }
        is Sequence->{
            Sequence(stm.statements.map { check(it) })
        }
        is Switch->{
            stm
        }
        is LinkedVariableAssignment -> {
            if (stm.variable.type is FuncType){
                val vtype = stm.variable.type as FuncType
                if (stm.expr is UnresolvedFunctionExpr){
                    LinkedVariableAssignment(stm.variable,
                        FunctionExpr(findFunction(vtype.from, vtype.to,stm.expr.function)),
                        stm.op)
                }
                else if (stm.expr is UnresolvedExpr) {
                    throw Exception("Cannot be assign to function")
                }
                else{
                    throw Exception("Cannot be assign to function")
                }
            }
            else{
                val v = checkExpression(stm.expr)
                checkOwnership(v.second, stm.variable.type)
                LinkedVariableAssignment(stm.variable, v.first, stm.op)
            }
        }
        else -> stm
    }
}

fun checkExpression(stm: Expression): Pair<Expression, DataType>{
    return when(stm){
        is IntLitExpr -> {
            Pair(stm, IntType())
        }
        is BoolLitExpr -> {
            Pair(stm, BoolType())
        }
        is FloatLitExpr -> {
            Pair(stm, FloatType())
        }
        is StringLitExpr -> {
            Pair(stm, StringType())
        }
        is VariableExpr -> {
            Pair(stm, stm.variable.type)
        }
        is UnresolvedFunctionExpr -> {
            throw NotImplementedError()
        }
        is UnresolvedExpr -> {
            throw NotImplementedError()
        }
        is CallExpr -> {
            throw NotImplementedError()
        }
        is BinaryExpr ->{
            val first = checkExpression(stm.first)
            val second = checkExpression(stm.second)
            val expr = when(stm.op){
                "+" -> {
                    Pair(BinaryExpr(stm.op, first.first,  second.first),
                        biggestType(first.second, second.second))
                }
                "-" -> {
                    Pair(BinaryExpr(stm.op, first.first,  second.first),
                        biggestType(first.second, second.second))
                }
                "*" -> {
                    Pair(BinaryExpr(stm.op, first.first,  second.first),
                        biggestType(first.second, second.second))
                }
                "%" -> {
                    Pair(BinaryExpr(stm.op, first.first,  second.first),
                        biggestType(first.second, second.second))
                }
                "/" -> {
                    Pair(BinaryExpr(stm.op, first.first,  second.first),
                        biggestType(first.second, second.second))
                }
                "<=" -> {
                    checkNumerical(first.second)
                    checkNumerical(second.second)
                    Pair(BinaryExpr(stm.op, first.first,  second.first),
                        BoolType())
                }
                "<" -> {
                    checkNumerical(first.second)
                    checkNumerical(second.second)
                    Pair(BinaryExpr(stm.op, first.first,  second.first),
                        BoolType())
                }
                ">" -> {
                    checkNumerical(first.second)
                    checkNumerical(second.second)
                    Pair(BinaryExpr(stm.op, first.first,  second.first),
                        BoolType())
                }
                ">=" -> {
                    checkNumerical(first.second)
                    checkNumerical(second.second)
                    Pair(BinaryExpr(stm.op, first.first,  second.first),
                        BoolType())
                }
                "&&" -> {
                    checkBoolean(first.second)
                    checkBoolean(second.second)
                    Pair(BinaryExpr(stm.op, first.first,  second.first),
                        BoolType())
                }
                "||" -> {
                    checkBoolean(first.second)
                    checkBoolean(second.second)
                    Pair(BinaryExpr(stm.op, first.first,  second.first),
                        BoolType())
                }
                else -> throw NotImplementedError()
            }
            expr
        }
        else -> throw NotImplementedError()
    }
}

fun biggestType(t1: DataType, t2: DataType):DataType{
    throw NotImplementedError()
}

fun checkOwnership(t1: DataType, t2: DataType): Boolean{
    return when(t2){
        is VoidType -> true
        is FloatType -> t1 is IntType || t1 is FloatType
        is IntType -> t1 is IntType
        is StringType -> t1 is StringType
        is BoolType -> t1 is BoolType
        is TupleType -> t1 is TupleType && t1.type.zip(t2.type).map { (x,y) -> checkOwnership(x,y) }.all { it }
        is FuncType -> t1 is FuncType && t1.from.zip(t2.from).map { (x,y) -> checkOwnership(x,y) }.all { it }
                            && checkOwnership(t1.to, t2.to)
        else -> throw NotImplementedError()
    }
}

fun checkOwnershipCost(t1: DataType, t2: DataType): Int{
    return when(t2){
        is VoidType -> 1000
        is FloatType -> if (t1 is IntType){1}else{0}
        is IntType -> 0
        is StringType -> 0
        is BoolType -> 0
        is TupleType -> {
            val t = t1 as TupleType
            t.type.zip(t2.type).sumOf { (x, y) -> checkOwnershipCost(x, y) }
        }
        is FuncType -> {
            val t = t1 as FuncType
            t.from.zip(t2.from).sumOf { (x, y) -> checkOwnershipCost(x, y) } + checkOwnershipCost(t.to, t2.to)}
        else -> throw NotImplementedError()
    }
}

fun checkBoolean(t: DataType){
    if (t !is BoolType){
        throw InvalidTypeException(t)
    }
}

fun checkNumerical(t: DataType){
    when(t){
        is IntType -> {}
        is FloatType -> {}
        else -> throw InvalidTypeException(t)
    }
}

fun findFunction(from: List<DataType>, to: DataType, lst: List<Function>): Function {
    val fit =
        lst.filter { fct -> fct.input
                .map {vr -> vr.type}
                .zip(from)
                .map { (x, y) -> checkOwnership(y, x)}
                .all { it } }
            .filter { fct -> checkOwnership(fct.output.type, to) }
            .map { fct -> Pair(fct.input
                .map {vr -> vr.type}
                .zip(from)
                .sumOf { (x, y) -> checkOwnershipCost(y, x)}
                + checkOwnershipCost(fct.output.type, to),
                fct) }
            .sortedBy { (c, fct) -> c }

    return if (fit.isEmpty()){
        throw Exception("Function not found with arguments: ${from.joinToString(", ")} -> $to")
    }
    else if (fit.size == 1){
        fit[0].second
    }
    else{
        if (fit[0].first < fit[1].first){
            fit[0].second
        }
        else{
            throw Exception("Cannot resolve function with arguments: ${from.joinToString(", ")} -> $to")
        }
    }
}