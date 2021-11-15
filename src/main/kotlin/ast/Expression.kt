package ast

abstract class Expression: Statement()


/**
 *  Literal Float, Int, String, Boolean Expression
 */
abstract class LitExpr : Expression()

/**
 *  Int Value in Expression
 */
data class IntLitExpr(val value: Int) : LitExpr()

/**
 *  Float Value in Expression
 */
data class FloatLitExpr(val value: Float) : LitExpr()

/**
 *  Boolean Value in Expression
 */
data class BoolLitExpr(val value: Boolean) : LitExpr()

/**
 *  String Value in Expression
 */
data class StringLitExpr(val value: String) : LitExpr()

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

/**
 *  Tuple Value in Expression
 */
data class TupleExpr(val value: List<Expression>) : Expression()

/**
 *  Range Expression use for switch
 */
data class RangeLitExpr(val min: Expression, val max: Expression) : LitExpr()