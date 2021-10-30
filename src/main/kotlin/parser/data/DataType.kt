package parser

import parser.data.Identifier

abstract class DataType() {
}

class VoidType: DataType()
class IntType: DataType()
class FloatType: DataType()
class BoolType: DataType()
class StringType: DataType()
class GeneratedType(val name: Identifier):DataType()
class GeneratedGenericType(val name: Identifier, val type: List<DataType>):DataType()
class ArrayType(val subtype: DataType, val length: Int):DataType()
class TupleType(val type: List<DataType>):DataType()
class FuncType(val from: DataType, val to: DataType):DataType()
