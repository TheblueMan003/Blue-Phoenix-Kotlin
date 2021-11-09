package lexer

import guru.zoroark.lixy.LixyToken
import guru.zoroark.lixy.lixy
import guru.zoroark.lixy.matchers.*
import lexer.MyTokenTypes.*

fun parse(input: String, removeSpace: Boolean, removeComment: Boolean):List<LixyToken>{
    val lexer = lixy {
        state {
            matches("[ \t\n]+") isToken SpaceTokenType
            anyOf("if\b","while\b","for\b","forgenerate\b",
                "class\b","abstract\b","struct\b","define\b","var\b","val\b",
                "return\b", "extends\b", "interface\b", "implements\b",
                "initer\b", "import\b", "blocktags\b", "enum\b", "enitytags\b",
                "itemtags\b", "static\b", "private\b", "public\b", "protected\b") isToken KeywordTokenType
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