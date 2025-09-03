package org.utl.huertovertical

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        db = FirebaseFirestore.getInstance()

        setupRegisterClickListener()

        setupLoginClickListener()
    }

    private fun setupRegisterClickListener() {
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupLoginClickListener() {
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validar campos vacíos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (task.result != null && !task.result!!.isEmpty) {
                            val document = task.result!!.documents[0]
                            val storedPassword = document.getString("password")

                            if (storedPassword == password) {
                                Log.d("LoginFirestore", "Inicio de sesión exitoso para: $email")
                                Toast.makeText(this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()

                                // Redirigir al Dashboard
                                val intent = Intent(this, DashboardActivity::class.java)
                                startActivity(intent)
                                finish()

                            } else {
                                Log.e("LoginFirestore", "Contraseña incorrecta para: $email")
                                Toast.makeText(this, "Correo electrónico o contraseña incorrectos.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Log.e("LoginFirestore", "Usuario no encontrado en la base de datos: $email")
                            Toast.makeText(this, "Correo electrónico o contraseña incorrectos.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("LoginFirestore", "Error al consultar la base de datos: ${task.exception?.message}")
                        Toast.makeText(this, "Error al intentar iniciar sesión. Inténtelo de nuevo.", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}