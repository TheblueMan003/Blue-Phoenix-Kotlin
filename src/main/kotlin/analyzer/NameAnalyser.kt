package analyzer

import analyzer.data.*
import parser.*
import parser.data.Identifier

fun analyse(stm: Statement, context: Context): Statement{
    fun passUpward(stm: Statement): Statement{
        context.resolve()
        return stm
    }

    return passUpward(when(stm){
        is Block -> {
            val sub = context.sub("")
            Block(stm.statements.map { s -> analyse(s, sub) })
        }
        is If -> {
            If(analyse(stm.Condition, context) as Expression,
                      analyse(stm.IfBlock, context))
        }
        is IfElse -> {
            IfElse(analyse(stm.Condition, context) as Expression,
                          analyse(stm.IfBlock, context),
                          analyse(stm.ElseBlock, context))
        }
        is Switch -> {
            Switch(analyse(stm.function, context) as Expression,
                stm.cases.map { s -> analyse(s, context) as Case })
        }
        is Case -> {
            Case(analyse(stm.expr, context) as Expression,
                        analyse(stm.statement, context))
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

            stm.from.map { it -> sub.update(it.identifier,
                Variable(modifier, sub.currentPath.append(stm.identifier), it.type)) }

            FunctionDeclaration(stm.modifier, stm.identifier, stm.from, stm.to,
                analyse(stm.body, sub), stm.parent)
        }



        is UnlinkedVariableAssignment -> {
            LinkedVariableAssignment(context.getVariable(stm.identifier),
                analyse(stm.expr, context) as Expression)
        }


        is IdentifierExpr -> {
            val choice = ArrayList<AbstractIdentifierExpr>()
            if (context.hasVariable(stm.value)){ choice.add(VariableExpr(context.getVariable(stm.value))) }
            if (context.hasFunction(stm.value)){ choice.add(FunctionExpr(context.getFunction(stm.value))) }
            when (choice.size) {
                0 -> { throw Context.IdentifierNotFound(stm.value) }
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
        is CallExpr -> {
            CallExpr(stm.value,
                stm.args.map { s -> analyse(s, context) as Expression })
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
                analyse(VariableDeclaration(it.modifier, it.identifier, it.type, variable ), sub)}
            )

            // Add Methods
            stmList.addAll(
            struct.methods.map{ it ->
                analyse(FunctionDeclaration(it.modifier, it.identifier, it.from, it.to, it.body, variable ), sub)}
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
                    analyse(VariableDeclaration(it.modifier, it.identifier, it.type, variable ), sub)}
            )

            // Add Methods
            stmList.addAll(
                struct.methods.map{ it ->
                    analyse(FunctionDeclaration(it.modifier, it.identifier, it.from, it.to, it.body, variable ), sub)}
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
            analyse(VariableDeclaration(modifier, Identifier(listOf("$index")), it, variable ), sub)}
        Empty()
    }
    else if (type is FuncType) {
        Empty()
    }
    else{
        Empty()
    }
}