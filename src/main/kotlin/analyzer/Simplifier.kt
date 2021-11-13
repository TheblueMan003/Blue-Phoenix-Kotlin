package analyzer

import analyzer.data.*
import ast.Identifier
import parser.*
import kotlin.math.pow

fun simplify(stm: Statement, context: Context): Statement {
    return when(stm){
        is If -> {
            If(simplifyExpression(stm.Condition, context),
                simplify(stm.IfBlock, context))
        }
        is IfElse -> {
            IfElse(simplifyExpression(stm.Condition, context),
                simplify(stm.IfBlock, context),
                simplify(stm.ElseBlock, context))
        }
        is Block ->{
            val nstm = stm.statements.map { simplify(it, context) }.filter{ it !is Empty}
            when (nstm.size) {
                0 -> { Empty() }
                1 -> { nstm[0] }
                else -> { Block(nstm) }
            }
        }
        is Sequence ->{
            val nstm = stm.statements.map { simplify(it, context) }.filter{ it !is Empty }
            simplifySequence(nstm)
        }
        is Switch ->{
            Switch(simplifyExpression(stm.function, context),
                stm.cases.map {
                    Case(simplifyExpression(it.expr,context),
                        simplify(it.statement, context))})
        }
        is LinkedVariableAssignment -> {
            if (stm.variable.type is TupleType){
                val types = (stm.variable.type as TupleType).type
                val expr = stm.expr
                val var_ = stm.variable
                when(expr){
                    is TupleExpr -> {
                        Sequence(types.zip(expr.value).mapIndexed{ id, (_,v) ->
                            LinkedVariableAssignment(var_.childrenVariable[Identifier(listOf("_$id"))]!!, v, stm.op)
                        })
                    }
                    is VariableExpr -> {
                        Sequence(types.mapIndexed{ id, _ ->
                            LinkedVariableAssignment(var_.childrenVariable[Identifier(listOf("_$id"))]!!,
                                VariableExpr(expr.variable.childrenVariable[Identifier(listOf("_$id"))]!!), stm.op)
                        })
                    }
                    is CallExpr -> {
                        val fctCall = simplifyFunctionCall(expr.value, expr.args, context)
                        Sequence(listOf(fctCall.first,
                            simplify(LinkedVariableAssignment(stm.variable, fctCall.second, stm.op), context)))
                    }
                    else -> throw NotImplementedError()
                }
            }
            else{
                LinkedVariableAssignment(stm.variable, simplifyExpression(stm.expr, context), stm.op)
            }
        }
        is CallExpr -> {
            simplifyFunctionCall(stm.value, stm.args, context).first
        }
        else -> stm
    }
}

fun simplifyExpression(expr: Expression, context: Context):Expression{
    return when(expr){
        is BinaryExpr -> {
            val left = simplifyExpression(expr.first, context)
            val right = simplifyExpression(expr.second, context)
            return if (left is IntLitExpr && right is IntLitExpr){
                IntLitExpr(applyOperation(expr.op, left.value, right.value))
            }
            else if (left is FloatLitExpr && right is IntLitExpr){
                FloatLitExpr(applyOperation(expr.op, left.value, right.value.toFloat()))
            }
            else if (left is IntLitExpr && right is FloatLitExpr){
                FloatLitExpr(applyOperation(expr.op, left.value.toFloat(), right.value))
            }
            else if (left is FloatLitExpr && right is FloatLitExpr){
                FloatLitExpr(applyOperation(expr.op, left.value, right.value))
            }
            else if (left is BoolLitExpr && right is BoolLitExpr){
                BoolLitExpr(applyOperation(expr.op, left.value, right.value))
            } else {
                BinaryExpr(expr.op, left, right)
            }
        }
        is UnaryExpr -> {
            val inter = simplifyExpression(expr.first, context)
            when(expr.op){
                "-" -> {
                    when (inter) {
                        is IntLitExpr -> { IntLitExpr(-inter.value) }
                        is UnaryExpr -> { inter.first }
                        else -> { UnaryExpr(expr.op, inter) }
                    }
                }
                "!" -> {
                    when (inter) {
                        is BoolLitExpr -> { BoolLitExpr(!inter.value) }
                        is UnaryExpr -> { inter.first }
                        else -> { UnaryExpr(expr.op, inter) }
                    }
                }
                else -> throw NotImplementedError(expr.op)
            }
        }
        is CallExpr -> {
            val fctCall = simplifyFunctionCall(expr.value, expr.args, context)
            StatementThanExpression(fctCall.first, fctCall.second)
        }
        else -> expr
    }
}

fun simplifyFunctionCall(stm: Expression, args: List<Expression>, context: Context): Pair<Statement, Expression>{
    if (stm is FunctionExpr) {
        return Pair(
            simplifySequence(
                stm.function.input.zip(args)
                    .map { (v, e) -> simplify(LinkedVariableAssignment(v, e, AssignmentType.SET), context) } +
                        RawFunctionCall(stm.function)
            ),
            VariableExpr(stm.function.output)
        )
    }else throw NotImplementedError()
}

fun simplifySequence(nstm: List<Statement>):Statement{
    return when (nstm.size) {
        0 -> { Empty() }
        1 -> { nstm[0] }
        else -> { Sequence(nstm) }
    }
}

fun applyOperation(op: String, left: Boolean, right: Boolean): Boolean{
    return when(op){
        "&&" -> left && right
        "||" -> left || right
        else -> throw NotImplementedError()
    }
}
fun applyOperation(op: String, left: Int, right: Int): Int{
    return when(op){
        "+" -> left + right
        "-" -> left - right
        "*" -> left * right
        "/" -> left / right
        "%" -> left % right
        "^" -> left.toDouble().pow(right.toDouble()).toInt()
        else -> throw NotImplementedError()
    }
}
fun applyOperation(op: String, left: Float, right: Float): Float{
    return when(op){
        "+" -> left + right
        "-" -> left - right
        "*" -> left * right
        "/" -> left / right
        "%" -> left % right
        "^" -> left.toDouble().pow(right.toDouble()).toFloat()
        else -> throw NotImplementedError()
    }
}