package com.example.projeto2bimestre.actvity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.projeto2bimestre.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class CadastroActvity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var TAG: String = "APP"
    private lateinit var txtsenha: EditText
    private lateinit var txtEmail: EditText
    private lateinit var txtNome: EditText
    private lateinit var btnCadastrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)


        auth = FirebaseAuth.getInstance()
        txtNome = findViewById(R.id.txtNome)
        txtEmail = findViewById(R.id.txtEmail)
        txtsenha = findViewById(R.id.txtSenha)
        btnCadastrar = findViewById(R.id.btnCadastar)

        Cadastrar()
    }


    fun Cadastrar(){
        btnCadastrar.setOnClickListener {
            val email: String = txtEmail.text.toString()
            val senha: String = txtsenha.text.toString()

            CriarUsuario(email, senha)
        }
    }

    fun CriarUsuario(email:String, password:String){
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser = auth.currentUser!!
                    val idUsuario:String = user.uid
                    Toast.makeText(baseContext, "Usuário Criado com sucesso",Toast.LENGTH_LONG).show()
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(baseContext, "Falha ao criar usuário",
                        Toast.LENGTH_LONG).show()
                }
            }
    }




}