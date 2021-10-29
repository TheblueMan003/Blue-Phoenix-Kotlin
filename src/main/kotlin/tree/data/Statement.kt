package tree

import tree.data.Identifier

abstract class Statement

class Empty(): Statement()
data class If(val Condition: Expression, val IfBlock: Statement): Statement()
data class IfElse(val Condition: Expression, val IfBlock: Statement, val ElseBlock: Statement): Expression()
data class Block(val statements: List<Statement>): Statement()
data class Call(val function: Identifier, val args: List<Expression>, val ret: List<Variable>): Expression()
data class Case(val expr: Expression, val statement: Statement): Statement()
data class Switch(val function: Expression, val cases: List<Case>): Expression()