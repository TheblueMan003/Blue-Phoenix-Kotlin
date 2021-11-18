package compiler

import analyzer.Context
import analyzer.runAnalyse
import analyzer.runChecker
import analyzer.runSimplifier
import ast.*
import codegen.minecraft.genCode
import parser.TokenStream
import utils.OutputFile
import utils.mergeMapArray
import utils.pmap

class Compiler(private val files: List<Pair<String, String>>, private val filesGetter: IResourceGetter) {
    private var contexts = HashMap<String, Context>()
    private var imported = ArrayList<Pair<String,Statement>>()
    var mainContext = Context("pbpc", this)
    var treeSize = 20
    var printImportDebug = false

    fun compile(): List<OutputFile> {
        val parsed = files.pmap { Pair(it.first, lexer.parse(it.second)) }
        println("Parsed:\t$parsed")
        val tree = parsed.pmap { parser.parse(it.first, TokenStream(it.second)) }
                        .groupBy { it.first }
                        .map { Pair(it.key, Sequence(it.value.map { listOf(it.second) }.reduce{ x, y -> x+y })) }

        println("Trees:\t$tree")
        tree.map { contexts[it.first] = Context(it.first, this) }

        var symTree = tree.pmap { Pair(it.first, runAnalyse(it.second, contexts[it.first]!!)) }
        while(contexts.any{it.value.nameResolvedCheck}) {
            contexts.map { it.value.resetState() }

            println("Resolving:\t$symTree")

            symTree = symTree.pmap { Pair(it.first, runAnalyse(it.second, contexts[it.first]!!)) }
            val failAll = contexts.all { !it.value.nameResolvedGet }
            contexts.map { it.value.nameResolvedAllowCrash = failAll }
        }
        println("Resolved:\t$symTree")

        val checkedTree = (symTree + imported).pmap { Pair(it.first, runChecker(it.second, contexts[it.first]!!)) }
        println("Checked:\t$checkedTree")

        val simplifiedTree = checkedTree.pmap {
            Pair(it.first, runSimplifier(it.second, contexts[it.first]!!) { s, c -> runChecker(runAnalyse(s, c), c) })
        }
        println("Simplified:\t$simplifiedTree")

        val lambda = generateLambdaTree()
        return simplifiedTree.pmap { genCode(it.second, it.first) }.flatten() + genCode(lambda, "pbpc")
    }
    private fun generateLambdaTree():Statement{
        val fcts = contexts.map { it.value.lambdasResolver.toMap() }.reduce{ a,b -> a+b }
        val lambdas = mergeMapArray(contexts.map{ it.value.lambdas }).map{ (k,v) -> k to Pair(v, fcts[k]) }.toMap()

        return runSimplifier(Sequence(lambdas.map{ it ->
            it.value.second!!.use()
            FunctionBody(
                Switch(VariableExpr(it.value.second!!.input[0]),
                    it.value.first.map { f -> Case(FunctionExpr(f),
                        CallExpr(FunctionExpr(f), it.value.second!!.input.drop(1).map { VariableExpr(it) })) }
                ),
                it.value.second!!
            )
        }), mainContext){ s, c -> runChecker(runAnalyse(s, c), c) }
    }
    fun import(string: String): Context{
        synchronized(this) {
            return if (string in contexts) {
                contexts[string]!!
            } else {
                if (printImportDebug) println("import: $string")
                val file = filesGetter.get(string)
                val parsed = lexer.parse(file.second)

                if (printImportDebug) println("Parsed: $parsed")

                val tree = parser.parse(file.first, TokenStream(parsed))

                if (printImportDebug) println("Tree: $tree")

                contexts[file.first] = Context(file.first, this)
                contexts[file.first]!!.isLib = true
                var symTree = runAnalyse(tree.second, contexts[file.first]!!)
                while(contexts[file.first]!!.nameResolvedCheck) {
                    contexts[file.first]!!.nameResolvedCheck = false
                    contexts[file.first]!!.nameResolvedGet = false
                    contexts[file.first]!!.isDone = false
                    symTree = runAnalyse(symTree, contexts[file.first]!!)
                    contexts.map { it.value.nameResolvedAllowCrash = !contexts[file.first]!!.nameResolvedGet }
                }
                imported += Pair(file.first, symTree)
                contexts[file.first]!!
            }
        }
    }
}