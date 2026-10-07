package com.sajib.smsexpensetracker.di

import com.sajib.smsexpensetracker.capture.FullLiveCaptureSetup
import com.sajib.smsexpensetracker.capture.LiveCaptureSetup
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Full flavor binding: live capture uses the SMS permission and inbox import. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LiveCaptureModule {

    /** Binds the SMS-backed implementation. */
    @Binds
    abstract fun bindLiveCaptureSetup(setup: FullLiveCaptureSetup): LiveCaptureSetup
}
