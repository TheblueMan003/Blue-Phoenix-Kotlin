package ast

enum class AssignmentType(val op: String){
    SET("="),
    ADD("+="),
    SUB("-="),
    MUL("*="),
    DIV("/="),
    MOD("%"),
    POW("^=")
}
enum class ReturnType{
    NONE,
    HALF,
    FULL
}
fun toReturnType(v1: Statement):ReturnType{
    return if (v1 is Splitter) v1.hasReturn else ReturnType.NONE
}
fun mergeReturnType(v1: Statement, v2: Statement):ReturnType{
    return mergeReturnType(toReturnType(v1), toReturnType(v2))
}
fun mergeReturnType(v1: ReturnType, v2: ReturnType):ReturnType{
    return if (v1 == ReturnType.FULL && v2 == ReturnType.FULL){
        ReturnType.FULL
    }
    else if (v1 == ReturnType.NONE && v2 == ReturnType.NONE){
        ReturnType.NONE
    }
    else {
        ReturnType.HALF
    }
}

abstract class Statement{
    var position: Int = 0
    var file: String = ""
    fun setPos(p: Int, f: String):Statement{
        position = p
        file = f
        return this
    }
}

class Empty : Statement()

/**
 * Class that split the execution (If, Block, ...)
 */
abstract class Splitter : Statement(){
    var hasReturn: ReturnType = ReturnType.NONE
    fun withParent(parent: Splitter):Statement{
        hasReturn = parent.hasReturn
        return this
    }
}
data class If(val Condition: Expression, val IfBlock: Statement): Splitter()
data class IfElse(val Condition: Expression, val IfBlock: Statement, val ElseBlock: Statement): Splitter()

/**
 * List of Instruction
 */
data class Block(val statements: List<Statement>): Splitter(){
    fun toSequence():Sequence{
        return Sequence(statements)
    }
}

/**
 * Block that doesn't change context
 */
data class Sequence(val statements: List<Statement>): Statement()


data class Case(val expr: Expression, val statement: Statement): Statement()
data class Switch(val function: Expression, val cases: List<Case>): Splitter()

/**
 *  Variable Before after Name Analysis
 */
data class UnlinkedVariableAssignment(var identifier: Identifier, val expr: Expression, val op: AssignmentType): Expression()

/**
 *  Variable Declaration
 */
data class VariableDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val type: DataType, val parent: Variable? = null): Statement()

/**
 * Function Arguments
 */
data class FunctionArgument(val identifier: Identifier, val type: DataType, val defaultValue: Expression?)

/**
 * Function Declaration
 */
data class FunctionDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val from: List<FunctionArgument>,
                               val to: DataType, val body: Statement, val parent: Variable? = null): Statement()

/**
 * Struct Declaration
 */
data class StructDeclaration(val modifier: DataStructModifier, val identifier: Identifier, val generic: List<DataType>?,
                             val fields: List<VariableDeclaration>, val methods: List<FunctionDeclaration>,
                             val builder: Sequence
): Statement()

/**
 * Lambda Declaration
 */
data class LambdaDeclaration(val from: List<DataType>, val to: DataType, val body: Statement): Expression()


data class UnlinkedReturnStatement(val expr: Expression): Statement()

data class StatementThanExpression(val statement: Statement, val expr: Expression): Expression()