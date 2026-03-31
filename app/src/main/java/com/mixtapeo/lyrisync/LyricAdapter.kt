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
    private var highlightedWords: List<List<String>> = emptyList(),
    private val onLineClick: (Int) -> Unit = {}
) : RecyclerView.Adapter<LyricAdapter.LyricViewHolder>() {

    var activeIndex: Int = -1
    var textSize: Float = 22f
    var furiganaSize: Float = 12f
    var translationSize: Float = 16f

    fun updateData(
        newLyrics: List<LyricLine>?, // Make these nullable for safety
        newTranslations: List<String>?,
        newFurigana: List<String>?,
        newHighlights: List<List<String>>?
    ) {
        // 1. Clear the active index so the "highlight" doesn't ghost onto the next song
        this.activeIndex = -1

        // 2. Assign new data or empty lists if null
        this.lyrics = newLyrics ?: emptyList()
        this.translations = newTranslations ?: emptyList()
        this.furiganaList = newFurigana ?: emptyList()
        this.highlightedWords = newHighlights ?: emptyList()

        // 3. Force a full refresh
        notifyDataSetChanged()

        Log.d("Lyrisync", "Data Reset. New size: ${this.lyrics.size}")
    }

    class LyricViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var textSize: Float = 24f
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
        // Notify the adapter to re-bind visible items with the new size
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        val lyricText = lyrics[position].text
        val hasJapanese = lyricText.contains(jpCharacterRegex)
        val isGap = lyricText == "..."
        holder.jp.text = lyricText
        holder.en.text = translations.getOrNull(position) ?: ""

        val furiganaContent = furiganaList.getOrNull(position) ?: ""
        holder.furigana?.text = furiganaContent

        // Apply text sizes in SP
        holder.jp.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        holder.furigana?.setTextSize(TypedValue.COMPLEX_UNIT_SP, furiganaSize)
        holder.en.setTextSize(TypedValue.COMPLEX_UNIT_SP, translationSize)


        if (position == activeIndex) {
            // Active State: Spotify Green, fully opaque
            holder.jp.setTextColor("#1DB954".toColorInt())
            holder.jp.alpha = 1.0f
        } else {
            // Inactive State: Pure White, slightly dimmed
            holder.jp.setTextColor(android.graphics.Color.WHITE)
            holder.jp.alpha = 0.5f // Dimming inactive lines looks super premium!
        }

        holder.itemView.setOnClickListener {
            onLineClick(position)
        }


        val spannable = android.text.SpannableString(lyrics[position].text)
        val text = lyrics[position].text

        val wordsToHighlight = highlightedWords.getOrNull(position) ?: emptyList()
        var searchStartIndex = 0

        // 1. Define your Color Palette
        val highlightColors = intArrayOf(
            "#FFD54F".toColorInt(), // Yellow
            "#81C784".toColorInt(), // Green
            "#64B5F6".toColorInt(), // Blue
            "#E57373".toColorInt(), // Red
            "#BA68C8".toColorInt()  // Purple
        )

        // 2. Use the index to pick the color
        wordsToHighlight.forEachIndexed { wordIndex, word ->
            val startIndex = text.indexOf(word, searchStartIndex)

            if (startIndex != -1) {
                val endIndex = startIndex + word.length

                // Assign the color from the palette
                val assignedColor = highlightColors[wordIndex % highlightColors.size]

                spannable.setSpan(
                    android.text.style.UnderlineSpan(),
                    startIndex, endIndex,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    android.text.style.ForegroundColorSpan(assignedColor),
                    startIndex, endIndex,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                searchStartIndex = endIndex
            }
        }

        val sharedPrefs = holder.itemView.context.getSharedPreferences("LyriSyncPrefs", Context.MODE_PRIVATE)
        val subtitleMode = sharedPrefs.getInt("SUBTITLE_MODE", 2)

        when (subtitleMode) {
            0 -> { // None
                holder.furigana?.visibility = View.GONE
                holder.en.visibility = View.GONE
            }
            1 -> { // Furigana Only
                // Only show if there is actually Japanese to show furigana for
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

        holder.jp.text = spannable

        // 2. Fire the callback when the user taps anywhere on this lyric row!
        if (position == activeIndex) {
            holder.jp.setTextColor("#1DB954".toColorInt())
            holder.jp.alpha = 1.0f
        } else {
            holder.jp.setTextColor(android.graphics.Color.WHITE)
            holder.jp.alpha = 0.5f
        }

        holder.itemView.setOnClickListener {
            onLineClick(position)
        }
    }

    override fun getItemCount() = lyrics.size
}