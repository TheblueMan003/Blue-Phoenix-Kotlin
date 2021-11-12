package parser

import analyzer.Context
import ast.Identifier

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
    var operator: Boolean = false
}
class Variable(val modifier: DataStructModifier, val name: Identifier,
               val type: DataType, val parent: Variable? = null): DataStruct(modifier, parent){
    var childrenVariable = HashMap<Identifier, Variable>()
    var childrenFunction = HashMap<Identifier, List<Function>>()
}

class Function(val modifier: DataStructModifier, val name: Identifier, val input: List<Variable>, val output: Variable,
               var body: Statement, val parent: Variable? = null): DataStruct(modifier, parent){
    private var usedBy = ArrayList<Function>()
    private var used = false

    fun use(fct: Function){
        usedBy.add(fct)
    }
    fun use(){
        used = true
    }
}

class Struct  (val modifier: DataStructModifier, val name: Identifier, val generic: List<DataType>?,
               val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
               val builder: Block, val parent: Variable? = null): DataStruct(modifier, parent)

class Class   (val modifier: DataStructModifier, val name: Identifier, val parent: Class?,
               val generic: List<DataType>?,
               val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
               val builder: Block, val parentStruct: Variable? = null): DataStruct(modifier, parentStruct)