package lexer

import guru.zoroark.lixy.LixyTokenType

enum class MyTokenTypes : LixyTokenType {
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
    RawCommandToken
}