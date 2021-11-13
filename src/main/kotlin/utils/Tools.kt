package utils

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