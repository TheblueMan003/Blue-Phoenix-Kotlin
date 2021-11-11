package analyzer.data

import parser.Expression
import parser.Function
import parser.Variable


abstract class AbstractIdentifierExpr: Expression()
data class LinkedVariableAssignment(var variable: Variable, val expr: Expression): Expression()
data class VariableExpr(var variable: Variable) : AbstractIdentifierExpr()
data class FunctionExpr(val function: Function) : AbstractIdentifierExpr()
data class UnresolvedExpr(val choice: List<AbstractIdentifierExpr>): Expression()