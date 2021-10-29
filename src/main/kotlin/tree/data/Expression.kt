package tree

import guru.zoroark.pangoro.PangoroNode
import guru.zoroark.pangoro.PangoroNodeDeclaration
import guru.zoroark.pangoro.reflective
import tree.data.Identifier

abstract class Expression: Statement()

data class IntLitExpr(val value: Int) : Expression()
data class FloatLitExpr(val value: Float) : Expression()
data class BoolLitExpr(val value: Boolean) : Expression()
data class StringLitExpr(val value: String) : Expression()
data class VarExpr(val value: Identifier) : Expression()
data class CallExpr(val value: Identifier, val args: List<Expression>) : Expression()

data class BinaryExpr(val op: String, val first: Expression, val second: Expression): Expression()
data class UnaryExpr(val op: String, val first: Expression): Expression()