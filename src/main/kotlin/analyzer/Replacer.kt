package analyzer

import ast.*

fun runReplace(statement: Statement, map: Map<Identifier, Expression>):Statement{
    return replace(statement, map)
}

private fun replace(stm: Statement, map: Map<Identifier, Expression>):Statement{
    return when(stm){
        is Block -> {
           Block(stm.statements.map { replace(it, map) }).withParent(stm)
        }
        is Sequence -> {
            Sequence(stm.statements.map { replace(it, map) })
        }
        is If -> {
            If(replaceExpression(stm.Condition, map), replace(stm.IfBlock, map)).withParent(stm)
        }
        is IfElse -> {
            IfElse(replaceExpression(stm.Condition, map), replace(stm.IfBlock, map), replace(stm.ElseBlock, map)).withParent(stm)
        }
        is Switch -> {
            Switch(replaceExpression(stm.scrutinee, map),
                stm.cases.map { s -> replace(s, map) as Case },
                stm.forgenerate.map { s -> replace(s, map) as Forgenerate }).withParent(stm)
        }
        is Case -> {
            Case(replaceExpression(stm.expr, map),
                replace(stm.statement, map)
            )
        }
        is LinkedVariableAssignment -> {
            if (stm.variable.name in map){
                LinkedVariableAssignment(
                    (map[stm.variable.name] as VariableExpr).variable,
                    replaceExpression(stm.expr, map), stm.op
                )
            } else {
                LinkedVariableAssignment(
                    stm.variable,
                    replaceExpression(stm.expr, map), stm.op
                )
            }
        }
        is RawCommandArg -> {
            RawCommandArg(stm.cmd, stm.args.map { replaceExpression(it, map) })
        }
        is LinkedForgenerate -> {
            LinkedForgenerate(stm.identifier, stm.generator, replace(stm.body, map) as Block)
        }
        is UnlinkedForgenerate -> {
            UnlinkedForgenerate(stm.identifier, stm.generator, replace(stm.body, map) as Block)
        }
        is UnlinkedVariableAssignment -> {
            if (stm.identifier in map){
                LinkedVariableAssignment(
                    (map[stm.identifier] as VariableExpr).variable,
                    replaceExpression(stm.expr, map), stm.op
                )
            } else {
                UnlinkedVariableAssignment(
                    stm.identifier,
                    replaceExpression(stm.expr, map), stm.op
                )
            }
        }
        is CallExpr -> {
            CallExpr(replaceExpression(stm.value, map), stm.args.map { replaceExpression(it, map) })
        }
        is FunctionDeclaration -> {
            FunctionDeclaration(stm.modifier, stm.identifier, stm.from, stm.to, replace(stm.body, map), stm.parent)
        }

        else ->{
            stm
        }
    }
}

private fun replaceExpression(stm: Expression, map: Map<Identifier, Expression>):Expression{
    return when(stm) {
        is BinaryExpr -> {
            BinaryExpr(stm.op, replaceExpression(stm.first, map), replaceExpression(stm.second, map))
        }
        is UnaryExpr -> {
            UnaryExpr(stm.op, replaceExpression(stm.first, map))
        }
        is TupleExpr -> {
            TupleExpr(stm.value.map { replaceExpression(it, map)})
        }
        is IdentifierExpr -> {
            if (stm.value in map){
                map[stm.value]!!
            } else {
                stm
            }
        }
        is VariableExpr -> {
            if (stm.variable.name in map){
                val ret = map[stm.variable.name]!!
                ret
            } else {
                stm
            }
        }
        is CallExpr -> {
            CallExpr(replaceExpression(stm.value, map), stm.args.map { replaceExpression(it, map) }, stm.operator)
        }
        else -> stm
    }
}