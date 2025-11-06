package com.example.reportescajamarca

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.reportescajamarca.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Mostrar/ocultar campo de credencial según tipo de usuario
        binding.radioGroupTipoUsuario.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbCiudadano -> {
                    binding.layoutCredencial.visibility = View.GONE
                }
                R.id.rbTrabajador -> {
                    binding.layoutCredencial.visibility = View.VISIBLE
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            registrarUsuario()
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun registrarUsuario() {
        val nombre = binding.etNombre.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Determinar tipo de usuario
        val tipoUsuario = if (binding.rbTrabajador.isChecked) "trabajador" else "ciudadano"
        val credencial = binding.etCredencial.text.toString().trim()

        // Validaciones
        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar credencial si es trabajador
        if (tipoUsuario == "trabajador" && credencial.isEmpty()) {
            Toast.makeText(this, "La credencial municipal es obligatoria", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear usuario en Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    // Guardar datos del usuario en Firestore
                    val usuario = hashMapOf(
                        "nombre" to nombre,
                        "email" to email,
                        "telefono" to telefono,
                        "tipoUsuario" to tipoUsuario,
                        "credencial" to credencial,
                        "fechaRegistro" to System.currentTimeMillis()
                    )

                    userId?.let {
                        db.collection("usuarios").document(it).set(usuario)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()

                                // Redirigir según tipo de usuario
                                if (tipoUsuario == "trabajador") {
                                    startActivity(Intent(this, TrabajadorMainActivity::class.java))
                                } else {
                                    startActivity(Intent(this, MainActivity::class.java))
                                }
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}