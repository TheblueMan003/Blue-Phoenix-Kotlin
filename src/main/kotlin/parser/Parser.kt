package parser

import analyzer.Context
import ast.*
import ast.DataStructVisibility.*
import java.rmi.UnexpectedException
import kotlin.collections.ArrayList


private val binaryOperationOrder = listOf("&&", "||","<", "<=", ">", ">=","+", "-", "*", "/", "%", "^",)
private val unaryOperationOrder = listOf("-", "!")


fun parse(tokens: TokenStream):Statement{
    val statements = ArrayList<Statement>()
    while (!tokens.isEmpty()){
        statements.add(parseBlock(tokens))
    }
    return if (statements.size > 0) { Sequence(statements)} else { Empty()}
}


/**
 * Parse Statement
 */
private fun parseBlock(tokens: TokenStream): Statement {
    if (isKeyword(tokens, "import")){
        return Empty()
    }
    val modifier = DataStructModifier()


    // If
    if (isKeyword(tokens, "if")){ return parseIf(tokens) }

    // RawCommand
    if (isRawCommand(tokens)) { return RawCommand(getRawCommand(tokens))}

    // Switch
    if (isKeyword(tokens, "switch")){ return parseSwitch(tokens) }

    // Return
    if (isKeyword(tokens, "return")){ return UnlinkedReturnStatement(parseExpression(tokens)) }

    // Blocks
    if (isDelimiter(tokens,"{")){ return parseBlockGroup(tokens) }

    // Variable Assignment / Function Call
    if (isIdentifier(tokens)){
        val state = tokens.getState()
        val identifier = parseIdentifier(tokens)
        if (isDelimiterNoConsume(tokens, "(")){
            return parseFunctionCall(tokens, identifier)
        } else if (isOperationTokenNoConsume(tokens, listOf("+","*","-","/","%","=","^"))) {
            return parseVariableAssignment(tokens, identifier)
        } else {
            tokens.restoreState(state)
        }
    }


    parseModifier(tokens, modifier)

    if (isKeyword(tokens, "typedef")){
        return parseTypeDef(tokens, modifier)
    }
    else if (isKeyword(tokens, "struct")) {
        return parseStructDeclaration(tokens, modifier)
    }
    else if (isKeyword(tokens, "class")) {
        return Empty()
    }
    else if (isType(tokens)){
        val type = parseType(tokens)
        val identifier = parseIdentifier(tokens)

        // Function
        return if (isDelimiter(tokens, "(")){
            parseFunctionDeclaration(tokens, identifier, type, modifier)
        } else{
            parseVariableDeclaration(tokens, identifier, type, modifier)
        }
    }

    // Error No Token Match Found
    if (tokens.isEmpty()){
        throw UnexpectedException("Unexpected EOF")
    }
    throw UnexpectedException("Unknown token:"+tokens.peekString()+" at pos: "+tokens.peekPos())
}

fun parseTypeDef(tokens: TokenStream, modifier: DataStructModifier): Statement {
    val type = parseType(tokens)
    val id = parseIdentifier(tokens)
    return TypeDefDeclaration(modifier, id, type)
}


private fun parseStructDeclaration(tokens: TokenStream, modifier: DataStructModifier): StructDeclaration {
    val identifier = parseIdentifier(tokens)
    val generics = parseGenerics(tokens)
    val fields = ArrayList<VariableDeclaration>()
    val methods = ArrayList<FunctionDeclaration>()
    val builders = ArrayList<Statement>()
    expectDelimiter(tokens, "{")
    fun acceptStatement(stm: Statement){
        when(stm){
            is Block -> {
                for (s in stm.statements){
                    when(s){
                        is VariableDeclaration -> { fields.add(s) }
                        is UnlinkedVariableAssignment -> { builders.add(s) }
                        else -> throw Exception("Invalid Statement in Struct")
                    }
                }
            }
            is Sequence -> {
                for (s in stm.statements){
                    when(s){
                        is VariableDeclaration -> { fields.add(s) }
                        is UnlinkedVariableAssignment -> { builders.add(s) }
                        else -> throw Exception("Invalid Statement in Struct")
                    }
                }
            }
            is VariableDeclaration -> { fields.add(stm) }
            is FunctionDeclaration -> { methods.add(stm) }
            is Empty -> {}
            else -> throw Exception("Invalid Statement in Struct $stm")
        }
    }

    // Parse Struct Body
    while(!isDelimiter(tokens, "}")){
        acceptStatement(parseBlock(tokens))
    }
    return StructDeclaration(modifier, identifier, generics, fields, methods, Sequence(builders))
}



private fun parseFunctionDeclaration(tokens: TokenStream, identifier: Identifier,
                                     type: DataType, modifier: DataStructModifier
): FunctionDeclaration {
    val args = parseFunctionArgumentsList(tokens)
    val body = parseBlock(tokens)
    return FunctionDeclaration(modifier, identifier, args, type, body)
}



private fun parseVariableDeclaration(tokens: TokenStream, identifier: Identifier,
                                     type: DataType, modifier: DataStructModifier): Sequence {
    // Variable Declaration
    val vars = parseIdentifierList(tokens, identifier)

    val declarations = vars.map { VariableDeclaration(modifier, it, type) }
    if (isOperationToken(tokens, "=")){
        // Get Expression
        val expr = parseExpressionList(tokens)

        // Create assignments list
        val assignments: List<Statement> = if (expr.size == 1){
            vars.map { UnlinkedVariableAssignment(it, expr[0], AssignmentType.SET) }
        }else{
            vars.zip(expr).map { (v, expr) -> UnlinkedVariableAssignment(v, expr, AssignmentType.SET) }
        }
        if (type is VarType){
            type.expr = expr[0]
        }
        return Sequence(declarations + assignments)
    }
    else{
        if (type is VarType) throw Exception("Var must be assigned")
        return Sequence(declarations)
    }
}



private fun parseSwitch(tokens: TokenStream): Switch {
    expectDelimiter(tokens, "(")
    val scrut = parseExpression(tokens)
    expectDelimiter(tokens, ")")
    expectDelimiter(tokens, "{")
    val cases = ArrayList<Case>()

    // Get Cases
    while(!isDelimiter(tokens, "}")){
        val expr = parseExpression(tokens)
        expectDelimiter(tokens, "->")
        val block = parseBlock(tokens)
        cases.add(Case(expr, block))
    }

    val ret = Switch(scrut, cases)
    ret.hasReturn = cases.map { toReturnType(it.statement) }.reduce{ a, b -> mergeReturnType(a,b) }
    return ret
}



private fun parseIf(tokens: TokenStream): Statement {
    expectDelimiter(tokens, "(")
    val cond = parseExpression(tokens)
    expectDelimiter(tokens, ")")
    val blockIf = parseBlock(tokens)
    return if (isKeyword(tokens, "else")){
        val blockElse = parseBlock(tokens)
        val ret = IfElse(cond, blockIf, blockElse)
        ret.hasReturn = mergeReturnType(ret.IfBlock, ret.ElseBlock)
        ret
    } else{
        val ret = If(cond, blockIf)
        ret.hasReturn = ReturnType.HALF
        ret
    }
}


private fun parseBlockGroup(tokens: TokenStream): Block {
    val statements = ArrayList<Statement>()
    val statementsAfterReturn = ArrayList<Statement>()
    var returned = ReturnType.NONE
    while(!isDelimiter(tokens,"}")){
        assert(!tokens.isEmpty())
        val stm = parseBlock(tokens)
        if (returned != ReturnType.FULL) {
            if (returned == ReturnType.NONE) {
                statements.add(stm)
            } else {
                statementsAfterReturn.add(stm)
            }
            if (stm is UnlinkedReturnStatement) returned = ReturnType.FULL
            if (stm is Splitter) returned = stm.hasReturn
        }
    }
    return Block(statements+statementsAfterReturn)
}



private fun parseVariableAssignment(tokens: TokenStream, identifier: Identifier): Statement {
    val vars = parseIdentifierList(tokens, identifier)
    return if (isOperationToken(tokens, "=")){
        val expr = parseExpressionList(tokens)
        if (expr.size == 1){
            Sequence(vars.map { UnlinkedVariableAssignment(it, expr[0], AssignmentType.SET) })
        }else{
            Sequence(vars.zip(expr).map { (v, e) -> UnlinkedVariableAssignment(v, e, AssignmentType.SET) })
        }
    }
    else{
        val op = getOperationToken(tokens)
        if (getOperationToken(tokens)!="=") throw UnexpectedException("Unknown token: "+tokens.peekString()+" at pos: "+tokens.peekPos())
        val expr = parseExpressionList(tokens)
        val opType: AssignmentType = when(op){
            "+" -> AssignmentType.ADD
            "-" -> AssignmentType.SUB
            "*" -> AssignmentType.MUL
            "/" -> AssignmentType.DIV
            "%" -> AssignmentType.MOD
            "^" -> AssignmentType.POW
            else -> throw NotImplementedError()
        }
        if (expr.size == 1){
            Sequence(vars.map { UnlinkedVariableAssignment(it, expr[0], opType) })
        }else{
            Sequence(vars.zip(expr).map { (v, e) -> UnlinkedVariableAssignment(v, e, opType) })
        }
    }
}



private fun parseExpressionList(tokens: TokenStream): List<Expression>{
    val expr = ArrayList<Expression>()
    do {
        expr.add(parseExpression(tokens))
    }while(isDelimiter(tokens, ","))
    return expr
}



private fun parseIdentifierList(tokens: TokenStream, identifier:Identifier): List<Identifier> {
    val vars = ArrayList<Identifier>()
    vars.add(identifier)
    while(isDelimiter(tokens, ",")){
        vars.add(parseIdentifier(tokens))
    }
    return vars
}



private fun parseFunctionCall(tokens: TokenStream, identifier:Identifier): Expression {
    var called: Expression = IdentifierExpr(identifier)
    while(isDelimiter(tokens, "(")) {
        called = if (isDelimiter(tokens, ")")){
            val args = emptyList<Expression>()
            CallExpr(called, args)
        }else {
            val args = parseExpressionList(tokens)
            expectDelimiter(tokens, ")")
            CallExpr(called, args)
        }
    }
    return called
}



private fun parseFunctionArgumentsList(tokens: TokenStream): List<FunctionArgument>{
    val args = ArrayList<FunctionArgument>()
    if (!isDelimiter(tokens, ")")) {
        do {
            args.add(parseFunctionArguments(tokens))
        } while (isDelimiter(tokens, ","))
        isDelimiter(tokens, ")")
    }
    return args
}



private fun parseFunctionArguments(tokens: TokenStream): FunctionArgument {
    val modifier = DataStructModifier()
    parseModifier(tokens, modifier)
    modifier.visibility = PRIVATE

    val type = parseType(tokens)
    val identifier = parseIdentifier(tokens)

    return if (isOperationToken(tokens, "=")){
        val value =  parseExpression(tokens)
        FunctionArgument(modifier, identifier, type, value)
    }
    else{
        FunctionArgument(modifier, identifier, type, null)
    }
}



private fun parseModifier(tokens: TokenStream, modifier: DataStructModifier){
    // Data Struct
    if (isKeyword(tokens, "public")){ modifier.visibility = PUBLIC }
    else if (isKeyword(tokens, "protected")){ modifier.visibility = PROTECTED }
    else if (isKeyword(tokens, "private")){ modifier.visibility = PRIVATE }

    do {
        var found = false
        if (isKeyword(tokens, "abstract")) {
            modifier.abstract = true
            found = true
        }
        if (isKeyword(tokens, "static")) {
            modifier.static = true
            found = true
        }
        if (isKeyword(tokens, "lazy")) {
            modifier.lazy = true
            found = true
        }
        if (isKeyword(tokens, "operator")) {
            modifier.operator = true
            found = true
        }
    }while(found)
}



/**
 * Parse expression
 */
private fun parseExpression(tokens: TokenStream, index: Int = 0): Expression {
    fun under() = if (index+1 == binaryOperationOrder.size){
        parseSimpleExpression(tokens)
    } else{
        parseExpression(tokens, index + 1)
    }
    var left = under()
    while(isOperationToken(tokens, binaryOperationOrder[index])){
        left = BinaryExpr(binaryOperationOrder[index], left, under())
    }
    return left
}



/**
 * Parse expression without operator on the outside
 */
private fun parseSimpleExpression(tokens: TokenStream): Expression {
    // Literals
    if (isFloatLit(tokens)){
        return FloatLitExpr(getFloatLit(tokens))
    }
    if (isIntLit(tokens)){
        return IntLitExpr(getIntLit(tokens))
    }
    if (isBoolLit(tokens)){
        return BoolLitExpr(getBoolLit(tokens))
    }
    if (isStringLit(tokens)){
        return StringLitExpr(getStringLit(tokens))
    }

    // Parent
    if (isDelimiter(tokens, "(")){
        var expr = parseExpression(tokens)
        if (isDelimiter(tokens, ",")){
            val lst = listOf(expr, parseExpression(tokens)).toMutableList()
            while((isDelimiter(tokens, ","))){lst.add(parseExpression(tokens))}
            expr = TupleExpr(lst)
        }
        expectDelimiter(tokens,")")
        return expr
    }

    // Unary Operation
    for(op in unaryOperationOrder){
        if(isOperationToken(tokens, op)){
            return UnaryExpr(op, parseSimpleExpression(tokens))
        }
    }

    // Var or Fun
    if (isIdentifier(tokens)){
        val identifier = parseIdentifier(tokens)
        return if (isDelimiterNoConsume(tokens, "(")){
            parseFunctionCall(tokens, identifier)
        } else{
            IdentifierExpr(identifier)
        }
    }
    throw UnexpectedException("Unknown token: '"+tokens.peekString()+"' at pos: "+tokens.peekPos())
}



/**
 * Parse Type
 */
fun parseType(tokens: TokenStream): DataType {
    var type = parseSimpleType(tokens)
    while(isDelimiter(tokens, "[")){
        if (type is VarType) throw UnexpectedException("Var cannot be used as a Type in Arrays")
        expectDelimiter(tokens, "]")
        type = ArrayType(type, -1)
    }
    return type
}



/**
 * Parse Type Base
 */
private fun parseSimpleType(tokens: TokenStream): DataType {
    if(isDelimiter(tokens, "(")){
        val lst = ArrayList<DataType>()
        do{
            lst.add(parseType(tokens))
        }while(isDelimiter(tokens,","))
        expectDelimiter(tokens, ")")
        if (lst.size > 2 && lst.contains(VoidType())){
            throw UnexpectedException("Unexpected Void Type in Tuple")
        }
        if (lst.size > 1 && lst.contains(VarType())){
            throw UnexpectedException("Var cannot be used as a Type in Tuple")
        }
        if (isOperationToken(tokens, "=>")){
            return FuncType(lst, parseType(tokens))
        }
        return TupleType(lst)
    }
    if (isIdentifier(tokens)){
        val identifier = parseIdentifier(tokens)
        val generics = parseGenerics(tokens)
        return if (generics != null){
            UnresolvedGeneratedGenericType(identifier, generics)
        }
        else UnresolvedGeneratedType(identifier)
    }
    if (isPrimTypeToken(tokens,"int")){ return IntType() }
    if (isPrimTypeToken(tokens,"float")){ return FloatType() }
    if (isPrimTypeToken(tokens,"bool")){ return BoolType() }
    if (isPrimTypeToken(tokens,"string")){ return StringType() }
    if (isPrimTypeToken(tokens,"void")){ return VoidType() }
    if (isPrimTypeToken(tokens,"var")){ return VarType() }
    throw UnexpectedException("Unknown type: "+tokens.peekString()+" at pos: "+tokens.peekPos())
}



private fun parseGenerics(tokens: TokenStream): ArrayList<DataType>? {
    return if (isOperationToken(tokens, "<")) {
        val args = ArrayList<DataType>()
        do {
            args.add(parseType(tokens))
        } while (isDelimiter(tokens, ","))
        expectOperationToken(tokens, ">")
        if (args.size > 1 && args.contains(VarType())){
            throw UnexpectedException("Var cannot be used as a Type in Generics")
        }
        args
    } else {
        null
    }
}


private fun parseIdentifier(tokens: TokenStream): Identifier {
    val lst = ArrayList<String>()
    do {
        lst.add(getIdentifier(tokens))
    }while (isDelimiter(tokens,"."))
    return Identifier(lst)
}