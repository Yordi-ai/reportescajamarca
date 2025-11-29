package com.example.reportescajamarca

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reportescajamarca.RegisterActivity
import com.example.reportescajamarca.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // ⭐ Guardar token FCM
                        guardarTokenFCM()

                        // Obtener tipo de usuario de Firestore
                        val userId = auth.currentUser?.uid
                        userId?.let {
                            db.collection("usuarios").document(it).get()
                                .addOnSuccessListener { document ->
                                    val tipoUsuario = document.getString("tipoUsuario") ?: "ciudadano"

                                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()

                                    // Redirigir según tipo de usuario
                                    if (tipoUsuario == "trabajador") {
                                        startActivity(
                                            Intent(
                                                this,
                                                TrabajadorMainActivity::class.java
                                            )
                                        )
                                    } else {
                                        startActivity(Intent(this, MainActivity::class.java))
                                    }
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error al obtener datos: ${e.message}", Toast.LENGTH_LONG).show()
                                    // Por defecto redirigir a ciudadano
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // ⭐ NUEVA FUNCIÓN: Guardar token FCM
    private fun guardarTokenFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                // Guardar token en Firestore
                db.collection("usuarios")
                    .document(userId)
                    .set(
                        hashMapOf(
                            "fcmToken" to token,
                            "email" to auth.currentUser?.email,
                            "ultimaActualizacion" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
            }
        }
    }
}