package lexer

import utils.isReturnLine


enum class TokenTypes{
    IdentifierTokenType,
    KeywordTokenType,
    PrimTypeTokenType,
    StringLitTokenType,
    BoolLitTokenType,
    FloatLitTokenType,
    IntLitTokenType,
    DelimiterTokenType,
    CommentTokenType,
    SpaceTokenType,
    OperationToken,
    RawCommandToken,
    DecoratorToken,
    MCSelector
}


data class Token(val tokenType: TokenTypes, val string: String, val startsAt: Int)

class StringStream(value: String, start: Int) {
    private var string: String = value
    private var index: Int = start
    private var sliceStart: Int = -1
    private var MCCState = 0

    fun next():Char{
        return string[index++]
    }
    fun peek():Char{
        return string[index]
    }
    fun hasNext():Boolean{
        return index < string.length
    }
    fun startSlice(){
        sliceStart = index
    }
    fun slice():String{
        return string.substring(sliceStart..(index-1))
    }
    fun nextSlice():String{
        return string.substring(sliceStart..(index))
    }
    fun hasDoubleNext():Boolean{
        return index + 1 < string.length
    }
    fun peekNext():Char{
        return string[index+1]
    }

    fun getSliceStart():Int{
        return sliceStart
    }
    fun isStartOfLine():Boolean{
        return index == 1 || string[index - 2].isReturnLine()
    }

    fun mccStep(step: Int){
        MCCState = if (MCCState == step -1) {
            1
        } else {
            0
        }
    }
    fun mccCheck(step: Int):Boolean {
        return if (MCCState == step){
            MCCState = 0
            true
        } else {
            false
        }
    }
}