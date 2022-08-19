package com.fmaghi.dictionarium

import android.util.Log
import java.io.File
import java.io.Serializable

class DictionaryReader() {

    private val entries = mutableListOf<DictionaryEntry>()
    lateinit var currentDict: String

    fun readDictionary(dictMeta: DictionaryMeta) {
        val dictFile = File(dictMeta.path)
        if (dictFile.exists()) {
            Log.i("DictionaryReader", "loading ${dictMeta.path}")
            val lines = dictFile.readLines()

            if (lines[0].startsWith("dict=")) {
                entries.clear()

                currentDict = lines[0].substring(5)

                var key = ""
                var word = ""
                var ipa = ""
                var ety = ""
                var pos = ""
                var defByPos = mutableListOf<DefinitionByPos>()
                var defGroups = mutableListOf<MutableList<String>>()
                var exps = mutableListOf<Expression>()
                var synonyms = mutableListOf<String>()


                for (line in lines) {
                    if (line.startsWith("key=")) key = line.substring(4)

                    if (line.startsWith("word=")) word = line.substring(5)

                    else if (line.startsWith("ipa=")) ipa = line.substring(4)

                    else if (line.startsWith("pos=")) {
                        if (pos.isNotEmpty() && defGroups.isNotEmpty()) {
                            // if the entry contains more that one pos, add the previous pos and
                            // definitions to a separate group
                            defByPos.add(DefinitionByPos(pos, defGroups.toList()))
                            pos = line.substring(4)
                            defGroups.clear()
                        } else {
                            pos = line.substring(4)
                        }
                    }

                    else if (line.startsWith("#def")) defGroups.add(mutableListOf())

                    else if (line.startsWith("def=")) {
                        if (defGroups.isEmpty()) defGroups.add(mutableListOf())
                        defGroups.last().add(line.substring(4))
                    }

                    else if (line.startsWith("ety=")) ety = line.substring(4)

                    else if (line.startsWith("exp=")) exps.add(Expression(line.substring(4), ""))

                    else if (line.startsWith("des=")) exps.last().meaning = line.substring(4)

                    else if (line.startsWith(("syn="))) synonyms.add(line.substring(4))

                    else if (line == "#eoe") {
                        defByPos.add(DefinitionByPos(pos, defGroups.toList()))

                        /**
                         *  use copies of the list to construct the Entry or it will be cleared!
                         */
                        entries.add(DictionaryEntry(currentDict, key, word, ipa, ety, defByPos.toList(), exps.toList(), synonyms.toList()))

                        // clear everything for the next entry

                        key = ""
                        word = ""
                        ipa = ""
                        pos = ""
                        ety = ""
                        defByPos.clear()
                        defGroups.clear()
                        exps.clear()
                        synonyms.clear()
                    }
                }

                Log.i("DictionaryReader", "loaded ${entries.size} entries.")
                Log.i("DictionaryReader", "entries=[${entries}].")

            } else {
                Log.e("DictionaryReader", "Wrong dictionary file format!")
            }
        }
        else {
            Log.e(dictMeta.path, "does not exist!")
        }
    }


    fun search(query: String): List<DictionaryEntry> {
        val queryLower = query.lowercase()
        val results = mutableListOf<DictionaryEntry>()

        for (entry in entries) {
            val key = entry.key
            if (key.startsWith(queryLower)) {
                results += entry
            }
        }
        Log.i("search@DictionaryReader", results.toString())
        return results
    }


}

data class DictionaryEntry(val dictionary: String, val key: String, val word: String, val ipa: String,
                           val etymology: String, val definitionsByPos: List<DefinitionByPos>,
                           val expressions: List<Expression>, val synonyms: List<String>)  : Serializable {
    override fun equals(other: Any?): Boolean {
        other as DictionaryEntry

        if (key != other.key) return false
        if (word != other.word) return false
        if (dictionary != other.dictionary) return false

        return true
    }

    override fun toString(): String = key
    override fun hashCode(): Int {
        var result = dictionary.hashCode()
        result = 31 * result + word.hashCode()
        return result
    }
}

data class DefinitionByPos(val pos: String, val definitionGroups: List<List<String>>)

data class Expression(var text: String, var meaning: String)

//data class DictionaryMeta (val dictionaryName: String, val language: String, val path: String)
data class DictionaryMeta(val internalDir: String, val fileName: String, val dictionaryName: String, val language: String) {
    val path = "$internalDir/$fileName"
    override fun toString(): String = "dict=$dictionaryName, lang=$language, name=$fileName"
}