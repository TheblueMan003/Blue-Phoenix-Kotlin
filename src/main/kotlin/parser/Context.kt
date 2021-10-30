package parser

import parser.data.Identifier

class Context(val path: String){
    var currentPath: Identifier = Identifier(listOf(path))
    val variables: HashMap<Identifier, Variable> = HashMap()
    val functions: HashMap<Identifier, Function> = HashMap()
    val structs  : HashMap<Identifier, Struct> = HashMap()
    val classes  : HashMap<Identifier, Class> = HashMap()

    fun update(id: Identifier, obj: Variable){
        variables[id] = obj
    }
    fun update(id: Identifier, obj: Function){
        functions[id] = obj
    }
    fun update(id: Identifier, obj: Struct){
        structs[id] = obj
    }
    fun update(id: Identifier, obj: Class){
        classes[id] = obj
    }

    fun sub(id: String){
        currentPath = currentPath.sub(id)
    }
    fun parent(){
        currentPath = currentPath.parent()
    }
}