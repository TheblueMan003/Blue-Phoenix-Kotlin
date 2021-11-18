package data_struct

import ast.DataType
import ast.Expression
import ast.Identifier


class Enum(val modifier: DataStructModifier, val name: Identifier,
           val fields: List<Field>, val values: List<Case>, val parent: Variable? = null): DataStruct(modifier, parent){
    override fun toString():String{
        return "enum $name"
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }

    fun hasField(value: Identifier):Boolean{
        return values.any{it.name == value}
    }

    fun getField(value: Identifier):Case{
        return values.first{it.name == value}
    }

    fun getFieldIndex(value: Identifier):Int{
        return values.indexOfFirst{it.name == value}
    }

    data class Case(val name: Identifier, val data: List<Expression>)
    data class Field(val name: Identifier, val type: DataType, val defaultValue: Expression?)
}