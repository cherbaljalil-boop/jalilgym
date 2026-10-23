package com.chdjalil.jalilgym.core.domain.repository

/** Imports bundled reference data (exercises, templates) into the local database. */
interface SeedRepository {
    /** Imports the bundled seed if it is newer than what's installed. Safe to call on every launch. */
    suspend fun seedIfNeeded()
}
