package parser

import parser.data.Identifier

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
data class Block(val statements: List<Statement>): Statement()
data class Sequence(val statements: List<Statement>): Statement()
data class Case(val expr: Expression, val statement: Statement): Statement()
data class Switch(val function: Expression, val cases: List<Case>): Expression()


data class UnlinkedVariableAssignment(var identifier: Identifier, val expr: Expression, val op: AssignmentType): Expression()
data class VariableDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val type: DataType, val parent: Variable? = null): Statement()
data class FunctionArgument(val identifier: Identifier, val type: DataType, val defaultValue: Expression?)

data class FunctionDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val from: List<FunctionArgument>,
                               val to: DataType, val body: Statement, val parent: Variable? = null): Statement()

data class StructDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val generic: List<DataType>?,
                             val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
                             val builder: Block): Statement()

data class LambdaDeclaration(val from: List<DataType>, val to: DataType, val body: Statement): Expression()