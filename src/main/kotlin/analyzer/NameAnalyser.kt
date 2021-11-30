package analyzer

import ast.*
import context.DualContext
import context.IContext
import data_struct.*
import data_struct.Enum
import data_struct.Function
import utils.withDefault

fun runAnalyse(stm: Statement, context: IContext): Statement{
    val ret = analyse(stm, context)
    context.runUnfinished{ s, c -> analyse(s, c) }
    val ret2 = Sequence(context.getNewFunctionsList().filter { !it.modifier.lazy }.map { FunctionBody(it.body, it) }+ret)
    context.setDone()
    return ret2
}

fun analyse(stm: Statement, context: IContext): Statement {
    fun passUpward(stm: Statement): Statement {
        context.resolve()
        return stm
    }
    try{
        return passUpward(
            when(stm) {
            is Block -> {
                val sub = context.sub("")
                Block(stm.statements.map { s -> analyse(s, sub) }).withParent(stm)
            }
            is Sequence -> {
                Sequence(stm.statements.map { s -> analyse(s, context) })
            }
            is If -> {
                If(
                    analyse(stm.Condition, context) as Expression,
                    analyse(stm.IfBlock, context)
                ).withParent(stm)
            }
            is IfElse -> {
                IfElse(
                    analyse(stm.Condition, context) as Expression,
                    analyse(stm.IfBlock, context),
                    analyse(stm.ElseBlock, context)
                ).withParent(stm)
            }
            is Switch -> {
                Switch(analyse(stm.scrutinee, context) as Expression,
                    stm.cases.map { s -> analyse(s, context) as Case },
                    stm.forgenerate.map { s -> analyse(s, context) as Forgenerate }
                ).withParent(stm)
            }
            is RawCommandArg -> {
                RawCommandArg(stm.cmd, stm.args.map { analyse(it, context) as Expression })
            }
            is Case -> {
                Case(
                    analyse(stm.expr, context) as Expression,
                    analyse(stm.statement, context)
                )
            }
            is Import -> {
                if (stm.identifier.toString() != context.getOwnerPackage()) {
                    val value = context.getCompiler().import(stm.identifier.toString())
                    context.update(value, DataStructVisibility.PUBLIC, true, stm.alias)
                    if (value.isLib()) {
                        Empty()
                    } else {
                        Import(stm.identifier, stm.alias)
                    }
                } else {
                    Empty()
                }
            }
            is FromImport -> {
                val other = context.getCompiler().import(stm.identifier.toString())

                if (stm.resource.contains(Identifier(listOf("*")))) {
                    throw NotImplementedError("*")
                } else {
                    Sequence(
                        stm.resource.map {
                            if (other.hasClass(it, DataStructVisibility.PUBLIC)) {
                                context.update(stm.alias ?: it, other.getClass(it, DataStructVisibility.PUBLIC), true)
                                Empty()
                            } else if (other.hasStruct(it, DataStructVisibility.PUBLIC)) {
                                context.update(stm.alias ?: it, other.getStruct(it, DataStructVisibility.PUBLIC), true)
                                Empty()
                            } else if (other.hasTypeDef(it, DataStructVisibility.PUBLIC)) {
                                context.update(stm.alias ?: it, other.getTypeDef(it, DataStructVisibility.PUBLIC), true)
                                Empty()
                            } else if (other.hasFunction(it, DataStructVisibility.PUBLIC)) {
                                other.getFunction(it, DataStructVisibility.PUBLIC).map { fct ->
                                    context.update(stm.alias ?: it, fct, true)
                                }
                                Empty()
                            } else if (other.hasVariable(it, DataStructVisibility.PUBLIC)) {
                                context.update(
                                    stm.alias ?: it,
                                    other.getVariable(it, DataStructVisibility.PUBLIC),
                                    true
                                )
                                Empty()
                            } else {
                                FromImport(listOf(it), stm.identifier, stm.alias)
                            }
                        }.filterNot { it is Empty })
                }
            }



            is VariableDeclaration -> {
                val type = analyseType(stm.type, context)
                if (type !is UnresolvedGeneratedType && type !is UnresolvedGeneratedGenericType) {
                    variableInstantiation(stm.modifier, stm.identifier, type, context, stm.parent, stm.tmp).first
                } else if (!context.areNameCrashAllowed()) {
                    stm
                } else {
                    throw Exception("$type Not Resolved Found")
                }
            }
            is StructDeclaration -> {
                context.update(
                    stm.identifier,
                    Struct(stm.modifier, stm.identifier, stm.generic, stm.fields, stm.methods, stm.builder, context)
                )
                Empty()
            }
            is TypeDefDeclaration -> {
                context.update(stm.identifier, TypeDef(stm.modifier, stm.identifier, stm.type, stm.parent))
                Empty()
            }
            is EnumDeclaration -> {
                val fields = stm.fields.map{ Enum.Field(it.identifier, it.type, it.defaultValue )}
                val default = fields.map { it.defaultValue }
                val entries = stm.entries.map{ Enum.Case(it.name, withDefault(it.data, default)) }
                context.update(stm.identifier, Enum(stm.modifier, stm.identifier, fields, entries))
                Empty()
            }
            is FunctionDeclaration -> {
                val uuid = context.getUniqueFunctionIdentifier(stm.identifier)
                val identifier = context.getCurrentPath().sub(uuid)
                val sub = context.sub(uuid.toString())
                val modifier = DataStructModifier.newPrivate()

                val inputs = stm.from.map {
                    val type = analyseType(it.type, context)
                    variableInstantiation(
                        it.modifier,
                        it.identifier, analyseType(it.type, context), sub, null,
                        if (context.getParentVariable() != null) {
                            context.getParentVariable()!!.type == type
                        } else {
                            false
                        }
                    ).second
                }

                val output = variableInstantiation(
                    modifier, Identifier("__ret__"),
                    analyseType(stm.to, context), sub, null, context.getParentVariable()?.type == analyseType(stm.to, context)
                ).second

                val body = if (stm.body is Block) {
                    stm.body.toSequence()
                } else {
                    stm
                }
                val from = stm.from.map {
                    FunctionArgument(
                        it.modifier,
                        it.identifier,
                        analyseType(it.type, context),
                        it.defaultValue
                    )
                }
                val function = Function(stm.modifier, identifier, from, inputs, output, body, context, context.getParentVariable())
                context.update(stm.identifier, sub.addUnfinished(function, sub))

                Empty()
            }
            is ShortLambdaDeclaration -> {
                val modifier = DataStructModifier.newPrivate()
                val name = context.freshLambdaName()
                analyse(FunctionDeclaration(modifier, name, emptyList(), VoidType(), stm.body), context)
                val fct = context.getFunction(name)[0]
                fct.use()
                FunctionExpr(fct)
            }

            is GetExpr -> {
                stm.value as IdentifierExpr
                analyse(CallExpr(IdentifierExpr(stm.value.value.sub("get")), stm.args, true), context)
            }
            is SetExpr -> {
                stm.value as IdentifierExpr
                analyse(CallExpr(IdentifierExpr(stm.value.value.sub("set")), stm.args + stm.setValue, true), context)
            }


            is UnlinkedVariableAssignment -> {
                if (context.hasVariable(stm.identifier)) {
                    val variable = context.getVariable(stm.identifier)
                    val expr = analyse(stm.expr, context) as Expression

                    if (variable.modifier.lazy){
                        variable.lazyValue = expr
                        Empty()
                    } else {
                        LinkedVariableAssignment(variable, expr, stm.op)
                    }
                } else if (!context.areNameCrashAllowed()) {
                    stm
                } else {
                    throw Exception("${stm.identifier} identifier Not Found")
                }
            }
            is UnlinkedReturnStatement -> {
                if (context.getCurrentFunction() == null) throw Exception("Return must me inside of a function")
                ReturnStatement(analyse(stm.expr, context) as Expression, context.getCurrentFunction()!!)
            }
            is UnlinkedSelectorVariableExpr ->{
                if (context.hasVariable(stm.variable)) {
                    val variable = context.getVariable(stm.variable)

                    if (variable.modifier.lazy){
                        throw Exception("Cannot have Lazy Selector Variable")
                    } else if (!variable.modifier.entity){
                        throw Exception("Field is not marked as entity.")
                    } else {
                        LinkedSelectorVariableExpr(stm.selector,  variable)
                    }
                } else if (!context.areNameCrashAllowed()) {
                    stm
                } else {
                    throw Exception("${stm.variable} identifier Not Found")
                }
            }
            is ReturnStatement -> {
                ReturnStatement(analyse(stm.expr, context) as Expression, stm.function)
            }
            is FunctionBody -> {
                val c = context.sub(stm.function.name.getLast().toString())
                val body = analyse(stm.body, DualContext(stm.function.context, c))
                stm.function.body = body
                FunctionBody(body, stm.function)
            }
            is UnlinkedForgenerate -> {
                when(val gen = analyse(stm.generator, context)) {
                    is IGenerator -> { LinkedForgenerate(stm.identifier, gen, stm.body) }
                    else -> throw NotImplementedError("$gen")
                }
            }

            is IdentifierExpr -> {
                val choice = ArrayList<AbstractIdentifierExpr>()
                if (context.hasVariable(stm.value)) {
                    val v = context.getVariable(stm.value)
                    if (v.modifier.lazy){
                        choice.add(LazyVariableExpr(v.lazyValue!!))
                    } else {
                        choice.add(VariableExpr(v))
                    }
                }
                if (context.hasFunction(stm.value)) {
                    choice.add(UnresolvedFunctionExpr(context.getFunction(stm.value)))
                }
                if (context.hasEnumValue(stm.value)){
                    choice.addAll(context.getEnumValue(stm.value).map { EnumValueExpr(it.first, it.second, it.third) })
                }
                if (context.hasEnum(stm.value)){
                    choice.add(EnumExpr(context.getEnum(stm.value)))
                }
                if (context.hasStruct(stm.value)) {
                    choice.add(UnresolvedStructConstructorExpr(context.getStruct(stm.value)))
                }

                when (choice.size) {
                    0 -> {
                        if (!context.areNameCrashAllowed()) {
                            stm
                        } else {
                            throw Exception("${stm.value} Not Found")
                        }
                    }
                    1 -> {
                        val expr = choice[0]
                        if (expr is LazyVariableExpr) {
                            expr.expr
                        } else{
                            expr
                        }
                    }
                    else -> {
                        UnresolvedExpr(choice)
                    }
                }
            }
            is BinaryExpr -> {
                BinaryExpr(
                    stm.op,
                    analyse(stm.first, context) as Expression,
                    analyse(stm.second, context) as Expression
                )
            }
            is UnaryExpr -> {
                UnaryExpr(
                    stm.op,
                    analyse(stm.first, context) as Expression
                )
            }
            is TupleExpr -> {
                TupleExpr(stm.value.map { analyse(it, context) as Expression })
            }
            is CallExpr -> {
                if (stm.value is IdentifierExpr &&
                    context.getCompiler().builtInFunction.containsKey(stm.value.value.toString())){
                    BuildInFunctionCall(stm.value.value, stm.args.map { s -> analyse(s, context) as Expression })
                } else {
                    CallExpr(
                        analyse(stm.value, context) as Expression,
                        stm.args.map { s -> analyse(s, context) as Expression },
                        stm.operator
                    )
                }
            }
            is BuildInFunctionCall -> {
                BuildInFunctionCall(stm.function, stm.expr.map { s -> analyse(s, context) as Expression })
            }
            is TypeExpr -> {
                try {
                    TypeExpr(analyseType(stm.type, context))
                } catch (_: Exception) {
                    stm
                }
            }
            else -> {
                stm
            }
        }
    )}catch(e: Exception){
        throw Exception("Failled to parse: $stm \n$e")
    }
}

private fun variableInstantiation(modifier: DataStructModifier, identifier: Identifier, foundType: DataType,
                                  context: IContext, parent: Variable? = null, noFunc: Boolean = false): Pair<Statement, Variable>{
    val variable = Variable(modifier, context.getCurrentPath().sub(identifier), foundType, parent)
    context.update(identifier, variable)
    val sub = context.sub(identifier.toString())
    sub.setParentVariable(variable)
    var type = variable.type

    val ret = when (type) {
        is StructType -> {
            val struct = type.name

            val dualContext = DualContext(struct.context, sub)

            val stmList = ArrayList<Statement>()

            val thiz = Identifier("this")


            if (struct.generic != null) {
                struct.generic.zip(type.type!!)
                    .map { (o, n) ->
                        when (o) {
                            is UnresolvedGeneratedType -> { dualContext.update(o.name, n) }
                            is UnresolvedGeneratedGenericType -> { dualContext.update(o.name, n) }
                            else -> { throw Exception("Type Parameter should be an identifier") }
                        }
                    }
            }

            // Add Fields
            stmList.addAll(
                struct.fields.map{ it ->
                    val mod = it.modifier.clone()
                    mod.entity = mod.entity || variable.modifier.entity
                    val ret = analyse(VariableDeclaration(mod, it.identifier, it.type, variable ), dualContext)
                    val vr = dualContext.getVariable(it.identifier)
                    dualContext.update(thiz.append(it.identifier), vr)
                    ret
                }
            )
            if (!noFunc) {
                // Add Methods
                stmList.addAll(
                    struct.methods.map { it ->
                        val ret = analyse(FunctionDeclaration(it.modifier, it.identifier, it.from, it.to, it.body, variable), dualContext)
                        val vr = dualContext.getFunction(it.identifier)
                        vr.map{ fct -> dualContext.update(thiz.append(it.identifier), fct)}
                        ret
                    }
                )
            }
            stmList.add(analyse(struct.builder, dualContext))

            Sequence(stmList)
        }
        is EnumType -> {
            val enm = type.enum
            val stmList = ArrayList<Statement>()
            if (!noFunc) {
                // Add Methods

                stmList.addAll(
                    enm.fields.mapIndexed { id, it ->
                        analyse(FunctionDeclaration(DataStructModifier.newPublic(), it.name, emptyList(), it.type,
                            Block(listOf(
                            Switch(VariableExpr(variable),
                            enm.values.map { Case(IdentifierExpr(it.name), UnlinkedReturnStatement(it.data[id])) },
                            emptyList()
                            ))), variable), sub)
                    }
                )
            }

            Sequence(stmList)
        }
        is ArrayType -> {
            val stmList = instantiateArray(type, variable, sub)
            Sequence(stmList)
        }
        is TupleType -> {
            val subModifier = DataStructModifier()
            modifier.visibility = DataStructVisibility.PUBLIC
            modifier.entity = variable.modifier.entity
            Sequence(type.type.mapIndexed{ index, it ->
                analyse(VariableDeclaration(subModifier, Identifier("_$index"), it, variable ), sub)})
        }
        is FuncType -> {
            Empty()
        }
        is RangeType -> {
            val stmList = ArrayList<Statement>()

            // Add Fields
            stmList.add(
                analyse(VariableDeclaration(DataStructModifier.newPublic(), Identifier("min"), type.type, variable ), sub)
            )
            stmList.add(
                analyse(VariableDeclaration(DataStructModifier.newPublic(), Identifier("max"), type.type, variable ), sub)
            )

            Sequence(stmList)
        }
        else -> {
            Empty()
        }
    }
    context.resolve()
    return Pair(ret, variable)
}

private fun instantiateArray(type: ArrayType, variable: Variable, sub: IContext): ArrayList<Statement> {
    val stmList = ArrayList<Statement>()
    val casesGet = ArrayList<Case>()
    val casesSet = ArrayList<Case>()
    val varModifier = DataStructModifier.newPrivate()
    if (variable.modifier.entity) varModifier.entity = true

    stmList.addAll((0..type.length.reduce { x, y -> x * y }).map {
        val id = Identifier("_$it")
        val ret = analyse(VariableDeclaration(varModifier, id, type.subtype, variable), sub)
        val vr = sub.getVariable(id)

        casesGet.add(Case(IntLitExpr(it), UnlinkedReturnStatement(VariableExpr(vr))))
        casesSet.add(
            Case(
                IntLitExpr(it),
                UnlinkedVariableAssignment(id, IdentifierExpr(Identifier("value")), AssignmentType.SET)
            )
        )

        ret
    })

    // Set
    val scrut = if (type.length.size > 1) {
        type.length.mapIndexed { id, it ->
                Pair(IdentifierExpr(Identifier("index_$id")) as Expression, it)
            }.reduce { (id1, it1), (id2, it2) ->
                Pair(BinaryExpr("+", BinaryExpr("*", id1, IntLitExpr(it2)), id2), it1)
            }.first
    } else { IdentifierExpr(Identifier("index_0")) }

    stmList.add(
        analyse(
            FunctionDeclaration(
                DataStructModifier.newOperator(), Identifier("set"),
                (0 until type.length.size).map {
                    FunctionArgument(
                        DataStructModifier.newPrivate(),
                        Identifier("index_$it"),
                        IntType(),
                        null
                    )
                } +
                        FunctionArgument(
                            DataStructModifier.newPrivate(),
                            Identifier("value"),
                            type.subtype,
                            null
                        ),
                type.subtype,
                Block(listOf(Switch(scrut, casesSet, emptyList()))), variable
            ), sub
        )
    )
    // Get
    stmList.add(
        analyse(
            FunctionDeclaration(
                DataStructModifier.newOperator(), Identifier("get"),
                (0 until type.length.size).map {
                    FunctionArgument(
                        DataStructModifier.newPrivate(),
                        Identifier("index_$it"),
                        IntType(),
                        null
                    )
                },
                type.subtype,
                Block(listOf(Switch(scrut, casesGet, emptyList()))), variable
            ), sub
        )
    )

    return stmList
}

fun analyseType(stm: DataType, context: IContext): DataType {
    return when (stm) {
        is VarType -> {
           checkExpression(analyse(stm.expr!!, context) as Expression, context).second
        }
        is UnresolvedGeneratedType -> {
            if (context.hasStruct(stm.name)){
                StructType(context.getStruct(stm.name), null)
            } else if (context.hasClass(stm.name)) {
                ClassType(context.getClass(stm.name), null)
            } else if (context.hasEnum(stm.name)) {
                EnumType(context.getEnum(stm.name))
            } else if (context.hasGeneric(stm.name)){
                analyseType(context.getGeneric(stm.name), context)
            } else if (context.hasTypeDef(stm.name)){
                analyseType(context.getTypeDef(stm.name).type, context)
            } else if (!context.areNameCrashAllowed()) {stm} else {throw Exception("${stm.name} Type Not Found")}
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
            } else if (!context.areNameCrashAllowed()) {stm} else {throw Exception("${stm.name} Type Not Found")}
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