package codegen.minecraft

import ast.*

fun mcJavaToRawJson(expr: List<Expression>, sbi: ScoreboardInitializer):Pair<List<String>, String>{
    val prepare = ArrayList<String>()
    val print = ArrayList<Expression>()

    expr.map { mcJavaExprToRawJson(it, sbi) }
        .map { (l, o) -> prepare.addAll(l);print.add(o as Expression); }

    return Pair(prepare, ArrayExpr(print).toJsonString(true))
}
private fun mcJavaExprToRawJson(expr: Expression, sbi: ScoreboardInitializer):Pair<List<String>, JsonObject>{
    val prepare = ArrayList<String>()
    val jObject =
    when(expr){
        is IntLitExpr    -> DictionaryExpr(mapOf("text" to StringLitExpr(expr.value.toString())))
        is FloatLitExpr  -> DictionaryExpr(mapOf("text" to StringLitExpr(expr.value.toString())))
        is BoolLitExpr   -> DictionaryExpr(mapOf("text" to StringLitExpr(expr.value.toString())))
        is StringLitExpr -> DictionaryExpr(mapOf("text" to StringLitExpr(expr.value)))
        is SelectorExpr  -> DictionaryExpr(mapOf("selector" to StringLitExpr(expr.selector)))
        is VariableExpr  -> {
            val v = variableToScoreboard(expr.variable, sbi)
            DictionaryExpr(mapOf("score" to
                    DictionaryExpr(mapOf(
                        "name" to StringLitExpr(v.name),
                        "objective" to StringLitExpr(v.scoreboard.name)
                    ))
            ))
        }
        else -> DictionaryExpr(mapOf("text" to StringLitExpr(expr.toString())))
    }

    return Pair(prepare, jObject)
}