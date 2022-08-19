package com.fmaghi.dictionarium

import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment

class DictionaryEntryFragment(private val entry: DictionaryEntry, private var textSize: Float) : Fragment() {
    private val textSizeNormal = 0
    private val textSizeLarge = 1

    private val textViewsNormal = mutableListOf<TextView>()
    private val textViewsLarge = mutableListOf<TextView>()

    private lateinit var viewEntry: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dictionary_entry, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewEntry = view.findViewById(R.id.entry)

        // word
        val textViewWord = viewEntry.findViewById<TextView>(R.id.word)
        textViewsLarge.add(textViewWord)
        textViewWord.text = entry.word + " "

        // ipa
        setupTextView(viewEntry, R.id.ipa, entry.ipa, textSizeNormal)

        // etymology
        setupTextView(viewEntry, R.id.etymology, entry.etymology, textSizeNormal)

        // definitions
        addDefinitions()

        // expressions
        addExpressions()

        // set all texts' size with the current size
        resizeText(textSize)

    }

    private fun test() {
        Log.i("DictionaryEntryFragment", "${entry.definitionsByPos.count()}")
        Log.i("DictionaryEntryFragment", "${entry.definitionsByPos[0].definitionGroups[0][0]}")
    }

    /**
     *  View structure of definitions:
     *  dictionary_entry.xml -> LinearLayout(layoutLv0)
     *
     *      definitions_by_pos -> TextView(pos)
     *                         -> LinearLayout(layoutLv1) ->
     *
     *          definition_group.xml -> TextView(groupNumber)
     *                               -> LinearLayout(layoutLv2) ->
     *
     *              definition.xml -> TextView(groupNumber)
     *                             -> LinearLayout(layoutLv3) -> definition strings
     *
     *  hierarchy:
     *    layoutEntryDefinition < layoutDefinitionGroups < layoutDefinitions < layoutGroup
     */
    private fun addDefinitions() {
        /**
         * dictionary_entry.xml -> LinearLayout(definitions)
         */
        val layoutLv0 = viewEntry.findViewById<LinearLayout>(R.id.definitions_by_pos)

        for (defByPos in entry.definitionsByPos) {
            // definitions by pos
            val viewLv1 = layoutInflater.inflate(R.layout.definitions_by_pos, null)
            setupTextView(viewLv1, R.id.pos, defByPos.pos, textSizeNormal)
            val layoutLv1 = viewLv1.findViewById<LinearLayout>(R.id.definition_groups)

            // no numbering when there is only one group
            var isGroupNumbered = false
            if (defByPos.definitionGroups.count() > 1) isGroupNumbered = true

            for (i in defByPos.definitionGroups.indices) {
                /**
                 *  definition_group.xml -> TextView(groupNumber)
                 *                       -> LinearLayout(layoutDefinitions) ->
                 */
                val viewLv2 = layoutInflater.inflate(R.layout.definition_group, null)

                if (isGroupNumbered) {
                    val textViewGroupNum =
                        viewLv2.findViewById<TextView>(R.id.groupNumber)
                    textViewsNormal.add(textViewGroupNum)
                    textViewGroupNum.text = "${i + 1} "
                }

                val layoutLv2 =
                    viewLv2.findViewById<LinearLayout>(R.id.definitions)

                // no numbering when there is only one definition in the group
                var isDefNumbered = false
                if (defByPos.definitionGroups[i].count() > 1) isDefNumbered = true

                for (j in defByPos.definitionGroups[i].indices) {
                    // inside a definition group
                    val viewLv3 = layoutInflater.inflate(R.layout.definition, null)

                    if (isDefNumbered) {
                        setupTextView(viewLv3, R.id.defLetter, "${'a' + j}. ", textSizeNormal)
                    }

                    setupTextView(viewLv3, R.id.definition, defByPos.definitionGroups[i][j], textSizeNormal)

                    layoutLv2.addView(viewLv3)
                }
                layoutLv1.addView(viewLv2)
            }
            layoutLv0.addView(viewLv1)
        }
    }

    private fun addExpressions() {
        for (expression in entry.expressions) {
            val viewExpression = layoutInflater.inflate(R.layout.expression, null)

            setupTextView(viewExpression, R.id.expression_title, expression.text, textSizeNormal)

            setupTextView(viewExpression, R.id.expression_meaning, expression.meaning, textSizeNormal)

            // add it to the entry's view
            val layoutEntry = view?.findViewById<LinearLayout>(R.id.entry)
            layoutEntry?.addView(viewExpression)
        }
    }

    private fun addSynonyms(synonyms: List<String>) {
        if (synonyms.isNotEmpty()) {
            var syns = ""

            val viewSyn = layoutInflater.inflate(R.layout.synonyms, null)

            val textViewSyn = viewSyn.findViewById<TextView>(R.id.synonyms)
            textViewSyn.textSize = textSize
            textViewsNormal.add(textViewSyn)

            for (synonym in synonyms) {
                syns += synonym + '\n'
            }

            textViewSyn.text = syns

            viewEntry.addView(viewSyn)
        }
    }

    private fun setupTextView(parentView: View, textViewID: Int, text: String, textSize: Int) {
        val targetTextView = parentView.findViewById<TextView>(textViewID)
        when(textSize) {
            textSizeNormal -> textViewsNormal.add(targetTextView)
            textSizeLarge -> textViewsLarge.add(targetTextView)
            else -> textViewsNormal.add(targetTextView)
        }
        targetTextView.text = Html.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
        return
    }

    fun getWord(): String = entry.key

    fun getEntry(): DictionaryEntry = entry

    fun resizeText(newSize: Float) {
        for (normal in textViewsNormal) {
            normal.textSize = newSize
        }
        for (large in textViewsLarge) {
            large.textSize = newSize * 1.5f
        }
    }
}