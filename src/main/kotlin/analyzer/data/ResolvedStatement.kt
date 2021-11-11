package analyzer.data

import parser.AssignmentType
import parser.Expression
import parser.Function
import parser.Variable


abstract class AbstractIdentifierExpr: Expression()
data class LinkedVariableAssignment(var variable: Variable, val expr: Expression, val op: AssignmentType): Expression()
data class VariableExpr(var variable: Variable) : AbstractIdentifierExpr()
data class UnresolvedFunctionExpr(val function: List<Function>) : AbstractIdentifierExpr()
data class UnresolvedExpr(val choice: List<AbstractIdentifierExpr>): Expression()