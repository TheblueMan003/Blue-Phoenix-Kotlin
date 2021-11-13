package analyzer

import ast.*
import utils.withDefault
import kotlin.math.pow

var compile: (Statement, Context)->Statement = {s,_ -> s}

fun runSimplifier(stm: Statement, context: Context, callback: (Statement, Context)->Statement): Statement{
    compile = callback
    return simplify(stm, context)
}

fun simplify(stm: Statement, context: Context): Statement {
    return when(stm){
        is If -> {
            when(val expr = simplifyExpression(stm.Condition, context)){
                is BoolLitExpr -> {
                    if (expr.value){
                        simplify(stm.IfBlock, context)
                    }else{
                        Empty()
                    }
                }
                is BinaryExpr ->{
                    val block = simplify(stm.IfBlock, context)
                    when(expr.op){
                        "&&" -> {
                            simplify(If(expr.first, If(expr.second, block)), context)
                        }
                        "||" -> {
                            simplify(IfElse(expr.first, block, If(expr.second, block)), context)
                        }
                        else -> {
                            val extracted = extractExpression(expr, context)
                            Sequence(listOf(
                                extracted.second,
                                If(extracted.first, simplify(stm.IfBlock, context)).withParent(stm)))
                        }
                    }
                }
                else -> If(expr, simplify(stm.IfBlock, context)).withParent(stm)
            }
        }
        is IfElse -> {
            when(val expr = simplifyExpression(stm.Condition, context)){
                is BoolLitExpr -> {
                    if (expr.value){
                        simplify(stm.IfBlock, context)
                    }else{
                        simplify(stm.ElseBlock, context)
                    }
                }
                is BinaryExpr -> {
                    when(expr.op) {
                        "&&" -> {
                            simplify(IfElse(expr.first, IfElse(expr.second, stm.IfBlock, stm.ElseBlock), stm.ElseBlock), context)
                        }
                        "||" -> {
                            simplify(IfElse(expr.first, stm.IfBlock, IfElse(expr.second, stm.IfBlock, stm.ElseBlock)), context)
                        }
                        else -> {
                            val extracted = extractExpression(expr, context)
                            Sequence(listOf(
                                extracted.second,
                                simplifyIfElse(IfElse(extracted.first, stm.IfBlock, stm.ElseBlock).withParent(stm) as IfElse,
                                    extracted.first, context)))
                        }
                    }
                }
                else -> {
                    simplifyIfElse(stm, expr, context)
                }
            }
        }
        is Block ->{
            val nstm = stm.statements.map { simplify(it, context) }
                          .filter{ it !is Empty }
                          .map{it -> if (it is Block){ Sequence(it.statements) }else{ it }}
            when (nstm.size) {
                0 -> { Empty() }
                1 -> { nstm[0] }
                else -> { Block(nstm).withParent(stm) }
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
                        simplify(it.statement, context))
                }).withParent(stm)
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
        is ReturnStatement -> {
            simplify(LinkedVariableAssignment(stm.function.output, stm.expr, AssignmentType.SET), context)
        }
        is CallExpr -> {
            simplifyFunctionCall(stm.value, stm.args, context).first
        }
        is FunctionBody -> {
            FunctionBody(simplify(stm.body, context), stm.function)
        }
        else -> stm
    }
}


fun simplifyIfElse(stm: IfElse, expr: Expression, context: Context):Statement{
    val variable = getTMPVariable(BoolType(), context)
    return Sequence(listOf(
        LinkedVariableAssignment(variable, BoolLitExpr(false), AssignmentType.SET),
        If(expr, simplify(Block(listOf(
            stm.IfBlock,
            LinkedVariableAssignment(variable, BoolLitExpr(true), AssignmentType.SET))
        ).withParent(stm), context)),
        If(UnaryExpr("!", VariableExpr(variable)), simplify(
            stm.ElseBlock
            , context))
    ))
}

fun extractExpression(expr: Expression, context: Context): Pair<Expression, Statement>{
    return when(expr){
        is VariableExpr -> { Pair(expr, Empty()) }
        is LitExpr -> { Pair(expr, Empty()) }
        is BinaryExpr -> {
            when(expr.op){
                in listOf("<","<=", ">", ">=", "==", "!=") -> {
                    val lst = emptyList<Statement>().toMutableList()
                    var left = expr.first
                    if (left !is LitExpr && left !is VariableExpr){
                        val ret = putInTMPVariable(left, AssignmentType.SET,context)
                        lst.add(ret.second)
                        left = VariableExpr(ret.first)
                    }
                    var right = expr.second
                    if (right !is LitExpr && right !is VariableExpr){
                        val ret = putInTMPVariable(right, AssignmentType.SET, context)
                        lst.add(ret.second)
                        right = VariableExpr(ret.first)
                    }
                    Pair(BinaryExpr(expr.op, left, right), Sequence(lst))
                }
                else -> {
                    throw NotImplementedError()
                }
            }
        }
        is UnaryExpr -> {
            val ext = extractExpression(expr.first, context)
            Pair(UnaryExpr(expr.op, ext.first), ext.second)
        }
        else -> throw NotImplementedError()
    }
}
fun simplifyExpression(expr: Expression, context: Context): Expression {
    return when(expr){
        is BinaryExpr -> {
            val left = simplifyExpression(expr.first, context)
            val right = simplifyExpression(expr.second, context)
            return if (left is IntLitExpr && right is IntLitExpr){
                applyOperation(expr.op, left.value, right.value)
            }
            else if (left is FloatLitExpr && right is IntLitExpr){
                applyOperation(expr.op, left.value, right.value.toFloat())
            }
            else if (left is IntLitExpr && right is FloatLitExpr){
                applyOperation(expr.op, left.value.toFloat(), right.value)
            }
            else if (left is FloatLitExpr && right is FloatLitExpr){
                applyOperation(expr.op, left.value, right.value)
            }
            else if (left is BoolLitExpr && right is BoolLitExpr){
                applyOperation(expr.op, left.value, right.value)
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
                stm.function.input.zip(withDefault(args, stm.function.from.map { it.defaultValue }))
                    .map { (v, e) -> simplify(LinkedVariableAssignment(v, e, AssignmentType.SET), context) } +
                        RawFunctionCall(stm.function)
            ),
            VariableExpr(stm.function.output)
        )
    }else throw NotImplementedError()
}

fun simplifySequence(nstm: List<Statement>): Statement {
    return when (nstm.size) {
        0 -> { Empty() }
        1 -> { nstm[0] }
        else -> { Sequence(nstm) }
    }
}



fun applyOperation(op: String, left: Boolean, right: Boolean): Expression{
    return when(op){
        "&&" -> BoolLitExpr(left && right)
        "||" -> BoolLitExpr(left || right)
        else -> throw NotImplementedError()
    }
}
fun applyOperation(op: String, left: Int, right: Int): Expression{
    return when(op){
        "+" -> IntLitExpr(left + right)
        "-" -> IntLitExpr(left - right)
        "*" -> IntLitExpr(left * right)
        "/" -> IntLitExpr(left / right)
        "%" -> IntLitExpr(left % right)
        "^" -> IntLitExpr(left.toDouble().pow(right.toDouble()).toInt())
        "<=" -> BoolLitExpr(left <= right)
        "<"  -> BoolLitExpr(left < right)
        ">"  -> BoolLitExpr(left > right)
        ">=" -> BoolLitExpr(left >= right)
        "==" -> BoolLitExpr(left == right)
        "!=" -> BoolLitExpr(left != right)
        else -> throw NotImplementedError()
    }
}
fun applyOperation(op: String, left: Float, right: Float): Expression{
    return when(op){
        "+" -> FloatLitExpr(left + right)
        "-" -> FloatLitExpr(left - right)
        "*" -> FloatLitExpr(left * right)
        "/" -> FloatLitExpr(left / right)
        "%" -> FloatLitExpr(left % right)
        "^" -> FloatLitExpr(left.toDouble().pow(right.toDouble()).toFloat())
        "<=" -> BoolLitExpr(left <= right)
        "<"  -> BoolLitExpr(left < right)
        ">"  -> BoolLitExpr(left > right)
        ">=" -> BoolLitExpr(left >= right)
        "==" -> BoolLitExpr(left == right)
        "!=" -> BoolLitExpr(left != right)
        else -> throw NotImplementedError()
    }
}
fun putInTMPVariable(expr: Expression, op: AssignmentType, context: Context): Pair<Variable, Statement>{
    val modifier = DataStructModifier()
    val id = context.getTmpVarIdentifier()
    val ret = compile(Sequence(listOf(
        VariableDeclaration(modifier, id, VarType(expr)),
        UnlinkedVariableAssignment(id,expr,op))),
        context)
    return Pair(context.getVariable(id), ret)
}
fun getTMPVariable(type: DataType, context: Context): Variable{
    val modifier = DataStructModifier()
    val id = context.getTmpVarIdentifier()
    compile(VariableDeclaration(modifier, id, type), context)
    return context.getVariable(id)
}