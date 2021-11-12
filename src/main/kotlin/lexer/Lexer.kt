package lexer

import ast.Identifier
import guru.zoroark.lixy.LixyToken
import guru.zoroark.lixy.lixy
import guru.zoroark.lixy.matchers.*
import lexer.MyTokenTypes.*

val keyword = HashSet(listOf("if","while","for","forgenerate",
    "class","abstract","struct","define","var","val",
    "return", "extends", "interface", "implements",
    "initer", "import", "blocktags", "enum", "enitytags",
    "itemtags", "static", "private", "public", "protected", "operator"))

val primTypes = HashSet(listOf("int","float","string","bool", "void"))

fun parse(input: String, removeSpace: Boolean, removeComment: Boolean):List<LixyToken>{
    val lexer = lixy {
        state {
            matches("[\\s;]+") isToken SpaceTokenType
            matches("[A-Za-z_][\\w]*") isToken IdentifierTokenType
            matches("^\\s*/[^/].*") isToken RawCommandToken
            matches("[\\d]+") isToken IntLitTokenType
            matches("[\\d]*\\.[\\d]+") isToken FloatLitTokenType
            anyOf("(",")","{","}","[","]",".", ",", "->") isToken DelimiterTokenType
            anyOf("+", "-", "*", "/", "%", "&", "|", "^", "?","=", "=>", "<=", "<", ">=", ">", "&&", "||") isToken OperationToken
            matches("//[^\n]*") isToken CommentTokenType
            matches("/\\*([^\\*/]|(\\*[^/])|(/))*\\*/") isToken CommentTokenType
            anyOf("true","false") isToken BoolLitTokenType
            matches("\"([^\"]|(\"))*\"") isToken StringLitTokenType
        }
    }
    return lexer.tokenize(input).filterNot {
        (it.tokenType == CommentTokenType && removeComment) ||
                (it.tokenType == SpaceTokenType && removeSpace)}
        //
        // Done because Lixy doesn't manage word well
        //
        .map { it ->LixyToken(it.string, it.startsAt, it.endsAt,
            if (keyword.contains(it.string)){
                KeywordTokenType
            }
            else if (primTypes.contains(it.string)){
                PrimTypeTokenType
            }else{
                it.tokenType
            })
        }
}