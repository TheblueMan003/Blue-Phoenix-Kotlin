package utils

class StackedHashMap<K, V>(parent:StackedHashMap<K, V>? = null) {
    private val map = HashMap<K, V>()
    private val parentStack: StackedHashMap<K, V>? = parent

    fun set(key: K, value: V){
        map[key] = value
    }
    fun get(key: K, top: Boolean = true):V?{
        var value = map[key]
        if (value != null){
            return value
        }
        if (parentStack!=null){
            value = parentStack.get(key, false)
        }
        if (top && value == null){
            throw ElementNotFoundException(key.toString())
        }
        return value
    }
    fun sub():StackedHashMap<K,V>{
        return StackedHashMap(this)
    }

    data class ElementNotFoundException(val key: String): Exception()
}