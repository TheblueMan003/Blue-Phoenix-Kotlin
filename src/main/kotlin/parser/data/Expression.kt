package parser

import parser.data.Identifier

abstract class Expression: Statement()

data class IntLitExpr(val value: Int) : Expression()
data class FloatLitExpr(val value: Float) : Expression()
data class BoolLitExpr(val value: Boolean) : Expression()
data class StringLitExpr(val value: String) : Expression()
data class IdentifierExpr(var value: Identifier) : Expression()
data class CallExpr(val value: Expression, val args: List<Expression>) : Expression()

data class BinaryExpr(val op: String, val first: Expression, val second: Expression): Expression()
data class UnaryExpr(val op: String, val first: Expression): Expression()