package tree

import tree.DataStructVisibility.*
import tree.data.Identifier
import java.rmi.UnexpectedException
import kotlin.collections.ArrayList

class Parser {
    private val binaryOperationOrder = listOf("+", "-", "*", "/", "%", "^", "<", "<=", ">", ">=", "||", "&&")
    private val unaryOperationOrder = listOf("-", "!")


    fun parse(path: String, tokens: TokenStream):Pair<Statement, Context>{
        val statements = ArrayList<Statement>()
        val context = Context(path)
        while (tokens.isEmpty()){
            statements += parseBlock(tokens, context)
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
        if (isKeyword(tokens, "if")){
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
        // Blocks
        if (isDelimiter(tokens,"{")){
            val statements = ArrayList<Statement>();
            while(!isDelimiter(tokens,"}")){
                assert(!tokens.isEmpty())
                statements.add(parseBlock(tokens, context))
            }
            return Block(statements)
        }


        // Data Struct
        if (isKeyword(tokens, "public")){ modifier.visibility = PUBLIC }
        else if (isKeyword(tokens, "protected")){ modifier.visibility = PROTECTED }
        else if (isKeyword(tokens, "private")){ modifier.visibility = PRIVATE }

        if (isKeyword(tokens, "abstract")){ modifier.abstract = true }
        if (isKeyword(tokens, "static")){ modifier.static = true }

        if (isKeyword(tokens, "struct")) {
            return Empty()
        }
        else if (isKeyword(tokens, "class")) {
            return Empty()
        }
        else {
            val type = parseType(tokens, context)
            val identifier = getIdentifier(tokens)
            // Function
            if (isDelimiter(tokens, "(")){
                return Empty()
            }
        }

        // Error No Token Match Found
        if (tokens.isEmpty()){
            throw UnexpectedException("Unexpected EOF")
        }
        throw UnexpectedException("Unknown token:"+tokens.peekString()+" at pos: "+tokens.peekPos())
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
            return if (isDelimiter(tokens, "(")){
                val args = ArrayList<Expression>()
                while(!isDelimiter(tokens, ")")) {
                    args.add(parseExpression(tokens, context))
                }
                CallExpr(identifier, args)
            } else{
                VarExpr(identifier)
            }
        }
        throw UnexpectedException("Unknown token:"+tokens.peekString()+" at pos: "+tokens.peekPos())
    }

    /**
     * Parse Type
     */
    private fun parseType(tokens: TokenStream, context: Context):DataType {
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
            return if (isOperationToken(tokens, "<")){
                val args = ArrayList<DataType>()
                do{
                    args.add(parseType(tokens, context))
                }while (!isDelimiter(tokens, ","))
                expectDelimiter(tokens, ">")
                GeneratedGenericType(identifier, args)
            }
            else GeneratedType(identifier)
        }
        if (isPrimTypeToken(tokens,"int")){ return IntType() }
        if (isPrimTypeToken(tokens,"float")){ return FloatType() }
        if (isPrimTypeToken(tokens,"bool")){ return BoolType() }
        if (isPrimTypeToken(tokens,"string")){ return StringType() }
        throw UnexpectedException("Unknown type: "+tokens.peekString()+" at pos: "+tokens.peekPos())
    }



    private fun parseIdentifier(tokens: TokenStream, context: Context): Identifier {
        val lst = ArrayList<String>()
        do {
            lst.add(getIdentifier(tokens))
        }while (isDelimiter(tokens,"."))
        return Identifier(lst)
    }
}