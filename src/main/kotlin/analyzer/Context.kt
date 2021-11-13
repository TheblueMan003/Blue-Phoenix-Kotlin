package analyzer

import ast.*
import ast.Function
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
    private var unfinishedAnalyse = ArrayList<Pair<Function,Context>>()
    private var tmpVarNb = 0
    var currentFunction: Function? = null
    var functionsList = ArrayList<Function>()
    var parentVariable: ast.Variable? = null
    var top = true

    private fun update(child: Context){
        child.variables.getTopLevel()
            .filter { (k, v) -> v.isVisible(ast.DataStructVisibility.PUBLIC, this)}
            .map { (k, v) -> child.currentFolder.append(k) to v }
            .map { (k, v) -> update(k, v)}
        child.functions.getTopLevel()
            .map { (k, v) -> k to v.filter { it.isVisible(ast.DataStructVisibility.PUBLIC, this) } }
            .filter { (k, v) -> v.isNotEmpty() }
            .map { (k, v) -> child.currentFolder.append(k) to v }
            .map { (k, v) -> v.forEach { update(k, it, true) }}
        child.structs.getTopLevel()
            .filter { (k, v) -> v.isVisible(ast.DataStructVisibility.PUBLIC, this)}
            .map { (k, v) -> child.currentFolder.append(k) to v }
            .map { (k, v) -> update(k, v)}
        child.classes.getTopLevel()
            .filter { (k, v) -> v.isVisible(ast.DataStructVisibility.PUBLIC, this)}
            .map { (k, v) -> child.currentFolder.append(k) to v }
            .map { (k, v) -> update(k, v)}
    }

    //
    // UPDATE
    //
    fun update(id: Identifier, obj: ast.Variable){
        if (variables.hasKeyTopLevel(id)) throw Exception("$id was already defined in scope")
        variables[id] = obj

        // Add Variable to Parent
        if (parentVariable != null) {
            parentVariable!!.childrenVariable[id] = obj
        }
    }
    fun update(id: Identifier, obj: Function, resolving: Boolean = false){
        if (!functions.hasKeyTopLevel(id)) {
            functions[id] = ArrayList<Function>()

            // Add Function List to Parent
            if (parentVariable != null) {
                parentVariable!!.childrenFunction[id] = functions[id]!!
            }
        }
        if (!resolving){functionsList.add(obj)}
        functions[id]!!.add(obj)
    }
    fun update(id: Identifier, obj: ast.Struct){
        structs[id] = obj
    }
    fun update(id: Identifier, obj: ast.Class){
        classes[id] = obj
    }
    fun update(id: Identifier, obj: DataType){
        generics[id] = obj
    }

    //
    // CHECK
    //
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

    //
    // GETTER
    //
    fun getVariable(id: Identifier): ast.Variable {
        return variables.get(id) ?: throw IdentifierNotFound(id)
    }
    fun getFunction(id: Identifier): List<Function> {
        return functions.get(id) ?: throw IdentifierNotFound(id)
    }
    fun getClass(id: Identifier): ast.Class {
        return classes.get(id) ?: throw IdentifierNotFound(id)
    }
    fun getStruct(id: Identifier): ast.Struct {
        return structs.get(id) ?: throw IdentifierNotFound(id)
    }
    fun getGeneric(id: Identifier): DataType {
        return generics.get(id) ?: throw IdentifierNotFound(id)
    }

    private fun getOverloadNumber(id: Identifier): Int{
        return if (!functions.hasKeyTopLevel(id)) {
            0
        } else {
            functions[id]!!.size
        }
    }
    fun getUniqueFunctionIdenfier(id: Identifier):Identifier{
        val nb = getOverloadNumber(id)
        return if (nb == 0){
            id
        }else{
            id.toUnique("\$${nb}")
        }
    }


    fun sub(id: String):Context{
        val context = Context(id)
        context.currentPath       = currentPath.sub(id)
        context.variables         = variables.sub()
        context.functions         = functions.sub()
        context.structs           = structs.sub()
        context.classes           = classes.sub()
        context.generics          = generics.sub()
        context.unfinishedAnalyse = unfinishedAnalyse
        context.functionsList     = functionsList
        children.add(context)
        return context
    }



    fun resolve(){
        while(children.size > 0){
            update(children.last())
            children.removeLast()
        }
    }

    fun addUnfinished(function: Function, context: Context): Function {
        unfinishedAnalyse.add(Pair(function, context))
        return function
    }
    fun runUnfinished(func: (Statement, Context)-> Statement){
        top = false

        while (unfinishedAnalyse.isNotEmpty()){
            val unfinished = ArrayList(unfinishedAnalyse)
            unfinishedAnalyse.clear()

            unfinished.forEach { (fct, c) ->
                run {
                    val pre = c.currentFunction
                    c.currentFunction = fct
                    fct.body = func(fct.body, c)
                    c.currentFunction = pre
                }
            }
        }
    }

    fun getTmpVarIdentifier():Identifier{
        val nb = tmpVarNb++
        return Identifier(listOf("\$tmp_$nb"))
    }

    data class IdentifierNotFound(val identifier: Identifier): Exception()
}