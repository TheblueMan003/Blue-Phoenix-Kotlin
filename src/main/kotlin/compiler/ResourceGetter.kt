package compiler

import getResourceAsText

class ResourceGetter(): IResourceGetter {
    override fun get(string: String): Pair<String, String> {
        return Pair(string, getResourceAsText(string.replace(".","/")+".bp"))
    }
}