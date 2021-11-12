package parser

import ast.Identifier

abstract class DataType() {
}

class VoidType: DataType()
class IntType: DataType()
class FloatType: DataType()
class BoolType: DataType()
class StringType: DataType()

/**
 * Either Generic Type, Struct or Class
 */
class UnresolvedGeneratedType(val name: Identifier):DataType()

class StructType(val name: Struct, val type: List<DataType>?):DataType()
class ClassType(val name: Class, val type: List<DataType>?):DataType()

/**
 * Either Generic Type, Struct or Class with generics type in it
 */
class UnresolvedGeneratedGenericType(val name: Identifier, val type: List<DataType>):DataType()
class ArrayType(val subtype: DataType, val length: Int):DataType()
class TupleType(val type: List<DataType>):DataType()
class FuncType(val from: List<DataType>, val to: DataType):DataType()
