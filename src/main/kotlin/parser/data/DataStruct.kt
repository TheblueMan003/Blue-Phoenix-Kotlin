package parser

import analyzer.Context
import parser.data.Identifier

abstract class DataStruct(private val d_modifier: DataStructModifier, private val d_parent: Variable? = null) {
    fun isVisible(visibility: DataStructVisibility, context: Context):Boolean{
        return if (d_parent != null && context.hasVariable(d_parent.name)){
            d_parent.isVisible(visibility, context)
        }
        else{
            d_modifier.visibility == visibility
        }
    }
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
class Variable(val modifier: DataStructModifier, val name: Identifier,
               val type: DataType, val parent: Variable? = null): DataStruct(modifier, parent)

class Function(val modifier: DataStructModifier, val name: Identifier, val input: List<Variable>, val output: Variable,
               val parent: Variable? = null): DataStruct(modifier, parent)

class Struct  (val modifier: DataStructModifier, val name: Identifier, val generic: List<DataType>?,
               val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
               val builder: Block, val parent: Variable? = null): DataStruct(modifier, parent)

class Class   (val modifier: DataStructModifier, val name: Identifier, val parent: Class?,
               val generic: List<DataType>?,
               val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
               val builder: Block, val parentStruct: Variable? = null): DataStruct(modifier, parentStruct)