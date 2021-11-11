package analyzer

import parser.*
import parser.Function
import parser.data.Identifier
import utils.StackedHashMap

class Context(private val path: String){
    var currentPath: Identifier = Identifier(listOf(path))
    var currentFolder: Identifier = Identifier(listOf(path))
    private var children = ArrayList<Context>()
    private var variables: StackedHashMap<Identifier, Variable> = StackedHashMap()
    private var functions: StackedHashMap<Identifier, ArrayList<Function>> = StackedHashMap()
    private var structs  : StackedHashMap<Identifier, Struct> = StackedHashMap()
    private var classes  : StackedHashMap<Identifier, Class> = StackedHashMap()
    private var generics  : StackedHashMap<Identifier, DataType> = StackedHashMap()

    private var unfinishedAnalyse = ArrayList<Pair<Statement,Context>>()

    private fun update(child: Context){
        child.variables.getTopLevel()
            .filter { (k, v) -> v.isVisible(DataStructVisibility.PUBLIC, this)}
            .map { (k, v) -> child.currentFolder.append(k) to v }
            .map { (k, v) -> update(k, v)}
        child.functions.getTopLevel()
            .map { (k, v) -> k to v.filter { it.isVisible(DataStructVisibility.PUBLIC, this) } }
            .filter { (k, v) -> v.isNotEmpty() }
            .map { (k, v) -> child.currentFolder.append(k) to v }
            .map { (k, v) -> v.forEach { update(k, it) }}
        child.structs.getTopLevel()
            .filter { (k, v) -> v.isVisible(DataStructVisibility.PUBLIC, this)}
            .map { (k, v) -> child.currentFolder.append(k) to v }
            .map { (k, v) -> update(k, v)}
        child.classes.getTopLevel()
            .filter { (k, v) -> v.isVisible(DataStructVisibility.PUBLIC, this)}
            .map { (k, v) -> child.currentFolder.append(k) to v }
            .map { (k, v) -> update(k, v)}
    }
    fun update(id: Identifier, obj: Variable){
        variables[id] = obj
    }
    fun update(id: Identifier, obj: Function){
        if (!functions.hasKey(id)) { functions[id] = ArrayList<Function>()}
        functions[id]!!.add(obj)
    }
    fun update(id: Identifier, obj: Struct){
        structs[id] = obj
    }
    fun update(id: Identifier, obj: Class){
        classes[id] = obj
    }
    fun update(id: Identifier, obj: DataType){
        generics[id] = obj
    }
    fun hasVariable(id: Identifier): Boolean{
        return variables.get(id, false) != null
    }
    fun hasFunction(id: Identifier): Boolean{
        return functions.get(id, false) != null
    }
    fun hasStruct(id: Identifier): Boolean{
        return structs.get(id, false) != null
    }
    fun hasClass(id: Identifier): Boolean{
        return classes.get(id, false) != null
    }
    fun hasGeneric(id: Identifier): Boolean{
        return generics.get(id) != null
    }
    fun getVariable(id: Identifier): Variable {
        return variables.get(id) ?: throw IdentifierNotFound(id)
    }
    fun getFunction(id: Identifier): List<Function> {
        return functions.get(id) ?: throw IdentifierNotFound(id)
    }
    fun getClass(id: Identifier): Class {
        return classes.get(id) ?: throw IdentifierNotFound(id)
    }
    fun getStruct(id: Identifier): Struct {
        return structs.get(id) ?: throw IdentifierNotFound(id)
    }
    fun getGeneric(id: Identifier): DataType {
        return generics.get(id) ?: throw IdentifierNotFound(id)
    }

    fun sub(id: String):Context{
        val context = Context(id)
        context.currentPath = currentPath.sub(id)
        context.variables   = variables.sub()
        context.functions   = functions.sub()
        context.structs     = structs.sub()
        context.classes     = classes.sub()
        context.generics    = generics.sub()
        context.unfinishedAnalyse = unfinishedAnalyse
        children.add(context)
        return context
    }

    fun resolve(){
        while(children.size > 0){
            update(children.last())
            children.removeLast()
        }
    }

    fun addUnfinished(statement: Statement, context: Context):Statement{
        unfinishedAnalyse.add(Pair(statement, context))
        return statement
    }
    fun runUnfinished(func: (Statement, Context)->Unit){
        unfinishedAnalyse.map{ (stm, c) -> func(stm, c)}
        unfinishedAnalyse.clear()
    }

    data class IdentifierNotFound(val identifier: Identifier): Exception()
}