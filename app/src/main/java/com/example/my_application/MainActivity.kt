package com.example.my_application

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView

    // The icons
    private lateinit var icName: ImageView
    private lateinit var icRegNumber: ImageView
    private lateinit var icEmail: ImageView
    private lateinit var icPassword: ImageView
    private lateinit var icSchool: ImageView
    private lateinit var icDepartment: ImageView
    private lateinit var icCourse: ImageView
    private lateinit var icYear: ImageView

    // The fields
    private lateinit var etFullName: EditText
    private lateinit var etRegNumber: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var spSchool: Spinner
    private lateinit var spDepartment: Spinner
    private lateinit var spCourse: Spinner
    private lateinit var spYear: Spinner
    private lateinit var btnSubmit: Button
    private lateinit var tvResult: TextView

    private lateinit var db: DatabaseHelper

    private val blue = Color.parseColor("#1565C0")
    private val green = Color.parseColor("#2E7D32")

    // School -> Departments
    private val departments = mapOf(
        "SIST" to listOf("Computing Sciences"),
        "SOBE" to listOf("Economics", "Tourism"),
        "SHS" to listOf("Lab Med"),
        "SPASS" to listOf("Mathematics")
    )

    // Department -> Courses
    private val courses = mapOf(
        "Computing Sciences" to listOf("BIT", "SOEN"),
        "Economics" to listOf("BCOM"),
        "Tourism" to listOf("Hospitality"),
        "Lab Med" to listOf("Lab Science"),
        "Mathematics" to listOf("Applied Mathematics")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)

        scrollView = findViewById(R.id.scrollView)

        icName = findViewById(R.id.icName)
        icRegNumber = findViewById(R.id.icRegNumber)
        icEmail = findViewById(R.id.icEmail)
        icPassword = findViewById(R.id.icPassword)
        icSchool = findViewById(R.id.icSchool)
        icDepartment = findViewById(R.id.icDepartment)
        icCourse = findViewById(R.id.icCourse)
        icYear = findViewById(R.id.icYear)

        etFullName = findViewById(R.id.etFullName)
        etRegNumber = findViewById(R.id.etRegNumber)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        spSchool = findViewById(R.id.spSchool)
        spDepartment = findViewById(R.id.spDepartment)
        spCourse = findViewById(R.id.spCourse)
        spYear = findViewById(R.id.spYear)
        btnSubmit = findViewById(R.id.btnSubmit)
        tvResult = findViewById(R.id.tvResult)

        // Show the keyboard as soon as the screen opens
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        etFullName.requestFocus()

        // Fill the dropdowns (the first item in each is just a prompt)
        spSchool.adapter = makeAdapter(listOf("Select School") + departments.keys.toList())
        spDepartment.adapter = makeAdapter(listOf("Select School first"))
        spCourse.adapter = makeAdapter(listOf("Select Department first"))
        spYear.adapter = makeAdapter(
            listOf("Select Year of Study", "Year 1", "Year 2", "Year 3", "Year 4", "Year 5", "Year 6")
        )

        // Icons turn green while the typed field is filled in correctly
        watch(etFullName, icName) { it.trim().isNotEmpty() }
        watch(etRegNumber, icRegNumber) { it.trim().isNotEmpty() }
        watch(etEmail, icEmail) { Patterns.EMAIL_ADDRESS.matcher(it.trim()).matches() }
        watch(etPassword, icPassword) { it.length >= 6 }

        // Pressing Done on the password closes the keyboard
        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                true
            } else false
        }

        // Dropdown 1: School -> fills the Department list
        spSchool.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    markPending(icSchool)
                    spDepartment.adapter = makeAdapter(listOf("Select School first"))
                    spCourse.adapter = makeAdapter(listOf("Select Department first"))
                    return
                }
                markDone(icSchool)
                val school = spSchool.selectedItem.toString()
                spDepartment.adapter = makeAdapter(listOf("Select Department") + departments[school].orEmpty())
                spCourse.adapter = makeAdapter(listOf("Select Department first"))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Dropdown 2: Department -> fills the Course list
        spDepartment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    markPending(icDepartment)
                    spCourse.adapter = makeAdapter(listOf("Select Department first"))
                    return
                }
                markDone(icDepartment)
                val dept = spDepartment.selectedItem.toString()
                spCourse.adapter = makeAdapter(listOf("Select Course") + courses[dept].orEmpty())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Dropdown 3: Course
        spCourse.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) markPending(icCourse) else markDone(icCourse)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Dropdown 4: Year
        spYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) markPending(icYear) else markDone(icYear)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Submit: check everything at once, then save
        btnSubmit.setOnClickListener {
            if (!validName() || !validReg() || !validEmail() || !validPassword()) {
                return@setOnClickListener
            }
            if (spSchool.selectedItemPosition == 0) {
                Toast.makeText(this, "Please select your school", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (spDepartment.selectedItemPosition == 0) {
                Toast.makeText(this, "Please select your department", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (spCourse.selectedItemPosition == 0) {
                Toast.makeText(this, "Please select your course", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (spYear.selectedItemPosition == 0) {
                Toast.makeText(this, "Please select your year of study", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val yearNumber = spYear.selectedItemPosition // Year 1 = 1, Year 2 = 2, ...

            // Save to the database
            val saved = db.addStudent(
                fullName = etFullName.text.toString().trim(),
                regNo = etRegNumber.text.toString().trim(),
                email = etEmail.text.toString().trim(),
                password = etPassword.text.toString(),
                school = spSchool.selectedItem.toString(),
                dept = spDepartment.selectedItem.toString(),
                yearOfStudy = yearNumber,
                course = spCourse.selectedItem.toString()
            )

            if (saved) {
                hideKeyboard()
                tvResult.text = "Registration Successful!\n\n" +
                        "Name: ${etFullName.text.toString().trim()}\n" +
                        "Reg No: ${etRegNumber.text.toString().trim()}\n" +
                        "Email: ${etEmail.text.toString().trim()}\n" +
                        "School: ${spSchool.selectedItem}\n" +
                        "Department: ${spDepartment.selectedItem}\n" +
                        "Course: ${spCourse.selectedItem}\n" +
                        "Year: $yearNumber"
                Toast.makeText(this, "Saved to database!", Toast.LENGTH_LONG).show()
                scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
            } else {
                etRegNumber.error = "This registration number is already registered"
                etRegNumber.requestFocus()
            }
        }
    }

    // Turns the icon green when the text passes the check, blue when it does not
    private fun watch(field: EditText, icon: ImageView, isOk: (String) -> Boolean) {
        field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isOk(s.toString())) markDone(icon) else markPending(icon)
            }
        })
    }

    private fun markDone(icon: ImageView) = icon.setColorFilter(green)

    private fun markPending(icon: ImageView) = icon.setColorFilter(blue)

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(btnSubmit.windowToken, 0)
    }

    // Makes a dropdown list using our own layouts, so the text is always black
    private fun makeAdapter(items: List<String>): ArrayAdapter<String> {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, items)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        return adapter
    }

    // Checks for each typed field. They show a red error and return false if something is wrong.
    private fun validName(): Boolean {
        if (etFullName.text.toString().trim().isEmpty()) {
            etFullName.error = "Please enter your full name"
            etFullName.requestFocus()
            return false
        }
        return true
    }

    private fun validReg(): Boolean {
        if (etRegNumber.text.toString().trim().isEmpty()) {
            etRegNumber.error = "Please enter your registration number"
            etRegNumber.requestFocus()
            return false
        }
        return true
    }

    private fun validEmail(): Boolean {
        val email = etEmail.text.toString().trim()
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email address"
            etEmail.requestFocus()
            return false
        }
        return true
    }

    private fun validPassword(): Boolean {
        if (etPassword.text.toString().length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return false
        }
        return true
    }
}