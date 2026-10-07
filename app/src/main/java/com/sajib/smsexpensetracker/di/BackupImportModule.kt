package com.sajib.smsexpensetracker.di

import android.util.Xml
import com.sajib.smsexpensetracker.data.backup.BackupFileOpener
import com.sajib.smsexpensetracker.data.backup.ContentResolverBackupFileOpener
import com.sajib.smsexpensetracker.data.backup.XmlParserProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Bindings for importing an SMS Backup & Restore file. Shared by both
 * flavors: the import needs no permission at all.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupImportModule {

    /** Picked files are read through ContentResolver. */
    @Binds
    abstract fun bindBackupFileOpener(opener: ContentResolverBackupFileOpener): BackupFileOpener

    companion object {

        /** The platform XML pull parser, a fresh instance per import. */
        @Provides
        fun provideXmlParserProvider(): XmlParserProvider = XmlParserProvider { Xml.newPullParser() }
    }
}
