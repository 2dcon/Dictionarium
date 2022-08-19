package com.fmaghi.dictionarium

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private val requiredPermissions = arrayOf<String>(READ_EXTERNAL_STORAGE)

    private var currentTextSize: Float = 15f
    private var previousTextSize: Float = 15f

    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter
    private lateinit var tabs: TabLayout

    lateinit var searchPane: SlidingPaneLayout

    private val openFolderLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) {
            Log.i("openFolderLauncher @ MainActivity", it.toString())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
//        if(ContextCompat.checkSelfPermission(this@MainActivity, READ_EXTERNAL_STORAGE) == PERMISSION_DENIED) {
//            Log.i("checkSelfPermission@MainActivity", "Permission denied")
//            requestPermissions(requiredPermissions, 1)
//        } else {
//            Log.i("checkSelfPermission@MainActivity", "Permission granted")
//        }
//        requestPermissionLauncher.launch(READ_EXTERNAL_STORAGE)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO: deal with file permission

        /**
         * search pane
         */
        searchPane = findViewById(R.id.sliding_pane_layout)

        val buttonSearch = findViewById<Button>(R.id.buttonSearch)
        buttonSearch.setOnClickListener { searchPane.open() }


        /**
         * entry tabs
         */
        viewPager = findViewById(R.id.pager)
        pagerAdapter = ScreenSlidePagerAdapter(this)

        viewPager.adapter = pagerAdapter

        tabs = findViewById(R.id.entryTabs)
        tabs.tabMode = TabLayout.MODE_SCROLLABLE
        tabs.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val tabTextView = tab?.view?.findViewById<TextView>(R.id.word)
                tabTextView?.setTextColor(Color.parseColor("#000000"))
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val tabTextView = tab?.view?.findViewById<TextView>(R.id.word)
                tabTextView?.setTextColor(Color.parseColor("#858585"))
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}

        })

        TabLayoutMediator(tabs, viewPager) {tab, position ->
            tab.setCustomView(R.layout.entry_tab)
            val tabTextView = tab.view.findViewById<TextView>(R.id.word)
            tabTextView.text = pagerAdapter.getItem(position).getWord()

            val buttonCLoseTab = tab.view.findViewById<Button>(R.id.closeTab)
            buttonCLoseTab.setOnClickListener {
                pagerAdapter.removeFragment(position)
            }

        }.attach()

        /**
         * text size slider
         */
        val layoutSlider = findViewById<RelativeLayout>(R.id.relativeLayoutTextSize)
        val viewSlider = layoutInflater.inflate(R.layout.slider_text_size, null)

        val buttonApplySize = viewSlider.findViewById<Button>(R.id.applySize)
        buttonApplySize.setOnClickListener {
            previousTextSize = currentTextSize
            layoutSlider.removeAllViews()
        }

        val buttonDiscardSize = viewSlider.findViewById<Button>(R.id.discardSize)
        buttonDiscardSize.setOnClickListener {
            currentTextSize = previousTextSize
            pagerAdapter.setTextSize(currentTextSize)
            layoutSlider.removeAllViews()
        }


        /**
         * popup menu
         */
        val buttonMenu = findViewById<Button>(R.id.buttonMenu)
        buttonMenu.setOnClickListener {
            val popupMenu = PopupMenu(this, buttonMenu)
            popupMenu.menuInflater.inflate(R.menu.menu_popup, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener{ item ->
                when(item.itemId){
                    R.id.openSettings -> {
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                    }

                    R.id.textSize -> {
                        layoutSlider.addView(viewSlider)

                        val textSizeSlider = layoutSlider.findViewById<Slider>(R.id.textSizeSlider)
                        textSizeSlider.addOnChangeListener { _, value, _ ->
                            currentTextSize = value
                            pagerAdapter.setTextSize(value)
                        }
                    }
                }
                true
            }

            popupMenu.show()
        }

    }

    fun openFile(pickerInitialUri: Uri) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
//            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }

    }

    fun addTab(entry: DictionaryEntry){
        // duplicate check
        var index = pagerAdapter.findEntry(entry)

        if (index < 0) {
            index = pagerAdapter.itemCount
            pagerAdapter.addFragment(index, DictionaryEntryFragment(entry, currentTextSize))

        }

        viewPager.currentItem = index
    }

    fun closeSearchPane(){
        searchPane.closePane()
    }

    private inner class ScreenSlidePagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        var fragments: MutableList<DictionaryEntryFragment> = mutableListOf()

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]

        override fun getItemId(position: Int): Long = fragments[position].hashCode().toLong()

        override fun containsItem(itemId: Long): Boolean = fragments.find { it.hashCode().toLong() == itemId } != null

        fun findEntry(entry: DictionaryEntry): Int {
            for (i in fragments.indices) {
                if (entry == fragments[i].getEntry()) {
                    return i
                }
            }

            return -1
        }

        fun addFragment(index: Int, newFragment: DictionaryEntryFragment) {
            fragments.add(index, newFragment)
            notifyItemInserted(index)
        }

        fun removeFragment(index: Int){
            fragments.removeAt(index)
            notifyItemRemoved(index)
        }

        fun getItem(index: Int): DictionaryEntryFragment = fragments[index]

        fun getWord(index: Int): String = fragments[index].getWord()

        fun setTextSize(size: Float) {
            for(fragment in fragments) {
                fragment.resizeText(size)
            }
        }
    }
}

