package com.example.examentp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.examentp.data.Contact
import com.example.examentp.data.ContactDao
import com.example.examentp.data.ContactDbHelper
import com.example.examentp.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = ContactDbHelper.getInstance(application)
    private val dao = ContactDao(dbHelper)
    private val prefsManager = PreferencesManager(application)

    private val _contacts = MutableLiveData<List<Contact>>(emptyList())
    val contacts: LiveData<List<Contact>> = _contacts

    private val _filteredContacts = MutableLiveData<List<Contact>>(emptyList())
    val filteredContacts: LiveData<List<Contact>> = _filteredContacts

    private val _selectedContactId = MutableLiveData<Long?>()
    val selectedContactId: LiveData<Long?> = _selectedContactId

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    init {
        loadContacts()
    }

    private fun sortContacts(list: List<Contact>, sortOrder: String): List<Contact> {
        return when (sortOrder) {
            "prenom" -> list.sortedWith(
                compareByDescending<Contact> { it.isFavorite }
                    .thenBy(String.CASE_INSENSITIVE_ORDER, Contact::firstName)
                    .thenBy(String.CASE_INSENSITIVE_ORDER, Contact::lastName)
            )
            else -> list.sortedWith(
                compareByDescending<Contact> { it.isFavorite }
                    .thenBy(String.CASE_INSENSITIVE_ORDER, Contact::lastName)
                    .thenBy(String.CASE_INSENSITIVE_ORDER, Contact::firstName)
            )
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteGeneratedTestContacts()
            }
            val sortOrder = prefsManager.sortOrder
            val list = withContext(Dispatchers.IO) {
                dao.getAll(sortOrder)
            }
            _contacts.value = sortContacts(list, sortOrder)
            filter(_searchQuery.value ?: "")
        }
    }

    fun selectContact(id: Long?) {
        _selectedContactId.value = id
    }

    fun getContact(id: Long, callback: (Contact?) -> Unit) {
        viewModelScope.launch {
            val contact = withContext(Dispatchers.IO) {
                dao.getById(id)
            }
            callback(contact)
        }
    }

    fun addContact(contact: Contact, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val newId = withContext(Dispatchers.IO) {
                dao.insert(contact)
            }
            loadContacts()
            onComplete(newId)
        }
    }

    fun updateContact(updatedContact: Contact, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.update(updatedContact)
            }
            loadContacts()
            onComplete()
        }
    }

    fun deleteContact(id: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.delete(id)
            }
            loadContacts()
            onComplete()
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val contact = dao.getById(id)
                contact?.let {
                    it.isFavorite = !it.isFavorite
                    dao.update(it)
                }
            }
            loadContacts()
        }
    }



    private fun removeAccents(str: String): String {
        val normalized = Normalizer.normalize(str, Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    fun filter(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val sortOrder = prefsManager.sortOrder
            val filtered = withContext(Dispatchers.IO) {
                val trimmed = query.trim()
                if (trimmed.isEmpty()) {
                    return@withContext sortContacts(dao.getAll(sortOrder), sortOrder)
                }

                val dbResults = dao.search(trimmed, sortOrder)
                val normalizedQuery = removeAccents(trimmed.lowercase())
                val tokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }

                val candidates = if (dbResults.isNotEmpty()) dbResults else dao.getAll(sortOrder)

                val matched = candidates.filter { c ->
                    val emailUsername = c.email?.substringBefore('@') ?: ""
                    val fullEmailIfAt = if (trimmed.contains("@")) c.email ?: "" else ""
                    val searchableText = removeAccents(
                        "${c.lastName} ${c.firstName} ${c.phoneNumber} $emailUsername $fullEmailIfAt"
                    ).lowercase()

                    tokens.all { token -> searchableText.contains(token) }
                }

                sortContacts(matched, sortOrder)
            }
            _filteredContacts.value = filtered
        }
    }
}