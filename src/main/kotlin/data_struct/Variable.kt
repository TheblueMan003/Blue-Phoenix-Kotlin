package data_struct

import ast.DataType
import ast.Expression
import ast.Identifier

class Variable(val modifier: DataStructModifier, val name: Identifier,
               val type: DataType, val parent: Variable? = null): DataStruct(modifier, parent){
    var childrenVariable = HashMap<Identifier, Variable>()
    var childrenFunction = HashMap<Identifier, List<Function>>()
    var lazyValue: Expression? = null

    override fun toString():String{
        return name.toString()
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}