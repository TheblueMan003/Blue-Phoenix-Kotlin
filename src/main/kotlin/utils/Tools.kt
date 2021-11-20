package utils

import ast.AssignmentType
import ast.Identifier

fun <T> withDefault(arg: List<T>, defaults: List<T?>):List<T>{
    val lst = ArrayList<T>()
    for (i in defaults.indices){
        if (arg.size > i){
            lst.add(arg[i])
        }
        else if (defaults[i] != null){
            lst.add(defaults[i]!!)
        }else{
            return emptyList()
        }
    }
    return lst
}
fun <A, B>List<A>.pmap(f: (A) -> B): List<B> = this.parallelStream().map(f).toList()

fun getOperationFunctionName(op: String): Identifier {
    return when(op){
        "+" -> Identifier(listOf("plus"))
        "-" -> Identifier(listOf("minus"))
        "*" -> Identifier(listOf("times"))
        "/" -> Identifier(listOf("div"))
        "%" -> Identifier(listOf("mod"))
        "^" -> Identifier(listOf("pow"))
        else -> throw NotImplementedError()
    }
}
fun getOperationFunctionName(op: AssignmentType): Identifier{
    return when(op){
        AssignmentType.SET -> Identifier(listOf("assign"))
        AssignmentType.ADD -> Identifier(listOf("plusAssign"))
        AssignmentType.SUB -> Identifier(listOf("minusAssign"))
        AssignmentType.MUL -> Identifier(listOf("timesAssign"))
        AssignmentType.DIV -> Identifier(listOf("divAssign"))
        AssignmentType.MOD -> Identifier(listOf("modAssign"))
        AssignmentType.POW -> Identifier(listOf("powAssign"))
    }
}
fun <K,V> mergeMapArray(m1: List<Map<K, List<V>>>): Map<K, MutableList<V>>{
    val ret = HashMap<K, MutableList<V>>()
    m1.map{it -> it.map { (k,v) -> if(ret.containsKey(k)){ret[k]!!.addAll(v)}else{ ret[k] = v.toMutableList() }}}
    return ret
}