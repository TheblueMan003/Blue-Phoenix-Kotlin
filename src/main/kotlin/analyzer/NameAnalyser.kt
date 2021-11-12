package analyzer

import analyzer.data.*
import parser.*
import parser.data.Identifier

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
            Block(stm.statements.map { s -> analyseTop(s, context) })
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
            val variable = Variable(stm.modifier, context.currentPath.append(stm.identifier), stm.type, stm.parent)
            context.update(stm.identifier, variable)
            Block(listOf(stm, variableInstantiation(variable, context)))
        }
        is StructDeclaration -> {
            context.update(stm.identifier, Struct(stm.modifier, stm.identifier, stm.generic, stm.fields, stm.methods, stm.builder))
            stm
        }
        is FunctionDeclaration -> {
            val sub = context.sub(stm.identifier.toString())
            val modifier = DataStructModifier()
            modifier.visibility = DataStructVisibility.PRIVATE

            val variables = stm.from.map { it -> Variable(modifier, sub.currentPath.append(it.identifier), it.type) }
            variables.zip(stm.from).map { (v,t) -> sub.update(t.identifier, v) }

            val output = Variable(modifier, sub.currentPath.sub("__ret_0__"), stm.to)
            sub.update(Identifier(listOf("__ret_0__")), output)

            context.update(stm.identifier,
                sub.addUnfinished(Function(stm.modifier, stm.identifier, variables, output, stm.body,null), sub))

            Empty()
        }



        is UnlinkedVariableAssignment -> {
            LinkedVariableAssignment(context.getVariable(stm.identifier),
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
            CallExpr(stm.value,
                stm.args.map { s -> analyseTop(s, context) as Expression })
        }
        else -> {
            stm
        }
    })
}

private fun variableInstantiation(variable: Variable, context: Context): Statement{
    val sub = context.sub(variable.name.toString())
    var type = variable.type
    if (type is GeneratedType && context.hasGeneric(type.name)) type = context.getGeneric(type.name)
    if (type is GeneratedGenericType && context.hasGeneric(type.name)) type = context.getGeneric(type.name)

    return if (type is GeneratedType){
        if (context.hasStruct(type.name)) {
            val struct = context.getStruct(type.name)
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
            Block(stmList)
        }else{
            Empty()
        }
    }
    else if (type is GeneratedGenericType) {
        if (context.hasStruct(type.name)) {
            val struct = context.getStruct(type.name)
            val stmList = ArrayList<Statement>()
            if (struct.generic == null) throw Exception("Struct doesn't have generics")
            struct.generic.zip(type.type).map { p -> sub.update((p.first as GeneratedType).name, p.second) }

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
            Block(stmList)
        }else{
            Empty()
        }
    }
    else if (type is ArrayType) {
        Empty()
    }
    else if (type is TupleType) {
        val modifier = DataStructModifier()
        modifier.visibility = DataStructVisibility.PUBLIC
        type.type.mapIndexed{ index, it ->
            analyseTop(VariableDeclaration(modifier, Identifier(listOf("$index")), it, variable ), sub)}
        Empty()
    }
    else if (type is FuncType) {
        Empty()
    }
    else{
        Empty()
    }
}