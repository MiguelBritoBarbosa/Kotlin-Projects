package com.example.projeto2bimestre.actvity


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projeto2bimestre.R
import com.google.firebase.auth.FirebaseAuth


class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var TAG: String = "APP"
    private lateinit var txtsenha: EditText
    private lateinit var txtEmail: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        auth = FirebaseAuth.getInstance()
        txtEmail = findViewById(R.id.txtEmail)
        txtsenha = findViewById(R.id.txtSenha)
        btnLogin = findViewById(R.id.btnLogin)

        Logar()
    }

    fun Logar(){
        btnLogin.setOnClickListener {
            val email: String = txtEmail.text.toString()
            val senha: String = txtsenha.text.toString()

            VerificarUsuario(email, senha)
        }
    }


    fun VerificarUsuario(email:String, password:String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        if(!user.isEmailVerified) {
                            user.sendEmailVerification()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d(TAG, "Email enviado.")
                                    }
                                }
                        }
                    }
                    Toast.makeText(baseContext, "Logado com sucesso.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(baseContext, "Erro ao tentar fazer login! Verifique seu Email e Senha.", Toast.LENGTH_LONG).show()
                }
            }
    }



}