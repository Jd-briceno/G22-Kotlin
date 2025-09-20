package com.g22.orbitsoundkotlin.ui.components

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.g22.orbitsoundkotlin.R

class Navbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val titleTextView: TextView
    private val subtitleTextView: TextView
    private val profileImageView: ImageView
    private val homeButton: ImageView
    private val notificationButton: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.navbar, this, true)

        titleTextView = findViewById(R.id.titleTextView)
        subtitleTextView = findViewById(R.id.subtitleTextView)
        profileImageView = findViewById(R.id.profileImageView)
        homeButton = findViewById(R.id.homeButton)
        notificationButton = findViewById(R.id.notificationButton)
    }

    fun setData(
        username: String,
        title: String,
        profileImage: String? = null,
        subtitle: String? = null
    ) {
        titleTextView.text = title
        subtitleTextView.text = subtitle ?: "Hello, $username"

        // Configurar imagen de perfil si está disponible
        profileImage?.let { imagePath ->
            if (imagePath.startsWith("http")) {
                // Cargar imagen de red (necesitarías una librería como Glide o Picasso)
                // Glide.with(context).load(imagePath).into(profileImageView)
            } else {
                // Cargar imagen local
                val resourceId = context.resources.getIdentifier(
                    imagePath.replace("assets/images/", "").replace(".jpg", "").replace(".png", ""),
                    "drawable",
                    context.packageName
                )
                if (resourceId != 0) {
                    profileImageView.setImageResource(resourceId)
                }
            }
        }
    }
}
