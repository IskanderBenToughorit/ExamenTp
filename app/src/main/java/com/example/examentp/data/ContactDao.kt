package com.example.examentp.data

import android.content.ContentValues
import android.database.Cursor

class ContactDao(private val dbHelper: ContactDbHelper) {

    private fun cursorToContact(cursor: Cursor): Contact {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_ID))
        val nom = cursor.getString(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_NOM))
        val prenom = cursor.getString(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_PRENOM))
        val telephone = cursor.getString(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_TELEPHONE))
        val email = if (cursor.isNull(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_EMAIL))) null
                    else cursor.getString(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_EMAIL))
        val photoUri = if (cursor.isNull(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_PHOTO_URI))) null
                    else cursor.getString(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_PHOTO_URI))
        val latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_LATITUDE))) null
                    else cursor.getDouble(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_LATITUDE))
        val longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_LONGITUDE))) null
                    else cursor.getDouble(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_LONGITUDE))
        val favoriInt = if (cursor.isNull(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_FAVORI))) 0
                    else cursor.getInt(cursor.getColumnIndexOrThrow(ContactContract.ContactEntry.COLUMN_FAVORI))

        return Contact(
            id = id,
            firstName = prenom,
            lastName = nom,
            phoneNumber = telephone,
            email = email,
            photoUri = photoUri,
            latitude = latitude,
            longitude = longitude,
            isFavorite = (favoriInt == 1)
        )
    }

    private fun getOrderByClause(sortOrder: String): String {
        return when (sortOrder) {
            "prenom" -> "${ContactContract.ContactEntry.COLUMN_FAVORI} DESC, ${ContactContract.ContactEntry.COLUMN_PRENOM} COLLATE NOCASE ASC, ${ContactContract.ContactEntry.COLUMN_NOM} COLLATE NOCASE ASC"
            else -> "${ContactContract.ContactEntry.COLUMN_FAVORI} DESC, ${ContactContract.ContactEntry.COLUMN_NOM} COLLATE NOCASE ASC, ${ContactContract.ContactEntry.COLUMN_PRENOM} COLLATE NOCASE ASC"
        }
    }

    fun insert(contact: Contact): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ContactContract.ContactEntry.COLUMN_NOM, contact.lastName)
            put(ContactContract.ContactEntry.COLUMN_PRENOM, contact.firstName)
            put(ContactContract.ContactEntry.COLUMN_TELEPHONE, contact.phoneNumber)
            put(ContactContract.ContactEntry.COLUMN_EMAIL, contact.email)
            put(ContactContract.ContactEntry.COLUMN_PHOTO_URI, contact.photoUri)
            put(ContactContract.ContactEntry.COLUMN_LATITUDE, contact.latitude)
            put(ContactContract.ContactEntry.COLUMN_LONGITUDE, contact.longitude)
            put(ContactContract.ContactEntry.COLUMN_FAVORI, if (contact.isFavorite) 1 else 0)
        }
        return db.insert(ContactContract.ContactEntry.TABLE_NAME, null, values)
    }

    fun update(contact: Contact): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ContactContract.ContactEntry.COLUMN_NOM, contact.lastName)
            put(ContactContract.ContactEntry.COLUMN_PRENOM, contact.firstName)
            put(ContactContract.ContactEntry.COLUMN_TELEPHONE, contact.phoneNumber)
            put(ContactContract.ContactEntry.COLUMN_EMAIL, contact.email)
            put(ContactContract.ContactEntry.COLUMN_PHOTO_URI, contact.photoUri)
            put(ContactContract.ContactEntry.COLUMN_LATITUDE, contact.latitude)
            put(ContactContract.ContactEntry.COLUMN_LONGITUDE, contact.longitude)
            put(ContactContract.ContactEntry.COLUMN_FAVORI, if (contact.isFavorite) 1 else 0)
        }
        return db.update(
            ContactContract.ContactEntry.TABLE_NAME,
            values,
            "${ContactContract.ContactEntry.COLUMN_ID} = ?",
            arrayOf(contact.id.toString())
        )
    }

    fun delete(id: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            ContactContract.ContactEntry.TABLE_NAME,
            "${ContactContract.ContactEntry.COLUMN_ID} = ?",
            arrayOf(id.toString())
        )
    }

    fun deleteGeneratedTestContacts(): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            ContactContract.ContactEntry.TABLE_NAME,
            "${ContactContract.ContactEntry.COLUMN_EMAIL} LIKE ?",
            arrayOf("%@test.com")
        )
    }

    fun getById(id: Long): Contact? {
        val db = dbHelper.readableDatabase
        return db.query(
            ContactContract.ContactEntry.TABLE_NAME,
            null,
            "${ContactContract.ContactEntry.COLUMN_ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursorToContact(cursor)
            } else {
                null
            }
        }
    }

    fun getAll(sortOrder: String = "nom"): List<Contact> {
        val db = dbHelper.readableDatabase
        val contacts = mutableListOf<Contact>()
        db.query(
            ContactContract.ContactEntry.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            getOrderByClause(sortOrder)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                contacts.add(cursorToContact(cursor))
            }
        }
        return contacts
    }

    fun search(query: String, sortOrder: String = "nom"): List<Contact> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getAll(sortOrder)

        val db = dbHelper.readableDatabase
        val contacts = mutableListOf<Contact>()

        val tokens = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val selectionBuilder = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        tokens.forEachIndexed { index, token ->
            if (index > 0) selectionBuilder.append(" AND ")
            selectionBuilder.append("(")
            selectionBuilder.append("${ContactContract.ContactEntry.COLUMN_NOM} LIKE ? OR ")
            selectionBuilder.append("${ContactContract.ContactEntry.COLUMN_PRENOM} LIKE ? OR ")
            selectionBuilder.append("${ContactContract.ContactEntry.COLUMN_TELEPHONE} LIKE ? OR ")
            selectionBuilder.append("(${ContactContract.ContactEntry.COLUMN_PRENOM} || ' ' || ${ContactContract.ContactEntry.COLUMN_NOM}) LIKE ? OR ")
            selectionBuilder.append("(${ContactContract.ContactEntry.COLUMN_NOM} || ' ' || ${ContactContract.ContactEntry.COLUMN_PRENOM}) LIKE ? OR ")
            selectionBuilder.append("substr(${ContactContract.ContactEntry.COLUMN_EMAIL}, 1, instr(${ContactContract.ContactEntry.COLUMN_EMAIL}, '@') - 1) LIKE ? OR ")
            selectionBuilder.append("(${ContactContract.ContactEntry.COLUMN_EMAIL} LIKE ? AND ? LIKE '%@%')")
            selectionBuilder.append(")")

            val pattern = "%$token%"
            selectionArgs.add(pattern)
            selectionArgs.add(pattern)
            selectionArgs.add(pattern)
            selectionArgs.add(pattern)
            selectionArgs.add(pattern)
            selectionArgs.add(pattern)
            selectionArgs.add(pattern)
            selectionArgs.add(token)
        }

        db.query(
            ContactContract.ContactEntry.TABLE_NAME,
            null,
            selectionBuilder.toString(),
            selectionArgs.toTypedArray(),
            null,
            null,
            getOrderByClause(sortOrder)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                contacts.add(cursorToContact(cursor))
            }
        }
        return contacts
    }


}