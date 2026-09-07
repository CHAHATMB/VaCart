package com.vacart

import android.app.Application
import com.vacart.roomdatabase.PnrCacheDao
import com.vacart.roomdatabase.VacartCacheDao
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class VaCartApplication : Application() {

    @Inject
    lateinit var vacartCacheDao: VacartCacheDao

    @Inject
    lateinit var pnrCacheDao: PnrCacheDao

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        evictExpiredCaches()
    }

    /**
     * Evict expired cache entries on app startup.
     * - VaCart cache: entries older than 24 hours
     * - PNR auto-cache: entries older than 30 minutes
     * - SAVED_PERMANENT PNR entries are never evicted
     */
    private fun evictExpiredCaches() {
        applicationScope.launch {
            val vacartExpiry = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
            val pnrAutoExpiry = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
            vacartCacheDao.evictExpired(vacartExpiry)
            pnrCacheDao.evictExpiredAuto(pnrAutoExpiry)
        }
    }
}