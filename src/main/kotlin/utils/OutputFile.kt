package utils

class OutputFile(val name: String) {
    var data = ArrayList<String>()

    fun add(value: String){
        data.add(value)
    }

    override fun toString():String{
        return name+":"+data.joinToString("\n")
    }
}