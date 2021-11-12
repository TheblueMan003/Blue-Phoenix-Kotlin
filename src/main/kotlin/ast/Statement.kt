package parser

import ast.Identifier

enum class AssignmentType(val op: String){
    SET("="),
    ADD("+="),
    SUB("-="),
    MUL("*="),
    DIV("/="),
    MOD("%"),
    POW("^=")
}


abstract class Statement

class Empty(): Statement()
data class If(val Condition: Expression, val IfBlock: Statement): Statement()
data class IfElse(val Condition: Expression, val IfBlock: Statement, val ElseBlock: Statement): Expression()

/**
 * List of Instruction
 */
data class Block(val statements: List<Statement>): Statement()

/**
 * Block that doesn't change context
 */
data class Sequence(val statements: List<Statement>): Statement()


data class Case(val expr: Expression, val statement: Statement): Statement()
data class Switch(val function: Expression, val cases: List<Case>): Expression()

/**
 *  Variable Before after Name Analysis
 */
data class UnlinkedVariableAssignment(var identifier: Identifier, val expr: Expression, val op: AssignmentType): Expression()

/**
 *  Variable Declaration
 */
data class VariableDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val type: DataType, val parent: Variable? = null): Statement()

/**
 * Function Arguments
 */
data class FunctionArgument(val identifier: Identifier, val type: DataType, val defaultValue: Expression?)

/**
 * Function Declaration
 */
data class FunctionDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val from: List<FunctionArgument>,
                               val to: DataType, val body: Statement, val parent: Variable? = null): Statement()

/**
 * Struct Declaration
 */
data class StructDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val generic: List<DataType>?,
                             val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
                             val builder: Block): Statement()

/**
 * Lambda Declaration
 */
data class LambdaDeclaration(val from: List<DataType>, val to: DataType, val body: Statement): Expression()

data class StatementThanExpression(val statement: Statement, val expr: Expression):Expression()