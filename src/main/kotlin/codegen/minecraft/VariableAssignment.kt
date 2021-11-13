package codegen.minecraft

import ast.*

var floatScale: Int = 1000


fun setVariableExpression(variable: Variable, expr: Expression, op: AssignmentType, sbi: ScoreboardInitializer,
                          callBack: (Statement)->Unit): List<String>{
    val s = variableToScoreboard(variable)
    var counter = -1

    fun getTMP(): ScoreboardEntry{
        counter++
        return ScoreboardEntry("$counter", Scoreboard("bp.tmp","dummy"))
    }
    fun internal(sbe: ScoreboardEntry, expr: Expression, op: AssignmentType): List<String>{
        return when(expr){
            is IntLitExpr -> {
                when(op) {
                    AssignmentType.SET -> listOf(sbe.set(expr.value))
                    AssignmentType.ADD -> listOf(sbe.add(expr.value))
                    AssignmentType.SUB -> listOf(sbe.remove(expr.value))
                    else -> listOf(sbe.operation(sbi.get(expr.value), op.op))
                }
            }
            is BoolLitExpr -> {
                internal(sbe, IntLitExpr(if (expr.value){1}else{0}), op)
            }
            is FloatLitExpr -> {
                internal(sbe, IntLitExpr((expr.value * floatScale).toInt()), op)
            }
            is VariableExpr -> {
                listOf(sbe.operation(variableToScoreboard(expr.variable), op.op))
            }
            is StatementThanExpression -> {
                callBack(expr.statement)
                internal(sbe, expr.expr, op)
            }
            is BinaryExpr -> {
              when(op) {
                  AssignmentType.SET -> {
                      when (expr.op) {
                          "+"  -> {
                              internal(sbe, expr.first, AssignmentType.SET) +
                                      internal(sbe, expr.second, AssignmentType.ADD)
                          }
                          "-" -> {
                              internal(sbe, expr.first, AssignmentType.SET) +
                                      internal(sbe, expr.second, AssignmentType.SUB)
                          }
                          "*" -> {
                              internal(sbe, expr.first, AssignmentType.SET) +
                                      internal(sbe, expr.second, AssignmentType.MUL)
                          }
                          "/" -> {
                              internal(sbe, expr.first, AssignmentType.SET) +
                                      internal(sbe, expr.second, AssignmentType.DIV)
                          }
                          "%" -> {
                              internal(sbe, expr.first, AssignmentType.SET) +
                                      internal(sbe, expr.second, AssignmentType.ADD)
                          }
                          else -> throw NotImplementedError()
                      }
                  }

                  AssignmentType.ADD -> {
                      when (expr.op) {
                          "+" -> {
                              internal(sbe, expr.first, AssignmentType.ADD) +
                                      internal(sbe, expr.second, AssignmentType.ADD)
                          }
                          "-" -> {
                              internal(sbe, expr.first, AssignmentType.ADD) +
                                      internal(sbe, expr.second, AssignmentType.SUB)
                          }
                          "*" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "+="))
                          }
                          "/" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "+="))
                          }
                          "%" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "+="))
                          }
                          else -> throw NotImplementedError()
                      }
                  }

                  AssignmentType.SUB -> {
                      when (expr.op) {
                          "+" -> {
                              internal(sbe, expr.first, AssignmentType.SUB) +
                                      internal(sbe, expr.second, AssignmentType.SUB)
                          }
                          "-" -> {
                              internal(sbe, expr.first, AssignmentType.SUB) +
                                      internal(sbe, expr.second, AssignmentType.ADD)
                          }
                          "*" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "-="))
                          }
                          "/" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "-="))
                          }
                          "%" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "-="))
                          }
                          else -> throw NotImplementedError()
                      }
                  }

                  AssignmentType.MUL -> {
                      when (expr.op) {
                          "+" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "*="))
                          }
                          "-" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "*="))
                          }
                          "*" -> {
                              internal(sbe, expr.first, AssignmentType.MUL) +
                                      internal(sbe, expr.second, AssignmentType.MUL)
                          }
                          "/" -> {
                              internal(sbe, expr.first, AssignmentType.MUL) +
                                      internal(sbe, expr.second, AssignmentType.DIV)
                          }
                          "%" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "*="))
                          }
                          else -> throw NotImplementedError()
                      }
                  }

                  AssignmentType.DIV -> {
                      when (expr.op) {
                          "+" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "/="))
                          }
                          "-" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "/="))
                          }
                          "*" -> {
                              internal(sbe, expr.first, AssignmentType.DIV) +
                                      internal(sbe, expr.second, AssignmentType.DIV)
                          }
                          "/" -> {
                              internal(sbe, expr.first, AssignmentType.DIV) +
                                      internal(sbe, expr.second, AssignmentType.MUL)
                          }
                          "%" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "/="))
                          }
                          else -> throw NotImplementedError()
                      }
                  }

                  AssignmentType.MOD -> {
                      when (expr.op) {
                          "+" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "%="))
                          }
                          "-" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "%="))
                          }
                          "*" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "%="))
                          }
                          "/" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "%="))
                          }
                          "%" -> {
                              val tmp = getTMP()
                              internal(tmp, expr, AssignmentType.SET) +
                                      listOf(sbe.operation(tmp, "%="))
                          }
                          else -> throw NotImplementedError()
                      }
                  }

                  else -> throw NotImplementedError()
              }
            }
            else -> throw  NotImplementedError(expr.toString())
        }
    }
    return if (checkForVarInExpression(variable, expr)){
        val tmp = getTMP()
        internal(tmp, expr, op)+s.operation(tmp, "=")
    }
    else {
        internal(s, expr, op)
    }
}

private fun checkForVarInExpression(variable: Variable, expr: Expression):Boolean{
    return when(expr){
        is BinaryExpr -> {
            checkForVarInExpression(variable, expr.first) || checkForVarInExpression(variable, expr.second)
        }
        is UnaryExpr -> {
            checkForVarInExpression(variable, expr.first)
        }
        is LinkedVariableAssignment -> {
            variable == expr.variable
        }
        else -> false
    }
}