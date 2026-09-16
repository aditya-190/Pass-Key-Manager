package com.bhardwaj.passkey.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.bhardwaj.passkey.analytics.AnalyticsEvent
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.crash.CrashReporter
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.model.Detail
import com.bhardwaj.passkey.domain.model.Preview
import com.bhardwaj.passkey.domain.repository.PasskeyRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class RecordingAnalyticsTracker : AnalyticsTracker {
    val events = mutableListOf<AnalyticsEvent>()
    val userProperties = mutableMapOf<String, String?>()
    var collectionEnabled: Boolean? = null
        private set

    val names: List<String> get() = events.map { it.name }

    override fun log(event: AnalyticsEvent) {
        events += event
    }

    override fun setUserProperty(name: String, value: String?) {
        userProperties[name] = value
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
    }
}

class RecordingCrashReporter : CrashReporter {
    val exceptions = mutableListOf<Throwable>()
    val logs = mutableListOf<String>()
    val keys = mutableMapOf<String, Any>()
    var collectionEnabled: Boolean? = null
    var discardedUnsentReports = 0

    override fun discardUnsentReports() {
        discardedUnsentReports++
    }

    override fun setCustomKey(key: String, value: String) {
        keys[key] = value
    }

    override fun setCustomKey(key: String, value: Long) {
        keys[key] = value
    }

    override fun setCustomKey(key: String, value: Boolean) {
        keys[key] = value
    }

    override fun log(message: String) {
        logs += message
    }

    override fun recordException(throwable: Throwable) {
        exceptions += throwable
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
    }
}

/** Replaces Dispatchers.Main for ViewModel tests: `@get:Rule val main = MainDispatcherRule()`. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** A real Preferences DataStore in a temp dir (pair with JUnit4 `TemporaryFolder`). */
@OptIn(ExperimentalCoroutinesApi::class)
fun testDataStore(
    dir: File,
    scope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher() + SupervisorJob())
): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) { File(dir, "test.preferences_pb") }

/** In-memory vault with Room-like id assignment, for ViewModel tests. */
class FakePasskeyRepository : PasskeyRepository {
    private val previews = MutableStateFlow<List<Preview>>(emptyList())
    private val details = MutableStateFlow<List<Detail>>(emptyList())
    private var nextId = 1L

    val allPreviews: List<Preview> get() = previews.value
    val allDetails: List<Detail> get() = details.value

    override fun getPreviews(): Flow<List<Preview>> = previews.map { list -> list.sortedBy { it.sequence } }

    override fun getPreviewsByCategory(category: Category): Flow<List<Preview>> =
        getPreviews().map { list -> list.filter { it.category == category } }

    override suspend fun getPreviewById(previewId: Long): Preview? = previews.value.find { it.id == previewId }

    // Case-insensitive, like the DAO's COLLATE NOCASE.
    override suspend fun getPreviewByHeading(heading: String, category: Category): Preview? =
        previews.value.find { it.heading.equals(heading, ignoreCase = true) && it.category == category }

    override suspend fun createPreview(heading: String, category: Category, sequence: Long): Long {
        val id = nextId++
        previews.value = previews.value + Preview(id = id, heading = heading, category = category, sequence = sequence)
        return id
    }

    /** Room upsert: replaces the row with this id, or inserts it (with that id) when it is gone. */
    override suspend fun updatePreview(preview: Preview) {
        previews.value = previews.value.filterNot { it.id == preview.id } + preview
    }

    override suspend fun deletePreview(preview: Preview) {
        previews.value = previews.value.filterNot { it.id == preview.id }
    }

    override suspend fun updatePreviewSequence(previewId: Long, sequence: Long) {
        previews.value = previews.value.map { if (it.id == previewId) it.copy(sequence = sequence) else it }
    }

    override fun getDetails(): Flow<List<Detail>> = details

    override fun getDetailsByPreviewId(previewId: Long): Flow<List<Detail>> =
        details.map { list -> list.filter { it.previewId == previewId }.sortedBy { it.sequence } }

    override suspend fun getDetailById(detailId: Long): Detail? = details.value.find { it.id == detailId }

    override suspend fun getDetailByContent(previewId: Long, question: String, answer: String): Detail? =
        details.value.find { it.previewId == previewId && it.question == question && it.answer == answer }

    override suspend fun createDetail(
        previewId: Long,
        question: String,
        answer: String,
        sequence: Long,
        isSecret: Boolean
    ): Long {
        val id = nextId++
        details.value = details.value + Detail(id, previewId, question, answer, sequence, isSecret)
        return id
    }

    override suspend fun updateDetail(detail: Detail) {
        details.value = details.value.filterNot { it.id == detail.id } + detail
    }

    override suspend fun deleteDetail(detail: Detail) {
        details.value = details.value.filterNot { it.id == detail.id }
    }

    override suspend fun deleteDetailsByPreviewId(previewId: Long) {
        details.value = details.value.filterNot { it.previewId == previewId }
    }

    override suspend fun updateDetailSequence(detailId: Long, sequence: Long) {
        details.value = details.value.map { if (it.id == detailId) it.copy(sequence = sequence) else it }
    }

    override suspend fun <R> runInTransaction(block: suspend () -> R): R = block()

    override suspend fun deleteAll() {
        previews.value = emptyList()
        details.value = emptyList()
    }
}

class FakeClock(var now: Long = 1_700_000_000_000) : com.bhardwaj.passkey.core.Clock {
    override fun nowMs() = now
}

/** Config with per-key overrides by Remote Config name; everything else is the in-app default. */
class FakeConfig(overrides: Map<String, Any> = emptyMap()) : com.bhardwaj.passkey.config.ConfigProvider {
    val flow =
        MutableStateFlow(com.bhardwaj.passkey.config.AppConfig(com.bhardwaj.passkey.config.MapConfigReader(overrides)))

    override val current: com.bhardwaj.passkey.config.AppConfig get() = flow.value

    fun set(overrides: Map<String, Any>) {
        flow.value = com.bhardwaj.passkey.config.AppConfig(com.bhardwaj.passkey.config.MapConfigReader(overrides))
    }
}
