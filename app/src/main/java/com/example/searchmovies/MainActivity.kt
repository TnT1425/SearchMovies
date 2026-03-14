package com.example.searchmovies

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Callback
import java.io.File
import java.io.FileOutputStream
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import android.provider.OpenableColumns

class MainActivity : AppCompatActivity() {

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.146.1:8000/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val apiService = retrofit.create(ApiService::class.java)

    private fun uriToFile(uri: Uri): File? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null

        // Cố gắng đọc tên thật của bức ảnh trong điện thoại
        var fileName = "unknown_movie.jpg"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }

        // Tạo file vật lý với đúng tên đó để gửi đi
        val tempFile = File(cacheDir, fileName)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        return tempFile
    }
    //chuyển uri của ảnh thành file vật lý gửi lên server
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val edtSearch = findViewById<EditText>(R.id.edtSearch)
        val txtResult = findViewById<TextView>(R.id.tvResult)
        val imgPoster = findViewById<ImageView>(R.id.imgPoster)
        val btnPickImage = findViewById<Button>(R.id.btnPickImage)



        btnSearch.setOnClickListener {
            val movieName = edtSearch.text.toString()
            if (movieName.isEmpty()) {
                txtResult.text = "Vui lòng nhập tên phim!"
                return@setOnClickListener
            }
            apiService.searchByName(movieName).enqueue(object : retrofit2.Callback<MovieResponse> {
                override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                    if (response.isSuccessful) {
                        val movie = response.body()?.movie_info
                        if (movie != null) {
                            txtResult.text = "Phim: ${movie.title}\nNgày chiếu: ${movie.release_date}\nNội dung: ${movie.overview}"
                            if (!movie.poster_path.isNullOrEmpty()) {
                                Glide.with(this@MainActivity)
                                    .load(movie.poster_path) // Link ảnh Backend trả về
                                    .into(imgPoster)         // Ném vào ImageView
                            } else {
                                // Nếu phim không có ảnh thì xóa ảnh cũ đi
                                imgPoster.setImageDrawable(null)
                            }
                        } else {
                            txtResult.text = "Không tìm thấy phim trên TMDb!"
                            imgPoster.setImageDrawable(null)
                        }
                    } else {
                        txtResult.text = "Lỗi từ Server: ${response.code()}"
                    }
                }
                override fun onFailure(
                    call: Call<MovieResponse?>,
                    t: Throwable
                ) {
                    txtResult.text = "Lỗi mạng/Kết nối: ${t.message}"                }
            })
                }

        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                Glide.with(this).load(uri).into(imgPoster)
                txtResult.text = "Vui lòng đợi"

                val file = uriToFile(uri)
                if (file != null) {
                    val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
                    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                    apiService.indentifyByImage(body).enqueue(object : Callback<MovieResponse>{
                        override fun onResponse(
                            call: Call<MovieResponse?>,
                            response: Response<MovieResponse?>
                        ) {
                            if (response.isSuccessful){
                                val movie = response.body()?.movie_info
                                if(movie!=null){
                                    txtResult.text="AI nhận ra phim:\${movie.title}\\nNgày chiếu: \${movie.release_date}\\nNội dung: \${movie.overview} "
                                    if (!movie.poster_path.isNullOrEmpty()) {
                                        Glide.with(this@MainActivity)
                                            .load(movie.poster_path)
                                            .into(imgPoster)
                                    }
                                }else{
                                    txtResult.text="Không tìm thấy phim trên TMDb!"
                                    imgPoster.setImageDrawable(null)
                                }
                            }else{
                                txtResult.text="Lỗi từ Server: ${response.code()}"
                            }
                        }

                        override fun onFailure(call: Call<MovieResponse?>, t: Throwable) {
                            txtResult.text="Lỗi mạng/Kết nối: ${t.message}"
                        }
                    })
                }
            }
        }
        btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }
}


