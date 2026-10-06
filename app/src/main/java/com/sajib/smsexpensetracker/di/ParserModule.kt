package com.sajib.smsexpensetracker.di

import com.sajib.smsexpensetracker.parser.core.ParserEngine
import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.institutions.BkashParser
import com.sajib.smsexpensetracker.parser.institutions.BracBankParser
import com.sajib.smsexpensetracker.parser.institutions.DutchBanglaParser
import com.sajib.smsexpensetracker.parser.institutions.NagadParser
import com.sajib.smsexpensetracker.parser.institutions.PubaliBankParser
import com.sajib.smsexpensetracker.parser.institutions.UttaraBankParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Registers every institution parser and builds the [ParserEngine] from the
 * full set. Adding a new institution means one new @Provides @IntoSet
 * function here plus the parser file itself; nothing else changes.
 */
@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides
    @IntoSet
    fun provideBkashParser(): SmsParser = BkashParser()

    @Provides
    @IntoSet
    fun provideDutchBanglaParser(): SmsParser = DutchBanglaParser()

    @Provides
    @IntoSet
    fun provideNagadParser(): SmsParser = NagadParser()

    @Provides
    @IntoSet
    fun provideUttaraBankParser(): SmsParser = UttaraBankParser()

    @Provides
    @IntoSet
    fun providePubaliBankParser(): SmsParser = PubaliBankParser()

    @Provides
    @IntoSet
    fun provideBracBankParser(): SmsParser = BracBankParser()

    /**
     * The engine takes a List, so the multibound Set is converted here.
     * Set multibinding keeps registration order-independent; no parser may
     * rely on running before another.
     */
    @Provides
    @Singleton
    fun provideParserEngine(parsers: Set<@JvmSuppressWildcards SmsParser>): ParserEngine =
        ParserEngine(parsers.toList())
}
