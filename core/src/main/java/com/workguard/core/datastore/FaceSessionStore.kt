package com.workguard.core.datastore

import com.workguard.core.model.FaceSession
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

interface FaceSessionStore {
    fun getSession(): FaceSession?
    fun saveSession(session: FaceSession)
    fun clear()
}

@Singleton
class InMemoryFaceSessionStore @Inject constructor() : FaceSessionStore {
    private val sessionRef = AtomicReference<FaceSession?>(null)

    override fun getSession(): FaceSession? = sessionRef.get()

    override fun saveSession(session: FaceSession) {
        sessionRef.set(session)
    }

    override fun clear() {
        sessionRef.set(null)
    }
}
