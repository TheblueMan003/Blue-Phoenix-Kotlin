package ast

import data_struct.Function
import data_struct.Struct
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
data class VariableExpr(var variable: Variable) : AbstractIdentifierExpr(), IGenerator{
    override fun getIterator(): Iterator<Map<String, Expression>> {
        return ListIterator(variable.childrenVariable.values.map { VariableExpr(it) })
    }

    override fun toString(): String {
        return "VariableExpr($variable)"
    }
}

/**
 *  Lazy Variable Expression after Name Analysis
 */
data class LazyVariableExpr(var expr: Expression) : AbstractIdentifierExpr(){
    override fun toString(): String {
        return "LazyVariableExpr($expr)"
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
 * Function Expression After Type Checker
 */
data class StructConstructorExpr(var struct: Function) : AbstractIdentifierExpr(){
    override fun toString(): String {
        return "StructConstructorExpr($struct)"
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
 *  Function Expression Before Type Checker
 */
data class UnresolvedStructConstructorExpr(val struct: Struct) : AbstractIdentifierExpr(){
    override fun toString(): String {
        return "UnresolvedStructConstructorExpr($struct)"
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
data class BuildInFunctionCall(val function: Identifier, val expr: List<Expression>): Expression(){
    override fun toString(): String {
        return "BuildInFunctionCall($function, $expr)"
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

/**
 *  Variable Expression after Name Analysis
 */
data class UnlinkedSelectorVariableExpr(val selector: SelectorExpr, var variable: Identifier) : Expression(){
    override fun toString(): String {
        return "UnresolvedSelectorVariableExpr(${selector.selector}.$variable)"
    }
}

/**
 *  Variable Expression after Name Analysis
 */
data class LinkedSelectorVariableExpr(val selector: SelectorExpr, var variable: Variable) : Expression(){
    override fun toString(): String {
        return "ResolvedSelectorVariableExpr(${selector.selector}.$variable)"
    }
}


/**
 *  Variable Before after Name Analysis
 */
data class UnlinkedSelectorVariableAssignment(val selector: SelectorExpr, var identifier: Identifier, val expr: Expression, val op: AssignmentType): Expression(){
    override fun toString(): String {
        return "UnlinkedSelectorVariableAssignment(${selector.selector}.$identifier $op $expr)"
    }
}


/**
 *  Variable Assigment after Name Analysis
 */
data class LinkedSelectorVariableAssignment(val selector: SelectorExpr, var variable: Variable, val expr: Expression, val op: AssignmentType): Expression(){
    override fun toString(): String {
        return "LinkedSelectorVariableAssignment(${selector.selector}.$variable $op $expr)"
    }
}