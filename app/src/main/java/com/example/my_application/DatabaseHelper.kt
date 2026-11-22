package com.example.my_application

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "students.db", null, 2) {

    // Runs once, the first time the app is opened: creates the table
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "full_name TEXT NOT NULL, " +
                    "reg_no TEXT NOT NULL UNIQUE, " +
                    "email TEXT NOT NULL, " +
                    "password_hash TEXT NOT NULL, " +
                    "school TEXT NOT NULL, " +
                    "dept TEXT NOT NULL, " +
                    "year_of_study INTEGER NOT NULL, " +
                    "course TEXT NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS students")
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    // Saves one student. Returns true if saved, false if it failed
    // (for example, when the registration number is already in the table).
    fun addStudent(
        fullName: String,
        regNo: String,
        email: String,
        password: String,
        school: String,
        dept: String,
        yearOfStudy: Int,
        course: String
    ): Boolean {
        val values = ContentValues().apply {
            put("full_name", fullName)
            put("reg_no", regNo)
            put("email", email)
            put("password_hash", hash(password))
            put("school", school)
            put("dept", dept)
            put("year_of_study", yearOfStudy)
            put("course", course)
        }
        val result = writableDatabase.insert("users", null, values)
        return result != -1L
    }

    // Turns the password into scrambled text so the real password is never stored
    private fun hash(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}