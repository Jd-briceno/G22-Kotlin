package com.g22.orbitsoundkotlin.ui.screens.activitystats

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

/**
 * Almacenamiento seguro del PIN de Activity Stats.
 * 
 * SEGURIDAD: El PIN se almacena como hash SHA-256, no en texto plano.
 * Esto mitiga el riesgo de almacenamiento inseguro de datos sensibles.
 * 
 * TODO: En producción, considerar usar Android Keystore para mayor seguridad.
 */
class ActivityStatsPinStorage(context: Context) {
    
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Verifica si existe un PIN configurado.
     */
    fun hasPin(): Boolean {
        return prefs.contains(KEY_PIN_HASH)
    }
    
    /**
     * Guarda un nuevo PIN (hasheado).
     * 
     * @param pin PIN de 4 dígitos en texto plano
     * @return true si se guardó correctamente
     */
    fun savePin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            return false
        }
        
        val hash = hashPin(pin)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .apply()
        
        return true
    }
    
    /**
     * Verifica si el PIN ingresado es correcto.
     * 
     * @param pin PIN de 4 dígitos en texto plano
     * @return true si el PIN es correcto
     */
    fun verifyPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            return false
        }
        
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val inputHash = hashPin(pin)
        
        return storedHash == inputHash
    }
    
    /**
     * Elimina el PIN almacenado (para reset).
     */
    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .apply()
    }
    
    /**
     * Genera hash SHA-256 del PIN.
     * 
     * SEGURIDAD: Usa hash unidireccional para evitar recuperar el PIN original.
     */
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    companion object {
        private const val PREFS_NAME = "activity_stats_pin"
        private const val KEY_PIN_HASH = "pin_hash"
    }
}

