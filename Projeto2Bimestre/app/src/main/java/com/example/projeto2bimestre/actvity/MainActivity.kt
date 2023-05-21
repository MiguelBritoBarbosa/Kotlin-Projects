package com.example.projeto2bimestre.actvity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.projeto2bimestre.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var btnImage: ImageButton
    private lateinit var txtLegenda: EditText
    private lateinit var btnPost: Button
    private lateinit var layoutTimeLine: LinearLayout
    private lateinit var caminhoImagemTemp: Uri
    private lateinit var idUsuario: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dados = intent.extras
        idUsuario = dados?.getString("idUsuario").toString()

        val database = FirebaseDatabase.getInstance()
        btnImage = findViewById(R.id.btnImage)
        txtLegenda = findViewById(R.id.txtLegenda)
        btnPost = findViewById(R.id.btnPost)
        layoutTimeLine = findViewById(R.id.layoutTimeLine)
        carreagarTimeLine()
        selecionarImagemGaleria()
        cadastrarPost()
    }

    private fun carreagarTimeLine(){
        layoutTimeLine.removeAllViews()
        listarTodosPosts()

    }




    private fun selecionarImagemGaleria() {
        btnImage.setOnClickListener{
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            val PICK_IMAGE_REQUEST = 22 //código para identificar resposta de intent
            startActivityForResult(Intent.createChooser(intent,"Selecione"),PICK_IMAGE_REQUEST)
        }
    }


    fun listarTodosPosts(){
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("/posts/")
        myRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val postsUsers = task.result
                if (postsUsers.exists()) {
                    for (postUser in postsUsers.children) {
                        val idDoUsuario = postUser.key
                        val userRef = database.getReference("/posts/" + idDoUsuario.toString() + "/")
                        userRef.get().addOnCompleteListener {task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this@MainActivity,"Carregando os Posts...", Toast.LENGTH_LONG).show()

                                val posts = task.result
                                if (posts.exists()) {
                                    for (post in posts.children){
                                        val idDoPost = post.key
                                        val caminhoImagem = post.child("imagem").value as String
                                        val legenda = post.child("legenda").value as String
                                        CriaPost(caminhoImagem, legenda)

                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    fun CriaPost(caminho:String, legenda: String){
        val storageReference = FirebaseStorage.getInstance().reference.child(caminho)
        val localFile = File.createTempFile("image", "jpg")
        storageReference.getFile(localFile).addOnSuccessListener { taskSnapshot ->
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            val newImageView = ImageView(this)
            newImageView.setImageBitmap(bitmap)
            val newTextView = TextView(this)
            newTextView.text = legenda

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutTimeLine.addView(newImageView, params)
            layoutTimeLine.addView(newTextView, params)
        }
        .addOnFailureListener { exception ->
            Log.e("TAG","Error downloading image", exception)
        }
    }

    private fun uploadImage(caminhoArquivo: Uri): String {
        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Enviando...")
        progressDialog.show()
        var caminhoImagem = "images/" + UUID.randomUUID().toString()
        val ref: StorageReference = FirebaseStorage.getInstance().getReference().child(caminhoImagem)
        ref.putFile(caminhoArquivo).addOnSuccessListener {
            progressDialog.dismiss()
            Toast.makeText(this@MainActivity,"Imagem enviada", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e -> progressDialog.dismiss()
            Toast.makeText(this@MainActivity,"Erro ao enviar " + e.message,Toast.LENGTH_SHORT).show()
        }.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
            progressDialog.setMessage("Enviando "+ progress.toInt() + "%")
        }
        return caminhoImagem
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult( requestCode,resultCode,data )
        val PICK_IMAGE_REQUEST = 22 //código para identificar resposta de intent
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            var caminhoDaImagem = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver,caminhoDaImagem)
                var img: ImageView = findViewById(R.id.imageViewNewPost)
                img.setImageBitmap(bitmap)
                if (caminhoDaImagem != null) {
                    caminhoImagemTemp = caminhoDaImagem
                }
            }catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun cadastrarPost(){
        btnPost.setOnClickListener{
            var legenda: String = txtLegenda.text.toString()
            var caminho: String = uploadImage(caminhoImagemTemp)
            val dados = HashMap<String, Any>()
            dados["legenda"] = legenda
            dados["imagem"] = caminho

            var tempCaminho = idUsuario + "/" + UUID.randomUUID().toString()
            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference("posts/"+tempCaminho+"/")
            myRef.setValue(dados)
            carreagarTimeLine()
        }

    }





}