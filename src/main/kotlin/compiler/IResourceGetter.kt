package compiler

interface IResourceGetter{
    fun get(string: String):Pair<String, String>
}