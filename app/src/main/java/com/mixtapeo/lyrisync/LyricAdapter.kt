package com.mixtapeo.lyrisync

import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.toColorInt

class LyricAdapter(
    private var lyrics: List<LyricLine> = emptyList(),
    private var translations: List<String> = emptyList(),
    private var furiganaList: List<String> = emptyList(),
    // 1. UPDATED TO EXPECT HIGHLIGHTSPAN
    private var highlightedWords: List<List<HighlightSpan>> = emptyList(),
    private val onLineClick: (Int) -> Unit = {}
) : RecyclerView.Adapter<LyricAdapter.LyricViewHolder>() {

    var activeIndex: Int = -1
    var textSize: Float = 22f
    var furiganaSize: Float = 12f
    var translationSize: Float = 16f

    fun updateData(
        newLyrics: List<LyricLine>?,
        newTranslations: List<String>?,
        newFurigana: List<String>?,
        // 2. UPDATED TO EXPECT HIGHLIGHTSPAN
        newHighlights: List<List<HighlightSpan>>?
    ) {
        this.activeIndex = -1
        this.lyrics = newLyrics ?: emptyList()
        this.translations = newTranslations ?: emptyList()
        this.furiganaList = newFurigana ?: emptyList()
        this.highlightedWords = newHighlights ?: emptyList()

        notifyDataSetChanged()
        Log.d("Lyrisync", "Data Reset. New size: ${this.lyrics.size}")
    }

    class LyricViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val jp: TextView = view.findViewById(R.id.itemJp)
        val en: TextView = view.findViewById(R.id.itemEn)
        val furigana: TextView? = view.findViewById(R.id.itemFurigana)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lyric, parent, false)
        return LyricViewHolder(view)
    }

    private val jpCharacterRegex = Regex("[\\u3040-\\u30ff\\u4e00-\\u9faf]")

    fun updateTextSize(newSize: Float) {
        this.textSize = newSize
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        val lyricText = lyrics[position].text
        val hasJapanese = lyricText.contains(jpCharacterRegex)

        holder.en.text = translations.getOrNull(position) ?: ""
        val furiganaContent = furiganaList.getOrNull(position) ?: ""
        holder.furigana?.text = furiganaContent

        // Apply text sizes in SP
        holder.jp.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        holder.furigana?.setTextSize(TypedValue.COMPLEX_UNIT_SP, furiganaSize)
        holder.en.setTextSize(TypedValue.COMPLEX_UNIT_SP, translationSize)

        // --- 3. SET BASE TEXT COLOR FIRST (Before applying spans!) ---
        if (position == activeIndex) {
            holder.jp.setTextColor("#1DB954".toColorInt()) // Active Green
            holder.jp.alpha = 1.0f
        } else {
            holder.jp.setTextColor(android.graphics.Color.WHITE) // Inactive White
            holder.jp.alpha = 0.5f
        }

        // --- 4. APPLY PRECISE SPAN COORDINATES ---
        val spannable = android.text.SpannableString(lyricText)
        val wordsToHighlight = highlightedWords.getOrNull(position) ?: emptyList()

        if (wordsToHighlight.isNotEmpty()) {
//            Log.d("Lyrisync-Color", "Adapter Line $position: Received ${wordsToHighlight.size} spans to color!")
        }

        val highlightColors = intArrayOf(
            "#FFD54F".toColorInt(), // Yellow
            "#81C784".toColorInt(), // Green
            "#64B5F6".toColorInt(), // Blue
            "#E57373".toColorInt(), // Red
            "#BA68C8".toColorInt()  // Purple
        )

        for (span in wordsToHighlight) {
            // Safety check
            if (span.start >= 0 && span.end <= lyricText.length) {
                val assignedColor = highlightColors[span.wordIndex % highlightColors.size]

                spannable.setSpan(
                    android.text.style.UnderlineSpan(),
                    span.start, span.end,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    android.text.style.ForegroundColorSpan(assignedColor),
                    span.start, span.end,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
//                Log.e("Lyrisync-Color", "SPAN BLOCKED! Text length is ${lyricText.length}, but span wants ${span.start}-${span.end}")
            }
        }

        // Apply the colored string to the TextView
        holder.jp.text = spannable

        // --- 5. SUBTITLE VISIBILITY LOGIC ---
        val sharedPrefs = holder.itemView.context.getSharedPreferences("LyriSyncPrefs", Context.MODE_PRIVATE)
        val subtitleMode = sharedPrefs.getInt("SUBTITLE_MODE", 2)

        when (subtitleMode) {
            0 -> { // None
                holder.furigana?.visibility = View.GONE
                holder.en.visibility = View.GONE
            }
            1 -> { // Furigana Only
                holder.furigana?.visibility = if (hasJapanese) View.VISIBLE else View.GONE
                holder.en.visibility = View.GONE
            }
            2 -> { // Both
                holder.furigana?.visibility = if (hasJapanese) View.VISIBLE else View.GONE
                holder.en.visibility = View.VISIBLE
            }
            3 -> { // English Only
                holder.furigana?.visibility = View.GONE
                holder.en.visibility = View.VISIBLE
            }
        }

        // Click Listener
        holder.itemView.setOnClickListener {
            onLineClick(position)
        }
    }

    override fun getItemCount() = lyrics.size
}