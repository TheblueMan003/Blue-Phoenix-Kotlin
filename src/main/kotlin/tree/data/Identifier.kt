package tree.data

class Identifier(val paths: List<String>) {
    override fun equals(other: Any?): Boolean {
        when (other) {
            is Identifier -> {
                if (paths.size != other.paths.size) return false;
                for(i in 0..paths.size){
                    if (paths[i] != other.paths[i]) return false;
                }
                return true;
            }
            else -> return false;
        }
    }
    override fun hashCode(): Int {
        return paths.toTypedArray().contentHashCode();
    }
    fun sub(path: String): Identifier {
        return Identifier(paths.plus(path));
    }
    fun parent(): Identifier {
        return Identifier(paths.subList(0, paths.size-1))
    }
}