package compiler

import analyzer.Context
import analyzer.runAnalyse
import analyzer.runChecker
import analyzer.runSimplifier
import ast.Statement
import codegen.minecraft.genCode
import parser.TokenStream
import utils.OutputFile
import utils.pmap

class Compiler(private val files: List<Pair<String, String>>, private val filesGetter: IResourceGetter) {
    private var contexts = HashMap<String, Context>()
    private var imported = ArrayList<Pair<String,Statement>>()

    fun compile(): List<OutputFile> {
        val parsed = files.pmap { Pair(it.first, lexer.parse(it.second)) }
        println("Parsed:\t$parsed")
        val tree = parsed.pmap { Pair(it.first, parser.parse(TokenStream(it.second))) }
        println("Trees:\t$tree")
        tree.map { contexts[it.first] = Context(it.first, this) }

        var symTree = tree.pmap { Pair(it.first, runAnalyse(it.second, contexts[it.first]!!)) }
        while(contexts.any{it.value.nameResolvedCheck}) {
            contexts.map { it.value.nameResolvedCheck = false }
            contexts.map { it.value.nameResolvedGet = false }
            println("Resolving:\t$symTree")
            symTree = symTree.pmap { Pair(it.first, runAnalyse(it.second, contexts[it.first]!!)) }
            val failAll = contexts.all { !it.value.nameResolvedGet }
            contexts.map { it.value.nameResolvedAllowCrash = failAll }
        }
        println("Resolved:\t$symTree")

        val checkedTree = (symTree + imported).pmap { Pair(it.first, runChecker(it.second, contexts[it.first]!!)) }
        println("Checked:\t$checkedTree")

        val simplifiedTree = checkedTree.pmap {
            Pair(it.first,
                runSimplifier(it.second, contexts[it.first]!!) { s, c -> runChecker(runAnalyse(s, c), c) })
        }
        println("Simplified:\t$simplifiedTree")

        return simplifiedTree.pmap { genCode(it.second, it.first) }.flatten()
    }
    fun import(string: String): Context{
        synchronized(this) {
            return if (string in contexts) {
                contexts[string]!!
            } else {
                println(string)
                val file = filesGetter.get(string)
                val parsed = lexer.parse(file.second)
                val tree = parser.parse(TokenStream(parsed))
                contexts[file.first] = Context(file.first, this)
                var symTree = runAnalyse(tree, contexts[file.first]!!)
                while(contexts[file.first]!!.nameResolvedCheck) {
                    contexts[file.first]!!.nameResolvedCheck = false
                    symTree = runAnalyse(tree, contexts[file.first]!!)
                }
                imported += Pair(file.first, symTree)
                contexts[file.first]!!
            }
        }
    }
}