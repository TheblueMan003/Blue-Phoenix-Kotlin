package data_struct

import ast.FunctionArgument
import ast.Identifier
import ast.Statement
import context.IContext

class Function(val modifier: DataStructModifier, val name: Identifier, val from: List<FunctionArgument>,
               val input: List<Variable>, val output: Variable,
               var body: Statement, val context: IContext, val parent: Variable? = null): DataStruct(modifier, parent){
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
    override fun hashCode(): Int {
        return name.hashCode()
    }
}