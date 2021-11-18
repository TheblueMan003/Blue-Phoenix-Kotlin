package data_struct

import analyzer.Context

abstract class DataStruct(private val d_modifier: DataStructModifier, private val d_parent: Variable? = null) {
    var ownerPackage: String? = null
    fun isVisible(visibility: DataStructVisibility, context: Context):Boolean{
        return if (d_parent != null && context.hasVariable(d_parent.name)){
            d_parent.isVisible(visibility, context)
        }
        else{
            d_modifier.visibility >= visibility
        }
    }
}