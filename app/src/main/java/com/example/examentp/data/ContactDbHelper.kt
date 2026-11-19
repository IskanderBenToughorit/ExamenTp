package com.example.examentp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ContactDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, ContactContract.DATABASE_NAME, null, ContactContract.DATABASE_VERSION) {

    companion object {
        @Volatile
        private var INSTANCE: ContactDbHelper? = null

        fun getInstance(context: Context): ContactDbHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ContactDbHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE ${ContactContract.ContactEntry.TABLE_NAME} (
                ${ContactContract.ContactEntry.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${ContactContract.ContactEntry.COLUMN_NOM} TEXT NOT NULL,
                ${ContactContract.ContactEntry.COLUMN_PRENOM} TEXT NOT NULL,
                ${ContactContract.ContactEntry.COLUMN_TELEPHONE} TEXT NOT NULL,
                ${ContactContract.ContactEntry.COLUMN_EMAIL} TEXT,
                ${ContactContract.ContactEntry.COLUMN_PHOTO_URI} TEXT,
                ${ContactContract.ContactEntry.COLUMN_LATITUDE} REAL,
                ${ContactContract.ContactEntry.COLUMN_LONGITUDE} REAL,
                ${ContactContract.ContactEntry.COLUMN_FAVORI} INTEGER DEFAULT 0
            );
        """.trimIndent()
        db.execSQL(createTableQuery)

        insertInitialData(db)
    }

    private fun insertInitialData(db: SQLiteDatabase) {
        val contacts = listOf(
            Contact(0L, "Jean", "Dupont", "0601020304", "jean.dupont@email.com", null, 48.8566, 2.3522, isFavorite = true),
            Contact(0L, "Marie", "Curie", "0605060708", "marie.curie@email.com", null, 43.6047, 1.4442, isFavorite = false),
            Contact(0L, "Pierre", "Martin", "0699887766", "pierre.martin@email.com", null, 45.764, 4.8357, isFavorite = true),
            Contact(0L, "Sophie", "Bernard", "0612345678", "sophie.bernard@email.com", null, null, null, isFavorite = false),
            Contact(0L, "Lucas", "Petit", "0789012345", "lucas.petit@email.com", null, null, null, isFavorite = false)
        )

        for (contact in contacts) {
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
            db.insert(ContactContract.ContactEntry.TABLE_NAME, null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE ${ContactContract.ContactEntry.TABLE_NAME} ADD COLUMN ${ContactContract.ContactEntry.COLUMN_FAVORI} INTEGER DEFAULT 0;")
        }
    }
}