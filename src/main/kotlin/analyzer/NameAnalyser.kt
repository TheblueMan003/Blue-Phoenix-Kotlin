package analyzer

import ast.*
import ast.Function

fun runAnalyse(stm: Statement, context: Context): Statement{
    val ret = analyse(stm, context)
    context.runUnfinished{ s, c -> analyse(s, c) }
    val ret2 = Sequence(context.functionsList.map { FunctionBody(it.body, it) }+ret)
    context.isDone = true
    return ret2
}

fun analyse(stm: Statement, context: Context): Statement {
    fun passUpward(stm: Statement): Statement {
        context.resolve()
        return stm
    }

    return passUpward(when(stm){
        is Block -> {
            val sub = context.sub("")
            Block(stm.statements.map { s -> analyse(s, sub) }).withParent(stm)
        }
        is Sequence -> {
            Sequence(stm.statements.map { s -> analyse(s, context) })
        }
        is If -> {
            If(analyse(stm.Condition, context) as Expression,
                      analyse(stm.IfBlock, context)).withParent(stm)
        }
        is IfElse -> {
            IfElse(analyse(stm.Condition, context) as Expression,
                          analyse(stm.IfBlock, context),
                          analyse(stm.ElseBlock, context)).withParent(stm)
        }
        is Switch -> {
            Switch(analyse(stm.function, context) as Expression,
                stm.cases.map { s -> analyse(s, context) as Case }).withParent(stm)
        }
        is Case -> {
            Case(analyse(stm.expr, context) as Expression,
                        analyse(stm.statement, context))
        }
        is Import -> {
            context.update(context.compiler.import(stm.identifier.toString()),
                DataStructVisibility.PUBLIC, true, stm.alias)
            Import(stm.identifier, stm.alias)
        }
        is FromImport -> {
            val other = context.compiler.import(stm.identifier.toString())

            if (stm.resource.contains(Identifier(listOf("*")))){
                throw NotImplementedError("*")
            }else{
                Sequence(
                stm.resource.map {
                    if (other.hasClass(it, DataStructVisibility.PUBLIC)){
                        context.update(stm.alias ?: it, other.getClass(it, DataStructVisibility.PUBLIC), true)
                        Empty()
                    } else if (other.hasStruct(it, DataStructVisibility.PUBLIC)){
                        context.update(stm.alias ?: it, other.getStruct(it, DataStructVisibility.PUBLIC), true)
                        Empty()
                    } else if (other.hasTypeDef(it, DataStructVisibility.PUBLIC)){
                        context.update(stm.alias ?: it, other.getTypeDef(it, DataStructVisibility.PUBLIC), true)
                        Empty()
                    } else if (other.hasFunction(it, DataStructVisibility.PUBLIC)){
                        other.getFunction(it, DataStructVisibility.PUBLIC).map { fct ->
                            context.update(stm.alias ?: it, fct, true)
                        }
                        Empty()
                    } else if (other.hasVariable(it, DataStructVisibility.PUBLIC)){
                        context.update(stm.alias ?: it, other.getVariable(it, DataStructVisibility.PUBLIC), true)
                        Empty()
                    } else {
                        FromImport(listOf(it),stm.identifier, stm.alias)
                    }
                }.filterNot{it is Empty})
            }
        }



        is VariableDeclaration -> {
            val type = analyseType(stm.type, context)
            if (type !is UnresolvedGeneratedType && type !is UnresolvedGeneratedGenericType) {
                variableInstantiation(stm.modifier, stm.identifier, type, context, stm.parent).first
            } else if (!context.nameResolvedAllowCrash) {
                stm
            } else {throw Exception("$type Not Resolved Found")}
        }
        is StructDeclaration -> {
            context.update(stm.identifier, Struct(stm.modifier, stm.identifier, stm.generic, stm.fields, stm.methods, stm.builder))
            Empty()
        }
        is TypeDefDeclaration -> {
            context.update(stm.identifier, TypeDef(stm.modifier, stm.identifier, stm.type, stm.parent))
            Empty()
        }
        is FunctionDeclaration -> {
            val uuid = context.getUniqueFunctionIdentifier(stm.identifier)
            val identifier = context.currentPath.sub(uuid)
            val sub = context.sub(uuid.toString())
            val modifier = DataStructModifier()
            modifier.visibility = DataStructVisibility.PRIVATE

            val inputs = stm.from.map {
                val type = analyseType(it.type, context)
                variableInstantiation(it.modifier,
                it.identifier, analyseType(it.type, context), sub, null,
                if (context.parentVariable!=null){context.parentVariable!!.type == type}else{false}
                ).second }

            val output = variableInstantiation(modifier, Identifier(listOf("__ret__")),
                analyseType(stm.to, context), sub, null, context.parentVariable?.type == stm.to).second

            val body = if (stm.body is Block){stm.body.toSequence()}else{stm}
            val from = stm.from.map { FunctionArgument(it.modifier, it.identifier, analyseType(it.type, context), it.defaultValue) }
            val function = Function(stm.modifier, identifier, from, inputs, output, body,context.parentVariable)
            context.update(stm.identifier, sub.addUnfinished(function, sub))

            Empty()
        }



        is UnlinkedVariableAssignment -> {
            if (context.hasVariable(stm.identifier)) {
                val variable = context.getVariable(stm.identifier)

                LinkedVariableAssignment(
                    variable,
                    analyse(stm.expr, context) as Expression, stm.op
                )
            }else if (!context.nameResolvedAllowCrash) {
                stm
            } else {
                throw Exception("${stm.identifier} identifier Not Found")
            }
        }
        is UnlinkedReturnStatement -> {
            if (context.currentFunction == null) throw Exception("Return must me inside of a function")
            ReturnStatement(analyse(stm.expr, context) as Expression, context.currentFunction!!)
        }
        is FunctionBody -> {
            val body = analyse(stm.body, context)
            stm.function.body = body
            FunctionBody(body, stm.function)
        }

        is IdentifierExpr -> {
            val choice = ArrayList<AbstractIdentifierExpr>()
            if (context.hasVariable(stm.value)){ choice.add(VariableExpr(context.getVariable(stm.value))) }
            if (context.hasFunction(stm.value)){ choice.add(UnresolvedFunctionExpr(context.getFunction(stm.value))) }
            when (choice.size) {
                0 -> { if (!context.nameResolvedAllowCrash) {stm} else {throw Exception("${stm.value} Not Found")} }
                1 -> { choice[0] }
                else -> { UnresolvedExpr(choice) }
            }
        }
        is BinaryExpr -> {
            BinaryExpr(stm.op,
                analyse(stm.first, context) as Expression,
                analyse(stm.second, context) as Expression)
        }
        is UnaryExpr -> {
            UnaryExpr(stm.op,
                analyse(stm.first, context) as Expression)
        }
        is TupleExpr -> {
            TupleExpr(stm.value.map { analyse(it, context) as Expression })
        }
        is CallExpr -> {
            CallExpr(analyse(stm.value, context) as Expression,
                stm.args.map { s -> analyse(s, context) as Expression })
        }
        else -> {
            stm
        }
    })
}

private fun variableInstantiation(modifier: DataStructModifier, identifier: Identifier, foundType: DataType,
                                  context: Context, parent: Variable? = null, noFunc: Boolean = false): Pair<Statement, Variable>{
    val variable = Variable(modifier, context.currentPath.sub(identifier), foundType, parent)
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
                    analyse(VariableDeclaration(it.modifier, it.identifier, it.type, variable ), sub)}
            )
            if (!noFunc) {
                // Add Methods
                stmList.addAll(
                    struct.methods.map { it ->
                        analyse(FunctionDeclaration(it.modifier, it.identifier, it.from, it.to, it.body, variable), sub)
                    }
                )
            }
            stmList.add(analyse(struct.builder, sub))

            Sequence(stmList)
        }
        is ArrayType -> {
            Empty()
        }
        is TupleType -> {
            val subModifier = DataStructModifier()
            modifier.visibility = DataStructVisibility.PUBLIC
            Sequence(type.type.mapIndexed{ index, it ->
                analyse(VariableDeclaration(subModifier, Identifier(listOf("_$index")), it, variable ), sub)})
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
        is VarType -> {
           checkExpression(stm.expr!!, context).second
        }
        is UnresolvedGeneratedType -> {
            if (context.hasStruct(stm.name)){
                StructType(context.getStruct(stm.name), null)
            } else if (context.hasClass(stm.name)) {
                ClassType(context.getClass(stm.name), null)
            } else if (context.hasGeneric(stm.name)){
                analyseType(context.getGeneric(stm.name), context)
            } else if (context.hasTypeDef(stm.name)){
                analyseType(context.getTypeDef(stm.name).type, context)
            } else if (!context.nameResolvedAllowCrash) {stm} else {throw Exception("${stm.name} Type Not Found")}
        }
        is UnresolvedGeneratedGenericType -> {
            if (context.hasStruct(stm.name)){
                StructType(context.getStruct(stm.name), stm.type.map { analyseType(it,context) })
            } else if (context.hasClass(stm.name)){
                ClassType(context.getClass(stm.name), stm.type.map { analyseType(it,context) })
            } else if (context.hasGeneric(stm.name)){
                analyseType(context.getGeneric(stm.name), context)
            } else if (context.hasTypeDef(stm.name)){
                analyseType(context.getTypeDef(stm.name).type, context)
            } else if (!context.nameResolvedAllowCrash) {stm} else {throw Exception("${stm.name} Type Not Found")}
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