package parser

import parser.data.Identifier

abstract class DataStruct() {
}
enum class DataStructVisibility{
    PRIVATE,
    PROTECTED,
    PUBLIC
}

class DataStructModifier{
    var visibility: DataStructVisibility = DataStructVisibility.PROTECTED
    var static: Boolean = false
    var abstract: Boolean = false
}
class Variable(val modifier: DataStructModifier, val name: Identifier, val type: DataType): DataStruct()
class Function(val modifier: DataStructModifier, val name: Identifier, val input: List<Variable>, val output: Variable): DataStruct()
class Struct  (val modifier: DataStructModifier, val name: Identifier, val fields: Statement): DataStruct()
class Class   (val modifier: DataStructModifier, val name: Identifier, val parent: Class?): DataStruct()