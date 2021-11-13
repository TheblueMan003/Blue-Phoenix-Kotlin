package analyzer

import ast.*

fun analyse(stm: Statement, context: Context): Pair<Statement, Context>{
    val ret = analyseTop(stm, context)
    context.runUnfinished{ s, c -> analyseTop(s, c) }
    return Pair(Sequence(context.functionsList.map { FunctionBody(it.body, it) }+ret), context)
}

fun analyseTop(stm: Statement, context: Context): Statement {
    fun passUpward(stm: Statement): Statement {
        context.resolve()
        return stm
    }

    return passUpward(when(stm){
        is Block -> {
            val sub = context.sub("")
            Block(stm.statements.map { s -> analyseTop(s, sub) }).withParent(stm)
        }
        is Sequence -> {
            Sequence(stm.statements.map { s -> analyseTop(s, context) })
        }
        is If -> {
            If(analyseTop(stm.Condition, context) as Expression,
                      analyseTop(stm.IfBlock, context)).withParent(stm)
        }
        is IfElse -> {
            IfElse(analyseTop(stm.Condition, context) as Expression,
                          analyseTop(stm.IfBlock, context),
                          analyseTop(stm.ElseBlock, context)).withParent(stm)
        }
        is Switch -> {
            Switch(analyseTop(stm.function, context) as Expression,
                stm.cases.map { s -> analyseTop(s, context) as Case }).withParent(stm)
        }
        is Case -> {
            Case(analyseTop(stm.expr, context) as Expression,
                        analyseTop(stm.statement, context))
        }



        is VariableDeclaration -> {
            val type = analyseType(stm.type, context)
            variableInstantiation(stm.modifier, stm.identifier, type, context, stm.parent).first
        }
        is StructDeclaration -> {
            context.update(stm.identifier, Struct(stm.modifier, stm.identifier, stm.generic, stm.fields, stm.methods, stm.builder))
            Empty()
        }
        is FunctionDeclaration -> {
            val uuid = context.getUniqueFunctionIdenfier(stm.identifier)
            val identifier = context.currentPath.sub(uuid)
            val sub = context.sub(uuid.toString())
            val modifier = DataStructModifier()
            modifier.visibility = DataStructVisibility.PRIVATE

            val inputs = stm.from.map { variableInstantiation(modifier,
                it.identifier, analyseType(it.type, context), sub).second }

            val output = variableInstantiation(modifier, Identifier(listOf("__ret__")), stm.to, sub).second
            val body = if (stm.body is Block){stm.body.toSequence()}else{stm}
            val function = Function(stm.modifier, identifier, stm.from, inputs, output, body,null)
            context.update(stm.identifier, sub.addUnfinished(function, sub))

            Empty()
        }



        is UnlinkedVariableAssignment -> {
            val variable = context.getVariable(stm.identifier)

            LinkedVariableAssignment(variable,
                analyseTop(stm.expr, context) as Expression, stm.op)
        }
        is UnlinkedReturnStatement -> {
            if (context.currentFunction == null) throw Exception("Return must me inside of a function")
            ReturnStatement(analyseTop(stm.expr, context) as Expression, context.currentFunction!!)
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
        is TupleExpr -> {
            TupleExpr(stm.value.map { analyseTop(it, context) as Expression })
        }
        is CallExpr -> {
            CallExpr(analyseTop(stm.value, context) as Expression,
                stm.args.map { s -> analyseTop(s, context) as Expression })
        }
        else -> {
            stm
        }
    })
}

private fun variableInstantiation(modifier: DataStructModifier, identifier: Identifier, type: DataType,
                                  context: Context, parent: Variable? = null): Pair<Statement, Variable>{
    val variable = Variable(modifier, context.currentPath.sub(identifier), type, parent)
    context.update(identifier, variable)
    val sub = context.sub(identifier.toString())
    sub.parentVariable = variable
    var type = variable.type

    val ret = when (type) {
        is StructType -> {
            val struct = type.name
            val stmList = ArrayList<Statement>()

            if (struct.generic != null) {
                struct.generic.zip(type.type!!)
                    .map { (o, n) ->
                        when (o) {
                            is UnresolvedGeneratedType -> { sub.update(o.name, n) }
                            is UnresolvedGeneratedGenericType -> { sub.update(o.name, n) }
                            else -> { throw Exception("Type Parameter should be an identifier") }
                        }
                    }
            }

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
    return Pair(ret, variable)
}

fun analyseType(stm: DataType, context: Context): DataType {
    return when (stm) {
        is UnresolvedGeneratedType -> {
            if (context.hasStruct(stm.name)){
                StructType(context.getStruct(stm.name), null)
            } else if (context.hasClass(stm.name)) {
                ClassType(context.getClass(stm.name), null)
            } else if (context.hasGeneric(stm.name)){
                analyseType(context.getGeneric(stm.name), context)
            } else throw Exception("${stm.name} Type Not Found")
        }
        is UnresolvedGeneratedGenericType -> {
            if (context.hasStruct(stm.name)){
                StructType(context.getStruct(stm.name), stm.type.map { analyseType(it,context) })
            } else if (context.hasClass(stm.name)){
                ClassType(context.getClass(stm.name), stm.type.map { analyseType(it,context) })
            } else if (context.hasGeneric(stm.name)){
                analyseType(context.getGeneric(stm.name), context)
            } else throw Exception("${stm.name} Type Not Found")
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