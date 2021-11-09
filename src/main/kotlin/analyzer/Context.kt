package analyzer

import parser.Class
import parser.Struct
import parser.Variable
import parser.Function
import parser.data.Identifier
import utils.StackedHashMap
import java.rmi.UnexpectedException

class Context(val path: String){
    var currentPath: Identifier = Identifier(listOf(path))
    private var variables: StackedHashMap<Identifier, Variable> = StackedHashMap()
    private var functions: StackedHashMap<Identifier, Function> = StackedHashMap()
    private var structs  : StackedHashMap<Identifier, Struct> = StackedHashMap()
    private var classes  : StackedHashMap<Identifier, Class> = StackedHashMap()

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

    fun getVariable(id: Identifier): Variable {
        return variables.get(id) ?: throw Exception()
    }
    fun getFunction(id: Identifier): Function {
        return functions.get(id) ?: throw Exception()
    }
    fun getClass(id: Identifier): Class {
        return classes.get(id) ?: throw Exception()
    }
    fun getStruct(id: Identifier): Struct {
        return structs.get(id) ?: throw Exception()
    }

    fun sub(id: String):Context{
        val context = Context(path)
        context.currentPath = currentPath.sub(id)
        context.variables = variables.sub()
        context.functions = functions.sub()
        context.structs   = structs.sub()
        context.classes   = classes.sub()
        return context
    }
}