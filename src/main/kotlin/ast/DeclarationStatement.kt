package ast

import data_struct.DataStructModifier
import data_struct.Enum
import data_struct.Variable


/**
 *  Variable Declaration
 */
data class VariableDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val type: DataType,
                               val parent: Variable? = null, val tmp: Boolean = false): Statement(){
    override fun toString(): String {
        return "VariableDeclaration($modifier $type $identifier)"
    }
}


/**
 * Function Arguments
 */
data class FunctionArgument(val modifier: DataStructModifier, val identifier: Identifier, val type: DataType, val defaultValue: Expression?){
    override fun toString(): String {
        return "FunctionArgument($modifier $type $identifier)"
    }
}


/**
 * Function Declaration
 */
data class FunctionDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val from: List<FunctionArgument>,
                               val to: DataType, val body: Statement, val parent: Variable? = null): Statement(){
    override fun toString(): String {
        return "FunctionDeclaration($modifier $to $identifier($from){$body})"
    }
}


/**
 * Enum Declaration
 */
data class EnumDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val fields: List<FunctionArgument>,
                           val entries: List<Enum.Case>): Statement(){
    override fun toString(): String {
        return "EnumDeclaration($modifier $identifier($fields){$entries})"
    }
}


/**
 * Struct Declaration
 */
data class StructDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val generic: List<DataType>?,
                             val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
                             val builder: Sequence
): Statement(){
    override fun toString(): String {
        return "StructDeclaration($modifier $identifier(fields: $fields builders: $builder methods: $methods))"
    }
}


/**
 *  Typedef Declaration
 */
data class TypeDefDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val type: DataType, val parent: Variable? = null): Statement(){
    override fun toString(): String {
        return "TypeDefDeclaration($modifier $type $identifier)"
    }
}


/**
 * Lambda Declaration
 */
data class LambdaDeclaration(val from: List<DataType>, val to: DataType, val body: Statement): Expression(){
    override fun toString(): String {
        return "LambdaDeclaration($from->$to {$body})"
    }
}

/**
 * Lambda (No Arg, No Out) Declaration
 */
data class ShortLambdaDeclaration(val body: Statement): Expression(){
    override fun toString(): String {
        return "ShortLambdaDeclaration({$body})"
    }
}
