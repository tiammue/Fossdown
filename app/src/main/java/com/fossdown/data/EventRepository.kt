package com.fossdown.data

import kotlinx.coroutines.flow.Flow

class EventRepository(private val dao: EventDao) {
    fun observeActive(): Flow<List<ProgressEvent>> = dao.observeActive()
    fun observeById(id: Long): Flow<ProgressEvent?> = dao.observeById(id)
    suspend fun getActive(): List<ProgressEvent> = dao.getActive()
    suspend fun getById(id: Long): ProgressEvent? = dao.getById(id)
    suspend fun save(event: ProgressEvent): Long = dao.upsert(event)
    suspend fun delete(event: ProgressEvent) = dao.delete(event)
}
