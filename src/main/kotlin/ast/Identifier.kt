package ast

class Identifier(val paths: List<String>) {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Identifier -> {
                other.toString() == toString()
            }
            else -> false
        }
    }
    override fun hashCode(): Int {
        return paths.toString().hashCode()
    }
    fun level(): Int{
        return paths.size
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
    fun getLast(): Identifier{
        return Identifier(listOf( paths.last()))
    }
    override fun toString():String{
        return paths.reduce { acc, s -> "$acc.$s" }
    }
}