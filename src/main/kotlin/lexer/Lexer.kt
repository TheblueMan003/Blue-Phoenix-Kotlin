package lexer

import guru.zoroark.lixy.LixyToken
import guru.zoroark.lixy.lixy
import guru.zoroark.lixy.matchers.*
import lexer.MyTokenTypes.*

class Lexer() {
    fun parse(input: String, removeSpace: Boolean, removeComment: Boolean):List<LixyToken>{
        val lexer = lixy {
            state {
                matches("[ \t\n]+") isToken SpaceTokenType
                anyOf("if","while","for","forgenerate",
                    "class","abstract","struct","define","var","val",
                    "return", "extends", "interface", "implements",
                    "initer", "import", "blocktags", "enum", "enitytags",
                    "itemtags", "static", "private", "public", "protected") isToken KeywordTokenType
                anyOf("int","float","string","bool", "void") isToken PrimTypeTokenType
                matches("[A-Za-z][\\w]*") isToken IdentifierTokenType
                matches("^\\s*/[^/].*") isToken RawCommandToken
                matches("[\\d]+") isToken IntLitTokenType
                matches("[\\d]*\\.[\\d]+") isToken FloatLitTokenType
                anyOf("+", "-", "*", "/", "%", "&", "|", "^", "?","=", "=>", "<=", "<", ">=", ">", "&&", "||") isToken OperationToken
                matches("\\{\\}\\[\\]\\(\\)\\.") isToken DelimiterTokenType
                matches("//[^\n]*") isToken CommentTokenType
                matches("/\\*([^\\*/]|(\\*[^/])|(/))*\\*/") isToken CommentTokenType
                anyOf("true","false") isToken BoolLitTokenType
                matches("\"([^\"]|(\\\"))*\"") isToken StringLitTokenType
            }
        }
        return lexer.tokenize(input).filterNot {
            (it.tokenType == CommentTokenType && removeComment) ||
                    (it.tokenType == SpaceTokenType && removeSpace)}
    }
}