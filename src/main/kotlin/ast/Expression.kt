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
data class IntLitExpr(val value: Int) : LitExpr(), JsonLeaves{
    override fun toJsonString(markStringKey: Boolean): String {
        return value.toString()
    }

    override fun toString(): String {
        return "IntLitExpr($value)"
    }
}


/**
 *  Float Value in Expression
 */
data class FloatLitExpr(val value: Float) : LitExpr(), JsonLeaves{
    override fun toJsonString(markStringKey: Boolean): String {
        return value.toString()
    }

    override fun toString(): String {
        return "FloatLitExpr($value)"
    }
}


/**
 *  Boolean Value in Expression
 */
data class BoolLitExpr(val value: Boolean) : LitExpr(), JsonLeaves{
    override fun toJsonString(markStringKey: Boolean): String {
        return value.toString()
    }

    override fun toString(): String {
        return "BoolLitExpr($value)"
    }
}

/**
 *  String Value in Expression
 */
data class StringLitExpr(val value: String) : LitExpr(), JsonLeaves{
    override fun toJsonString(markStringKey: Boolean): String {
        return "\"$value\""
    }

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
 *  Boolean Value in Expression
 */
data class SelectorExpr(val selector: String): Expression(){
    override fun toString(): String {
        return "SelectorType($selector)"
    }
}

/**
 *  Function Call Value in Expression
 */
data class CallExpr(val value: Expression, val args: List<Expression>, val operator: Boolean = false) : Expression(){
    override fun toString(): String {
        return "CallExpr($value($args))"
    }
}

/**
 *  Function Get Value in Expression
 */
data class GetExpr(val value: Expression, val args: List<Expression>) : Expression(){
    override fun toString(): String {
        return "$value[$args]"
    }
}

/**
 *  Function Set Value in Expression
 */
data class SetExpr(val value: Expression, val args: List<Expression>, val setValue: Expression) : Expression(){
    override fun toString(): String {
        return "$value[$args] = $setValue"
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
 *  Unary Expression
 */
data class TypeExpr(val type: DataType): Expression(){
    override fun toString(): String {
        return "TypeExpr($type)"
    }
}

/**
 *  Tuple Value in Expression
 */
data class TupleExpr(val value: List<Expression>) : Expression(), IGenerator{
    override fun getIterator(): Iterator<Map<String, Expression>> {
        return ListIterator(value)
    }

    override fun toString(): String {
        return "TupleExpr($value)"
    }
}

/**
 *  Tuple Value in Expression
 */
data class ArrayExpr(val value: List<Expression>) : Expression(), IGenerator, JsonObject{
    override fun getIterator(): Iterator<Map<String, Expression>> {
        return ListIterator(value)
    }

    override fun toString(): String {
        return "ArrayExpr($value)"
    }

    override fun get(key: String): JsonObject {
        return value[key.toInt()] as JsonObject
    }

    override fun get(key: Identifier): JsonObject {
        return if (key.hasTail()) {
            (value[key.getFirst().toString().toInt()] as JsonObject).get(key.getTail())
        } else {
            (value[key.getFirst().toString().toInt()] as JsonObject)
        }
    }

    override fun toJsonString(markStringKey: Boolean): String {
        return if (value.isNotEmpty())
            "[${value.map { (it as JsonObject).toJsonString(markStringKey) }.reduce{ x, y -> "$x, $y" }}]"
        else "[]"
    }
}

/**
 *  Range Expression use for switch
 */
data class RangeLitExpr(val min: Expression, val max: Expression) : LitExpr(), IGenerator{
    override fun getIterator(): Iterator<Map<String, Expression>> {
        return RangeIterator(min, max)
    }

    override fun toString(): String {
        return "RangeLitExpr($min..$max)"
    }

    private class RangeIterator(val min: Expression, val max: Expression):Iterator<Map<String,Expression>>{
        var cur = min
        override fun hasNext(): Boolean {
            val minV = (min as IntLitExpr).value
            val maxV = (max as IntLitExpr).value
            val index = (cur as IntLitExpr).value
            return if (minV < maxV) {
                index <= maxV
            } else {
                index >= minV
            }
        }

        override fun next(): Map<String, Expression> {
            val c = cur
            val minV = (min as IntLitExpr).value
            val maxV = (max as IntLitExpr).value
            val index = (c as IntLitExpr).value

            cur = if (minV < maxV) {
                IntLitExpr((cur as IntLitExpr).value + 1)
            } else {
                IntLitExpr((cur as IntLitExpr).value - 1)
            }

            return mapOf(Pair("", c), Pair("index", c),
                Pair("index", IntLitExpr(index-minV)),
                Pair("count", IntLitExpr(maxV-minV + 1)))
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
    override fun getIterator(): Iterator<Map<String, Expression>> {
        return EnumIterator(enum)
    }

    private class EnumIterator(val enum: Enum):Iterator<Map<String, Expression>>{
        var index = 0
        override fun hasNext(): Boolean {
            return index != enum.values.size
        }

        override fun next(): Map<String, Expression> {
            val c = index++

            val value = enum.values[c]
            val map = (value.data.zip(enum.fields).map { (v, f) -> Pair(f.name.toString(), v) }).toMap()

            return map + mapOf(
                Pair("",EnumValueExpr(enum.values[c], c, enum)),
                Pair("index", IntLitExpr(c)),
                Pair("count", IntLitExpr(enum.values.size))
            )
        }
    }
}

