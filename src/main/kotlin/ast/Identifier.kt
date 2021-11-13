package ast

class Identifier(val paths: List<String>) {
    override fun equals(other: Any?): Boolean {
        when (other) {
            is Identifier -> {
                if (paths.size != other.paths.size) return false
                for(i in paths.indices){
                    if (paths[i] != other.paths[i]) return false
                }
                return true
            }
            else -> return false
        }
    }
    override fun hashCode(): Int {
        return paths.toTypedArray().contentHashCode()
    }
    fun sub(path: String): Identifier {
        return Identifier(paths.plus(path))
    }
    fun sub(path: Identifier): Identifier {
        return Identifier(paths.plus(path.paths))
    }
    fun append(path: Identifier):Identifier{
        return Identifier(paths.plus(path.paths))
    }
    fun parent(): Identifier {
        return Identifier(paths.subList(0, paths.size-1))
    }
    fun toUnique(string: String): Identifier{
        return Identifier(paths.subList(0, paths.size-1)+(paths.last()+string))
    }
    override fun toString():String{
        return paths.reduce { acc, s -> "$acc.$s" }
    }
}