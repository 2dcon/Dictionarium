package com.fmaghi.dictionarium

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import java.io.File
import java.nio.file.Files.delete

/**
 *  about the class:
 *      1. loads the dictionaries on disk
 *      2. generate meta file the contains dict name, language, and path
 */
class DictListAdapter internal constructor(private val searchPaneFragment: SearchPaneFragment,
                                           private val popupWindow: PopupWindow,
                                           private val reader: DictionaryReader)
    : BaseExpandableListAdapter() {

    private val context = searchPaneFragment.context
    private val metaFileName = "dict.meta"
    private val dictMetaList = mutableListOf<DictionaryMeta>();
    private val dictByLang = HashMap<String, MutableList<DictionaryMeta>>();
    private val languageList = mutableListOf<String>()
    private val metaEndMark = "#eom"

    init {
        loadMeta()
    }


    // implementation of parent member
    private val inflater = LayoutInflater.from(context)

    override fun getGroupCount(): Int = languageList.size

    override fun getChildrenCount(groupPosition: Int): Int = dictByLang[languageList[groupPosition]]!!.size

    override fun getGroup(groupPosition: Int): String = languageList[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): DictionaryMeta = dictByLang[languageList[groupPosition]]!![childPosition]

    override fun getGroupId(groupPosition: Int): Long = (groupPosition * 1000).toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = (groupPosition * 1000 + childPosition).toLong()

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val groupView = inflater.inflate(R.layout.activity_select_dict_lang_group, null)

        val textViewGroup = groupView.findViewById<TextView>(R.id.language)
        textViewGroup.text = getGroup(groupPosition)

        return groupView
    }


    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val childView = inflater.inflate(R.layout.activity_select_dict_lang_child, null)

        val textViewChild = childView.findViewById<TextView>(R.id.dictionary_name)
        textViewChild.text = getChild(groupPosition, childPosition).dictionaryName

        val buttonSelDict = childView.findViewById<Button>(R.id.select_dictionary)
        buttonSelDict.setOnClickListener {
            Log.i("DictListAdapter", "select_dictionary clicked!")
            reader.readDictionary(getChild(groupPosition, childPosition))
            popupWindow.dismiss()
        }

        val buttonDelDict = childView.findViewById<Button>(R.id.delete_dictionary)
        buttonDelDict.setOnClickListener {
            val windowConfirm = PopupWindow(searchPaneFragment.view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            windowConfirm.contentView = inflater.inflate(R.layout.window_delete_dictionary, null)

            val confirmMessage = windowConfirm.contentView.findViewById<TextView>(R.id.delete_dictionary_message)
            confirmMessage.text = "Delete ${getChild(groupPosition, childPosition).dictionaryName}?"

            val buttonBackground = windowConfirm.contentView.findViewById<Button>(R.id.button_close_window)
            buttonBackground.setOnClickListener {
                windowConfirm.dismiss()
            }

            val buttonConfirm = windowConfirm.contentView.findViewById<Button>(R.id.button_delete_dictionary)
            buttonConfirm.setOnClickListener {
                deleteDictionary(getChild(groupPosition, childPosition))
                windowConfirm.dismiss()
            }

            windowConfirm.showAtLocation(searchPaneFragment.view, Gravity.CENTER, 0, 0)
        }

        return childView
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = false

    override fun areAllItemsEnabled(): Boolean = false

    fun loadMeta() {
        // clear lists
        dictMetaList.clear()
        languageList.clear()
        dictByLang.clear()

        val metaFile = File(context?.filesDir, metaFileName)
        if (!metaFile.exists()) {
            metaFile.createNewFile()
        }

        // load meta file
        var isMetaChanged = false
        val dir = "${context?.filesDir?.path}/dict"
        var dict = ""
        var lang = ""
        var name = ""
        var date = 0

        for (line in metaFile.readLines()) {
            if (line.startsWith("dict=")) dict = line.substring(5)
            if (line.startsWith("date=")) date = line.substring(5).toInt()
            if (line.startsWith("lang=")) lang = line.substring(5)
            if (line.startsWith("name=")) name = line.substring(5)

            if (line.startsWith(metaEndMark)) {
                dictMetaList.add(DictionaryMeta(dir, name, dict, date, lang))
                dict = ""
                lang = ""
                name = ""
                date = 0
            }
        }

        // find out dicts that no longer exist
        for (meta in dictMetaList) {
            val file = File(meta.path)
            if (!file.exists()) {
                dictMetaList.remove(meta)
                Log.i("DictListAdapter", "removing ${meta.dictionaryName} from metaList")
                isMetaChanged = true
            }
        }

        // find out the files that are not in meta, and create meta data for them
        File(dir).walk().forEach {
            if (it.name.endsWith(".dict") && !fileInMeta(it.name)) {
                val first2lines = it.bufferedReader().useLines { lines ->
                    lines.take(2).toList()
                }

                var dictName = ""
                if (first2lines[0].length > 5) dictName = first2lines[0].substring(5)

                var dictLang = ""
                if (first2lines[1].length > 5) dictLang = first2lines[1].substring(5)

                val newMeta = DictionaryMeta(dir, it.name, dictName, date, dictLang)
                dictMetaList.add(newMeta)
                isMetaChanged = true
            }
        }

        // update meta file
        if (isMetaChanged) {
            var metaString = ""
            for (meta in dictMetaList) {
                metaString += "${meta.dictionaryName}\n${meta.language}\n${meta.language}\n\n"
            }
            metaFile.writeText(metaString)
        }

        // categorize metas by language
        for (m in dictMetaList) {
            val lang = m.language
            if (!dictByLang.containsKey(lang)) {
                dictByLang[lang] = mutableListOf()
                languageList.add(lang)
            }
            dictByLang[lang]!!.add(m)
        }
        Log.i("DictListAdapter", "Loaded ${dictMetaList.size} dictionaries")
        languageList.sort()
        notifyDataSetChanged()
    }

    private fun fileInMeta(path: String) : Boolean {

        for (meta in dictMetaList) {
//            Log.i("fileInMeta", "meta.path = ${meta.path}")
            if (path == meta.fileName) return true
        }
//        Log.i("fileInMeta", "path = $path, not in meta")
        return false
    }

    private fun deleteDictionary(meta: DictionaryMeta) {
        val fileToDelete = File(meta.path)
        val success = fileToDelete.delete()
        if (success) {
            Log.i("DictListAdapter", "Deleted dictionary ${meta.dictionaryName}")
            loadMeta()
        } else {
            Log.i("DictListAdapter", "Failed to delete ${meta.path}")
        }
    }
}
