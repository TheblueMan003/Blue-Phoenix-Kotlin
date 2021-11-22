package ast

interface IGenerator {
    fun getIterator(): Iterator<Expression>
}