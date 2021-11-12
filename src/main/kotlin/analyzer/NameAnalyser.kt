package analyzer

import analyzer.data.*
import parser.*
import ast.Identifier

fun analyse(stm: Statement, context: Context): Pair<Statement, Context>{
    val ret = analyseTop(stm, context)
    context.runUnfinished{ s, c -> analyseTop(s, c)}
    return Pair(ret, context)
}

fun analyseTop(stm: Statement, context: Context): Statement{
    fun passUpward(stm: Statement): Statement{
        context.resolve()
        return stm
    }

    return passUpward(when(stm){
        is Block -> {
            val sub = context.sub("")
            Block(stm.statements.map { s -> analyseTop(s, sub) })
        }
        is Sequence -> {
            Sequence(stm.statements.map { s -> analyseTop(s, context) })
        }
        is If -> {
            If(analyseTop(stm.Condition, context) as Expression,
                      analyseTop(stm.IfBlock, context))
        }
        is IfElse -> {
            IfElse(analyseTop(stm.Condition, context) as Expression,
                          analyseTop(stm.IfBlock, context),
                          analyseTop(stm.ElseBlock, context))
        }
        is Switch -> {
            Switch(analyseTop(stm.function, context) as Expression,
                stm.cases.map { s -> analyseTop(s, context) as Case })
        }
        is Case -> {
            Case(analyseTop(stm.expr, context) as Expression,
                        analyseTop(stm.statement, context))
        }



        is VariableDeclaration -> {
            val type = analyseType(stm.type, context)
            val variable = Variable(stm.modifier, context.currentPath.append(stm.identifier), type, stm.parent)
            variableInstantiation(stm.identifier, variable, context)
        }
        is StructDeclaration -> {
            context.update(stm.identifier, Struct(stm.modifier, stm.identifier, stm.generic, stm.fields, stm.methods, stm.builder))
            Empty()
        }
        is FunctionDeclaration -> {
            val sub = context.sub(stm.identifier.toString())
            val modifier = DataStructModifier()
            modifier.visibility = DataStructVisibility.PRIVATE

            val variables = stm.from.map { Variable(modifier,
                sub.currentPath.append(it.identifier), analyseType(it.type, context)) }
            variables.zip(stm.from).map { (v,t) -> variableInstantiation(t.identifier, v, sub) }

            val output = Variable(modifier, sub.currentPath.sub("__ret_0__"), stm.to)
            variableInstantiation(Identifier(listOf("__ret_0__")), output, sub)

            context.update(stm.identifier,
                sub.addUnfinished(Function(stm.modifier, stm.identifier, variables, output, stm.body,null), sub))

            Empty()
        }



        is UnlinkedVariableAssignment -> {
            val variable = context.getVariable(stm.identifier)

            LinkedVariableAssignment(variable,
                analyseTop(stm.expr, context) as Expression, stm.op)
        }


        is IdentifierExpr -> {
            val choice = ArrayList<AbstractIdentifierExpr>()
            if (context.hasVariable(stm.value)){ choice.add(VariableExpr(context.getVariable(stm.value))) }
            if (context.hasFunction(stm.value)){ choice.add(UnresolvedFunctionExpr(context.getFunction(stm.value))) }
            when (choice.size) {
                0 -> { throw Context.IdentifierNotFound(stm.value) }
                1 -> { choice[0] }
                else -> { UnresolvedExpr(choice) }
            }
        }
        is BinaryExpr -> {
            BinaryExpr(stm.op,
                analyseTop(stm.first, context) as Expression,
                analyseTop(stm.second, context) as Expression)
        }
        is UnaryExpr -> {
            UnaryExpr(stm.op,
                analyseTop(stm.first, context) as Expression)
        }
        is CallExpr -> {
            CallExpr(analyseTop(stm.value, context) as Expression,
                stm.args.map { s -> analyseTop(s, context) as Expression })
        }
        is TupleExpr -> {
            TupleExpr(stm.value.map { analyseTop(it, context) as Expression })
        }
        else -> {
            stm
        }
    })
}

private fun variableInstantiation(identifier: Identifier, variable: Variable, context: Context): Statement{
    context.update(identifier, variable)
    val sub = context.sub(identifier.toString())
    sub.parentVariable = variable
    var type = variable.type
    if (type is UnresolvedGeneratedType && context.hasGeneric(type.name)) type = context.getGeneric(type.name)
    if (type is UnresolvedGeneratedGenericType && context.hasGeneric(type.name)) type = context.getGeneric(type.name)

    val ret = when (type) {
        is StructType -> {
            val struct = type.name
            val stmList = ArrayList<Statement>()

            // Add Fields
            stmList.addAll(
                struct.fields.map{ it ->
                    analyseTop(VariableDeclaration(it.modifier, it.identifier, it.type, variable ), sub)}
            )

            // Add Methods
            stmList.addAll(
                struct.methods.map{ it ->
                    analyseTop(FunctionDeclaration(it.modifier, it.identifier, it.from, it.to, it.body, variable ), sub)}
            )

            stmList.add(analyseTop(struct.builder, sub))

            Sequence(stmList)
        }
        is ArrayType -> {
            Empty()
        }
        is TupleType -> {
            val modifier = DataStructModifier()
            modifier.visibility = DataStructVisibility.PUBLIC
            Sequence(type.type.mapIndexed{ index, it ->
                analyseTop(VariableDeclaration(modifier, Identifier(listOf("_$index")), it, variable ), sub)})
        }
        is FuncType -> {
            Empty()
        }
        else -> {
            Empty()
        }
    }
    context.resolve()
    return ret
}

fun analyseType(stm: DataType, context: Context): DataType {
    return when (stm) {
        is UnresolvedGeneratedType -> {
            if (context.hasStruct(stm.name)){
                StructType(context.getStruct(stm.name), null)
            } else if (context.hasClass(stm.name)){
                ClassType(context.getClass(stm.name), null)
            } else throw NotImplementedError()
        }
        is UnresolvedGeneratedGenericType -> {
            if (context.hasStruct(stm.name)){
                StructType(context.getStruct(stm.name), stm.type.map { analyseType(it,context) })
            } else if (context.hasClass(stm.name)){
                ClassType(context.getClass(stm.name), stm.type.map { analyseType(it,context) })
            } else throw NotImplementedError()
        }
        is ArrayType -> {
            ArrayType(analyseType(stm.subtype, context), stm.length)
        }
        is FuncType -> {
            FuncType(stm.from.map { analyseType(it,context) }, analyseType(stm.to, context))
        }
        is TupleType -> {
            TupleType(stm.type.map { analyseType(it,context) })
        }
        else -> return stm
    }
}