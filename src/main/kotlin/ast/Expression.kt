package parser

import ast.Identifier

abstract class Expression: Statement()

/**
 *  Int Value in Expression
 */
data class IntLitExpr(val value: Int) : Expression()

/**
 *  Float Value in Expression
 */
data class FloatLitExpr(val value: Float) : Expression()

/**
 *  Boolean Value in Expression
 */
data class BoolLitExpr(val value: Boolean) : Expression()

/**
 *  String Value in Expression
 */
data class StringLitExpr(val value: String) : Expression()

/**
 *  Variable or Function Value in Expression
 */
data class IdentifierExpr(var value: Identifier) : Expression()

/**
 *  Function Call Value in Expression
 */
data class CallExpr(val value: Expression, val args: List<Expression>) : Expression()

/**
 *  Binary Expression
 */
data class BinaryExpr(val op: String, val first: Expression, val second: Expression): Expression()

/**
 *  Unary Expression
 */
data class UnaryExpr(val op: String, val first: Expression): Expression()