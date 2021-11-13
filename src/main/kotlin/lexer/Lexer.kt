package lexer

import ast.Identifier
import guru.zoroark.lixy.LixyToken
import guru.zoroark.lixy.lixy
import guru.zoroark.lixy.matchers.*
import lexer.MyTokenTypes.*

val keyword = HashSet(listOf("if","while","for","forgenerate", "else",
    "class","abstract","struct","define",
    "return", "extends", "interface", "implements",
    "initer", "import", "blocktags", "enum", "enitytags",
    "itemtags", "static", "private", "public", "protected", "operator",
    "typedef"))

val primTypes = HashSet(listOf("int","float","string","bool", "void", "var", "val"))
val boolLit = HashSet(listOf("true","false"))

fun parse(input: String):List<LixyToken>{
    val lexer = lixy {
        state {
            matches("[\\s;]+") isToken SpaceTokenType
            matches("/\\*([^\\*/]|(\\*[^/])|(/))*\\*/") isToken CommentTokenType
            matches("//[^\n]*") isToken CommentTokenType
            matches("mcc\\([^)]*\\):\\s*/[^/].*") isToken RawCommandToken
            matches("mcc:\\s*/[^/].*") isToken RawCommandToken
            matches("[\\d]+\\.[\\d]+") isToken FloatLitTokenType
            matches("[\\d]+") isToken IntLitTokenType
            matches("[A-Za-z_][\\w]*") isToken IdentifierTokenType
            matches("@[A-Za-z_][\\w]*") isToken DecoratorToken
            anyOf("(",")","{","}","[","]",".", ",", "->") isToken DelimiterTokenType
            anyOf("+", "-", "*", "/", "%", "&&", "||", "^", "?","=", "=>", "<=", "<", ">=", ">") isToken OperationToken
            matches("\"([^\"])*\"") isToken StringLitTokenType
        }
    }
    return lexer.tokenize(input).filterNot {
        (it.tokenType == CommentTokenType) || (it.tokenType == SpaceTokenType)}
        //
        // Done because Lixy doesn't manage word well
        //
        .map { it ->LixyToken(it.string, it.startsAt, it.endsAt,
            if (keyword.contains(it.string)){
                KeywordTokenType
            }
            else if (primTypes.contains(it.string)){
                PrimTypeTokenType
            }else if (boolLit.contains(it.string)){
                BoolLitTokenType
            }else{
                it.tokenType
            })
        }
}