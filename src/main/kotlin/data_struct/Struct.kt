package data_struct

import ast.DataType
import ast.FunctionDeclaration
import ast.Identifier
import ast.Sequence
import ast.VariableDeclaration

class Struct  (val modifier: DataStructModifier, val name: Identifier, val generic: List<DataType>?,
               val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
               val builder: Sequence, val parent: Variable? = null): DataStruct(modifier, parent){
    override fun toString():String{
        return "struct $name"
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }
}