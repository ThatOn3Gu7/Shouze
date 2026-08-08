package com.example.crossmediatracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crossmediatracker.data.local.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * UI state exposed to the Compose layer.
 */
data class HomeUiState(
    val items: List<MediaItemEntity> = emptyList(),
    val filter: MediaType? = null,   // null = ALL
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val syncMessage: String? = null, // transient messages about backup/restore
    val error: String? = null
)

/**
 * Main ViewModel for the tracking screen.
 * Manages local CRUD, filtering, and local .zip backup/restore.
 */
class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.mediaDao()
    private val syncController = DataSyncController(db)
    private val json = Json { ignoreUnknownKeys = true }

    // Reactive state
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Combine all items with current filter & search
        viewModelScope.launch {
            dao.getAllItems().collect { allItems ->
                _uiState.update { current ->
                    val filtered = filterItems(allItems, current.filter, current.searchQuery)
                    current.copy(items = filtered)
                }
            }
        }
    }

    // ----- Local CRUD -----

    fun addOrUpdate(item: MediaItemEntity) {
        viewModelScope.launch {
            dao.insertOrUpdate(item.copy(lastUpdated = System.currentTimeMillis()))
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            dao.deleteById(itemId)
        }
    }

    // ----- Filter & Search -----

    fun setFilter(mediaType: MediaType?) {
        _uiState.update { it.copy(filter = mediaType) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun filterItems(
        all: List<MediaItemEntity>,
        type: MediaType?,
        query: String
    ): List<MediaItemEntity> {
        return all
            .filter { type == null || it.mediaType == type }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
    }

    // ----- Local Zip Backup -----

    fun backupToLocalZip(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, syncMessage = null, error = null) }
            try {
                val result = syncController.exportToJson()
                result.fold(
                    onSuccess = { jsonString ->
                        val itemCount = try {
                            json.decodeFromString<BackupPayload>(jsonString).itemCount
                        } catch (_: Exception) {
                            0
                        }
                        val output = getApplication<Application>().contentResolver
                            .openOutputStream(uri)
                        if (output == null) {
                            showMessage("Failed to open file for writing", isError = true)
                            return@launch
                        }
                        output.use { os ->
                            ZipOutputStream(os).use { zos ->
                                val entry = ZipEntry("backup.json")
                                zos.putNextEntry(entry)
                                zos.write(jsonString.toByteArray(Charsets.UTF_8))
                                zos.closeEntry()
                            }
                        }
                        showMessage("Backup saved successfully ($itemCount items)")
                    },
                    onFailure = { e ->
                        showMessage("Export failed: ${e.message}", isError = true)
                    }
                )
            } catch (e: Exception) {
                showMessage("Unexpected error: ${e.message}", isError = true)
            }
        }
    }

    fun restoreFromLocalZip(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, syncMessage = null, error = null) }
            try {
                val input = getApplication<Application>().contentResolver.openInputStream(uri)
                if (input == null) {
                    showMessage("Failed to open backup file", isError = true)
                    return@launch
                }

                val backupJson = input.use { stream ->
                    var content = ""
                    ZipInputStream(stream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == "backup.json") {
                                content = zis.bufferedReader().readText()
                                break
                            }
                            entry = zis.nextEntry
                        }
                    }
                    content
                }

                if (backupJson.isBlank()) {
                    showMessage("Invalid backup file: backup.json not found", isError = true)
                    return@launch
                }

                val importResult = syncController.importFromJson(backupJson)
                importResult.fold(
                    onSuccess = { count ->
                        showMessage("Restore successful (imported $count items)")
                    },
                    onFailure = { e ->
                        showMessage("Import failed: ${e.message}", isError = true)
                    }
                )
            } catch (e: Exception) {
                showMessage("Unexpected error: ${e.message}", isError = true)
            }
        }
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null, error = null) }
    }

    private fun showMessage(message: String, isError: Boolean = false) {
        _uiState.update {
            if (isError) {
                it.copy(isLoading = false, error = message, syncMessage = null)
            } else {
                it.copy(isLoading = false, syncMessage = message, error = null)
            }
        }
    }
}