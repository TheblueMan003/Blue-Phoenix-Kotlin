package codegen.minecraft

import ast.Variable

data class Scoreboard(val name: String, val type: String)
data class ScoreboardEntry(val name: String, val scoreboard: Scoreboard){
    fun set(value: Int): String{
        return "scoreboard players set $name ${scoreboard.name} $value"
    }
    fun add(value: Int): String{
        return "scoreboard players add $name ${scoreboard.name} $value"
    }
    fun remove(value: Int): String{
        return "scoreboard players remove $name ${scoreboard.name} $value"
    }
    fun reset(): String{
        return "scoreboard players reset $name ${scoreboard.name}"
    }
    fun operation(v2: ScoreboardEntry, op: String): String{
        return "scoreboard players operation $name ${scoreboard.name} $op ${v2.name} ${v2.scoreboard.name}"
    }
}
class ScoreboardInitializer{
    private val lst = HashMap<Int, ScoreboardEntry>()
    private val output = ArrayList<String>()

    fun get(value: Int): ScoreboardEntry{
        if (!lst.containsKey(value)){
            val sbe = ScoreboardEntry("$value", Scoreboard("bp.const","dummy"))
            output.add(sbe.set(value))
            lst[value] = sbe
        }
        return lst[value] as ScoreboardEntry
    }
}


fun variableToScoreboard(variable: Variable): ScoreboardEntry{
    return ScoreboardEntry(variable.name.toString(), Scoreboard("bp.value", "dummy"))
}