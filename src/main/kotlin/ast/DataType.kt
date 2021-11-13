package ast

abstract class DataType() {
}

class VoidType: DataType()
class IntType: DataType()
class FloatType: DataType()
class BoolType: DataType()
class StringType: DataType()
class VarType(var expr: Expression? = null): DataType()

/**
 * Either Generic Type, Struct or Class
 */
data class UnresolvedGeneratedType(val name: Identifier): DataType()

data class StructType(val name: Struct, val type: List<DataType>?): DataType()
data class ClassType(val name: Class, val type: List<DataType>?): DataType()

/**
 * Either Generic Type, Struct or Class with generics type in it
 */
data class UnresolvedGeneratedGenericType(val name: Identifier, val type: List<DataType>): DataType()
data class ArrayType(val subtype: DataType, val length: Int): DataType()
data class TupleType(val type: List<DataType>): DataType()
data class FuncType(val from: List<DataType>, val to: DataType): DataType()
