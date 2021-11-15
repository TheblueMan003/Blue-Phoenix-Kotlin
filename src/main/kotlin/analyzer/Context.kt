package analyzer

import ast.*
import ast.Function
import utils.StackedHashMap
import compiler.Compiler

class Context(private val path: String, val compiler: Compiler){
    var currentPath: Identifier = Identifier(path.split('.'))
    var currentFolder: Identifier = Identifier(path.split('.'))
    private var children = ArrayList<Context>()
    private var variables: StackedHashMap<Identifier, Variable> = StackedHashMap()
    private var functions: StackedHashMap<Identifier, ArrayList<Function>> = StackedHashMap()
    private var structs  : StackedHashMap<Identifier, Struct> = StackedHashMap()
    private var classes  : StackedHashMap<Identifier, Class> = StackedHashMap()
    private var generics  : StackedHashMap<Identifier, DataType> = StackedHashMap()
    private var typedef  : StackedHashMap<Identifier, TypeDef> = StackedHashMap()
    private var unfinishedAnalyse = ArrayList<Pair<Function,Context>>()
    private var tmpVarNb = 0
    var currentFunction: Function? = null
    var functionsList = ArrayList<Function>()
    var parentVariable: Variable? = null
    var top = true
    var nameResolvedCheck = false
    var nameResolvedGet = false
    var nameResolvedAllowCrash = false

    fun update(child: Context, visibility: DataStructVisibility, import: Boolean = false, alias: Identifier? = null){
        synchronized(if (child.hashCode() < hashCode()){child}else{this}){
            synchronized(if (child.hashCode() < hashCode()){this}else{child}) {
                val base = alias ?: child.currentFolder
                child.variables.getTopLevel()
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .map { (k, v) -> base.append(k) to v }
                    .map { (k, v) -> update(k, v, import) }
                child.functions.getTopLevel()
                    .map { (k, v) -> k to v.filter { it.isVisible(visibility, this) } }
                    .filter { (_, v) -> v.isNotEmpty() }
                    .map { (k, v) -> base.append(k) to v }
                    .map { (k, v) -> v.forEach { update(k, it, true) } }
                child.structs.getTopLevel()
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .map { (k, v) -> base.append(k) to v }
                    .map { (k, v) -> update(k, v) }
                child.classes.getTopLevel()
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .map { (k, v) -> base.append(k) to v }
                    .map { (k, v) -> update(k, v) }
                child.typedef.getTopLevel()
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .map { (k, v) -> base.append(k) to v }
                    .map { (k, v) -> update(k, v) }
            }
        }
    }

    //
    // UPDATE
    //
    fun update(id: Identifier, obj: Variable, import: Boolean = false){
        if (variables.hasKeyTopLevel(id) && !import) throw Exception("$id was already defined in scope")
        if (variables.hasKeyTopLevel(id) && import) return
        variables[id] = obj

        // Add Variable to Parent
        if (parentVariable != null && id.level() == 1) {
            parentVariable!!.childrenVariable[id] = obj
        }
    }
    fun update(id: Identifier, obj: Function, resolving: Boolean = false){
        if (!functions.hasKeyTopLevel(id)) {
            functions[id] = ArrayList()

            // Add Function List to Parent
            if (parentVariable != null) {
                parentVariable!!.childrenFunction[id] = functions[id]!!
            }
        }
        if (functions[id]!!.all{ it.name != obj.name}) {
            if (!resolving) {
                functionsList.add(obj)
            }
            functions[id]!!.add(obj)
        }
    }
    fun update(id: Identifier, obj: TypeDef, import: Boolean = false){
        if (typedef.hasKeyTopLevel(id) && !import) throw Exception("$id was already defined in scope")
        if (typedef.hasKeyTopLevel(id) && import) return
        typedef[id] = obj
    }
    fun update(id: Identifier, obj: Struct, import: Boolean = false){
        if (structs.hasKeyTopLevel(id) && !import) throw Exception("$id was already defined in scope")
        if (structs.hasKeyTopLevel(id) && import) return
        structs[id] = obj
    }
    fun update(id: Identifier, obj: Class, import: Boolean = false){
        if (classes.hasKeyTopLevel(id) && !import) throw Exception("$id was already defined in scope")
        if (classes.hasKeyTopLevel(id) && import) return
        classes[id] = obj
    }
    fun update(id: Identifier, obj: DataType){
        generics[id] = obj
    }
    fun printFunctions(){
        println(functions)
    }

    //
    // CHECK
    //
    fun hasVariable(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Boolean{
        nameResolvedCheck = true
        return variables[id, false] != null && variables[id, false]!!.modifier.visibility >= visibility
    }
    fun hasFunction(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Boolean{
        nameResolvedCheck = true
        return functions[id, false] != null && functions[id, false]!!.any { it.modifier.visibility >= visibility }
    }
    fun hasStruct(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Boolean{
        nameResolvedCheck = true
        return structs[id, false] != null && structs[id, false]!!.modifier.visibility >= visibility
    }
    fun hasClass(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Boolean{
        nameResolvedCheck = true
        return classes[id, false] != null && classes[id, false]!!.modifier.visibility >= visibility
    }
    fun hasGeneric(id: Identifier): Boolean{
        nameResolvedCheck = true
        return generics[id, false] != null
    }
    fun hasTypeDef(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Boolean{
        nameResolvedCheck = true
        return typedef[id, false] != null && typedef[id, false]!!.modifier.visibility >= visibility
    }

    //
    // GETTER
    //
    fun getVariable(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Variable {
        nameResolvedGet = true
        val ret = variables[id] ?: throw IdentifierNotFound(id)
        return if (ret.modifier.visibility >= visibility){
            ret
        } else { throw IdentifierNotFound(id)}
    }
    fun getFunction(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): List<Function> {
        nameResolvedGet = true
        val ret = (functions[id] ?: throw IdentifierNotFound(id)).filter { it.modifier.visibility >= visibility}
        return ret.ifEmpty { throw IdentifierNotFound(id) }
    }
    fun getClass(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Class {
        nameResolvedGet = true
        val ret = classes[id] ?: throw IdentifierNotFound(id)
        return if (ret.modifier.visibility >= visibility){
            ret
        } else { throw IdentifierNotFound(id)}
    }
    fun getStruct(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Struct {
        nameResolvedGet = true
        val ret = structs[id] ?: throw IdentifierNotFound(id)
        return if (ret.modifier.visibility >= visibility){
            ret
        } else { throw IdentifierNotFound(id)}
    }
    fun getGeneric(id: Identifier): DataType {
        nameResolvedGet = true
        return generics[id] ?: throw IdentifierNotFound(id)
    }
    fun getTypeDef(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): TypeDef {
        nameResolvedGet = true
        val ret = (typedef[id] ?: throw IdentifierNotFound(id))
        return if (ret.modifier.visibility >= visibility){
            ret
        } else { throw IdentifierNotFound(id)}
    }

    private fun getOverloadNumber(id: Identifier): Int{
        return if (!functions.hasKeyTopLevel(id)) {
            0
        } else {
            functions[id]!!.size
        }
    }
    fun getUniqueFunctionIdentifier(id: Identifier):Identifier{
        val nb = getOverloadNumber(id)
        return if (nb == 0){
            id
        }else{
            id.toUnique("\$${nb}")
        }
    }


    fun sub(id: String):Context{
        val context = Context(id, compiler)
        context.currentPath       = currentPath.sub(id)
        context.variables         = variables.sub()
        context.functions         = functions.sub()
        context.structs           = structs.sub()
        context.classes           = classes.sub()
        context.generics          = generics.sub()
        context.unfinishedAnalyse = unfinishedAnalyse
        context.functionsList     = functionsList
        context.currentFunction   = currentFunction
        children.add(context)
        return context
    }



    fun resolve(){
        while(children.size > 0){
            update(children.last(), DataStructVisibility.PROTECTED)
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
                    if (!fct.modifier.lazy) {
                        val pre = c.currentFunction
                        c.currentFunction = fct
                        fct.body = func(fct.body, c)
                        c.currentFunction = pre
                    }
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