package analyzer

import analyzer.data.*
import parser.*

fun analyse(stm: Statement, context: Context): Statement{
    fun passUpward(stm: Statement): Statement{
        context.resolve()
        return stm
    }


    return passUpward(when(stm){
        is Block -> {
            Block(stm.statements.map { s -> analyse(s, context.sub("")) })
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
            context.update(stm.identifier, Variable(stm.modifier, context.currentPath.append(stm.identifier), stm.type))
            stm
        }
        is StructDeclaration -> {
            context.update(stm.identifier, Struct(stm.modifier, stm.identifier, stm.generic, stm.fields, stm.methods, stm.builder))
            stm
        }
        is FunctionDeclaration -> {
            throw NotImplementedError()
            stm
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