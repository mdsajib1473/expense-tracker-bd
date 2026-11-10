package com.sajib.smsexpensetracker.ui.manualentry

import com.sajib.smsexpensetracker.data.repository.TransactionLookup
import com.sajib.smsexpensetracker.data.repository.TransactionWriter
import com.sajib.smsexpensetracker.domain.model.ManualEntrySaveResult
import com.sajib.smsexpensetracker.domain.usecase.PrepareManualEntryUseCase
import com.sajib.smsexpensetracker.domain.usecase.SaveManualEntryUseCase
import com.sajib.smsexpensetracker.parser.ParserTestMessages
import com.sajib.smsexpensetracker.parser.core.ParsedTransaction
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.core.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Covers the share and paste intake state machine with the real parsers,
 * the real use cases and fake storage. Robolectric provides android.util.Log
 * so the tests can prove no message text is ever logged. Every value is
 * invented.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ManualEntryViewModelTest {

    /** In-memory store with the repository's duplicate rule and similar-transaction check. */
    private class FakeStore : TransactionWriter, TransactionLookup {
        val saved = mutableListOf<Pair<ParsedTransaction, Long>>()

        override suspend fun saveIfNew(parsed: ParsedTransaction, receivedAt: Long): Boolean {
            val duplicate = saved.any { (existing, at) ->
                existing.institutionName == parsed.institutionName && at == receivedAt &&
                    existing.result.amount == parsed.result.amount && existing.result.type == parsed.result.type &&
                    existing.result.balance == parsed.result.balance
            }
            if (duplicate) return false
            saved += parsed to receivedAt
            return true
        }

        override suspend fun hasSimilar(
            institutionName: String,
            type: TransactionType,
            amount: BigDecimal,
            balance: BigDecimal?
        ): Boolean = saved.any { (existing, _) ->
            existing.institutionName == institutionName && existing.result.type == type &&
                existing.result.amount == amount && existing.result.balance == balance
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val zone = ZoneId.of("Asia/Dhaka")
    private val now = LocalDateTime.of(2026, 3, 1, 10, 30, 45).atZone(zone).toInstant()
    private val nowToTheMinute = LocalDateTime.of(2026, 3, 1, 10, 30).atZone(zone).toInstant().toEpochMilli()
    private val store = FakeStore()

    private val bkashPayment =
        "Payment Tk 100.00 to SAMPLE MERCHANT-RM00001 is successful. Balance Tk 600.00. TrxID HHHHHHHHHH at 01/07/2026 10:00"
    private val otp = "531864"
    private val otpBody = "Your bKash verification code is $otp. Expires in 2 minutes."

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(parsers: List<SmsParser> = ParserTestMessages.allParsers()) = ManualEntryViewModel(
        PrepareManualEntryUseCase(ParserEngine(parsers), store),
        SaveManualEntryUseCase(store),
        Clock.fixed(now, zone)
    )

    private fun assertNothingLeaked(vm: ManualEntryViewModel, vararg secrets: String) {
        secrets.forEach { secret ->
            assertFalse(vm.state.value.toString().contains(secret))
            assertFalse(ShadowLog.getLogs().any { it.msg.orEmpty().contains(secret) })
        }
    }

    @Test
    fun `starts idle with an empty draft`() {
        val vm = viewModel()

        assertEquals(ManualEntryState.Idle, vm.state.value)
        assertEquals("", vm.draft.value)
    }

    @Test
    fun `pasting and checking a recognized message goes input, checking, preview and drops the text`() = runTest {
        val vm = viewModel()

        vm.openPaste()
        assertEquals(ManualEntryState.Input, vm.state.value)
        vm.onDraftChange(bkashPayment)
        assertEquals(bkashPayment, vm.draft.value)

        vm.check()
        assertEquals(ManualEntryState.Checking, vm.state.value)
        assertEquals("", vm.draft.value)
        advanceUntilIdle()

        val preview = vm.state.value as ManualEntryState.Preview
        assertEquals("bKash", preview.preview.institutionName)
        assertEquals(TransactionType.DEBIT, preview.preview.type)
        assertEquals(BigDecimal("100.00"), preview.preview.amount)
        assertEquals(BigDecimal("600.00"), preview.preview.balance)
        assertEquals("HHHHHHHHHH", preview.preview.reference)
        assertEquals(nowToTheMinute, preview.receivedAt)
        assertFalse(preview.isSaving)
        assertNothingLeaked(vm, bkashPayment, "SAMPLE MERCHANT")
    }

    @Test
    fun `saving a preview stores it once and reports saved`() = runTest {
        val vm = viewModel()
        vm.onSharedText(bkashPayment)
        advanceUntilIdle()

        vm.save()
        assertTrue((vm.state.value as ManualEntryState.Preview).isSaving)
        vm.save()
        advanceUntilIdle()

        assertEquals(ManualEntryState.Saved(ManualEntrySaveResult.Saved), vm.state.value)
        assertEquals(1, store.saved.size)
        assertEquals(nowToTheMinute, store.saved.single().second)
    }

    @Test
    fun `the same message saved again at the same time reports duplicate and warns before saving`() = runTest {
        val vm = viewModel()
        vm.onSharedText(bkashPayment)
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()

        vm.onSharedText(bkashPayment)
        advanceUntilIdle()
        assertTrue((vm.state.value as ManualEntryState.Preview).preview.similarExists)
        vm.save()
        advanceUntilIdle()

        assertEquals(ManualEntryState.Saved(ManualEntrySaveResult.Duplicate), vm.state.value)
        assertEquals(1, store.saved.size)
    }

    @Test
    fun `picked date and time replace the default and are saved`() = runTest {
        val vm = viewModel()
        vm.onSharedText(bkashPayment)
        advanceUntilIdle()

        vm.onDateSelected(LocalDate.of(2026, 2, 14).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        vm.onTimeSelected(21, 5)
        vm.save()
        advanceUntilIdle()

        val expected = LocalDateTime.of(2026, 2, 14, 21, 5).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, store.saved.single().second)
    }

    @Test
    fun `unrecognized otp message is an error, saves nothing and is never shown or logged`() = runTest {
        val vm = viewModel()
        vm.openPaste()
        vm.onDraftChange(otpBody)

        vm.check()
        advanceUntilIdle()

        assertEquals(ManualEntryState.Error(ManualEntryError.NOT_RECOGNIZED), vm.state.value)
        assertEquals("", vm.draft.value)
        assertTrue(store.saved.isEmpty())
        assertNothingLeaked(vm, otp, "verification")
    }

    @Test
    fun `message two providers accept is an ambiguous error`() = runTest {
        val bkash = ParserTestMessages.allParsers().first { it.institutionName == "bKash" }
        val lookalike = object : SmsParser by bkash {
            override val institutionName = "Sample Wallet"
            override val senderPatterns = listOf("SAMPLEWALLET")
        }
        val vm = viewModel(listOf(bkash, lookalike))

        vm.onSharedText(bkashPayment)
        advanceUntilIdle()

        assertEquals(ManualEntryState.Error(ManualEntryError.AMBIGUOUS), vm.state.value)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun `too long shared text is an error and a huge paste is cut to one past the limit`() = runTest {
        val vm = viewModel()
        val huge = "A".repeat(PrepareManualEntryUseCase.MAX_TEXT_LENGTH * 5)

        vm.onSharedText(huge)
        advanceUntilIdle()
        assertEquals(ManualEntryState.Error(ManualEntryError.TOO_LONG), vm.state.value)

        vm.openPaste()
        vm.onDraftChange(huge)
        assertEquals(PrepareManualEntryUseCase.MAX_TEXT_LENGTH + 1, vm.draft.value.length)
        vm.check()
        advanceUntilIdle()
        assertEquals(ManualEntryState.Error(ManualEntryError.TOO_LONG), vm.state.value)
    }

    @Test
    fun `checking an empty paste field is a blank error`() = runTest {
        val vm = viewModel()
        vm.openPaste()

        vm.check()
        advanceUntilIdle()

        assertEquals(ManualEntryState.Error(ManualEntryError.BLANK), vm.state.value)
    }

    @Test
    fun `try again after an error opens an empty paste field`() = runTest {
        val vm = viewModel()
        vm.onSharedText(otpBody)
        advanceUntilIdle()

        vm.openPaste()

        assertEquals(ManualEntryState.Input, vm.state.value)
        assertEquals("", vm.draft.value)
    }

    @Test
    fun `dismissing the paste field drops the typed text`() {
        val vm = viewModel()
        vm.openPaste()
        vm.onDraftChange(otpBody)

        vm.dismiss()

        assertEquals(ManualEntryState.Idle, vm.state.value)
        assertEquals("", vm.draft.value)
    }

    @Test
    fun `dismissing while checking cancels the check`() = runTest {
        val vm = viewModel()
        vm.onSharedText(bkashPayment)
        assertEquals(ManualEntryState.Checking, vm.state.value)

        vm.dismiss()
        advanceUntilIdle()

        assertEquals(ManualEntryState.Idle, vm.state.value)
    }

    @Test
    fun `typing and checking are ignored unless the paste field is open`() = runTest {
        val vm = viewModel()

        vm.onDraftChange(bkashPayment)
        vm.check()
        advanceUntilIdle()

        assertEquals(ManualEntryState.Idle, vm.state.value)
        assertEquals("", vm.draft.value)
    }

    @Test
    fun `a share replaces an open paste field and drops its text`() = runTest {
        val vm = viewModel()
        vm.openPaste()
        vm.onDraftChange(otpBody)

        vm.onSharedText(bkashPayment)
        advanceUntilIdle()

        assertTrue(vm.state.value is ManualEntryState.Preview)
        assertEquals("", vm.draft.value)
    }

    @Test
    fun `dismissing a saved result returns to idle`() = runTest {
        val vm = viewModel()
        vm.onSharedText(bkashPayment)
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()

        vm.dismiss()

        assertEquals(ManualEntryState.Idle, vm.state.value)
    }

    @Test
    fun `latest pickable date is today in the user's zone`() {
        val vm = viewModel()

        assertEquals(
            LocalDate.of(2026, 3, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            vm.latestPickableDateMillis()
        )
    }
}
