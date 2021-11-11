package analyzer

import analyzer.data.FunctionExpr
import analyzer.data.UnresolvedExpr
import analyzer.data.VariableExpr
import parser.*


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
            stm
        }
        is Case->{
            stm
        }
        is Switch->{
            stm
        }
        else -> throw NotImplementedError()
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
        is FunctionExpr -> {
            Pair(stm, (FuncType(stm.function.input.map { it.type }, stm.function.output.type)))
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