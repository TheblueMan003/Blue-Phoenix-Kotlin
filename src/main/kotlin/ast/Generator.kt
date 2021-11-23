package ast

interface IGenerator {
    fun getIterator(): Iterator<Map<String, Expression>>
}