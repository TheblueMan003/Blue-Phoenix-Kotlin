package codegen.minecraft

import ast.*
import kotlin.math.exp

fun genIf(expr: If, callBack: (Statement)->List<String>):String{
    fun ifUnpack(expr: If): Pair<List<MCCondition>, Statement> {
        fun inter(expr: Expression): MCCondition {
            return when (expr) {
                is VariableExpr -> {
                    variableToScoreboard(expr.variable).compare(1, ">=")
                }
                is BinaryExpr -> {
                    val left = expr.first
                    val right = expr.second
                    if (left is VariableExpr && right is VariableExpr) {
                        val s1 = variableToScoreboard(left.variable)
                        val s2 = variableToScoreboard(right.variable)
                        s1.compare(s2, expr.op)
                    }
                    // Range
                    else if (left is VariableExpr && right is RangeLitExpr) {
                        val s1 = variableToScoreboard(left.variable)
                        s1.isIn(litExprToInt(right.min as LitExpr), litExprToInt(right.max as LitExpr))
                    }
                    // Lit
                    else if (left is VariableExpr && right is LitExpr) {
                        val s1 = variableToScoreboard(left.variable)
                        s1.compare(litExprToInt(right), expr.op)
                    } else if (left is LitExpr && right is VariableExpr) {
                        val s1 = variableToScoreboard(right.variable)
                        s1.compare(litExprToInt(left), swapOperator(expr.op))
                    } else {
                        throw NotImplementedError()
                    }
                }
                is UnaryExpr -> {
                    inter(expr.first).inverted()
                }
                else -> throw NotImplementedError()
            }
        }

        val cond = listOf(inter(expr.Condition)).toMutableList()
        return when (expr.IfBlock) {
            is If -> {
                val p = ifUnpack(expr.IfBlock)
                Pair(p.first + cond, p.second)
            }
            else -> Pair(cond, expr.IfBlock)
        }
    }
    val unpacked = ifUnpack(expr)
    val body = callBack(unpacked.second)
    return if (body.isNotEmpty()){
        "execute ${unpacked.first.joinToString(" ")} run ${body[0]}"
    }else{
        ""
    }
}

fun swapOperator(op: String):String{
    return when(op){
        "<" -> ">="
        "<=" -> ">"
        ">" -> "<="
        ">=" -> "<"
        else -> op
    }
}