package lexer

import lexer.TokenTypes.*

val keyword = HashSet(listOf("if", "while","for","forgenerate", "else",
    "class", "abstract", "struct", "define",
    "return", "extends", "interface", "implements",
    "initer", "import", "from", "as", "blocktags", "enum", "enitytags",
    "itemtags", "static", "private", "public", "protected", "operator",
    "typedef", "lazy", "switch", "package", "in"))

val primTypes = HashSet(listOf("int","float","string","bool", "void", "var", "val", "range"))
val boolLit = HashSet(listOf("true","false"))
val delimiter = HashSet(listOf('(', ')', '{', '}', '[', ']', '.', ','))
val operationChar = HashSet(listOf('+', '-', '*', '/', '%', '&', '|', '^', '?','=', '>', '<'))
val operation = HashSet(listOf("+", "-", "*", "/", "%", "&&", "||", "^", "?","=", "==", "=>", "<=", "<", ">=", ">", "->"))


fun parse(input: String):List<Token>{
    var lst = ArrayList<Token>()
    val stream = StringStream(input, 0)
    while(stream.hasNext()){
        lst.add(parseOne(stream))
    }
    return lst.filterNot { (it.tokenType == CommentTokenType) || (it.tokenType == SpaceTokenType)}
}

fun parseOne(stream: StringStream):Token{
    stream.startSlice()
    val c = stream.next()
    return if (c.isDigit()){
        while(stream.hasNext() && stream.peek().isDigit()){
            stream.next()
        }
        if (stream.hasNext() && stream.peek() == '.' && stream.hasDoubleNext() && stream.peekNext().isDigit()){
            stream.next()
            while(stream.hasNext() && stream.peek().isDigit()){
                stream.next()
            }
            Token(FloatLitTokenType, stream.slice(), stream.getSliceStart())
        } else {
            Token(IntLitTokenType, stream.slice(), stream.getSliceStart())
        }
    } else if (c.isLetter() || c == '_'){
        while(stream.hasNext() && (stream.peek().isDigit() || stream.peek().isLetter() || stream.peek() == '_')){
            stream.next()
        }
        when (val word = stream.slice()) {
            in keyword ->   { Token(KeywordTokenType,    word, stream.getSliceStart()) }
            in boolLit ->   { Token(BoolLitTokenType,    word, stream.getSliceStart()) }
            in primTypes -> { Token(PrimTypeTokenType,   word, stream.getSliceStart()) }
            else ->         { Token(IdentifierTokenType, word, stream.getSliceStart()) }
        }
    } else if (c == '"'){
        var escaped = false
        while((stream.peek() != '"' || escaped)){
            val d = stream.next()
            escaped = d == '\\' && !escaped
        }
        Token(StringLitTokenType, stream.slice(), stream.getSliceStart())
    } else if (c == '/'){
        if (stream.hasNext() && stream.peek() == '/'){ // Single Line Comment
            while(stream.hasNext() && (stream.peek() != '\n' && stream.peek() != '\r')){
                stream.next()
            }
            Token(CommentTokenType, stream.slice(), stream.getSliceStart())
        } else if (stream.hasNext() && stream.peek() == '*'){ // Multiline Comment
            var prev = '*'
            while(!(prev == '*' && stream.peek() == '/')){
                prev = stream.next()
            }
            stream.next()
            Token(CommentTokenType, stream.slice(), stream.getSliceStart())
        } else {
            Token(OperationToken, stream.slice(), stream.getSliceStart())
        }
    } else if (c in delimiter){
        Token(DelimiterTokenType, stream.slice(), stream.getSliceStart())
    } else if (c in operationChar){
        while(stream.hasNext() && stream.nextSlice() in operation){
            stream.next()
        }
        Token(OperationToken, stream.slice(), stream.getSliceStart())
    } else if (c.isWhitespace()){
        Token(SpaceTokenType, stream.slice(), stream.getSliceStart())
    } else {
        throw NotImplementedError("$c not a valid token start")
    }
}