package data_struct

import ast.DataType
import ast.Identifier

class TypeDef(val modifier: DataStructModifier, val name: Identifier,
              val type: DataType, val parent: Variable? = null): DataStruct(modifier, parent){
    override fun toString():String{
        return "typedef $name"
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }
}