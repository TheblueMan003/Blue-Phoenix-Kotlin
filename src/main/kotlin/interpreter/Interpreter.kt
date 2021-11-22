package interpreter

import analyzer.applyOperation
import ast.*
import data_struct.Variable
import utils.withDefault

class Interpreter {
    private val variables = HashMap<Variable, Expression>()

    fun interpret(stm: Statement): Expression?{
        return when(stm){
            is Block -> {
                val values = stm.statements.mapNotNull { interpret(it) }
                if (values.isNotEmpty()){ values.last() } else null
            }
            is Sequence -> {
                val values = stm.statements.mapNotNull { interpret(it) }
                if (values.isNotEmpty()){ values.last() } else null
            }
            is LinkedVariableAssignment -> {
                val value = expressionEvaluation(stm.expr)
                val current = variables[stm.variable]
                when (stm.op) {
                    AssignmentType.SET -> { variables[stm.variable] = expressionEvaluation(value) }
                    AssignmentType.SUB -> { variables[stm.variable] = expressionEvaluation("-", current!!, value) }
                    AssignmentType.ADD -> { variables[stm.variable] = expressionEvaluation("+", current!!, value) }
                    AssignmentType.MUL -> { variables[stm.variable] = expressionEvaluation("*", current!!, value) }
                    AssignmentType.DIV -> { variables[stm.variable] = expressionEvaluation("/", current!!, value) }
                    AssignmentType.POW -> { variables[stm.variable] = expressionEvaluation("^", current!!, value) }
                    AssignmentType.MOD -> { variables[stm.variable] = expressionEvaluation("%", current!!, value) }
                }
                variables[stm.variable]
            }
            is CallExpr -> {
                when(stm.value){
                    is FunctionExpr -> {
                        val fct = stm.value.function
                        val args = fct.input.zip(withDefault(stm.args, fct.from.map { it.defaultValue }))
                        val instr = args.map { LinkedVariableAssignment(it.first, it.second, AssignmentType.SET) }
                        interpret(Sequence(instr))
                        interpret(stm.value.function.body)
                    }
                    is VariableExpr -> {
                        interpret(CallExpr(variables[stm.value.variable]!!, stm.args))
                    }
                    else -> throw NotImplementedError()
                }
            }
            is RawFunctionCall -> {
                interpret(stm.function.body)
            }
            is If -> {
                val cond = expressionEvaluation(stm.Condition) as BoolLitExpr
                if (cond.value){
                    interpret(stm.IfBlock)
                } else null
            }
            is IfElse -> {
                val cond = expressionEvaluation(stm.Condition) as BoolLitExpr
                if (cond.value){
                    interpret(stm.IfBlock)
                } else {
                    interpret(stm.ElseBlock)
                }
            }
            else -> throw NotImplementedError("$stm")
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
        else if (left is StringLitExpr && right is StringLitExpr){
            applyOperation(op, left.value, right.value)
        } else {
            throw NotImplementedError()
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
            else -> e
        }
    }
}