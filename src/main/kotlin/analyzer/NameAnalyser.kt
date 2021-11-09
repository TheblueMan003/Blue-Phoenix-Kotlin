package analyzer

import parser.*

fun analyse(stm: Statement, context: Context){
    when(stm){
        is Block -> {
            for (sub in stm.statements) {
                analyse(sub, context.sub(""))
            }
        }
        is If -> {
            analyse(stm.Condition, context)
            analyse(stm.IfBlock, context)
        }
        is IfElse -> {
            analyse(stm.Condition, context)
            analyse(stm.IfBlock, context)
            analyse(stm.ElseBlock, context)
        }
        is VariableDeclaration -> {
            context.update(stm.identifier, Variable(stm.modifier, context.currentPath.append(stm.identifier)))
        }
        is VariableAssignment -> {
            analyse(stm.expr, context)
            stm.identifier= context.getVariable(stm.identifier).name
        }
        is VarExpr ->{
            stm.value = context.getVariable(stm.value).name
        }
        is BinaryExpr ->{
            analyse(stm.first, context)
            analyse(stm.second, context)
        }
        is UnaryExpr -> {
            analyse(stm.first, context)
        }
    }
}