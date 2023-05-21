package com.example.projeto2bimestre.actvity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.projeto2bimestre.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.awaitAll
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
    private lateinit var imageView: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        val dados = intent.extras
        idUsuario = dados?.getString("idUsuario").toString()

        val database = FirebaseDatabase.getInstance()
        btnImage = findViewById(R.id.btnImage)
        txtLegenda = findViewById(R.id.txtLegenda)
        btnPost = findViewById(R.id.btnPost)
        layoutTimeLine = findViewById(R.id.layoutTimeLine)
        imageView = findViewById(R.id.imageViewNewPost)

        imageView.setVisibility(View.GONE)

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
            imageView.setVisibility(View.VISIBLE)
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
                                        criarPost(caminhoImagem, legenda, idDoPost, idDoUsuario)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun criarPost(caminho:String, legenda: String, idPost: String?, idPostUsuario: String?){
        val storageReference = FirebaseStorage.getInstance().reference.child(caminho)
        val localFile = File.createTempFile("image", "jpg")
        storageReference.getFile(localFile).addOnSuccessListener { taskSnapshot ->
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            val newImageView = ImageView(this)
            newImageView.setImageBitmap(bitmap)

            val newTextLegenda = TextView(this)
            newTextLegenda.text = legenda

            val newLinearLayout = LinearLayout(this)
            newLinearLayout.orientation = LinearLayout.HORIZONTAL

            // Mensagem de Like

            val newTextLikes = TextView(this)

            val database = FirebaseDatabase.getInstance()
            val myRefListLike = database.getReference("/posts/"+idPostUsuario+"/"+idPost+"/likes")
            contarLikes(myRefListLike, newTextLikes)

            // Botão de Like
            val layoutParamsBtn = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            layoutParamsBtn.width = 15
            layoutParamsBtn.height = 15
            layoutParamsBtn.setMargins(0, 30, 16, 30)

            val newBtnLike = ImageButton(this)

            val myRefSetLike = database.getReference("/posts/"+idPostUsuario+"/"+idPost+"/likes/")
            existLike(myRefSetLike, newBtnLike)

            newBtnLike.setOnClickListener {
                val database = FirebaseDatabase.getInstance()
                val myRefSetLike = database.getReference("/posts/"+idPostUsuario+"/"+idPost+"/likes/"+idUsuario)
                val myRefSearchLike = database.getReference("/posts/"+idPostUsuario+"/"+idPost+"/likes/")
                setOrRemoveLike(myRefSearchLike, myRefSetLike, newBtnLike)

                val myRefListLike = database.getReference("/posts/"+idPostUsuario+"/"+idPost+"/likes")
                contarLikes(myRefListLike, newTextLikes)
            }

            newBtnLike.layoutParams = layoutParamsBtn

            // Comentário

            val newLinearFazerComent = LinearLayout(this)
            newLinearFazerComent.orientation = LinearLayout.HORIZONTAL

            val newLinearComentarios = LinearLayout(this)
            newLinearFazerComent.orientation = LinearLayout.VERTICAL

            val newEditComent = EditText(this)
            newEditComent.setHint("Comentário ");
            newEditComent.setInputType(InputType.TYPE_CLASS_TEXT);

            val newButtonComent = Button(this)
            newButtonComent.text = "Comentar"

            newButtonComent.setOnClickListener {
                val text = newEditComent.text
                val idComentario = UUID.randomUUID().toString()
                val path = "/posts/"+idPostUsuario+"/"+idPost+"/comentarios/"+idUsuario+"/"+idComentario
                val myRef = database.getReference(path)
                myRef.setValue(text.toString())
                newEditComent.setText("")
                carreagarTimeLine()
            }

            newLinearFazerComent.addView(newEditComent)
            newLinearFazerComent.addView(newButtonComent)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val paramsComent = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

            newLinearLayout.addView(newBtnLike, params)
            newLinearLayout.addView(newTextLikes, params)

            layoutTimeLine.addView(newImageView, params)
            layoutTimeLine.addView(newTextLegenda, params)
            layoutTimeLine.addView(newLinearLayout, params)
            layoutTimeLine.addView(newLinearFazerComent, params)
            layoutTimeLine.addView(newLinearComentarios, paramsComent)

            // Listar comentarios
            val newCommentModel = TextView(this)
            listarComentario("/posts/"+idPostUsuario+"/"+idPost+"/comentarios/", newLinearComentarios, params, this)

            imageView.setVisibility(View.GONE)
        }
        .addOnFailureListener { exception ->
            Log.e("TAG","Error downloading image", exception)
        }
    }

    private fun contarLikes(myRef: DatabaseReference, textLikes: TextView): Int {
        var contLike = 0
        myRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val likes = task.result
                if (likes.exists()) {
                    for (like in likes.children) {
                        contLike++
                    }
                }
                textLikes.text = contLike.toString()
            }
        }
        return contLike;
    }

    private fun setOrRemoveLike(myRefSearchLike: DatabaseReference, myRefSetLike: DatabaseReference, btnLike: ImageButton) {
        val query = myRefSearchLike.orderByChild(idUsuario).equalTo(true)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(like: DataSnapshot) {
                if (like.exists()) {
                    val likeRef = like.ref
                    likeRef.removeValue()
                    btnLike.setImageResource(R.drawable.baseline_thumb_up_off_alt_24)
                } else {
                    myRefSetLike.setValue(true)
                    btnLike.setImageResource(R.drawable.baseline_thumb_up_alt_24)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    private fun existLike(myRef: DatabaseReference, btnLike: ImageButton) {
        val query = myRef.orderByChild(idUsuario).equalTo(true)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(like: DataSnapshot) {
                if (like.exists()) {
                    btnLike.setImageResource(R.drawable.baseline_thumb_up_alt_24)
                } else {
                    btnLike.setImageResource(R.drawable.baseline_thumb_up_off_alt_24)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    fun listarComentario(caminho: String, linearPost: LinearLayout, params: LinearLayout.LayoutParams, main: MainActivity){
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference(caminho)
        myRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val commentUsers = task.result
                if (commentUsers.exists()) {
                    for (user in commentUsers.children) {
                        val idDoUsuario = user.key
                        val userRef = database.getReference(caminho + "/" + idDoUsuario.toString() + "/")
                        userRef.get().addOnCompleteListener {task ->
                            if (task.isSuccessful) {
                                val comentarios = task.result
                                if (comentarios.exists()) {
                                    for (comentario in comentarios.children){
                                        val idDoComent = comentario.key
                                        val comentRef = database.getReference(caminho + "/" + idDoUsuario.toString() + "/" + idDoComent + "/")
                                        comentRef.addValueEventListener(object : ValueEventListener {
                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                val texto = dataSnapshot.getValue()
                                                val newComment = TextView(main)
                                                newComment.text = texto.toString()
                                                linearPost.orientation = LinearLayout.VERTICAL
                                                linearPost.addView(newComment, params)
                                            }
                                            override fun onCancelled(databaseError: DatabaseError) {

                                            }
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }
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