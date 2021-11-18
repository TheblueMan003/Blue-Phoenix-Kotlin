package data_struct

import ast.Expression
import ast.Identifier

class LazyVariable(val modifier: DataStructModifier, val name: Identifier,
                   val expr: Expression, val parent: Variable? = null): DataStruct(modifier, parent){
    override fun toString():String{
        return "lazy var $name"
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }
}