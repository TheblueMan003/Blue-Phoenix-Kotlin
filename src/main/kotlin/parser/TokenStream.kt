package parser

import lexer.Token

class TokenStream(tokens_lst: List<Token>, start: Int = 0) {
    private val tokens: List<Token> = tokens_lst
    private var index: Int = start

    fun peek(): Token {
        return tokens[index]
    }
    fun peekString():String{
        return tokens[index].string
    }
    fun peekPos():Int{
        return tokens[index].startsAt
    }
    fun next(){
        index++
    }
    fun hasNext():Boolean{
        return index+1 < tokens.size
    }
    fun isEmpty(): Boolean {
        return index >= tokens.size
    }
    fun copy():TokenStream{
        return TokenStream(tokens, index)
    }
    fun getState(): Int{
        return index
    }
    fun restoreState(state:Int){
        index = state
    }
}