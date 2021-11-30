package ast

import data_struct.Class
import data_struct.Enum
import data_struct.Struct
import javax.xml.crypto.Data

abstract class DataType() {
    override fun hashCode(): Int {
        return toString().hashCode()
    }
}

class VoidType: DataType(){
    override fun toString(): String {
        return "VoidType"
    }
}
class UnresolvedType: DataType(){
    override fun toString(): String {
        return "UnresolvedType"
    }
}
class IntType: DataType(){
    override fun toString(): String {
        return "IntType"
    }
}
class FloatType: DataType(){
    override fun toString(): String {
        return "FloatType"
    }
}
class BoolType: DataType(){
    override fun toString(): String {
        return "BoolType"
    }
}
class StringType: DataType(){
    override fun toString(): String {
        return "StringType"
    }
}
class VarType(var expr: Expression? = null): DataType(){
    override fun toString(): String {
        return "Var"
    }
}

/**
 * Either Generic Type, Struct or Class
 */
data class UnresolvedGeneratedType(val name: Identifier): DataType(){
    override fun toString(): String {
        return "UnresolvedGeneratedType($name)"
    }
}

data class StructType(val name: Struct, val type: List<DataType>?): DataType(){
    override fun toString(): String {
        return "StructType(${name.name}<${type}>)"
    }
}

data class ClassType(val name: Class, val type: List<DataType>?): DataType(){
    override fun toString(): String {
        return "ClassType(${name.name}<${type}>)"
    }
}

data class EnumType(val enum: Enum): DataType(){
    override fun toString(): String {
        return "EnumType(${enum.name})"
    }
}

/**
 * Either Generic Type, Struct or Class with generics type in it
 */
data class UnresolvedGeneratedGenericType(val name: Identifier, val type: List<DataType>): DataType(){
    override fun toString(): String {
        return "UnresolvedGeneratedGenericType(${name}<${type}>)"
    }
}

data class ArrayType(val subtype: DataType, val length: List<Int>): DataType(){
    override fun toString(): String {
        return "Array(${subtype}${length})"
    }
}

data class TupleType(val type: List<DataType>): DataType(){
    override fun toString(): String {
        return "TupleType(${type})"
    }
}
data class FuncType(val from: List<DataType>, val to: DataType): DataType(){
    override fun toString(): String {
        return "FuncType(${from} => $to)"
    }
}
data class RangeType(val type: DataType): DataType(){
    override fun toString(): String {
        return "Range(${type})"
    }
}
data class TypeType(val type: DataType):DataType(){
    override fun toString(): String {
        return "TypeType(${type})"
    }
}
class SelectorType:DataType(){
    override fun toString(): String {
        return "SelectorType"
    }
}
class AnyType:DataType(){
    override fun toString(): String {
        return "AnyType"
    }
}