package com.bhardwaj.passkey.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.bhardwaj.passkey.analytics.AnalyticsEvent
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.crash.CrashReporter
import com.bhardwaj.passkey.data.local.entity.Details
import com.bhardwaj.passkey.data.local.entity.Preview
import com.bhardwaj.passkey.data.repository.PasskeyRepository
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
    private val details = MutableStateFlow<List<Details>>(emptyList())
    private var nextId = 1L

    val allPreviews: List<Preview> get() = previews.value
    val allDetails: List<Details> get() = details.value

    override fun getDetails(): Flow<List<Details>> = details

    override fun getDetailsByPreviewId(previewId: Long): Flow<List<Details>> =
        details.map { list -> list.filter { it.previewId == previewId }.sortedBy { it.sequence } }

    override suspend fun getDetailById(detailId: Long): Details? = details.value.find { it.detailsId == detailId }

    override suspend fun getDetailByContent(previewId: Long, question: String, answer: String): Details? =
        details.value.find { it.previewId == previewId && it.question == question && it.answer == answer }

    override suspend fun upsertDetails(details: Details): Long {
        val id = details.detailsId ?: nextId++
        this.details.value = this.details.value.filterNot { it.detailsId == id } + details.copy(detailsId = id)
        return id
    }

    override suspend fun deleteDetail(details: Details) {
        this.details.value = this.details.value.filterNot { it.detailsId == details.detailsId }
    }

    override suspend fun deleteDetailByPreviewId(previewId: Long) {
        details.value = details.value.filterNot { it.previewId == previewId }
    }

    override suspend fun updateDetailSequence(detailId: Long, sequence: Long) {
        details.value = details.value.map { if (it.detailsId == detailId) it.copy(sequence = sequence) else it }
    }

    override fun getPreviews(): Flow<List<Preview>> = previews.map { list -> list.sortedBy { it.sequence } }

    override suspend fun getPreviewById(previewId: Long): Preview? = previews.value.find { it.previewId == previewId }

    override suspend fun getPreviewByHeading(previewHeading: String, categoryName: String): Preview? =
        previews.value.find { it.heading == previewHeading && it.categoryName.name == categoryName }

    override suspend fun upsertPreview(previews: Preview): Long {
        val id = previews.previewId ?: nextId++
        this.previews.value = this.previews.value.filterNot { it.previewId == id } + previews.copy(previewId = id)
        return id
    }

    override suspend fun deletePreview(previews: Preview) {
        this.previews.value = this.previews.value.filterNot { it.previewId == previews.previewId }
    }

    override suspend fun updatePreviewSequence(previewId: Long, sequence: Long) {
        previews.value = previews.value.map { if (it.previewId == previewId) it.copy(sequence = sequence) else it }
    }

    override suspend fun <R> runInTransaction(block: suspend () -> R): R = block()

    override suspend fun deleteAll() {
        previews.value = emptyList()
        details.value = emptyList()
    }
}
