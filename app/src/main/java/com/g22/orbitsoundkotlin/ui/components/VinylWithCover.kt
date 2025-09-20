package com.g22.orbitsoundkotlin.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.g22.orbitsoundkotlin.R

class VinylWithCover @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var albumArt: String? = null
    private var isSpinning: Boolean = false
    private var rotationAngle: Float = 0f

    private val vinylPaint = Paint().apply {
        color = Color.parseColor("#2C2C2C")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#B4B1B8")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val centerPaint = Paint().apply {
        color = Color.parseColor("#010B19")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        // Iniciar animación de rotación si está spinning
        if (isSpinning) {
            startSpinning()
        }
    }

    fun setAlbumArt(albumArt: String) {
        this.albumArt = albumArt
        invalidate()
    }

    fun setSpinning(spinning: Boolean) {
        this.isSpinning = spinning
        if (spinning) {
            startSpinning()
        } else {
            stopSpinning()
        }
    }

    private fun startSpinning() {
        // Implementar animación de rotación
        // Por simplicidad, aquí solo invalidamos la vista
        postInvalidate()
    }

    private fun stopSpinning() {
        // Detener animación
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) / 2f - 10f

        // Guardar el estado del canvas
        canvas.save()

        // Aplicar rotación si está spinning
        if (isSpinning) {
            canvas.rotate(rotationAngle, centerX, centerY)
        }

        // Dibujar el vinilo
        canvas.drawCircle(centerX, centerY, radius, vinylPaint)
        canvas.drawCircle(centerX, centerY, radius, borderPaint)

        // Dibujar círculos concéntricos
        val innerRadius1 = radius * 0.8f
        val innerRadius2 = radius * 0.6f
        val innerRadius3 = radius * 0.4f

        borderPaint.strokeWidth = 2f
        canvas.drawCircle(centerX, centerY, innerRadius1, borderPaint)
        canvas.drawCircle(centerX, centerY, innerRadius2, borderPaint)
        canvas.drawCircle(centerX, centerY, innerRadius3, borderPaint)

        // Dibujar el centro
        val centerRadius = radius * 0.15f
        canvas.drawCircle(centerX, centerY, centerRadius, centerPaint)
        canvas.drawCircle(centerX, centerY, centerRadius, borderPaint)

        // Restaurar el estado del canvas
        canvas.restore()

        // TODO: Aquí se cargaría la imagen del álbum si está disponible
        // Por ahora solo dibujamos el vinilo básico
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = minOf(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }
}
