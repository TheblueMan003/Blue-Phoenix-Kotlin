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
        return VariableIterator(variable.childrenVariable.values.toList())
    }

    override fun toString(): String {
        return "VariableExpr($variable)"
    }

    private class VariableIterator(val variable: List<Variable>):Iterator<Map<String,Expression>>{
        var index = 0
        override fun hasNext(): Boolean {
            return index < variable.size
        }

        override fun next(): Map<String, Expression> {
            val c = index ++

            return mapOf(Pair("", VariableExpr(variable[c])),
                Pair("index", IntLitExpr(c)),
                Pair("count", IntLitExpr(variable.size)))
        }
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