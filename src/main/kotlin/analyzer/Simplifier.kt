package analyzer

import ast.*
import context.DualContext
import context.IContext
import data_struct.DataStructModifier
import data_struct.Function
import data_struct.Variable
import interpreter.Interpreter
import utils.getOperationFunctionName
import utils.withDefault
import kotlin.math.pow

var compile: (Statement, IContext)->Statement = { s, _ -> s}
var compileSimply: (Statement, IContext)->Statement = { s, _ -> s}
fun runSimplifier(stm: Statement, context: IContext, callback: (Statement, IContext)->Statement): Statement{
    compile = callback
    compileSimply = {s, c -> simplify(compile(s, c), c)}
    return simplify(stm, context)
}

fun simplify(stm: Statement, context: IContext): Statement {
    return when(stm){
        is If -> {
            when(val expr = simplifyExpression(stm.Condition, context)){
                is BoolLitExpr -> {
                    if (expr.value){
                        simplify(stm.IfBlock, context)
                    } else {
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
        is Switch -> {
            val genCases = ArrayList<Case>()
            collectCases(Sequence(stm.forgenerate.map{ simplify(it, context) }), genCases)
            simplify(buildSwitchTree(stm.scrutinee, stm.cases+genCases, context), context)
        }
        is Block -> {
            val nstm = stm.statements.map { simplify(it, context) }
                          .filter{ it !is Empty }
                          .map{it -> if (it is Block){ Sequence(it.statements) }else{ it }}
            when (nstm.size) {
                0 -> { Empty() }
                1 -> { nstm[0] }
                else -> { Block(nstm).withParent(stm) }
            }
        }
        is Sequence -> {
            val nstm = stm.statements.map { simplify(it, context) }.filter{ it !is Empty }
            simplifySequence(nstm)
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
            else if (stm.variable.type is StructType){
                val expr = stm.expr
                val variable = stm.variable
                val funcId = getOperationFunctionName(stm.op)
                if (stm.expr is CallExpr && stm.expr.value is UnresolvedStructConstructorExpr){
                    compileSimply(CallExpr(
                        UnresolvedFunctionExpr(variable.childrenFunction[Identifier("init")]!!.filter { it.modifier.operator }),
                        stm.expr.args), context)
                }
                else if (variable.childrenFunction[funcId] != null  &&
                    variable.childrenFunction[funcId]!!.any { it.modifier.operator }){
                    compileSimply(CallExpr(
                        UnresolvedFunctionExpr(variable.childrenFunction[funcId]!!.filter { it.modifier.operator }),
                        listOf(stm.expr)), context)
                } else {
                    if (expr is VariableExpr && stm.op == AssignmentType.SET) {
                        Sequence(stm.variable.childrenVariable.map{
                            LinkedVariableAssignment(it.value, VariableExpr(expr.variable.childrenVariable[it.key]!!), stm.op)
                        })
                    } else if (expr is CallExpr && expr.value is FunctionExpr) {
                        Sequence(listOf(
                            simplify(stm.expr, context),
                            compileSimply(LinkedVariableAssignment(stm.variable, VariableExpr(expr.value.function.output), stm.op), context)
                        ))
                    } else throw NotImplementedError()
                }
            }
            else if (stm.variable.type is RangeType){
                val expr = stm.expr
                if (expr is VariableExpr){
                    Sequence(stm.variable.childrenVariable.map{
                        LinkedVariableAssignment(it.value, VariableExpr(expr.variable.childrenVariable[it.key]!!), stm.op)
                    })
                } else if (expr is RangeLitExpr){
                    val children = stm.variable.childrenVariable
                    Sequence(listOf(
                        LinkedVariableAssignment(children[Identifier("min")]!!, expr.min, stm.op),
                        LinkedVariableAssignment(children[Identifier("max")]!!, expr.max, stm.op))
                    )
                } else throw NotImplementedError()
            }
            else if (stm.variable.type is FuncType){
                if (stm.expr is FunctionExpr) { context.addLambda(stm.expr.function, compile) }
                stm
            }
            else if (stm.variable.type is ArrayType){
                if (stm.expr is VariableExpr){
                    if (stm.expr.variable.type is ArrayType){
                        simplify(Sequence(
                            stm.variable.childrenVariable.map{ (k, it) ->
                                LinkedVariableAssignment(it, VariableExpr(stm.expr.variable.childrenVariable[k]!!), stm.op)
                            }
                        ), context)
                    } else {
                        simplify(Sequence(
                            stm.variable.childrenVariable.map {
                                LinkedVariableAssignment(it.value, stm.expr, stm.op)
                            }
                        ), context)
                    }

                } else if (stm.expr is ArrayExpr) {
                    simplify(Sequence(
                        stm.variable.childrenVariable.values.sortedBy { it.name.toString() }
                            .zip(stm.expr.value)
                            .map { (v, e) -> LinkedVariableAssignment(v, e, stm.op) }
                    ), context)
                } else if (stm.expr is TupleExpr) {
                    simplify(Sequence(
                        stm.variable.childrenVariable.values.sortedBy { it.name.toString() }
                            .zip(stm.expr.value)
                            .map { (v, e) -> LinkedVariableAssignment(v, e, stm.op) }
                    ), context)
                } else {
                    simplify(Sequence(
                        stm.variable.childrenVariable.map {
                            LinkedVariableAssignment(it.value, stm.expr, stm.op)
                        }
                    ), context)
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
            if (stm.function.modifier.lazy || stm.function.modifier.inline){
                stm
            } else {
                val body = simplify(stm.body, context)
                stm.function.body = body
                FunctionBody(body, stm.function)
            }
        }
        is LinkedForgenerate -> {
            val gn = stm.generator.getIterator()
            val statements = ArrayList<Statement>()
            while(gn.hasNext()){
                statements += runReplace(stm.body,
                    gn.next().map{ (k,v) ->
                        if (k == "") {
                            stm.identifier to v
                        } else {
                            stm.identifier.sub(k) to v
                        }
                    }.toMap())
            }
            compileSimply(Sequence(statements), context)
        }
        is RawCommandArg -> {
            var str = stm.cmd
            val args = stm.args.map { simplifyExpression(it, context) }
                .mapIndexed{ id, it -> Pair(id, it) }
                .reversed()
                .map { str = str.replace("\$${it.first}", expressionToString(it.second)) }

            RawCommand(str)
        }
        else -> stm
    }
}


fun simplifyIfElse(stm: IfElse, expr: Expression, context: IContext):Statement{
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

fun extractExpression(expr: Expression, context: IContext): Pair<Expression, Statement>{
    return when(expr){
        is VariableExpr -> { Pair(expr, Empty()) }
        is LitExpr -> { Pair(expr, Empty()) }
        is BinaryExpr -> {
            when(expr.op){
                in listOf("<","<=", ">", ">=", "==", "!=") -> {
                    val lst = emptyList<Statement>().toMutableList()

                    val left = extractSideExpression(expr.first, context, lst)
                    val right = extractSideExpression(expr.second, context, lst)

                    Pair(BinaryExpr(expr.op, left, right), Sequence(lst))
                }
                "in" -> Pair(expr, Empty())
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
fun extractSideExpression(right: Expression, context: IContext, lst: MutableList<Statement>): Expression {
    return when (right) {
        is LitExpr, is VariableExpr -> {
            right
        }
        is FunctionExpr -> {
            IntLitExpr(right.function.hashCode())
        }
        is EnumValueExpr -> {
            IntLitExpr(right.index)
        }
        else -> {
            val ret = putInTMPVariable(right, AssignmentType.SET, context)
            lst.add(ret.second)
            VariableExpr(ret.first)
        }
    }
}

fun simplifyExpression(expr: Expression, context: IContext): Expression {
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
            }
            else if (left is StringLitExpr && right is StringLitExpr){
                applyOperation(expr.op, left.value, right.value)
            }
            else if (right is StringLitExpr){
                applyOperation(expr.op, expressionToString(left), right.value)
            }
            else if (left is StringLitExpr){
                applyOperation(expr.op, left.value, expressionToString(right))
            }
            else {
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

fun simplifyFunctionCall(stm: Expression, args: List<Expression>, context: IContext): Pair<Statement, Expression>{
    return if (stm is FunctionExpr && !stm.function.modifier.lazy && !stm.function.modifier.inline) {
        Pair(
            simplifySequence(
                stm.function.input.zip(withDefault(args, stm.function.from.map { it.defaultValue }))
                    .map { (v, e) -> simplify(LinkedVariableAssignment(v, e, AssignmentType.SET), context) } +
                        RawFunctionCall(stm.function)
            ),
            VariableExpr(stm.function.output)
        )
    } else if (stm is FunctionExpr && stm.function.modifier.lazy) {
        val map = lazyFunctionMapLazyArg(stm.function, args)
        val assignment = lazyFunctionAssignArg(stm.function, args)

        val block = runReplace(stm.function.body, map)
        val sub = DualContext(stm.function.context, context).sub(block.hashCode().toString())

        stm.function.input.map { sub.update(it.name.getLast(), it) }

        Pair(
            Sequence(assignment+simplify(compile(block, sub), context)),
            VariableExpr(stm.function.output)
        )
    } else if (stm is FunctionExpr && stm.function.modifier.inline) {
        val interpreter = Interpreter()
        Pair(Empty(), interpreter.interpret(CallExpr(stm, args), null)!!)
    } else if (stm is VariableExpr) {
        simplifyFunctionCall(
            FunctionExpr(context.getLambdaFunction(stm.variable.type as FuncType, compile)),
        listOf(stm)+args,
            context)
    } else if (stm is CallExpr){
        val ret = simplifyFunctionCall(stm.value, stm.args, context)
        val inter = simplifyFunctionCall(
            ret.second,
            args,
            context)
        return Pair(Sequence(listOf(ret.first)+inter.first), inter.second)
    } else {
        throw NotImplementedError(stm.toString())
    }
}

fun lazyFunctionAssignArg(function: Function, args: List<Expression>):List<Statement>{
    return function.input
        .zip(withDefault(args, function.from.map { it.defaultValue }))
        .filter { (v, _) -> !v.modifier.lazy }
        .map { (v, a) -> LinkedVariableAssignment(v, a, AssignmentType.SET) }
}
fun lazyFunctionMapLazyArg(function: Function, args: List<Expression>):Map<Identifier, Expression>{
    return function.input
        .zip(withDefault(args, function.from.map { it.defaultValue }))
        .filter { (v, _) -> v.modifier.lazy }
        .associate { (v, a) -> Pair(v.name.getLast(), a) }
        .toMap()
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
fun applyOperation(op: String, left: String, right: String): Expression{
    return when(op){
        "+" -> StringLitExpr(left + right)
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
fun putInTMPVariable(expr: Expression, op: AssignmentType, context: IContext): Pair<Variable, Statement>{
    val modifier = DataStructModifier()
    val id = context.getTmpVarIdentifier()
    val ret = compile(Sequence(listOf(
        VariableDeclaration(modifier, id, VarType(expr), null, true),
        UnlinkedVariableAssignment(id,expr,op))),
        context)
    return Pair(context.getVariable(id), ret)
}
fun getTMPVariable(type: DataType, context: IContext): Variable {
    val modifier = DataStructModifier()
    val id = context.getTmpVarIdentifier()
    compile(VariableDeclaration(modifier, id, type), context)
    return context.getVariable(id)
}

fun expressionToString(expr: Expression): String{
    return when(expr){
        is BoolLitExpr -> expr.value.toString()
        is IntLitExpr -> expr.value.toString()
        is FloatLitExpr -> expr.value.toString()
        is StringLitExpr -> expr.value
        is VariableExpr -> expr.variable.name.toString()
        is FunctionExpr -> expr.function.name.toString()
        is StatementThanExpression -> expressionToString(expr.expr)
        is EnumValueExpr -> expr.value.name.toString()
        else -> expr.toString()
    }
}

fun collectCases(stm: Statement, lst: ArrayList<Case>){
    when(stm){
        is Case -> lst.add(stm)
        is Block -> stm.statements.map { collectCases(it, lst) }
        is Sequence -> stm.statements.map { collectCases(it, lst) }
        else -> throw Exception("Switch can only contains cases. Found: $stm")
    }
}