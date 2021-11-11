package analyzer

import analyzer.data.*
import parser.*

fun analyse(stm: Statement, context: Context): Statement{
    when(stm){
        is Block -> {
            return Block(stm.statements.map { s -> analyse(s, context.sub("")) })
        }
        is If -> {
            return If(analyse(stm.Condition, context) as Expression,
                      analyse(stm.IfBlock, context))
        }
        is IfElse -> {
            return IfElse(analyse(stm.Condition, context) as Expression,
                          analyse(stm.IfBlock, context),
                          analyse(stm.ElseBlock, context))
        }
        is Switch -> {
            return Switch(analyse(stm.function, context) as Expression,
                stm.cases.map { s -> analyse(s, context) as Case })
        }
        is Case -> {
            return Case(analyse(stm.expr, context) as Expression,
                        analyse(stm.statement, context))
        }



        is VariableDeclaration -> {
            context.update(stm.identifier, Variable(stm.modifier, context.currentPath.append(stm.identifier), stm.type))
            return stm
        }
        is UnlinkedVariableAssignment -> {
            return LinkedVariableAssignment(context.getVariable(stm.identifier),
                analyse(stm.expr, context) as Expression)
        }


        is IdentifierExpr -> {
            val choice = ArrayList<AbstractIdentifierExpr>()
            if (context.hasVariable(stm.value)){ choice.add(VariableExpr(context.getVariable(stm.value))) }
            if (context.hasFunction(stm.value)){ choice.add(FunctionExpr(context.getFunction(stm.value))) }
            return when (choice.size) {
                0 -> { throw Context.IdentifierNotFound(stm.value) }
                1 -> { choice[0] }
                else -> { UnresolvedExpr(choice) }
            }
        }
        is BinaryExpr -> {
            return BinaryExpr(stm.op,
                analyse(stm.first, context) as Expression,
                analyse(stm.second, context) as Expression)
        }
        is UnaryExpr -> {
            return UnaryExpr(stm.op,
                analyse(stm.first, context) as Expression)
        }
        is CallExpr -> {
            return CallExpr(stm.value,
                stm.args.map { s -> analyse(s, context) as Expression })
        }
        else -> {
            return stm
        }
    }
}