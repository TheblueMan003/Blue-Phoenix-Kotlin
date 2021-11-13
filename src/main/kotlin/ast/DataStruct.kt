package ast

import analyzer.Context
import javax.xml.crypto.Data

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

    override fun toString():String{
        return name.toString()
    }
}

class Function(val modifier: DataStructModifier, val name: Identifier, val from: List<FunctionArgument>,
               val input: List<Variable>, val output: Variable,
               var body: Statement, val parent: Variable? = null): DataStruct(modifier, parent){
    private var used = false
    val called = ArrayList<Function>()
    private var compiled = false

    fun use(){
        used = true
        called.map { if (!it.isUsed()){it.use()} }
    }
    fun addFuncCall(function: Function){
        if (isUsed()){
            if (!function.used){ function.use() }
        }else{
            called.add(function)
        }
    }

    fun isUsed(): Boolean{
        return used
    }
    fun compile(){
        compiled = true
    }

    override fun toString(): String {
        return "$name: ${from.map { it.type }} -> ${output.type}"
    }
}

class Struct  (val modifier: DataStructModifier, val name: Identifier, val generic: List<DataType>?,
               val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
               val builder: Sequence, val parent: Variable? = null): DataStruct(modifier, parent)

class Class   (val modifier: DataStructModifier, val name: Identifier, val parent: Class?,
               val generic: List<DataType>?,
               val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
               val builder: Sequence, val parentStruct: Variable? = null): DataStruct(modifier, parentStruct)

class TypeDef(val modifier: DataStructModifier, val name: Identifier,
               val type: DataType, val parent: Variable? = null): DataStruct(modifier, parent)
