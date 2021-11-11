package utils

class OutputFile(val name: String) {
    var data = ArrayList<String>()

    fun add(value: String){
        data.add(value)
    }
    fun add(value: List<String>){
        data.addAll(value)
    }

    override fun toString():String{
        return name+":"+data.joinToString("\n")
    }
}