package data_struct

class DataStructModifier{
    var visibility: DataStructVisibility = DataStructVisibility.PROTECTED
    var static:     Boolean = false
    var abstract:   Boolean = false
    var operator:   Boolean = false
    var lazy:       Boolean = false
    var inline:     Boolean = false
    var entity:     Boolean = false
    var params:     Boolean = false


    companion object {
        fun newPrivate(): DataStructModifier {
            val modifier = DataStructModifier()
            modifier.visibility = DataStructVisibility.PRIVATE
            return modifier
        }
        fun newPublic(): DataStructModifier {
            val modifier = DataStructModifier()
            modifier.visibility = DataStructVisibility.PUBLIC
            return modifier
        }
        fun newOperator(): DataStructModifier {
            val modifier = DataStructModifier()
            modifier.visibility = DataStructVisibility.PUBLIC
            modifier.operator = true
            return modifier
        }
    }


    override fun toString():String{
        return "($visibility${if(static){" static"}else{""}}${if(abstract){" abstract"}else{""}}${if(operator){" operator"}else{""}}${if(lazy){" lazy"}else{""}})"
    }

    fun clone(): DataStructModifier {
        val modifier = DataStructModifier()
        modifier.visibility = visibility
        modifier.static = static
        modifier.abstract = abstract
        modifier.operator = operator
        modifier.lazy = lazy
        modifier.inline = inline
        modifier.entity = entity
        return modifier
    }
}