package parser

import analyzer.Context
import ast.*
import ast.DataStructVisibility.*
import java.rmi.UnexpectedException
import kotlin.collections.ArrayList


private val binaryOperationOrder = listOf("&&", "||","<", "<=", ">", ">=","+", "-", "*", "/", "%", "^",)
private val unaryOperationOrder = listOf("-", "!")


fun parse(path: String, tokens: TokenStream):Pair<Statement, ParserContext>{
    val statements = ArrayList<Statement>()
    val context = ParserContext(Context(path))
    while (!tokens.isEmpty()){
        statements.add(parseBlock(tokens, context))
    }
    return if (statements.size > 0) { Pair(Sequence(statements), context)} else { Pair(Empty(), context)}
}


/**
 * Parse Statement
 */
private fun parseBlock(tokens: TokenStream, context: ParserContext): Statement {
    if (isKeyword(tokens, "import")){
        return Empty()
    }
    val modifier = DataStructModifier()


    // If
    if (isKeyword(tokens, "if")){ return parseIf(tokens, context) }

    // RawCommand
    if (isRawCommand(tokens)) { return RawCommand(getRawCommand(tokens))}

    // Switch
    if (isKeyword(tokens, "switch")){ return parseSwitch(tokens, context) }

    // Return
    if (isKeyword(tokens, "return")){ return UnlinkedReturnStatement(parseExpression(tokens, context)) }

    // Blocks
    if (isDelimiter(tokens,"{")){ return parseBlockGroup(tokens, context.sub("")) }

    // Variable Assignment / Function Call
    if (isIdentifier(tokens)){
        val state = tokens.getState()
        val identifier = parseIdentifier(tokens, context)
        if (isDelimiterNoConsume(tokens, "(")){
            return parseFunctionCall(tokens, context, identifier)
        } else if (isOperationTokenNoConsume(tokens, listOf("+","*","-","/","%","=","^"))) {
            return parseVariableAssignment(tokens, context, identifier)
        } else {
            tokens.restoreState(state)
        }
    }


    parseModifier(tokens, context, modifier)

    if (isKeyword(tokens, "typedef")){
        return parseTypeDef(tokens, context, modifier)
    }
    else if (isKeyword(tokens, "struct")) {
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
            parseFunctionDeclaration(tokens, context.sub(identifier.toString()), identifier, type, modifier)
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

fun parseTypeDef(tokens: TokenStream, context: ParserContext, modifier: DataStructModifier): Statement {
    val type = parseType(tokens, context)
    val id = parseIdentifier(tokens, context)
    return TypeDefDeclaration(modifier, id, type)
}


private fun parseStructDeclaration(tokens: TokenStream, context: ParserContext, modifier: DataStructModifier): StructDeclaration {
    val identifier = parseIdentifier(tokens, context)
    val generics = parseGenerics(tokens, context)
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
        acceptStatement(parseBlock(tokens, context))
    }
    return StructDeclaration(modifier, identifier, generics, fields, methods, Sequence(builders))
}



private fun parseFunctionDeclaration(tokens: TokenStream, context: ParserContext, identifier: Identifier,
                                     type: DataType, modifier: DataStructModifier
): FunctionDeclaration {
    val args = parseFunctionArgumentsList(tokens, context)
    val body = parseBlock(tokens, context)
    return FunctionDeclaration(modifier, identifier, args, type, body)
}



private fun parseVariableDeclaration(tokens: TokenStream, context: ParserContext, identifier: Identifier,
                                     type: DataType, modifier: DataStructModifier): Sequence {
    // Variable Declaration
    val vars = parseIdentifierList(tokens, context, identifier)

    val declarations = vars.map { VariableDeclaration(modifier, it, type) }
    if (isOperationToken(tokens, "=")){
        // Get Expression
        val expr = parseExpressionList(tokens, context)

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



private fun parseSwitch(tokens: TokenStream, context: ParserContext): Switch {
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

    val ret = Switch(scrut, cases)
    ret.hasReturn = cases.map { toReturnType(it.statement) }.reduce{ a, b -> mergeReturnType(a,b) }
    return ret
}



private fun parseIf(tokens: TokenStream, context: ParserContext): Statement {
    expectDelimiter(tokens, "(")
    val cond = parseExpression(tokens, context)
    expectDelimiter(tokens, ")")
    val blockIf = parseBlock(tokens, context)
    return if (isKeyword(tokens, "else")){
        val blockElse = parseBlock(tokens, context)
        val ret = IfElse(cond, blockIf, blockElse)
        ret.hasReturn = mergeReturnType(ret.IfBlock, ret.ElseBlock)
        ret
    } else{
        val ret = If(cond, blockIf)
        ret.hasReturn = ReturnType.HALF
        ret
    }
}


private fun parseBlockGroup(tokens: TokenStream, context: ParserContext): Block {
    val statements = ArrayList<Statement>()
    val statementsAfterReturn = ArrayList<Statement>()
    var returned = ReturnType.NONE
    while(!isDelimiter(tokens,"}")){
        assert(!tokens.isEmpty())
        val stm = parseBlock(tokens, context)
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
    context.resolve()
    return Block(statements)
}



private fun parseVariableAssignment(tokens: TokenStream, context: ParserContext, identifier: Identifier): Statement {
    val vars = parseIdentifierList(tokens, context, identifier)
    return if (isOperationToken(tokens, "=")){
        val expr = parseExpressionList(tokens, context)
        if (expr.size == 1){
            Sequence(vars.map { UnlinkedVariableAssignment(it, expr[0], AssignmentType.SET) })
        }else{
            Sequence(vars.zip(expr).map { (v, e) -> UnlinkedVariableAssignment(v, e, AssignmentType.SET) })
        }
    }
    else{
        val op = getOperationToken(tokens)
        if (getOperationToken(tokens)!="=") throw UnexpectedException("Unknown token: "+tokens.peekString()+" at pos: "+tokens.peekPos())
        val expr = parseExpressionList(tokens, context)
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



private fun parseExpressionList(tokens: TokenStream, context: ParserContext): List<Expression>{
    val expr = ArrayList<Expression>()
    do {
        expr.add(parseExpression(tokens, context))
    }while(isDelimiter(tokens, ","))
    return expr
}



private fun parseIdentifierList(tokens: TokenStream, context: ParserContext, identifier:Identifier): List<Identifier> {
    val vars = ArrayList<Identifier>()
    vars.add(identifier)
    while(isDelimiter(tokens, ",")){
        vars.add(parseIdentifier(tokens, context))
    }
    return vars
}



private fun parseFunctionCall(tokens: TokenStream, context: ParserContext, identifier:Identifier): Expression {
    var called: Expression = IdentifierExpr(identifier)
    while(isDelimiter(tokens, "(")) {
        called = if (isDelimiter(tokens, ")")){
            val args = emptyList<Expression>()
            CallExpr(called, args)
        }else {
            val args = parseExpressionList(tokens, context)
            expectDelimiter(tokens, ")")
            CallExpr(called, args)
        }
    }
    return called
}



private fun parseFunctionArgumentsList(tokens: TokenStream, context: ParserContext): List<FunctionArgument>{
    val args = ArrayList<FunctionArgument>()
    while(!isDelimiter(tokens, ")") || isDelimiter(tokens, ",")){
        args.add(parseFunctionArguments(tokens, context))
    }
    return args
}



private fun parseFunctionArguments(tokens: TokenStream, context: ParserContext): FunctionArgument {
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



private fun parseModifier(tokens: TokenStream, context: ParserContext, modifier: DataStructModifier){
    // Data Struct
    if (isKeyword(tokens, "public")){ modifier.visibility = PUBLIC }
    else if (isKeyword(tokens, "protected")){ modifier.visibility = PROTECTED }
    else if (isKeyword(tokens, "private")){ modifier.visibility = PRIVATE }

    if (isKeyword(tokens, "abstract")){ modifier.abstract = true }
    if (isKeyword(tokens, "static")){ modifier.static = true }
    if (isKeyword(tokens, "operator")){ modifier.operator = true }
}



/**
 * Parse expression
 */
private fun parseExpression(tokens: TokenStream, context: ParserContext, index: Int = 0): Expression {
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
private fun parseSimpleExpression(tokens: TokenStream, context: ParserContext): Expression {
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
        var expr = parseExpression(tokens, context)
        if (isDelimiter(tokens, ",")){
            val lst = listOf(expr, parseExpression(tokens, context)).toMutableList()
            while((isDelimiter(tokens, ","))){lst.add(parseExpression(tokens, context))}
            expr = TupleExpr(lst)
        }
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
    throw UnexpectedException("Unknown token: '"+tokens.peekString()+"' at pos: "+tokens.peekPos())
}



/**
 * Parse Type
 */
fun parseType(tokens: TokenStream, context: ParserContext): DataType {
    var type = parseSimpleType(tokens, context)
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
private fun parseSimpleType(tokens: TokenStream, context: ParserContext): DataType {
    if(isDelimiter(tokens, "(")){
        val lst = ArrayList<DataType>()
        do{
            lst.add(parseType(tokens, context))
        }while(isDelimiter(tokens,","))
        expectDelimiter(tokens, ")")
        if (lst.size > 2 && lst.contains(VoidType())){
            throw UnexpectedException("Unexpected Void Type in Tuple")
        }
        if (lst.size > 1 && lst.contains(VarType())){
            throw UnexpectedException("Var cannot be used as a Type in Tuple")
        }
        if (isOperationToken(tokens, "=>")){
            return FuncType(lst, parseType(tokens, context))
        }
        return TupleType(lst)
    }
    if (isIdentifier(tokens)){
        val identifier = parseIdentifier(tokens, context)
        val generics = parseGenerics(tokens, context)
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



private fun parseGenerics(tokens: TokenStream, context: ParserContext): ArrayList<DataType>? {
    return if (isOperationToken(tokens, "<")) {
        val args = ArrayList<DataType>()
        do {
            args.add(parseType(tokens, context))
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


private fun parseIdentifier(tokens: TokenStream, context: ParserContext): Identifier {
    val lst = ArrayList<String>()
    do {
        lst.add(getIdentifier(tokens))
    }while (isDelimiter(tokens,"."))
    return Identifier(lst)
}