package analyzer

import ast.*
import context.IContext
import data_struct.Function
import utils.getOperationFunctionName
import utils.withDefault


data class InvalidTypeException(val type: DataType): Exception()

fun runChecker(stm: Statement, context: IContext): Statement {
    return check(stm, context)
}
fun check(stm: Statement, context: IContext): Statement {
    try{
        return when(stm){
            is If -> {
                val cond = checkExpression(stm.Condition, context)
                expectBoolean(cond.second)
                If(cond.first, check(stm.IfBlock, context)).withParent(stm)
            }
            is IfElse -> {
                val cond = checkExpression(stm.Condition, context)
                expectBoolean(cond.second)
                IfElse(cond.first, check(stm.IfBlock, context), check(stm.ElseBlock, context)).withParent(stm)
            }
            is Block ->{
                Block(stm.statements.map { check(it, context) }).withParent(stm)
            }
            is Sequence ->{
                Sequence(stm.statements.map { check(it, context) })
            }
            is Switch ->{
                val expr = checkExpression(stm.scrutinee, context)
                Switch(expr.first, stm.cases.map {
                    val scrut = checkExpression(it.expr, context)
                    checkOwnership(scrut.second, expr.second)
                    Case(scrut.first, check(it.statement, context)) },
                    stm.forgenerate).withParent(stm)
            }
            is LinkedVariableAssignment -> {
                if (stm.variable.type is FuncType){
                    val vType = stm.variable.type as FuncType
                    when (stm.expr) {
                        is UnresolvedFunctionExpr -> {
                            LinkedVariableAssignment(stm.variable,
                                FunctionExpr(findFunction(vType.from, stm.expr.function)),
                                stm.op)
                        }
                        is VariableExpr -> {
                            stm
                        }
                        is FunctionExpr -> {
                            stm
                        }
                        is UnresolvedExpr -> {
                            throw Exception("Cannot be assign to function")
                        }
                        is CallExpr -> {
                            val call = checkExpression(stm.expr.value, context)
                            if (call.second is FuncType && checkOwnership((call.second as FuncType).to, stm.variable.type)){
                                stm
                            }else {
                                throw Exception("Cannot be assign to function ${call.second} not in ${stm.variable.type}")
                            }
                        }
                        else -> {
                            throw Exception("Cannot be assign to function $stm")
                        }
                    }
                }
                else if (stm.variable.type is EnumType){
                    val vType = stm.variable.type as EnumType
                    when (stm.expr) {
                        is EnumValueExpr -> {
                            if (stm.expr.enum != vType.enum) throw InvalidTypeException(EnumType(stm.expr.enum))
                            LinkedVariableAssignment(stm.variable, stm.expr, stm.op)
                        }
                        is UnresolvedExpr -> {
                            val expr = stm.expr.choice.filter { it is EnumValueExpr }
                                .map { it as EnumValueExpr }
                                .filter { it.enum == vType.enum }
                                .first()
                            LinkedVariableAssignment(stm.variable, expr, stm.op)
                        }
                        is VariableExpr -> {
                            stm
                        }
                        else -> {
                            throw Exception("Cannot be assign to enum $stm")
                        }
                    }
                } else {
                    val v = checkExpression(stm.expr, context)
                    if (!checkOwnership(v.second, stm.variable.type))
                        throw Exception("Invalid type error: ${v.second} not in ${stm.variable.type}")
                    LinkedVariableAssignment(stm.variable, v.first, stm.op)
                }
            }
            is CallExpr -> {
                checkExpression(stm, context).first
            }
            is ReturnStatement -> {
                val v = checkExpression(stm.expr, context)
                if (!checkOwnership(v.second, stm.function.output.type))
                    throw Exception("Invalid type error: ${v.second} not in ${stm.function.output.type}")
                ReturnStatement(v.first, context.getCurrentFunction()!!)
            }
            is FunctionBody -> {
                if (stm.function.modifier.lazy){
                    stm
                } else {
                    val prev = context.getCurrentFunction()
                    context.setCurrentFunction(stm.function)
                    val ret = FunctionBody(check(stm.body, context), stm.function)

                    var startedDefault = false
                    for (v in stm.function.input.zip(stm.function.from)) {
                        if (v.second.defaultValue != null) {
                            check(LinkedVariableAssignment(v.first, v.second.defaultValue!!, AssignmentType.SET), context)
                            startedDefault = true
                        } else if (startedDefault) {
                            throw Exception("${v.first.name} must have a default value")
                        }
                    }

                    context.setCurrentFunction(prev)
                    stm.function.body = ret.body
                    ret
                }
            }
            is RawCommandArg -> {
                RawCommandArg(stm.cmd, stm.args.map{ checkExpression(it, context).first})
            }
            else -> stm
        }
    }catch(e: Exception){
        throw Exception("Fail to analyse: $stm \n$e")
    }
}

fun checkExpression(stm: Expression, context: IContext): Pair<Expression, DataType>{
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
            if (stm.function.size == 1){
                val fct = stm.function[0]
                return Pair(FunctionExpr(fct), FuncType(fct.from.map { it.type }, fct.output.type))
            } else {
                throw NotImplementedError()
            }
        }
        is UnresolvedExpr -> {
            Pair(stm, UnresolvedType())
        }
        is TupleExpr -> {
            val lst = stm.value.map { checkExpression(it, context) }
            Pair(TupleExpr(lst.map { it.first }), TupleType(lst.map { it.second }))
        }
        is CallExpr -> {
            return when(stm.value){
                is UnresolvedStructConstructorExpr -> {
                    Pair(CallExpr(stm.value, stm.args, stm.operator), StructType(stm.value.struct, emptyList()))
                }
                is UnresolvedFunctionExpr -> {
                    val args = stm.args.map{checkExpression(it, context)}
                    val fct = findFunction(args.map { it.second }, stm.value.function, stm.operator)

                    if (context.getCurrentFunction() != null){
                        context.getCurrentFunction()!!.addFuncCall(fct)
                    }else{
                        fct.use()
                    }

                    Pair(CallExpr(FunctionExpr(fct), args.map { it.first }, stm.operator), fct.output.type)
                }
                is VariableExpr -> {
                    val variable = stm.value.variable
                    val args = stm.args.map { checkExpression(it, context) }
                    if (variable.type is FuncType) {
                        val to = variable.type as FuncType

                        if (to.from.size != args.size) throw Exception("Wrong Number Of Arguments!")
                        args.zip(to.from).map { (a, b) ->
                            if (!checkOwnership(a.second, b)) {
                                throw Exception("cannot put ${a.second} in $b")
                            }
                        }


                        Pair(CallExpr(stm.value, args.map { it.first }, stm.operator), to.to)
                    } else if (variable.childrenFunction[Identifier("invoke")] != null) {
                        if (variable.childrenFunction[Identifier("invoke")] != null) {
                            val fct = findFunction(
                                args.map { it.second },
                                variable.childrenFunction[Identifier("invoke")]!!,
                                true
                            )

                            Pair(CallExpr(FunctionExpr(fct), stm.args, stm.operator), fct.output.type)
                        } else {
                            throw Exception("$variable Not a function.")
                        }
                    } else {
                        throw Exception("$variable Not a function.")
                    }
                }
                is FunctionExpr -> {
                    Pair(stm, stm.value.function.output.type)
                }
                is CallExpr -> {
                    val inter = checkExpression(stm.value, context)
                    return if (inter.second is FuncType) {
                        Pair(CallExpr(inter.first, stm.args, stm.operator), (inter.second as FuncType).to)
                    } else {
                        throw Exception("${inter.first} Not a function. (${inter.second})")
                    }
                }
                else -> throw Exception("Invalid Function Call Expression: $stm")
            }
        }
        is BinaryExpr ->{
            val first = checkExpression(stm.first, context)
            val second = checkExpression(stm.second, context)
            operationCombine(stm.op, first, second, context)
        }
        is UnaryExpr ->{
            val p = checkExpression(stm.first, context)
            Pair(UnaryExpr(stm.op, p.first), p.second)
        }
        is FunctionExpr -> {
            val fct = stm.function
            return Pair(stm, FuncType(fct.from.map { it.type }, fct.output.type))
        }
        is EnumValueExpr -> {
            Pair(stm, EnumType(stm.enum))
        }
        is RangeLitExpr -> {
            val first = checkExpression(stm.min, context)
            val second = checkExpression(stm.max, context)
            operationCombine("..", first, second, context)
        }
        is ArrayExpr -> {
            val checked = stm.value.map { checkExpression(it, context) }
            val types = checked.map { it.second }
            val data = checked.map { it.first }
            Pair(ArrayExpr(data), ArrayType(types.reduce { a,b -> biggestType(a,b) }, listOf(types.size)))
        }
        else -> throw NotImplementedError("$stm")
    }
}

fun biggestType(t1: DataType, t2: DataType): DataType {
    if (isNumerical(t1) && isNumerical(t2)){
        return if (t1 is FloatType || t2 is FloatType) FloatType() else IntType()
    }
    throw NotImplementedError()
}

fun checkOwnership(t1: DataType, t2: DataType): Boolean{
    return when(t2){
        is VoidType -> true
        is FloatType -> t1 is IntType || t1 is FloatType
        is IntType -> t1 is IntType
        is StringType -> true
        is BoolType -> t1 is BoolType
        is TupleType -> t1 is TupleType && t1.type.zip(t2.type).map { (x,y) -> checkOwnership(x,y) }.all { it }
        is FuncType -> t1 is FuncType && t1.from.zip(t2.from).map { (x,y) -> checkOwnership(y,x) }.all { it }
                            && checkOwnership(t1.to, t2.to)
        is ArrayType -> t1 is ArrayType && checkOwnership(t1.subtype, t2.subtype) && t1.length == t2.length
        is StructType ->
            (t1 is StructType && t1.name == t2.name)||
            (
                t2.name.methods.filter { it.identifier == Identifier("set") && it.modifier.operator}
            ).isNotEmpty()
        is EnumType -> t1 is EnumType
        is RangeType -> t1 is RangeType && checkOwnership(t1.type, t2.type)
        else -> throw NotImplementedError("$t1 < $t2")
    }
}

fun checkOwnershipCost(t1: DataType, t2: DataType): Int {
    return when (t2) {
        is VoidType -> 1000
        is FloatType -> if (t1 is IntType) {
            1
        } else {
            0
        }
        is IntType -> 0
        is StringType -> 0
        is BoolType -> 0
        is TupleType -> {
            val t = t1 as TupleType
            t.type.zip(t2.type).sumOf { (x, y) -> checkOwnershipCost(x, y) }
        }
        is FuncType -> {
            val t = t1 as FuncType
            t.from.zip(t2.from).sumOf { (x, y) -> checkOwnershipCost(x, y) } + checkOwnershipCost(t.to, t2.to)
        }
        is ArrayType -> {
            val t = t1 as ArrayType
            checkOwnershipCost(t.subtype, t2.subtype)
        }
        is StructType -> { if (t1 is StructType && t1.name == t2.name){0}else{1} }
        is EnumType -> { if (t1 is EnumType && t1.enum == t2.enum){0}else{1} }
        else -> throw NotImplementedError()
    }
}

fun isBoolean(t: DataType):Boolean{
    return (t is BoolType)
}
fun expectBoolean(t: DataType){
    if (t !is BoolType) throw Exception("Boolean Type Expected")
}

fun isNumerical(t: DataType):Boolean{
    return when(t){
        is IntType -> true
        is FloatType -> true
        else -> false
    }
}

fun findFunction(from: List<DataType>, lst: List<Function>, isOperator: Boolean = false): Function {
    val fit =
        lst.asSequence()
            .filter { it.modifier.operator || !isOperator }
            .filter { fct -> fct.input
                .map {vr -> vr.type}
                .zip(from)
                .map { (x, y) -> checkOwnership(y, x)}
                .all { it } }
            .filter { fct -> withDefault(from, fct.from.map { it.defaultValue }).size == fct.input.size }
            .map { fct -> Pair(fct.input
                .map {vr -> vr.type}
                .zip(withDefault(from, fct.from.map { it.type }))
                .sumOf { (x, y) -> checkOwnershipCost(y, x)},
                fct) }
            .sortedBy { (c, _) -> c }.toList()

    return if (fit.isEmpty()){
        throw Exception("Function not found with arguments: ${from.joinToString(", ")}")
    }
    else if (fit.size == 1){
        fit[0].second
    }
    else{
        if (fit[0].first < fit[1].first){
            fit[0].second
        }
        else{
            throw Exception("Cannot resolve function with arguments: ${from.joinToString(", ")} in $lst")
        }
    }
}

fun operationCombine(op: String, p1: Pair<Expression, DataType>, p2: Pair<Expression, DataType>, context: IContext):Pair<Expression, DataType>{
    val s1 = p1.first
    val s2 = p2.first
    val t1 = p1.second
    val t2 = p2.second
    if (isNumerical(t1) && isNumerical(t2)) {
        return when (op) {
            in listOf("+","-","*","%","/") -> {
                Pair(BinaryExpr(op, s1, s2), biggestType(t1, t2))
            }
            in listOf("<","<=",">",">=", "==") -> {
                Pair(BinaryExpr(op, s1, s2), BoolType())
            }
            ".." -> {
                Pair(RangeLitExpr(s1, s2), RangeType(biggestType(t1, t2)))
            }
            else -> throw NotImplementedError()
        }
    }
    else if (isBoolean(t1) && isBoolean(t2)){
        if (op in listOf("&&", "||", "==")){
            return Pair(BinaryExpr(op, s1, s2), BoolType())
        }
        throw NotImplementedError()
    }
    else if (t1 is StructType){
        val funcName = getOperationFunctionName(op)
        val variable = (s1 as VariableExpr).variable
        return checkExpression(CallExpr(UnresolvedFunctionExpr(variable.childrenFunction[funcName]!!), listOf(s2)), context)
    }
    else if (t1 is StringType){
        return Pair(BinaryExpr(op, s1, s2), StringType())
    }
    else if (t2 is StringType){
        return Pair(BinaryExpr(op, s1, s2), StringType())
    }
    else if (t1 is EnumType){
        val other: EnumValueExpr =
        if (s2 is EnumValueExpr){
            s2
        } else if (s2 is UnresolvedExpr) {
            s2.choice.filter { it is EnumValueExpr }
                .map { it as EnumValueExpr }
                .filter { it.enum == t1.enum }
                .first()
        } else if (s2 is VariableExpr){
            when (op) {
                in listOf("<","<=",">",">=", "==") -> { return Pair(BinaryExpr(op, s1, s2), BoolType()) }
                else -> throw NotImplementedError("$op for Enums")
            }
        } else {
            throw InvalidTypeException(t2)
        }


        return when (op) {
            in listOf("<","<=",">",">=", "==") -> {
                Pair(BinaryExpr(op, s1, other), BoolType())
            }
            else -> throw NotImplementedError()
        }
    }
    else if (t2 is StructType){
        throw NotImplementedError()
    }
    else if (t1 is ClassType){
        throw NotImplementedError()
    }
    else if (t2 is ClassType){
        throw NotImplementedError()
    }
    else {
        throw NotImplementedError()
    }
}