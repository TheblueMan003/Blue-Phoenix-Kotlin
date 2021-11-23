package context

import ast.*
import data_struct.Function
import utils.StackedHashMap
import compiler.Compiler
import data_struct.*
import data_struct.Enum
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class Context(private val path: String, val com: Compiler): IContext{
    var m_currentPath: Identifier = Identifier(path.split('.'))
    var m_ownerPackage = path
    var m_currentFolder: Identifier = Identifier(path.split('.'))
    var children = ArrayList<Context>()
    var variables          : StackedHashMap<Identifier, Variable>             = StackedHashMap()
    var functions          : StackedHashMap<Identifier, ArrayList<Function>>  = StackedHashMap()
    var structs            : StackedHashMap<Identifier, Struct>               = StackedHashMap()
    var classes            : StackedHashMap<Identifier, Class>                = StackedHashMap()
    var enums              : StackedHashMap<Identifier, Enum>                 = StackedHashMap()
    var generics           : StackedHashMap<Identifier, DataType>             = StackedHashMap()
    var typedef            : StackedHashMap<Identifier, TypeDef>              = StackedHashMap()
    var m_lambdas          : HashMap<FuncType, ArrayList<Function>>           = HashMap()
    var m_lambdasResolver  : HashMap<FuncType, Function>                      = HashMap()
    var enumsList: HashSet<Enum>                                              = HashSet()
    var lambdasCount = 0


    private var m_unfinishedAnalyse = ArrayList<Pair<Function, IContext>>()
    private var tmpVarNb = 0
    private var m_currentFunction: Function? = null
    private var m_functionsList = ArrayList<Function>()
    private var m_newFunctionsList: MutableList<Function> = ArrayList<Function>()
    private var m_parentVariable: Variable? = null

    private var m_nameResolvedCheck = false
    private var m_nameResolvedGet = false
    private var m_nameResolvedAllowCrash = false
    private var m_isLib = false
    private var isDone = false

    //
    // UPDATE
    //
    override fun update(child: IContext, visibility: DataStructVisibility, import: Boolean, alias: Identifier?){
        child as Context
        synchronized(if (child.hashCode() < hashCode()){child}else{this}){
            synchronized(if (child.hashCode() < hashCode()){this}else{child}) {
                val base = alias ?: child.m_currentFolder
                child.variables.getTopLevel()
                    .filter { (_, v) -> v.ownerPackage == child.m_ownerPackage }
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .map { (k, v) -> base.append(k) to v }
                    .map { (k, v) -> update(k, v, import) }
                child.functions.getTopLevel()
                    .map { (k, v) -> k to v.filter { it.isVisible(visibility, this) && it.ownerPackage == child.m_ownerPackage } }
                    .filter { (_, v) -> v.isNotEmpty() }
                    .map { (k, v) -> base.append(k) to v }
                    .map { (k, v) -> v.forEach { update(k, it, true) } }
                child.structs.getTopLevel()
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .map { (k, v) -> base.append(k) to v }
                    .map { (k, v) -> update(k, v, import) }
                child.classes.getTopLevel()
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .map { (k, v) -> base.append(k) to v }
                    .map { (k, v) -> update(k, v, import) }
                child.enums.getTopLevel()
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .map { (k, v) -> base.append(k) to v }
                    .map { (k, v) -> enumsList.add(v);update(k, v, import) }
                child.typedef.getTopLevel()
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .filter { (_, v) -> v.isVisible(visibility, this) }
                    .map { (k, v) -> base.append(k) to v }
                    .map { (k, v) -> update(k, v, import) }
            }
        }
    }
    override fun update(id: Identifier, obj: Variable, import: Boolean){
        if (variables.hasKeyTopLevel(id) && !import) throw Exception("$id was already defined in scope")
        if (variables.hasKeyTopLevel(id) && import) return
        variables[id] = obj

        if (obj.ownerPackage == null) obj.ownerPackage = m_ownerPackage

        // Add Variable to Parent
        if (m_parentVariable != null && id.level() == 1) {
            m_parentVariable!!.childrenVariable[id] = obj
        }
    }
    override fun update(id: Identifier, obj: Function, resolving: Boolean){
        if (!functions.hasKeyTopLevel(id)) {
            functions[id] = ArrayList()

            // Add Function List to Parent
            if (m_parentVariable != null) {
                m_parentVariable!!.childrenFunction[id] = functions[id]!!
            }
        }
        if (functions[id]!!.all{ it.name != obj.name}) {
            if (!resolving) {
                m_functionsList.add(obj)
            }
            functions[id]!!.add(obj)
        }
        if (obj.ownerPackage == null) obj.ownerPackage = m_ownerPackage
    }
    override fun update(id: Identifier, obj: TypeDef, import: Boolean){
        if (typedef.hasKeyTopLevel(id) && !import) throw Exception("$id was already defined in scope")
        if (typedef.hasKeyTopLevel(id) && import) return
        typedef[id] = obj
        if (obj.ownerPackage == null) obj.ownerPackage = m_ownerPackage
    }
    override fun update(id: Identifier, obj: Struct, import: Boolean){
        if (structs.hasKeyTopLevel(id) && !import) throw Exception("$id was already defined in scope")
        if (structs.hasKeyTopLevel(id) && import) return
        structs[id] = obj
        if (obj.ownerPackage == null) obj.ownerPackage = m_ownerPackage
    }
    override fun update(id: Identifier, obj: Class, import: Boolean){
        if (classes.hasKeyTopLevel(id) && !import) throw Exception("$id was already defined in scope")
        if (classes.hasKeyTopLevel(id) && import) return
        classes[id] = obj
        if (obj.ownerPackage == null) obj.ownerPackage = m_ownerPackage
    }
    override fun update(id: Identifier, obj: Enum, import: Boolean){
        if (enums.hasKeyTopLevel(id) && !import) throw Exception("$id was already defined in scope")
        if (enums.hasKeyTopLevel(id) && import) return
        enumsList.add(obj)
        enums[id] = obj
        if (obj.ownerPackage == null) obj.ownerPackage = m_ownerPackage
    }
    override fun update(id: Identifier, obj: DataType){
        generics[id] = obj
    }


    //
    // CHECK
    //
    override fun hasVariable(id: Identifier, visibility: DataStructVisibility): Boolean{
        m_nameResolvedCheck = true
        return (variables[id, false] != null && variables[id, false]!!.modifier.visibility >= visibility)
    }
    override fun hasFunction(id: Identifier, visibility: DataStructVisibility): Boolean{
        m_nameResolvedCheck = true
        return functions[id, false] != null && functions[id, false]!!.any { it.modifier.visibility >= visibility }
    }
    override fun hasStruct(id: Identifier, visibility: DataStructVisibility): Boolean{
        m_nameResolvedCheck = true
        return structs[id, false] != null && structs[id, false]!!.modifier.visibility >= visibility
    }
    override fun hasClass(id: Identifier, visibility: DataStructVisibility): Boolean{
        m_nameResolvedCheck = true
        return classes[id, false] != null && classes[id, false]!!.modifier.visibility >= visibility
    }
    override fun hasEnum(id: Identifier, visibility: DataStructVisibility): Boolean{
        m_nameResolvedCheck = true
        return enums[id, false] != null && enums[id, false]!!.modifier.visibility >= visibility
    }
    override fun hasEnumValue(id: Identifier):Boolean{
        return enumsList.any { it.hasField(id) }
    }
    override fun hasTypeDef(id: Identifier, visibility: DataStructVisibility): Boolean{
        m_nameResolvedCheck = true
        return typedef[id, false] != null && typedef[id, false]!!.modifier.visibility >= visibility
    }
    override fun hasGeneric(id: Identifier): Boolean{
        m_nameResolvedCheck = true
        return generics[id, false] != null
    }

    //
    // GETTER
    //
    override fun getVariable(id: Identifier, visibility: DataStructVisibility): Variable {
        m_nameResolvedGet = true
        val ret = variables[id] ?: throw IdentifierNotFound(id)

        return if (ret.modifier.visibility >= visibility){
            ret
        } else {
            throw IdentifierNotFound(id)
        }
    }
    override fun getFunction(id: Identifier, visibility: DataStructVisibility): List<Function> {
        m_nameResolvedGet = true
        val ret = (functions[id] ?: throw IdentifierNotFound(id)).filter { it.modifier.visibility >= visibility}
        return ret.ifEmpty { throw IdentifierNotFound(id) }
    }
    override fun getStruct(id: Identifier, visibility: DataStructVisibility): Struct {
        m_nameResolvedGet = true
        val ret = structs[id] ?: throw IdentifierNotFound(id)

        return if (ret.modifier.visibility >= visibility){
            ret
        } else {
            throw IdentifierNotFound(id)
        }
    }
    override fun getClass(id: Identifier, visibility: DataStructVisibility): Class {
        m_nameResolvedGet = true
        val ret = classes[id] ?: throw IdentifierNotFound(id)

        return if (ret.modifier.visibility >= visibility){
            ret
        } else {
            throw IdentifierNotFound(id)
        }
    }
    override fun getEnum(id: Identifier, visibility: DataStructVisibility): Enum {
        m_nameResolvedGet = true
        val ret = enums[id] ?: throw IdentifierNotFound(id)
        return if (ret.modifier.visibility >= visibility){
            ret
        } else {
            throw IdentifierNotFound(id)
        }
    }
    override fun getEnumValue(id: Identifier):List<Triple<Enum.Case, Int, Enum>>{
        val enum = enumsList.filter { it.hasField(id) }
        return enum.map{Triple(it.getField(id), it.getFieldIndex(id), it)}
    }
    override fun getTypeDef(id: Identifier, visibility: DataStructVisibility): TypeDef {
        m_nameResolvedGet = true
        val ret = (typedef[id] ?: throw IdentifierNotFound(id))

        return if (ret.modifier.visibility >= visibility){
            ret
        } else {
            throw IdentifierNotFound(id)
        }
    }
    override fun getGeneric(id: Identifier): DataType {
        m_nameResolvedGet = true
        return generics[id] ?: throw IdentifierNotFound(id)
    }



    private fun getOverloadNumber(id: Identifier): Int{
        return if (!functions.hasKeyTopLevel(id)) {
            0
        } else {
            functions[id]!!.size
        }
    }
    override fun getUniqueFunctionIdentifier(id: Identifier):Identifier{
        val nb = getOverloadNumber(id)
        return if (nb == 0){
            id
        }else{
            id.toUnique("\$${nb}")
        }
    }


    override fun sub(id: String): IContext {
        val context = Context(id, com)
        context.m_currentPath       = m_currentPath.sub(id)
        context.m_ownerPackage      = m_ownerPackage
        context.variables         = variables.sub()
        context.functions         = functions.sub()
        context.structs           = structs.sub()
        context.classes           = classes.sub()
        context.generics          = generics.sub()
        context.m_unfinishedAnalyse = m_unfinishedAnalyse
        context.m_functionsList     = m_functionsList
        context.m_currentFunction   = m_currentFunction
        context.m_nameResolvedAllowCrash = m_nameResolvedAllowCrash
        context.enumsList = enumsList
        children.add(context)
        return context
    }
    override fun withPath(id: Identifier): IContext {
        val s = sub("_") as Context
        s.m_currentPath = id
        return s
    }
    override fun resolve(){
        while(children.size > 0){
            update(children.last(), DataStructVisibility.PROTECTED)
            m_nameResolvedCheck = m_nameResolvedCheck || children.any{it.m_nameResolvedCheck}
            m_nameResolvedGet = m_nameResolvedGet || children.any{it.m_nameResolvedGet}
            children.removeLast()
        }
    }

    override fun getCompiler(): Compiler {
        return com
    }

    override fun getOwnerPackage(): String {
        return m_ownerPackage
    }


    override fun addUnfinished(function: Function, context: IContext): Function {
        m_unfinishedAnalyse.add(Pair(function, context))
        return function
    }
    override fun runUnfinished(func: (Statement, IContext)-> Statement){
        m_newFunctionsList.clear()

        while (m_unfinishedAnalyse.isNotEmpty()){
            val unfinished = ArrayList(m_unfinishedAnalyse)
            m_unfinishedAnalyse.clear()
            m_newFunctionsList = unfinished.map { it.first }.toMutableList()
            unfinished.forEach { (fct, c) ->
                run {
                    if (!fct.modifier.lazy) {
                        val pre = c.getCurrentFunction()
                        c.setCurrentFunction(fct)
                        fct.body = func(fct.body, c)
                        c.setCurrentFunction(pre)
                    }
                }
            }
        }
    }
    override fun getNewFunctionsList(): List<Function> {
        return m_newFunctionsList
    }

    override fun getTmpVarIdentifier():Identifier{
        val nb = tmpVarNb++
        return Identifier(listOf("\$tmp_$nb"))
    }

    override fun getCurrentPath(): Identifier {
        return m_currentPath
    }

    override fun getCurrentFunction(): Function? {
        return m_currentFunction
    }
    override fun setCurrentFunction(fct: Function?) {
        m_currentFunction = fct
    }
    override fun getParentVariable(): Variable? {
        return m_parentVariable
    }
    override fun setParentVariable(variable: Variable?) {
        m_parentVariable = variable
    }

    override fun areNameCrashAllowed(): Boolean {
        return m_nameResolvedAllowCrash
    }
    override fun allowNameCrash(v: Boolean) {
        m_nameResolvedAllowCrash = v
    }
    override fun nameResolvedCheck() {
        m_nameResolvedCheck = true
    }
    override fun hasNameResolvedCheck(): Boolean {
        return m_nameResolvedCheck
    }
    override fun nameResolvedGet() {
        m_nameResolvedGet = true
    }
    override fun hasNameResolvedGet(): Boolean {
        return m_nameResolvedGet
    }

    override fun addLambda(fct: Function, run: (Statement, IContext)->Statement): Function {
        val type = FuncType(fct.from.map { it.type }, fct.output.type)
        val ret = getLambdaFunction(type, run)
        m_lambdas[type]!!.add(fct)
        return ret
    }
    override fun getLambdasResolver(): HashMap<FuncType, Function> {
        return m_lambdasResolver
    }
    override fun getLambdas(): HashMap<FuncType, ArrayList<Function>> {
        return m_lambdas
    }
    override fun getLambdaFunction(type: FuncType, run: (Statement, IContext)->Statement): Function {
        if (!m_lambdas.containsKey(type)){
            m_lambdas[type] = ArrayList()
            val id = Identifier(listOf(typeToName(type.toString())))
            val args = type.from.filter { it !is VoidType }.mapIndexed { index, it ->
                FunctionArgument(
                    DataStructModifier.newPrivate(),
                    Identifier(listOf("_$index")),
                    it, null)}.toMutableList()

            args.add(0, FunctionArgument(DataStructModifier.newPrivate(), Identifier(listOf("fct")), type,null))

            synchronized(com.mainContext) {
                run(
                    FunctionDeclaration(DataStructModifier.newPrivate(), id, args, type.to, Block(listOf(Empty()))),
                    com.mainContext
                )
                m_lambdasResolver[type] = com.mainContext.functions[id]!![0]
            }
        }
        return m_lambdasResolver[type]!!
    }
    override fun freshLambdaName():Identifier{
        return Identifier(listOf("lambda_${lambdasCount++}"))
    }

    private fun typeToName(value: String):String{
        return "lambda_"+value.replace(Regex("[() \\[\\]]"),"_").replace("=>","to")
    }

    override fun resetState(){
        m_nameResolvedCheck = false
        m_nameResolvedGet = false
        isDone = false
    }

    override fun isLib(): Boolean {
        return m_isLib
    }
    override fun setLib() {
        m_isLib = true
    }

    override fun setDone() {
        isDone = true
    }


    data class IdentifierNotFound(val identifier: Identifier): Exception()
}