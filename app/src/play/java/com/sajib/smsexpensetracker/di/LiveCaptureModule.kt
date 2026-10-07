package com.sajib.smsexpensetracker.di

import com.sajib.smsexpensetracker.capture.LiveCaptureSetup
import com.sajib.smsexpensetracker.capture.PlayLiveCaptureSetup
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Play flavor binding: live capture is a no-op, since play has no SMS permission. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LiveCaptureModule {

    /** Binds the no-op implementation. */
    @Binds
    abstract fun bindLiveCaptureSetup(setup: PlayLiveCaptureSetup): LiveCaptureSetup
}
