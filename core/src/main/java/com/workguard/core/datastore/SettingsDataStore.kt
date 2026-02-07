package com.workguard.core.datastore

import javax.inject.Inject

interface SettingsDataStore {
    fun clear()
}

class InMemorySettingsDataStore @Inject constructor() : SettingsDataStore {
    override fun clear() {
    }
}
