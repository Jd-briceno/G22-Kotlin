package com.g22.orbitsoundkotlin.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.g22.orbitsoundkotlin.R

class SearchBarCustom @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var onSearchListener: ((String) -> Unit)? = null
    private val searchEditText: EditText
    private val searchIcon: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.search_bar_custom, this, true)

        searchEditText = findViewById(R.id.searchEditText)
        searchIcon = findViewById(R.id.searchIcon)

        setupSearchBar()
    }

    private fun setupSearchBar() {
        // Configurar el EditText
        searchEditText.hint = "Find your rhythm..."
        searchEditText.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        searchEditText.setHintTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        searchEditText.textSize = 15f
        searchEditText.typeface = Typeface.MONOSPACE

        // Listener para cambios en el texto
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                onSearchListener?.invoke(s?.toString() ?: "")
            }
        })
    }

    fun setOnSearchListener(listener: (String) -> Unit) {
        onSearchListener = listener
    }
}
