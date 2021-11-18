package data_struct

import ast.DataType
import ast.FunctionDeclaration
import ast.Identifier
import ast.Sequence
import ast.VariableDeclaration

class Class   (val modifier: DataStructModifier, val name: Identifier, val parent: Class?,
               val generic: List<DataType>?,
               val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
               val builder: Sequence, val parentStruct: Variable? = null): DataStruct(modifier, parentStruct){
    override fun toString():String{
        return "class $name"
    }
}
