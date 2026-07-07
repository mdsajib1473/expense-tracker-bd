package com.sajib.smsexpensetracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Annotated with [HiltAndroidApp] so Hilt generates
 * the application-level dependency graph. Hilt modules live under di/ and are
 * added starting at M1; at M0 this only proves the Hilt toolchain compiles.
 */
@HiltAndroidApp
class SmsExpenseTrackerApp : Application()
