package ast

import data_struct.Enum

abstract class Expression: Statement()


/**
 *  Literal Float, Int, String, Boolean Expression
 */
abstract class LitExpr : Expression()


/**
 *  Int Value in Expression
 */
data class IntLitExpr(val value: Int) : LitExpr(){
    override fun toString(): String {
        return "IntLitExpr($value)"
    }
}


/**
 *  Float Value in Expression
 */
data class FloatLitExpr(val value: Float) : LitExpr(){
    override fun toString(): String {
        return "FloatLitExpr($value)"
    }
}


/**
 *  Boolean Value in Expression
 */
data class BoolLitExpr(val value: Boolean) : LitExpr(){
    override fun toString(): String {
        return "BoolLitExpr($value)"
    }
}

/**
 *  String Value in Expression
 */
data class StringLitExpr(val value: String) : LitExpr(){
    override fun toString(): String {
        return "StringLitExpr(\"$value\")"
    }
}


/**
 *  Variable or Function Value in Expression
 */
data class IdentifierExpr(var value: Identifier) : Expression(){
    override fun toString(): String {
        return "IdentifierExpr($value)"
    }
}

/**
 *  Function Call Value in Expression
 */
data class CallExpr(val value: Expression, val args: List<Expression>) : Expression(){
    override fun toString(): String {
        return "CallExpr($value($args))"
    }
}

/**
 *  Binary Expression
 */
data class BinaryExpr(val op: String, val first: Expression, val second: Expression): Expression(){
    override fun toString(): String {
        return "BinaryExpr($first $op $second)"
    }
}

/**
 *  Unary Expression
 */
data class UnaryExpr(val op: String, val first: Expression): Expression(){
    override fun toString(): String {
        return "UnaryExpr($op $first)"
    }
}

/**
 *  Tuple Value in Expression
 */
data class TupleExpr(val value: List<Expression>) : Expression(){
    override fun toString(): String {
        return "TupleExpr($value)"
    }
}

/**
 *  Range Expression use for switch
 */
data class RangeLitExpr(val min: Expression, val max: Expression) : LitExpr(), IGenerator{
    override fun getIterator(): Iterator<Expression> {
        return RangeIterator(min, max)
    }

    override fun toString(): String {
        return "RangeLitExpr($min..$max)"
    }

    private class RangeIterator(val min: Expression, val max: Expression):Iterator<Expression>{
        var cur = min
        override fun hasNext(): Boolean {
            return cur != max
        }

        override fun next(): Expression {
            val c = cur
            when(cur){
                is IntLitExpr ->{ cur = IntLitExpr((cur as IntLitExpr).value+1) }
                else -> throw NotImplementedError()
            }
            return c
        }

    }
}

/**
 *  Range Expression use for switch
 */
data class EnumValueExpr(val value: Enum.Case, val index: Int, val enum: Enum) : AbstractIdentifierExpr(){
    override fun toString(): String {
        return "EnumValueExpr($value)"
    }
}

/**
 *  Range Expression use for switch
 */
data class EnumExpr(val enum: Enum) : AbstractIdentifierExpr(), IGenerator{
    override fun toString(): String {
        return "EnumExpr($enum)"
    }
    override fun getIterator(): Iterator<Expression> {
        return EnumIterator(enum)
    }

    private class EnumIterator(val enum: Enum):Iterator<Expression>{
        var index = 0
        override fun hasNext(): Boolean {
            return index != enum.values.size
        }

        override fun next(): Expression {
            val c = index++
            return EnumValueExpr(enum.values[index++], c, enum)
        }

    }
}