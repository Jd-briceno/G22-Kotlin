package com.g22.orbitsoundkotlin.data.repositories

import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.local.entities.OutboxEntity
import com.g22.orbitsoundkotlin.data.local.entities.OutboxOperationType
import com.g22.orbitsoundkotlin.data.local.entities.UserInterestsEntity
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository para intereses del usuario con versionado y outbox pattern.
 * Implementa conflict resolution con Last-Write-Wins.
 */
class InterestsRepository(
    private val db: AppDatabase
) {
    private val interestsDao = db.interestsDao()
    private val outboxDao = db.outboxDao()
    private val gson = Gson()

    /**
     * Obtiene intereses del usuario. Retorna cache local si existe.
     */
    suspend fun getInterests(uid: String): List<String>? {
        return interestsDao.getInterestsByUid(uid)?.interests
    }

    /**
     * Obtiene intereses como Flow para observación reactiva.
     */
    fun getInterestsFlow(uid: String): Flow<List<String>?> {
        return interestsDao.getInterestsByUidFlow(uid).map { it?.interests }
    }

    /**
     * Guarda intereses localmente y encola operación en Outbox para sincronización.
     */
    suspend fun saveInterests(uid: String, interests: List<String>) {
        val existing = interestsDao.getInterestsByUid(uid)

        val newVersion = (existing?.version ?: 0) + 1
        val interestsEntity = UserInterestsEntity(
            uid = uid,
            interests = interests,
            version = newVersion,
            lastModified = System.currentTimeMillis(),
            needsSync = true
        )

        // Guardar en Room
        interestsDao.insertInterests(interestsEntity)

        // Agregar a Outbox para sincronización
        val payload = JsonObject().apply {
            addProperty("uid", uid)
            add("interests", gson.toJsonTree(interests))
            addProperty("version", newVersion)
            addProperty("lastModified", System.currentTimeMillis())
        }

        val outboxOp = OutboxEntity(
            operationType = OutboxOperationType.UPSERT_INTERESTS,
            payload = payload,
            synced = false
        )

        outboxDao.insertOperation(outboxOp)
    }

    /**
     * Marca intereses como sincronizados.
     */
    suspend fun markAsSynced(uid: String, serverTimestamp: Long) {
        val existing = interestsDao.getInterestsByUid(uid) ?: return

        val updated = existing.copy(
            needsSync = false,
            serverTimestamp = serverTimestamp
        )

        interestsDao.updateInterests(updated)
    }

    /**
     * Obtiene todos los intereses que necesitan sincronización.
     */
    suspend fun getInterestsNeedingSync(): List<UserInterestsEntity> {
        return interestsDao.getInterestsNeedingSync()
    }
}

