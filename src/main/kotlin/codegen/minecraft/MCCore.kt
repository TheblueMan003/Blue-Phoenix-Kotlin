package codegen.minecraft

import parser.Variable

data class Scoreboard(val name: String, val type: String)
data class ScoreboardEntry(val name: String, val scoreboard: Scoreboard)


fun variableToScoreboard(variable: Variable): ScoreboardEntry{
    throw NotImplementedError()
}

fun setVariableValue(variable: Variable, value: Int): String{
    val s = variableToScoreboard(variable)
    return "scoreboard players set ${s.name} ${s.scoreboard.name} $value"
}
fun addVariableValue(variable: Variable, value: Int): String{
    val s = variableToScoreboard(variable)
    return "scoreboard players add ${s.name} ${s.scoreboard.name} $value"
}
fun subVariableValue(variable: Variable, value: Int): String{
    val s = variableToScoreboard(variable)
    return "scoreboard players remove ${s.name} ${s.scoreboard.name} $value"
}
fun resetVariableValue(variable: Variable): String{
    val s = variableToScoreboard(variable)
    return "scoreboard players reset ${s.name} ${s.scoreboard.name}"
}
fun variableOperation(v1: Variable, v2: Variable, op: String): String{
    val s1 = variableToScoreboard(v1)
    val s2 = variableToScoreboard(v2)
    return "scoreboard players operation ${s1.name} ${s1.scoreboard.name} $op ${s2.name} ${s2.scoreboard.name}"
}