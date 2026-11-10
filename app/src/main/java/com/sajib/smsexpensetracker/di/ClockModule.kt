package com.sajib.smsexpensetracker.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** Provides the clock used for user-facing "now" defaults, so tests can fix the time. */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {

    /**
     * The system clock in the device's current time zone. Not a singleton:
     * each consumer reads the zone when it is created, so a zone change is
     * picked up by the next screen.
     */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
