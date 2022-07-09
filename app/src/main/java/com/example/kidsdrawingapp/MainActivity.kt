package com.example.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.loader.content.AsyncTaskLoader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var mImageButtonCurrentPaint: ImageButton? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val drawing_view = findViewById<DrawingView>(R.id.drawing_view)
        val ib_brush = findViewById<ImageButton>(R.id.ib_brush)
        val ll_paint_colors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )
        drawing_view.setsizeforBrush(20.toFloat())
        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }
        val iv_background = findViewById<ImageView>(R.id.iv_background)
        val ib_gallery = findViewById<ImageButton>(R.id.ib_gallery)
        val getAction = registerForActivityResult(ActivityResultContracts.GetContent(),
            ActivityResultCallback { uri ->
                iv_background.setImageURI(uri)
            })

        ib_gallery.setOnClickListener {
            getAction.launch("image/*")
        }
        val ib_undo = findViewById<ImageButton>(R.id.ib_undo)
        ib_undo.setOnClickListener {
            drawing_view.onClickUndo()
        }
        val fl_drawing_view_layout = findViewById<FrameLayout>(R.id.fl_drawing_view_layout)
        val ib_save = findViewById<ImageButton>(R.id.ib_save)
        ib_save.setOnClickListener {
            if(isReadStorageAllowed())
            {
                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_layout)).execute()
            }
            else{
                requestStoragePermission()
            }
        }

    }



    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        val drawing_view = findViewById<DrawingView>(R.id.drawing_view)

        smallBtn.setOnClickListener {
            drawing_view.setsizeforBrush(10.toFloat())
            brushDialog.dismiss()
        }
        mediumBtn.setOnClickListener {
            drawing_view.setsizeforBrush(20.toFloat())
            brushDialog.dismiss()
        }
        largeBtn.setOnClickListener {
            drawing_view.setsizeforBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()

    }


    fun paintClicked(view: View){
        val drawing_view = findViewById<DrawingView>(R.id.drawing_view)
        if(view!== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawing_view.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view
        }
    }
    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this,"Need Permission to add background",Toast.LENGTH_LONG).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==PackageManager.PERMISSION_GRANTED){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this@MainActivity,"Permission granted to read storage file",Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this,"Oops you were just denied the permission",Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

   private fun getBitmapFromView(view: View):Bitmap{
       val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
       val canvas = Canvas(returnedBitmap)
       val bgDrawable = view.background
       if(bgDrawable != null)
       {
           bgDrawable.draw(canvas)
       }else {
           canvas.drawColor(Color.WHITE)
       }
       view.draw(canvas)
       return returnedBitmap
   }
    private inner class BitmapAsyncTask(val mBitmap:Bitmap): AsyncTask<Any, Void, String>(){
        private lateinit var mProgressDialog:Dialog
        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }
        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if(mBitmap != null)
            {   try {
                val bytes = ByteArrayOutputStream()
                mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                val f = File(externalCacheDir!!.absoluteFile.toString()+File.separator
                        +"KidDrawingApp_"+ System.currentTimeMillis()/1000+".png")
                val fos = FileOutputStream(f)
                fos.write(bytes.toByteArray())
                fos.close()
                result = f.absolutePath

            }catch (e: Exception){
                result = ""
                e.printStackTrace()
            }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result),null){
                path, uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
                shareIntent.type = "image/png"
                startActivity(
                    Intent.createChooser(shareIntent,"Share")
                )
            }
        }
       private fun showProgressDialog(){
           mProgressDialog = Dialog(this@MainActivity)
           mProgressDialog.setContentView(R.layout.dialog_custom_progress)
           mProgressDialog.show()
       }
        private fun cancelProgressDialog(){
            mProgressDialog.dismiss()
        }
    }
    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}