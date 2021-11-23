package context

import ast.*
import compiler.Compiler
import data_struct.Function
import data_struct.*
import data_struct.Enum


interface IContext {
    fun update(child: IContext, visibility: DataStructVisibility, import: Boolean = false, alias: Identifier? = null)

    //
    // UPDATE
    //
    fun update(id: Identifier, obj: Variable, import: Boolean = false)
    fun update(id: Identifier, obj: Function, resolving: Boolean = false)
    fun update(id: Identifier, obj: TypeDef, import: Boolean = false)
    fun update(id: Identifier, obj: Struct, import: Boolean = false)
    fun update(id: Identifier, obj: Class, import: Boolean = false)
    fun update(id: Identifier, obj: Enum, import: Boolean = false)
    fun update(id: Identifier, obj: DataType)

    //
    // CHECK
    //
    fun hasVariable(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Boolean
    fun hasFunction(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Boolean
    fun hasStruct(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Boolean
    fun hasClass(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Boolean
    fun hasEnum(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Boolean
    fun hasEnumValue(id: Identifier):Boolean
    fun hasTypeDef(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Boolean
    fun hasGeneric(id: Identifier): Boolean

    //
    // GETTER
    //
    fun getVariable(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Variable
    fun getFunction(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): List<Function>
    fun getStruct(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Struct
    fun getClass(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Class
    fun getEnum(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): Enum
    fun getEnumValue(id: Identifier):List<Triple<Enum.Case, Int, Enum>>
    fun getTypeDef(id: Identifier, visibility: DataStructVisibility = DataStructVisibility.PRIVATE): TypeDef
    fun getGeneric(id: Identifier): DataType

    fun sub(id: String): IContext
    fun withPath(id: Identifier): IContext
    fun resolve()

    fun getCompiler(): Compiler
    fun getOwnerPackage(): String

    fun getUniqueFunctionIdentifier(id: Identifier):Identifier
    fun runUnfinished(func: (Statement, IContext)-> Statement)
    fun addUnfinished(function: Function, context: IContext): Function
    fun getNewFunctionsList(): List<Function>

    fun getLambdaFunction(type: FuncType, run: (Statement, IContext)->Statement): Function
    fun addLambda(fct: Function, run: (Statement, IContext)->Statement): Function
    fun getLambdasResolver():HashMap<FuncType, Function>
    fun getLambdas():HashMap<FuncType, ArrayList<Function>>
    fun freshLambdaName():Identifier
    fun getTmpVarIdentifier():Identifier

    fun getCurrentPath(): Identifier

    fun getCurrentFunction(): Function?
    fun setCurrentFunction(fct: Function?)

    fun getParentVariable(): Variable?
    fun setParentVariable(variable: Variable?)

    fun areNameCrashAllowed(): Boolean
    fun allowNameCrash(v: Boolean)

    fun nameResolvedCheck()
    fun hasNameResolvedCheck():Boolean

    fun nameResolvedGet()
    fun hasNameResolvedGet():Boolean

    fun resetState()

    fun isLib():Boolean
    fun setLib()

    fun setDone()
}