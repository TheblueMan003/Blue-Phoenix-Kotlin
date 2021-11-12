package analyzer

import analyzer.data.*
import ast.Identifier
import parser.*
import parser.Function


data class InvalidTypeException(val type: DataType): Exception()

fun check(stm: Statement, context: Context): Statement{
    return when(stm){
        is If -> {
            val cond = checkExpression(stm.Condition, context)
            expectBoolean(cond.second)
            If(cond.first, check(stm.IfBlock, context))
        }
        is IfElse-> {
            val cond = checkExpression(stm.Condition, context)
            expectBoolean(cond.second)
            IfElse(cond.first, check(stm.IfBlock, context), check(stm.ElseBlock, context))
        }
        is Block->{
            Block(stm.statements.map { check(it, context) })
        }
        is Sequence->{
            Sequence(stm.statements.map { check(it, context) })
        }
        is Switch->{
            val expr = checkExpression(stm, context)
            Switch(expr.first, stm.cases.map {
                val scrut = checkExpression(it.expr, context)
                checkOwnership(scrut.second, expr.second)
                Case(scrut.first, check(it.statement, context))
            })
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
                    is UnresolvedExpr -> {
                        throw Exception("Cannot be assign to function")
                    }
                    else -> {
                        throw Exception("Cannot be assign to function")
                    }
                }
            }
            else{
                val v = checkExpression(stm.expr, context)
                checkOwnership(v.second, stm.variable.type)
                LinkedVariableAssignment(stm.variable, v.first, stm.op)
            }
        }
        else -> stm
    }
}

fun checkExpression(stm: Expression, context: Context): Pair<Expression, DataType>{
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
        is TupleExpr -> {
            val lst = stm.value.map { checkExpression(it, context) }
            Pair(TupleExpr(lst.map { it.first }), TupleType(lst.map { it.second }))
        }
        is CallExpr -> {
            return when(stm.value){
                is UnresolvedFunctionExpr -> {
                    val args = stm.args.map{checkExpression(it, context)}
                    val fct = findFunction(args.map { it.second }, stm.value.function)
                    Pair(CallExpr(FunctionExpr(fct), args.map { it.first }), fct.output.type)
                }
                is VariableExpr -> {
                    val to = stm.value.variable.type as FuncType
                    val args = stm.args.map{checkExpression(it, context)}
                    Pair(CallExpr(stm.value, args.map { it.first }), to.to)
                }
                else -> throw Exception("Invalid Function Call Expression: $stm")
            }
        }
        is BinaryExpr ->{
            val first = checkExpression(stm.first, context)
            val second = checkExpression(stm.second, context)
            operationCombine(stm.op, first, second, context)
        }
        else -> throw NotImplementedError()
    }
}

fun biggestType(t1: DataType, t2: DataType):DataType{
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
        is StringType -> t1 is StringType
        is BoolType -> t1 is BoolType
        is TupleType -> t1 is TupleType && t1.type.zip(t2.type).map { (x,y) -> checkOwnership(x,y) }.all { it }
        is FuncType -> t1 is FuncType && t1.from.zip(t2.from).map { (x,y) -> checkOwnership(x,y) }.all { it }
                            && checkOwnership(t1.to, t2.to)
        is ArrayType -> t1 is ArrayType && checkOwnership(t1.subtype, t2.subtype) && t1.length == t2.length
        else -> throw NotImplementedError()
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

fun getOperationFunctionName(op: String): Identifier{
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
fun findFunction(from: List<DataType>, lst: List<Function>, isOperator: Boolean = false): Function {
    val fit =
        lst.filter { it.modifier.operator || !isOperator }
           .filter { fct -> fct.input
                .map {vr -> vr.type}
                .zip(from)
                .map { (x, y) -> checkOwnership(y, x)}
                .all { it } }
            .map { fct -> Pair(fct.input
                .map {vr -> vr.type}
                .zip(from)
                .sumOf { (x, y) -> checkOwnershipCost(y, x)},
                fct) }
            .sortedBy { (c, _) -> c }

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
            throw Exception("Cannot resolve function with arguments: ${from.joinToString(", ")}")
        }
    }
}

fun operationCombine(op: String, p1: Pair<Expression, DataType>, p2: Pair<Expression, DataType>, context: Context):Pair<Expression, DataType>{
    val s1 = p1.first
    val s2 = p2.first
    val t1 = p1.second
    val t2 = p2.second
    if (isNumerical(t1) && isNumerical(t2)) {
        return when (op) {
            in listOf("+","-","*","%","/") -> {
                Pair(BinaryExpr(op, s1, s2), biggestType(t1, t2))
            }
            in listOf("<","<=",">",">=") -> {
                Pair(BinaryExpr(op, s1, s2), BoolType())
            }
            else -> throw NotImplementedError()
        }
    }
    else if (isBoolean(t1) && isBoolean(t2)){
        if (op in listOf("&&", "||")){
            return Pair(BinaryExpr(op, s1, s2), BoolType())
        }
        throw NotImplementedError()
    }
    else if (t1 is StructType){
        throw NotImplementedError()
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