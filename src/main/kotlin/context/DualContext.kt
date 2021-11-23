package context

import ast.DataType
import ast.FuncType
import ast.Identifier
import ast.Statement
import compiler.Compiler
import data_struct.*
import data_struct.Enum
import data_struct.Function

class DualContext(val get: IContext, val set: IContext): IContext {
    override fun update(child: IContext, visibility: DataStructVisibility, import: Boolean, alias: Identifier?) {
        set.update(child, visibility, import, alias)
    }
    override fun update(id: Identifier, obj: Variable, import: Boolean) {
        set.update(id, obj, import)
    }
    override fun update(id: Identifier, obj: Function, resolving: Boolean) {
        set.update(id, obj, resolving)
    }
    override fun update(id: Identifier, obj: TypeDef, import: Boolean) {
        set.update(id, obj, import)
    }
    override fun update(id: Identifier, obj: Struct, import: Boolean) {
        set.update(id, obj, import)
    }
    override fun update(id: Identifier, obj: Class, import: Boolean) {
        set.update(id, obj, import)
    }
    override fun update(id: Identifier, obj: Enum, import: Boolean) {
        set.update(id, obj, import)
    }
    override fun update(id: Identifier, obj: DataType) {
        set.update(id, obj)
    }


    override fun hasVariable(id: Identifier, visibility: DataStructVisibility): Boolean {
        return get.hasVariable(id, visibility) || set.hasVariable(id, visibility)
    }
    override fun hasFunction(id: Identifier, visibility: DataStructVisibility): Boolean {
        return get.hasFunction(id, visibility) || set.hasFunction(id, visibility)
    }
    override fun hasStruct(id: Identifier, visibility: DataStructVisibility): Boolean {
        return get.hasStruct(id, visibility) || set.hasStruct(id, visibility)
    }
    override fun hasClass(id: Identifier, visibility: DataStructVisibility): Boolean {
        return get.hasClass(id, visibility) || set.hasClass(id, visibility)
    }
    override fun hasEnum(id: Identifier, visibility: DataStructVisibility): Boolean {
        return get.hasEnum(id, visibility) || set.hasEnum(id, visibility)
    }
    override fun hasEnumValue(id: Identifier): Boolean {
        return get.hasEnumValue(id) || set.hasEnumValue(id)
    }
    override fun hasTypeDef(id: Identifier, visibility: DataStructVisibility): Boolean {
        return get.hasTypeDef(id, visibility) || set.hasTypeDef(id, visibility)
    }
    override fun hasGeneric(id: Identifier): Boolean {
        return set.hasGeneric(id) || get.hasGeneric(id)
    }

    override fun getVariable(id: Identifier, visibility: DataStructVisibility): Variable {
        return if (get.hasVariable(id, visibility)){
            get.getVariable(id, visibility)
        } else{
            set.getVariable(id, visibility)
        }
    }
    override fun getFunction(id: Identifier, visibility: DataStructVisibility): List<Function> {
        return if (get.hasFunction(id, visibility)) {
            get.getFunction(id, visibility)
        } else {
            set.getFunction(id, visibility)
        }
    }
    override fun getStruct(id: Identifier, visibility: DataStructVisibility): Struct {
        return if (get.hasStruct(id, visibility)) {
            get.getStruct(id, visibility)
        } else {
            set.getStruct(id, visibility)
        }
    }
    override fun getClass(id: Identifier, visibility: DataStructVisibility): Class {
        return if (get.hasClass(id, visibility)){
            get.getClass(id, visibility)
        } else {
            set.getClass(id, visibility)
        }
    }
    override fun getEnum(id: Identifier, visibility: DataStructVisibility): Enum {
        return if (get.hasEnum(id, visibility)){
            get.getEnum(id, visibility)
        } else {
            set.getEnum(id, visibility)
        }
    }
    override fun getEnumValue(id: Identifier): List<Triple<Enum.Case, Int, Enum>> {
        return if (get.hasEnumValue(id)){
            get.getEnumValue(id)
        } else {
            set.getEnumValue(id)
        }
    }
    override fun getTypeDef(id: Identifier, visibility: DataStructVisibility): TypeDef {
        return if (get.hasTypeDef(id, visibility)){
            get.getTypeDef(id, visibility)
        } else {
            set.getTypeDef(id, visibility)
        }
    }
    override fun getGeneric(id: Identifier): DataType {
        return if (set.hasGeneric(id)) {
            set.getGeneric(id)
        } else {
            get.getGeneric(id)
        }
    }

    override fun sub(id: String): IContext {
        return DualContext(get, set.sub(id))
    }

    override fun withPath(id: Identifier): IContext {
        return DualContext(get, set.withPath(id))
    }

    override fun resolve() {
        set.resolve()
    }

    override fun getCompiler(): Compiler {
        return set.getCompiler()
    }
    override fun getOwnerPackage(): String {
        return set.getOwnerPackage()
    }
    override fun getUniqueFunctionIdentifier(id: Identifier): Identifier {
        return set.getUniqueFunctionIdentifier(id)
    }
    override fun runUnfinished(func: (Statement, IContext) -> Statement) {
        return set.runUnfinished(func)
    }
    override fun addUnfinished(function: Function, context: IContext): Function {
        return set.addUnfinished(function, context)
    }
    override fun getNewFunctionsList(): List<Function> {
        return set.getNewFunctionsList()
    }

    override fun getLambdaFunction(type: FuncType, run: (Statement, IContext) -> Statement): Function {
        return set.getLambdaFunction(type, run)
    }
    override fun addLambda(fct: Function, run: (Statement, IContext) -> Statement): Function {
        return set.addLambda(fct, run)
    }

    override fun getLambdasResolver(): HashMap<FuncType, Function> {
        return set.getLambdasResolver()
    }
    override fun getLambdas(): HashMap<FuncType, ArrayList<Function>> {
        return set.getLambdas()
    }

    override fun freshLambdaName(): Identifier {
        return set.freshLambdaName()
    }
    override fun getTmpVarIdentifier(): Identifier {
        return set.getTmpVarIdentifier()
    }
    override fun getCurrentPath(): Identifier {
        return set.getCurrentPath()
    }
    override fun getCurrentFunction(): Function? {
        return set.getCurrentFunction()
    }
    override fun setCurrentFunction(fct: Function?) {
        return set.setCurrentFunction(fct)
    }
    override fun getParentVariable(): Variable? {
        return set.getParentVariable()
    }

    override fun setParentVariable(variable: Variable?) {
        return set.setParentVariable(variable)
    }

    override fun areNameCrashAllowed(): Boolean {
        return set.areNameCrashAllowed()
    }
    override fun allowNameCrash(v: Boolean) {
        return set.allowNameCrash(v)
    }

    override fun nameResolvedCheck() {
        return set.nameResolvedCheck()
    }
    override fun hasNameResolvedCheck(): Boolean {
        return set.hasNameResolvedCheck()
    }
    override fun nameResolvedGet() {
        return set.nameResolvedGet()
    }
    override fun hasNameResolvedGet(): Boolean {
        return set.hasNameResolvedGet()
    }

    override fun resetState() {
        return set.resetState()
    }
    override fun isLib(): Boolean {
        return set.isLib()
    }
    override fun setLib() {
        return set.setLib()
    }
    override fun setDone() {
        return set.setDone()
    }
}