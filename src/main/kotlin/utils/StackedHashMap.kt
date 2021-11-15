package utils

class StackedHashMap<K, V>(parent:StackedHashMap<K, V>? = null) {
    private val map = HashMap<K, V>()
    private val parentStack: StackedHashMap<K, V>? = parent

    operator fun set(key: K, value: V){
        map[key] = value
    }
    fun hasKeyTopLevel(key: K):Boolean{
        return map.containsKey(key)
    }
    operator fun get(key: K, top: Boolean = true):V?{
        var value = map[key]
        if (value != null){
            return value
        }
        if (parentStack!=null){
            value = parentStack.get(key, false)
        }
        if (top && value == null){
            throw ElementNotFoundException(key.toString()+" in "+map.keys.joinToString(", "))
        }
        return value
    }
    fun sub():StackedHashMap<K,V>{
        return StackedHashMap(this)
    }
    fun getTopLevel():HashMap<K, V>{
        return map
    }
    override fun toString():String{
        return if (parentStack != null) {
            "[$map, $parentStack]"
        }else{
            "$map"
        }
    }

    data class ElementNotFoundException(val key: String): Exception()
}