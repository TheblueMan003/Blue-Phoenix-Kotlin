package tree

import guru.zoroark.lixy.LixyToken

class TokenStream(tokens_lst: List<LixyToken>, start: Int) {
    val tokens: List<LixyToken> = tokens_lst
    var index: Int = start

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
        return index < tokens.size
    }
}