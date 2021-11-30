package codegen.minecraft

import ast.SelectorExpr
import data_struct.Variable

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
    private fun isEquals(value: Int): MCCondition{
        return MCCondition(false,"score $name ${scoreboard.name} matches $value")
    }
    fun isIn(min: Int, max: Int): MCCondition{
        return MCCondition(false,"score $name ${scoreboard.name} matches $min..$max")
    }
    private fun isBiggerEqual(value: Int): MCCondition{
        return MCCondition(false,"score $name ${scoreboard.name} matches $value..")
    }
    private fun isSmallerEqual(value: Int): MCCondition{
        return MCCondition(false,"score $name ${scoreboard.name} matches ..$value")
    }
    fun compare(v2: ScoreboardEntry, op: String): MCCondition{
        return if (op == "!="){
            MCCondition(true,"score $name ${scoreboard.name} $op ${v2.name} ${v2.scoreboard.name}")
        } else {
            MCCondition(false,"score $name ${scoreboard.name} $op ${v2.name} ${v2.scoreboard.name}")
        }
    }
    fun compare(value: Int, op: String): MCCondition{
        return when(op){
            "<"  -> isSmallerEqual(value-1)
            "<=" -> isSmallerEqual(value)
            ">"  -> isBiggerEqual(value+1)
            ">=" -> isBiggerEqual(value)
            "==" -> isEquals(value)
            "!=" -> isEquals(value).inverted()
            else -> throw NotImplementedError()
        }
    }
}
data class MCCondition(val unless: Boolean, val cond: String){
    fun inverted(): MCCondition {
        return MCCondition(!unless, cond)
    }
    override fun toString():String{
        return if (unless){
            "unless $cond"
        } else{
          "if $cond"
        }
    }
}

class ScoreboardInitializer{
    private val lst = HashMap<Int, ScoreboardEntry>()
    private val output = ArrayList<String>()

    fun getConstant(value: Int): ScoreboardEntry{
        if (!lst.containsKey(value)){
            val sbe = ScoreboardEntry("$value", Scoreboard("bp.const","dummy"))
            output.add(sbe.set(value))
            lst[value] = sbe
        }
        return lst[value] as ScoreboardEntry
    }
}


fun variableToScoreboard(variable: Variable, sbi: ScoreboardInitializer): ScoreboardEntry{
    return if (variable.modifier.entity){
        ScoreboardEntry("@s", Scoreboard(variable.name.toString(), "dummy"))
    } else {
        ScoreboardEntry(variable.name.toString(), Scoreboard("bp.value", "dummy"))
    }
}
fun variableToScoreboard(selector: SelectorExpr, variable: Variable, sbi: ScoreboardInitializer): ScoreboardEntry{
    return if (variable.modifier.entity){
        ScoreboardEntry(selector.selector, Scoreboard(variable.name.toString(), "dummy"))
    } else {
        throw UnsupportedOperationException()
    }
}