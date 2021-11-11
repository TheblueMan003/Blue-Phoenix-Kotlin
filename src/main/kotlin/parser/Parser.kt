package parser

import parser.DataStructVisibility.*
import parser.data.Identifier
import java.rmi.UnexpectedException
import kotlin.collections.ArrayList


private val binaryOperationOrder = listOf("+", "-", "*", "/", "%", "^", "<", "<=", ">", ">=", "||", "&&")
private val unaryOperationOrder = listOf("-", "!")


fun parse(path: String, tokens: TokenStream):Pair<Statement, Context>{
    val statements = ArrayList<Statement>()
    val context = Context(path)
    while (!tokens.isEmpty()){
        statements.add(parseBlock(tokens, context))
        println("1")
    }
    return if (statements.size > 0) { Pair(Block(statements), context)} else { Pair(Empty(), context)}
}


/**
 * Parse Statement
 */
private fun parseBlock(tokens: TokenStream, context: Context):Statement {
    if (isKeyword(tokens, "import")){
        return Empty()
    }
    val modifier = DataStructModifier()


    // If
    if (isKeyword(tokens, "if")){ return parseIf(tokens, context) }

    // Switch
    if (isKeyword(tokens, "switch")){ return parseSwitch(tokens, context) }

    // Blocks
    if (isDelimiter(tokens,"{")){ parseBlockGroup(tokens, context) }

    // Variable Assignment / Function Call
    if (isIdentifier(tokens)){
        val identifier = parseIdentifier(tokens, context)
        return if (isDelimiterNoConsume(tokens, "(")){
            parseFunctionCall(tokens, context, identifier)
        } else { parseVariableAssignment(tokens, context, identifier) }
    }


    parseModifier(tokens, context, modifier)


    if (isKeyword(tokens, "struct")) {
        return parseStructDeclaration(tokens, context, modifier)
    }
    else if (isKeyword(tokens, "class")) {
        return Empty()
    }
    else if (isType(tokens)){
        val type = parseType(tokens, context)
        val identifier = parseIdentifier(tokens, context)

        // Function
        return if (isDelimiter(tokens, "(")){
            parseFunctionDeclaration(tokens, context, identifier, type, modifier)
        } else{
            parseVariableDeclaration(tokens, context, identifier, type, modifier)
        }
    }

    // Error No Token Match Found
    if (tokens.isEmpty()){
        throw UnexpectedException("Unexpected EOF")
    }
    throw UnexpectedException("Unknown token:"+tokens.peekString()+" at pos: "+tokens.peekPos())
}



private fun parseStructDeclaration(tokens: TokenStream, context: Context, modifier: DataStructModifier): StructDeclaration {
    val identifier = parseIdentifier(tokens, context)
    val generics = parseGenerics(tokens, context)
    val fields = ArrayList<VariableDeclaration>()
    val methods = ArrayList<FunctionDeclaration>()
    val builders = ArrayList<Statement>()
    expectDelimiter(tokens, "{")

    // Parse Struct Body
    while(!isDelimiter(tokens, "}")){
        when(val stm = parseBlock(tokens, context)){
            is Block -> {
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
            else -> throw Exception("Invalid Statement in Struct")
        }
    }
    return StructDeclaration(modifier, identifier, generics, fields, methods, Block(builders))
}



private fun parseFunctionDeclaration(tokens: TokenStream, context: Context, identifier: Identifier,
                                     type: DataType, modifier: DataStructModifier): FunctionDeclaration {
    val args = parseFunctionArgumentsList(tokens, context)
    val body = parseBlock(tokens, context)
    return FunctionDeclaration(modifier, identifier, args, type, body)
}



private fun parseVariableDeclaration(tokens: TokenStream, context: Context, identifier: Identifier,
                                     type: DataType, modifier: DataStructModifier): Block {
    // Variable Declaration
    val vars = parseIdentifierList(tokens, context, identifier)

    val declarations = vars.map { VariableDeclaration(modifier, it, type) }
    if (isOperationToken(tokens, "=")){
        // Get Expression
        val expr = parseExpressionList(tokens, context)

        // Create assignments list
        val assignments: List<Statement> = if (expr.size == 1){
            vars.map { UnlinkedVariableAssignment(it, expr[0]) }
        }else{
            vars.zip(expr).map { (v, expr) -> UnlinkedVariableAssignment(v, expr) }
        }

        return Block(declarations + assignments)
    }
    else{
        return Block(declarations)
    }
}



private fun parseSwitch(tokens: TokenStream, context: Context): Switch {
    expectDelimiter(tokens, "(")
    val scrut = parseExpression(tokens, context)
    expectDelimiter(tokens, ")")
    expectDelimiter(tokens, "{")
    val cases = ArrayList<Case>()

    // Get Cases
    while(!isDelimiter(tokens, "}")){
        val expr = parseExpression(tokens, context)
        expectDelimiter(tokens, "->")
        val block = parseBlock(tokens, context)
        cases.add(Case(expr, block))
    }

    return Switch(scrut, cases)
}



private fun parseIf(tokens: TokenStream, context: Context): Statement {
    expectDelimiter(tokens, "(")
    val cond = parseExpression(tokens, context)
    expectDelimiter(tokens, ")")
    val blockIf = parseBlock(tokens, context)
    return if (isKeyword(tokens, "else")){
        val blockElse = parseBlock(tokens, context)
        IfElse(cond, blockIf, blockElse)
    } else{
        If(cond, blockIf)
    }
}



private fun parseBlockGroup(tokens: TokenStream, context: Context): Block {
    val statements = ArrayList<Statement>()
    while(!isDelimiter(tokens,"}")){
        assert(!tokens.isEmpty())
        statements.add(parseBlock(tokens, context))
    }
    return Block(statements)
}



private fun parseVariableAssignment(tokens: TokenStream, context: Context, identifier: Identifier): Statement {
    val vars = parseIdentifierList(tokens, context, identifier)
    return if (isOperationToken(tokens, "=")){
        val expr = parseExpressionList(tokens, context)
        if (expr.size == 1){
            Block(vars.map { UnlinkedVariableAssignment(it, expr[0]) })
        }else{
            Block(vars.zip(expr).map { (v, e) -> UnlinkedVariableAssignment(v, e) })
        }
    }
    else{
        val op = getOperationToken(tokens)
        val expr = parseExpressionList(tokens, context)
        if (expr.size == 1){
            Block(vars.map { UnlinkedVariableAssignment(it, BinaryExpr(op, IdentifierExpr(it), expr[0])) })
        }else{
            Block(vars.zip(expr).map { (v, e) -> UnlinkedVariableAssignment(v, BinaryExpr(op, IdentifierExpr(v), e)) })
        }
    }
}



private fun parseExpressionList(tokens: TokenStream, context: Context): List<Expression>{
    val expr = ArrayList<Expression>()
    do {
        expr.add(parseExpression(tokens, context))
    }while(isDelimiter(tokens, ","))
    return expr
}



private fun parseIdentifierList(tokens: TokenStream, context: Context, identifier:Identifier): List<Identifier> {
    val vars = ArrayList<Identifier>()
    vars.add(identifier)
    while(isDelimiter(tokens, ",")){
        vars.add(parseIdentifier(tokens, context))
    }
    return vars
}



private fun parseFunctionCall(tokens: TokenStream, context: Context, identifier:Identifier): Expression{
    var called: Expression = IdentifierExpr(identifier)
    while(isDelimiter(tokens, "(")) {
        val args = parseExpressionList(tokens, context)
        expectDelimiter(tokens, ")")
        called = CallExpr(called, args)
    }
    return called
}



private fun parseFunctionArgumentsList(tokens: TokenStream, context: Context): List<FunctionArgument>{
    val args = ArrayList<FunctionArgument>()
    while(!isDelimiter(tokens, ")") || isDelimiter(tokens, ",")){
        args.add(parseFunctionArguments(tokens, context))
    }
    return args
}



private fun parseFunctionArguments(tokens: TokenStream, context: Context): FunctionArgument{
    val type = parseType(tokens, context)
    val identifier = parseIdentifier(tokens, context)
    return if (isOperationToken(tokens, "=")){
        val value =  parseExpression(tokens, context)
        FunctionArgument(identifier, type, value)
    }
    else{
        FunctionArgument(identifier, type, null)
    }
}



private fun parseModifier(tokens: TokenStream, context: Context, modifier: DataStructModifier){
    // Data Struct
    if (isKeyword(tokens, "public")){ modifier.visibility = PUBLIC }
    else if (isKeyword(tokens, "protected")){ modifier.visibility = PROTECTED }
    else if (isKeyword(tokens, "private")){ modifier.visibility = PRIVATE }

    if (isKeyword(tokens, "abstract")){ modifier.abstract = true }
    if (isKeyword(tokens, "static")){ modifier.static = true }
}



/**
 * Parse expression
 */
private fun parseExpression(tokens: TokenStream, context: Context, index: Int = 0):Expression{
    fun under() = if (index+1 == binaryOperationOrder.size){
        parseSimpleExpression(tokens, context)
    } else{
        parseExpression(tokens, context, index + 1)
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
private fun parseSimpleExpression(tokens: TokenStream, context: Context):Expression{
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
        val expr = parseExpression(tokens, context)
        expectDelimiter(tokens,")")
        return expr
    }

    // Unary Operation
    for(op in unaryOperationOrder){
        if(isOperationToken(tokens, op)){
            return UnaryExpr(op, parseSimpleExpression(tokens, context))
        }
    }

    // Var or Fun
    if (isIdentifier(tokens)){
        val identifier = parseIdentifier(tokens, context)
        return if (isDelimiterNoConsume(tokens, "(")){
            parseFunctionCall(tokens, context, identifier)
        } else{
            IdentifierExpr(identifier)
        }
    }
    throw UnexpectedException("Unknown token:"+tokens.peekString()+" at pos: "+tokens.peekPos())
}



/**
 * Parse Type
 */
fun parseType(tokens: TokenStream, context: Context):DataType {
    var type = parseSimpleType(tokens, context)
    while(isDelimiter(tokens, "[")){
        expectDelimiter(tokens, "]")
        type = ArrayType(type, -1)
    }
    return type
}



/**
 * Parse Type Base
 */
private fun parseSimpleType(tokens: TokenStream, context: Context):DataType{
    if(isDelimiter(tokens, "(")){
        val lst = ArrayList<DataType>()
        do{
            lst.add(parseType(tokens, context))
        }while(isDelimiter(tokens,","))
        expectDelimiter(tokens, ")")
        if (lst.size > 2 && lst.contains(VoidType())){
            throw UnexpectedException("Unexpected Void Type in Tuple")
        }
        if (isOperationToken(tokens, "=>")){
            return FuncType(TupleType(lst), parseType(tokens, context))
        }
        return TupleType(lst)
    }
    if (isIdentifier(tokens)){
        val identifier = parseIdentifier(tokens, context)
        val generics = parseGenerics(tokens, context)
        return if (generics != null){
            GeneratedGenericType(identifier, generics)
        }
        else GeneratedType(identifier)
    }
    if (isPrimTypeToken(tokens,"int")){ return IntType() }
    if (isPrimTypeToken(tokens,"float")){ return FloatType() }
    if (isPrimTypeToken(tokens,"bool")){ return BoolType() }
    if (isPrimTypeToken(tokens,"string")){ return StringType() }
    throw UnexpectedException("Unknown type: "+tokens.peekString()+" at pos: "+tokens.peekPos())
}



private fun parseGenerics(tokens: TokenStream, context: Context): ArrayList<DataType>? {
    return if (isOperationToken(tokens, "<")) {
        val args = ArrayList<DataType>()
        do {
            args.add(parseType(tokens, context))
        } while (!isDelimiter(tokens, ","))
        expectDelimiter(tokens, ">")
        args
    } else {
        null
    }
}


private fun parseIdentifier(tokens: TokenStream, context: Context): Identifier {
    val lst = ArrayList<String>()
    do {
        lst.add(getIdentifier(tokens))
    }while (isDelimiter(tokens,"."))
    return Identifier(lst)
}