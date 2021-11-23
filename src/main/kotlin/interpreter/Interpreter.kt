package interpreter

import analyzer.applyOperation
import analyzer.expressionToString
import ast.*
import data_struct.Variable
import utils.withDefault

class Interpreter {
    private val variables = HashMap<Variable, Expression>()

    fun interpret(stm: Statement, output: Variable?): Expression?{
        try {
            return when (stm) {
                is Block -> {
                    val values = stm.statements.mapNotNull { interpret(it, output) }
                    if (values.isNotEmpty()) {
                        values.last()
                    } else null
                }
                is Sequence -> {
                    val values = stm.statements.mapNotNull { interpret(it, output) }
                    if (values.isNotEmpty()) {
                        values.last()
                    } else null
                }
                is LinkedVariableAssignment -> {
                    val value = expressionEvaluation(stm.expr)
                    val current = variables[stm.variable]
                    when (stm.op) {
                        AssignmentType.SET -> {
                            variables[stm.variable] = expressionEvaluation(value)
                        }
                        AssignmentType.SUB -> {
                            variables[stm.variable] = expressionEvaluation("-", current!!, value)
                        }
                        AssignmentType.ADD -> {
                            variables[stm.variable] = expressionEvaluation("+", current!!, value)
                        }
                        AssignmentType.MUL -> {
                            variables[stm.variable] = expressionEvaluation("*", current!!, value)
                        }
                        AssignmentType.DIV -> {
                            variables[stm.variable] = expressionEvaluation("/", current!!, value)
                        }
                        AssignmentType.POW -> {
                            variables[stm.variable] = expressionEvaluation("^", current!!, value)
                        }
                        AssignmentType.MOD -> {
                            variables[stm.variable] = expressionEvaluation("%", current!!, value)
                        }
                    }
                    null
                }
                is CallExpr -> {
                    expressionEvaluation(stm)
                }
                is RawFunctionCall -> {
                    interpret(stm.function.body, output)
                }
                is If -> {
                    val cond = expressionEvaluation(stm.Condition) as BoolLitExpr
                    if (cond.value) {
                        interpret(stm.IfBlock, output)
                    } else null
                }
                is Empty -> null
                is IfElse -> {
                    val cond = expressionEvaluation(stm.Condition) as BoolLitExpr
                    if (cond.value) {
                        interpret(stm.IfBlock, output)
                    } else {
                        interpret(stm.ElseBlock, output)
                    }
                }
                is ReturnStatement -> {
                    var expr = expressionEvaluation(stm.expr)
                    if (output!!.type is FloatType && expr is IntLitExpr){
                        expr = FloatLitExpr(expr.value.toFloat())
                    }
                    variables[output] = expr
                    return variables[output]!!
                }
                else -> throw NotImplementedError("$stm")
            }
        }catch(e: Exception){
            throw Exception("Fail to interpret: $stm \n$e")
        }
    }
    fun expressionEvaluation(op: String, e1: Expression, e2: Expression):Expression{
        val left = expressionEvaluation(e1)
        val right = expressionEvaluation(e2)

        return if (left is IntLitExpr && right is IntLitExpr){
            applyOperation(op, left.value, right.value)
        }
        else if (left is FloatLitExpr && right is IntLitExpr){
            applyOperation(op, left.value, right.value.toFloat())
        }
        else if (left is IntLitExpr && right is FloatLitExpr){
            applyOperation(op, left.value.toFloat(), right.value)
        }
        else if (left is FloatLitExpr && right is FloatLitExpr){
            applyOperation(op, left.value, right.value)
        }
        else if (left is BoolLitExpr && right is BoolLitExpr){
            applyOperation(op, left.value, right.value)
        }
        else if (right is StringLitExpr){
            applyOperation(op, expressionToString(left), right.value)
        }
        else if (left is StringLitExpr){
            applyOperation(op, left.value, expressionToString(right))
        }
        else if (left is EnumValueExpr && right is EnumValueExpr){
            applyOperation(op, left.index, right.index)
        }
        else if (left is EnumValueExpr && right is IntLitExpr){
            applyOperation(op, left.index, right.value)
        }
        else if (left is IntLitExpr && right is EnumValueExpr){
            applyOperation(op, left.value, right.index)
        }
        else {
            throw NotImplementedError("$e1 $op $e2")
        }
    }
    fun expressionEvaluation(e: Expression):Expression{
        return when(e){
            is BinaryExpr -> {
                expressionEvaluation(e.op, e.first, e.second)
            }
            is UnaryExpr -> {
                val value = expressionEvaluation(e.first)
                when(e.op){
                    "!" -> {
                        val v = value as BoolLitExpr
                        BoolLitExpr(!v.value)
                    }
                    "-" -> {
                        when (value) {
                            is IntLitExpr -> IntLitExpr(-value.value)
                            is FloatLitExpr -> FloatLitExpr(-value.value)
                            else -> throw NotImplementedError()
                        }
                    }
                    else -> throw NotImplementedError()
                }
            }
            is VariableExpr -> {
                variables[e.variable]!!
            }
            is StatementThanExpression -> {
                interpret(e.statement, null)
                expressionEvaluation(e.expr)
            }
            is CallExpr -> {
                when(e.value){
                    is FunctionExpr -> {
                        val fct = e.value.function
                        val args = fct.input.zip(withDefault(e.args, fct.from.map { it.defaultValue }))
                        val instr = args.map { LinkedVariableAssignment(it.first, it.second, AssignmentType.SET) }
                        interpret(Sequence(instr), null)
                        interpret(e.value.function.body, fct.output)
                        variables[fct.output]!!
                    }
                    is VariableExpr -> {
                        expressionEvaluation(CallExpr(variables[e.value.variable]!!, e.args))
                    }
                    else -> throw NotImplementedError("$e")
                }
            }
            else -> e
        }
    }
}