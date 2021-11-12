package parser

import analyzer.Context

class ParserContext(val context: Context){
    fun sub(id: String): ParserContext{
        return ParserContext(context.sub(id))
    }
    fun resolve(){
        context.resolve()
    }
}