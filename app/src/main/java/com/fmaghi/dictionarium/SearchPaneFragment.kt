package com.fmaghi.dictionarium

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import java.io.File

class SearchPaneFragment : Fragment() {
    private lateinit var editTextTextSearch: EditText
    private lateinit var searchResults: ListView
    private lateinit var buttonSetDict: Button

    private val reader = DictionaryReader()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                selectFile.launch("*/*")
            }
        }

    private val selectFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val stream = context?.contentResolver?.openInputStream(uri)
            val fileName = uri.path.toString().split(':').last().split('/').last()
            Log.i("SettingsFragment", "fileName = $fileName")

            val dictDir = "${context?.filesDir.toString()}/dict"
            val dictDirFile = File(dictDir)
            val targetFile = File("$dictDir/$fileName")

            if (!dictDirFile.exists()) {
                dictDirFile.mkdir()
            }

            stream.use { input ->
                targetFile.outputStream().use { out ->
                    input?.copyTo(out)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.pane_search, container, false)

        val close = view.findViewById<Button>(R.id.buttonCloseSearch)
        close.setOnClickListener { (activity as MainActivity).searchPane.close() }

        searchResults = view.findViewById(R.id.listViewSearchResults)

        /**
         *  popup window for selecting dictionary
         */
        buttonSetDict = view.findViewById(R.id.buttonSetDict)
        buttonSetDict.setOnClickListener {
            val popupWindow = PopupWindow(this.view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            popupWindow.contentView = inflater.inflate(R.layout.window_select_dict, null)

            val buttonAddDictionary = popupWindow.contentView.findViewById<Button>(R.id.add_dictionary)
            buttonAddDictionary.setOnClickListener {
                // check permission
                val permissionStatus = context?.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                if (permissionStatus == PackageManager.PERMISSION_DENIED){
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    selectFile.launch("*/*")
                }
            }

            val viewDictList = popupWindow.contentView.findViewById<ExpandableListView>(R.id.dictionary_list)
            val dictListAdapter = DictListAdapter(this, popupWindow, reader)

            viewDictList.setAdapter(dictListAdapter)
//            viewDictList.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
//                reader.readDictionary(dictListAdapter.getChild(groupPosition, childPosition))
//                popupWindow.dismiss()
//                false
//            }

            // a close window button that fills the background
            val buttonCloseWindow = popupWindow.contentView.findViewById<Button>(R.id.button_close_window)
            buttonCloseWindow.setOnClickListener {
                popupWindow.dismiss()
            }

            popupWindow.showAtLocation(this.view, Gravity.CENTER , 0,0)

        }

        /**
         * search input
         */
        editTextTextSearch = view.findViewById(R.id.editTextTextSearch)
        editTextTextSearch.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                search(editTextTextSearch.text.toString())
            }
        })

        val buttonClearSearch = view.findViewById<Button>(R.id.buttonClearSearch)
        buttonClearSearch.setOnClickListener {clearSearch()}

        return view
    }

    fun search(query: String) {
        if (query.isEmpty()) {
            buttonSetDict.visibility = View.VISIBLE
            clearSearchResults()
        } else {
            buttonSetDict.visibility = View.GONE
            var results: List<DictionaryEntry> = reader.search(query)

            genSearchResults(results)
        }

    }

    private fun genSearchResults(results: List<DictionaryEntry>) {
//        val arrayAdapter: ArrayAdapter<*>

        val arrayAdapter = ArrayAdapter(activity as Context, android.R.layout.simple_list_item_1, results)
        searchResults.adapter = arrayAdapter

        // handle item click
        searchResults.setOnItemClickListener { _, _, position, _ ->
            val entry = results[position]
//            Log.i("SearchPaneFragment", "entry.subEntries.count() = ${entry.subEntries.count()}")
            (activity as MainActivity).addTab(entry)
            (activity as MainActivity).closeSearchPane()
        }
    }

    private fun clearSearch() {
        editTextTextSearch.text?.clear()
    }

    private fun clearSearchResults() {
        searchResults.adapter = null
    }

}