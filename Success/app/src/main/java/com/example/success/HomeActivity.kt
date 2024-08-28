package com.example.success

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.success.databinding.ActivityHomeBinding
import com.example.success.ml.BirdsModel
import org.tensorflow.lite.support.image.TensorImage
import java.io.IOException

class HomeActivity : AppCompatActivity() {
    private lateinit var binding : ActivityHomeBinding
    private lateinit var imageView : ImageView
    private lateinit var button: Button
    private lateinit var tvOutput: TextView
    private val GALLERY_REQUEST_CODE = 123
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val myText = intent.getStringExtra("text")
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        imageView = binding.imageView
        button= binding.btnCaptureImage
        tvOutput = binding.tvOutput
        if(myText!=null) {
            tvOutput.text = myText
            val byteArray = intent.getByteArrayExtra("bitmap")
            val mybitmap = byteArray?.let { BitmapFactory.decodeByteArray(byteArray, 0, it.size) }
            imageView.setImageBitmap(mybitmap)
        }

        val buttonLoad = binding.btnLoadImage

        button.setOnClickListener{
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
        }
        buttonLoad.setOnClickListener{
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                == PackageManager.PERMISSION_GRANTED){
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                val mimeTypes = arrayOf("image/jpeg","image/png","image/jpg")
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                intent.flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                onresult.launch(intent)
            } else {
                requestPermission.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
        tvOutput.setOnClickListener{
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${tvOutput.text}"))
            startActivity(intent)
        }
    }
    private val onresult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
        Log.i("TAG", "This is the result: ${result.data} ${result.resultCode}")
        onResultRecived(GALLERY_REQUEST_CODE,result)
    }
    private fun onResultRecived (requestCode: Int, result: androidx.activity.result.ActivityResult?){
        when(requestCode){
            GALLERY_REQUEST_CODE -> {
                if (result?.resultCode == Activity.RESULT_OK){
                    result.data?.data?.let { uri ->
                        Log.i("TAG", "OnResultRecived : $uri")
                        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                        imageView.setImageBitmap(bitmap)
                        outputGenerator(bitmap)
                    }
                } else {
                    Log.e("TAG", "onActivityResult: error in selecting image")
                }
            }
        }
    }
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){granted ->
        if (granted){
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            val mimeTypes = arrayOf("image/jpeg","image/png","image/jpg")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            intent.flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            onresult.launch(intent)
        } else {
            Toast.makeText(this,"Permission Denied!! Try again", Toast.LENGTH_SHORT).show()
        }
    }
    private fun outputGenerator (bitmap: Bitmap){
        val birdsModel = BirdsModel.newInstance(this)
        var newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val tfimage = TensorImage.fromBitmap(newBitmap)
        val outputs = birdsModel.process(tfimage)
            .probabilityAsCategoryList.apply {
                sortByDescending { it.score }
            }
        val highProbabilityOutput = outputs[0]

        tvOutput.text = highProbabilityOutput.label
        Log.i("TAG", "outputGenerator: $highProbabilityOutput")

    }


}