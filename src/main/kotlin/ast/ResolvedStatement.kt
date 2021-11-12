package analyzer.data

import parser.*
import parser.Function


abstract class AbstractIdentifierExpr: Expression()

/**
 *  Variable Assigment after Name Analysis
 */
data class LinkedVariableAssignment(var variable: Variable, val expr: Expression, val op: AssignmentType): Expression()

/**
 *  Variable Expression after Name Analysis
 */
data class VariableExpr(var variable: Variable) : AbstractIdentifierExpr()

/**
 * Function Expression After Type Checker
 */
data class FunctionExpr(var function: Function) : AbstractIdentifierExpr()

/**
 *  Function Expression Before Type Checker
 */
data class UnresolvedFunctionExpr(val function: List<Function>) : AbstractIdentifierExpr()

/**
 *  Expression with conflicting name between Variable and Function
 */
data class UnresolvedExpr(val choice: List<AbstractIdentifierExpr>): Expression()

data class RawFunctionCall(val function: Function): Statement()