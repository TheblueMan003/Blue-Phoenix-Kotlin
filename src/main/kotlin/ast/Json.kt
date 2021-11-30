package ast

interface JsonObject {
    fun get(key: String): JsonObject
    fun get(key: Identifier): JsonObject
    fun toJsonString(markStringKey: Boolean = false): String
}
interface JsonLeaves: JsonObject{
    override fun get(key: String): JsonObject {
        throw Exception("Cannot Get After Leaves")
    }

    override fun get(key: Identifier): JsonObject {
        throw Exception("Cannot Get After Leaves")
    }
}

data class DictionaryExpr(val value: Map<String, JsonObject>): Expression(), JsonObject{
    override fun get(key: String): JsonObject {
        return value[key]!!
    }

    override fun get(key: Identifier): JsonObject {
        return if (key.hasTail()) {
            value[key.getFirst().toString()]!!.get(key.getTail())
        } else{
            value[key.toString()]!!
        }
    }

    override fun toJsonString(markStringKey: Boolean): String {
        return if (value.keys.isNotEmpty() && !markStringKey)
            "{${value.map {"${ it.key }: ${ it.value.toJsonString(markStringKey)}" }.reduce{ x,y -> "$x, $y" }}"
        else if (value.keys.isNotEmpty())
            "{${value.map {"\"${ it.key }\": ${ it.value.toJsonString(markStringKey)}" }.reduce{ x,y -> "$x, $y" }}"
        else "{}"
    }

    override fun toString(): String {
        return "DictionaryExpr($value)"
    }
}