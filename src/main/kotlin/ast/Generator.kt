package ast

interface IGenerator {
    fun getIterator(): Iterator<Map<String, Expression>>
}

class ListIterator(private val lst: List<Expression>):Iterator<Map<String, Expression>>{
    var index = 0
    override fun hasNext(): Boolean {
        return index != lst.size
    }

    override fun next(): Map<String, Expression> {
        val c = index++

        val value = lst[c]

        val map = HashMap<String,Expression>()
        if (value is IGenerator){
            val it = value.getIterator()
            var count = 0
            while(it.hasNext()){
                map.putAll(it.next().map {
                    if (it.key == ""){
                        "_$count" to it.value
                    } else {
                        "_$count."+it.key to it.value
                    }
                })
                count++
            }
        }

        return map+mapOf(
            Pair("",value),
            Pair("index", IntLitExpr(c)),
            Pair("count", IntLitExpr(lst.size))
        )
    }
}