package parser

import guru.zoroark.lixy.LixyToken

class TokenStream(tokens_lst: List<LixyToken>, start: Int) {
    private val tokens: List<LixyToken> = tokens_lst
    private var index: Int = start

    fun peek():LixyToken{
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