package ast

enum class AssignmentType(val op: String){
    SET("="),
    ADD("+="),
    SUB("-="),
    MUL("*="),
    DIV("/="),
    MOD("%"),
    POW("^="),
    NOTNULL("?")
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

    override fun toString(): String {
        return javaClass.name
    }

    override fun hashCode(): Int {
        return toString().hashCode()
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

data class If(val Condition: Expression, val IfBlock: Statement): Splitter(){
    override fun toString(): String {
        return "If($Condition){$IfBlock}"
    }
}
data class IfElse(val Condition: Expression, val IfBlock: Statement, val ElseBlock: Statement): Splitter(){
    override fun toString(): String {
        return "IfElse($Condition){$IfBlock}else{$ElseBlock}"
    }
}

/**
 * List of Instruction
 */
data class Block(val statements: List<Statement>): Splitter(){
    fun toSequence():Sequence{
        return Sequence(statements)
    }

    override fun toString(): String {
        return "{$statements}"
    }
}

/**
 * Block that doesn't change context
 */
data class Sequence(val statements: List<Statement>): Statement(){
    override fun toString(): String {
        return "($statements)"
    }
}


data class Case(val expr: Expression, val statement: Statement): Statement(){
    override fun toString(): String {
        return "$expr -> $statement"
    }
}
data class Switch(val scrutinee: Expression, val cases: List<Case>, val forgenerate: List<Forgenerate>): Splitter(){
    override fun toString(): String {
        return "switch($scrutinee){$cases}"
    }
}

/**
 *  Variable Before after Name Analysis
 */
data class UnlinkedVariableAssignment(var identifier: Identifier, val expr: Expression, val op: AssignmentType): Expression(){
    override fun toString(): String {
        return "UnlinkedVariableAssignment($identifier $op $expr)"
    }
}


data class UnlinkedReturnStatement(val expr: Expression): Statement(){
    override fun toString(): String {
        return "UnlinkedReturnStatement($expr)"
    }
}


data class StatementThanExpression(val statement: Statement, val expr: Expression): Expression(){
    override fun toString(): String {
        return "StatementThanExpression($statement $expr)"
    }
}

data class RawCommand(val cmd: String): Statement(){
    override fun toString(): String {
        return "RawCommand($cmd)"
    }
}

data class RawCommandArg(val cmd: String, val args: List<Expression>): Statement(){
    override fun toString(): String {
        return "RawCommandArg($cmd, $args)"
    }
}

data class Import(val identifier: Identifier, val alias: Identifier? = null): Statement(){
    override fun toString(): String {
        return "Import($identifier)"
    }
}

data class FromImport(val resource: List<Identifier>, val identifier: Identifier, val alias: Identifier? = null): Statement()
{
    override fun toString(): String {
        return "FromImport($resource, $identifier)"
    }
}

abstract class Forgenerate(): Statement()
data class UnlinkedForgenerate(val identifier: Identifier, val generator: Expression, val body: Block): Forgenerate()
data class LinkedForgenerate(val identifier: Identifier, val generator: IGenerator, val body: Block): Forgenerate()