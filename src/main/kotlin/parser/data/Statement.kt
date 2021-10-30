package parser

import parser.data.Identifier

abstract class Statement

class Empty(): Statement()
data class If(val Condition: Expression, val IfBlock: Statement): Statement()
data class IfElse(val Condition: Expression, val IfBlock: Statement, val ElseBlock: Statement): Expression()
data class Block(val statements: List<Statement>): Statement()
data class Call(val function: Identifier, val args: List<Expression>, val ret: List<Variable>): Expression()
data class Case(val expr: Expression, val statement: Statement): Statement()
data class Switch(val function: Expression, val cases: List<Case>): Expression()


data class VariableAssignment(val identifier: Identifier, val expr: Expression): Expression()
data class VariableDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val type: DataType): Statement()
data class FunctionArgument(val identifier: Identifier, val type: DataType, val defaultValue: Expression?)
data class FunctionDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val from: List<FunctionArgument>,
                               val to: DataType, val body: Statement): Statement()
data class LambdaDeclaration(val from: List<DataType>, val to: DataType, val body: Statement): Expression()