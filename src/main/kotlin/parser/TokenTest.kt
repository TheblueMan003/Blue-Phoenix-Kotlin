package parser

import lexer.TokenTypes.*

fun isKeyword(tokens: TokenStream, value: String):Boolean{
    return if (!tokens.isEmpty() && tokens.peek().tokenType == KeywordTokenType && tokens.peek().string == value){
        tokens.next()
        true
    } else{
        false
    }
}
fun expectKeyword(tokens: TokenStream, value: String){
    return if (!tokens.isEmpty() && tokens.peek().tokenType == KeywordTokenType && tokens.peek().string == value){
        tokens.next()
    } else{
        if (!tokens.isEmpty()) {
            throw UnexpectedToken(tokens.peek().string, tokens.peek().startsAt)
        }
        else{
            throw UnexpectedToken("EOF",0)
        }
    }
}
fun isDelimiter(tokens: TokenStream, value: String):Boolean{
    return if (!tokens.isEmpty() && tokens.peek().tokenType == DelimiterTokenType && tokens.peek().string == value){
        tokens.next()
        true
    } else{
        false
    }
}

fun isRawCommand(tokens: TokenStream):Boolean{
    return !tokens.isEmpty() && tokens.peek().tokenType == RawCommandToken
}
fun getRawCommand(tokens: TokenStream): String{
    val token = tokens.peek()
    tokens.next()
    return token.string
}

fun isDelimiterNoConsume(tokens: TokenStream, value: String):Boolean{
    return !tokens.isEmpty() && tokens.peek().tokenType == DelimiterTokenType && tokens.peek().string == value
}
fun expectDelimiter(tokens: TokenStream, value: String){
    return if (!tokens.isEmpty() && tokens.peek().tokenType == DelimiterTokenType && tokens.peek().string == value){
        tokens.next()
    } else{
        if (!tokens.isEmpty()) {
            throw UnexpectedToken(tokens.peek().string, tokens.peek().startsAt)
        }
        else{
            throw UnexpectedToken("EOF",0)
        }
    }
}
fun isPrimTypeToken(tokens: TokenStream, value: String):Boolean{
    return if (!tokens.isEmpty() && tokens.peek().tokenType == PrimTypeTokenType && tokens.peek().string == value){
        tokens.next()
        true
    } else{
        false
    }
}
fun isOperationTokenNoConsume(tokens: TokenStream, valid: List<String>):Boolean{
    return !tokens.isEmpty() && tokens.peek().tokenType == OperationToken && valid.contains(tokens.peek().string)
}
fun isOperationToken(tokens: TokenStream, value: String):Boolean{
    return if (!tokens.isEmpty() && tokens.peek().tokenType == OperationToken && tokens.peek().string == value){
        tokens.next()
        true
    } else{
        false
    }
}
fun expectOperationToken(tokens: TokenStream, value: String){
    return if (!tokens.isEmpty() && tokens.peek().tokenType == OperationToken && tokens.peek().string == value){
        tokens.next()
    } else{
        if (!tokens.isEmpty()) {
            throw UnexpectedToken(tokens.peek().string, tokens.peek().startsAt)
        }
        else{
            throw UnexpectedToken("EOF",0)
        }
    }
}
fun getOperationToken(tokens: TokenStream):String{
    return if (!tokens.isEmpty() && tokens.peek().tokenType == OperationToken){
        val value = tokens.peek().string
        tokens.next()
        value
    } else{
        if (!tokens.isEmpty()) {
            throw UnexpectedToken(tokens.peek().string, tokens.peek().startsAt)
        }
        else{
            throw UnexpectedToken("EOF",0)
        }
    }
}
fun isIdentifier(tokens: TokenStream):Boolean{
    return !tokens.isEmpty() && tokens.peek().tokenType == IdentifierTokenType
}
fun getIdentifier(tokens: TokenStream):String{
    val r = tokens.peek().string
    tokens.next()
    return r
}

// Float
fun isFloatLit(tokens: TokenStream):Boolean{
    return !tokens.isEmpty() && tokens.peek().tokenType == FloatLitTokenType
}
fun getFloatLit(tokens: TokenStream):Float{
    val r = tokens.peek().string.toFloat()
    tokens.next()
    return r
}

// Int
fun isIntLit(tokens: TokenStream):Boolean{
    return !tokens.isEmpty() && tokens.peek().tokenType == IntLitTokenType
}
fun getIntLit(tokens: TokenStream):Int{
    val r = tokens.peek().string.toInt()
    tokens.next()
    return r
}

// Bool
fun isBoolLit(tokens: TokenStream):Boolean{
    return !tokens.isEmpty() && tokens.peek().tokenType == BoolLitTokenType
}
fun getBoolLit(tokens: TokenStream):Boolean{
    val r = tokens.peek().string.toBoolean()
    tokens.next()
    return r
}

// String
fun isStringLit(tokens: TokenStream):Boolean{
    return !tokens.isEmpty() && tokens.peek().tokenType == StringLitTokenType
}
fun getStringLit(tokens: TokenStream):String{
    val r = tokens.peek().string
    tokens.next()
    return r
}

// Selector
fun isSelector(tokens: TokenStream):Boolean{
    return !tokens.isEmpty() && tokens.peek().tokenType == MCSelector
}
fun getSelector(tokens: TokenStream):String{
    val r = tokens.peek().string
    tokens.next()
    return r
}

fun isType(tokens: TokenStream):Boolean{
    return try{
        parseType(tokens.copy())
        true
    }
    catch (e: Exception){
        false
    }
}

data class UnexpectedToken(val token: String, val pos: Int): Exception()