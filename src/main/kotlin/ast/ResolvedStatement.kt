package ast

import data_struct.Function
import data_struct.Variable


abstract class AbstractIdentifierExpr: Expression()

/**
 *  Variable Assigment after Name Analysis
 */
data class LinkedVariableAssignment(var variable: Variable, val expr: Expression, val op: AssignmentType): Expression(){
    override fun toString(): String {
        return "LinkedVariableAssignment($variable $op $expr)"
    }
}

/**
 *  Variable Expression after Name Analysis
 */
data class VariableExpr(var variable: Variable) : AbstractIdentifierExpr(){
    override fun toString(): String {
        return "VariableExpr($variable)"
    }
}

/**
 * Function Expression After Type Checker
 */
data class FunctionExpr(var function: Function) : AbstractIdentifierExpr(){
    override fun toString(): String {
        return "FunctionExpr($function)"
    }
}

/**
 *  Function Expression Before Type Checker
 */
data class UnresolvedFunctionExpr(val function: List<Function>) : AbstractIdentifierExpr(){
    override fun toString(): String {
        return "UnresolvedFunctionExpr($function)"
    }
}

/**
 *  Expression with conflicting name between Variable and Function
 */
data class UnresolvedExpr(val choice: List<AbstractIdentifierExpr>): Expression(){
    override fun toString(): String {
        return "UnresolvedExpr($choice)"
    }
}

/**
 *  Function Call without any arg or return Type
 */
data class RawFunctionCall(val function: Function): Statement(){
    override fun toString(): String {
        return "RawFunctionCall($function)"
    }
}

/**
 *  Function Call without any arg or return Type
 */
data class FunctionBody(val body: Statement, val function: Function): Statement(){
    override fun toString(): String {
        return "FunctionBody($body, $function)"
    }
}

data class ReturnStatement(val expr: Expression, val function: Function): Statement(){
    override fun toString(): String {
        return "ReturnStatement($expr, $function)"
    }
}