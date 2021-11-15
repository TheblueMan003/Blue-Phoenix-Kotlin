package analyzer

import ast.*
import kotlin.math.ceil

fun buildSwitchTree(scrut: Expression, cases: List<Case>, context: Context, reassign: Boolean = true): Statement{
    val treeSize = context.compiler.treeSize
    val intCases = cases.filter { it.expr is IntLitExpr }.sortedBy { (it.expr as IntLitExpr).value }
    val floatCases = cases.filter { it.expr is FloatLitExpr }.sortedBy { (it.expr as FloatLitExpr).value }
    val otherCases = cases.filter { it.expr !is IntLitExpr && it.expr !is FloatLitExpr }
    val stm = if(reassign) { putInTMPVariable(scrut, AssignmentType.SET, context) } else { Pair((scrut as VariableExpr).variable, Empty()) }

    return Sequence(listOf(stm.second,
        buildOrderedSwitchTree(VariableExpr(stm.first), intCases + floatCases, treeSize),
        buildLastLevelSwitchTree(scrut, otherCases)))
}
fun buildOrderedSwitchTree(scrut: Expression, values: List<Case>, treeSize: Int, level: Int = 0):Statement {
    return if (values.size <= treeSize){
        buildLastLevelSwitchTree(scrut, values)
    } else {
        val parts =  values.chunked(ceil(values.size / treeSize.toFloat()).toInt())
        val inter = parts.map {
            If(BinaryExpr("in", scrut, RangeLitExpr(it.first().expr, it.last().expr)),
            Block(listOf(buildOrderedSwitchTree(scrut, it, treeSize, level+1)))) }
        if (level == 0){
            Sequence(inter)
        } else {
            Block(inter)
        }
    }
}
fun buildLastLevelSwitchTree(scrut: Expression, values: List<Case>):Statement {
    return Sequence(values.map { buildSwitchBaseTree(scrut, it.expr, it.statement) })
}
fun buildSwitchBaseTree(scrut: Expression, value: Expression, statement: Statement):Statement{
    return If(BinaryExpr("==",scrut, value), statement)
}