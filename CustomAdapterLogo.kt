package ai.tradesman.logo

import ai.tradesman.R
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.nio.file.Files
import kotlin.io.path.Path

class CustomAdapterLogo(
    private val logoImage: ImageView,
    private val context: Context,
    private val thumbnailSet: IntArray,
    private val previewSet: IntArray,
    private var sharedPref: SharedPreferences,
    private var previewView: ConstraintLayout,
    private var previewImageView: ImageView
) :
    RecyclerView.Adapter<CustomAdapterLogo.ViewHolder>() {

    val views = ArrayList<ViewHolder>()

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): CustomAdapterLogo.ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.logo_row, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: CustomAdapterLogo.ViewHolder, position: Int) {
        viewHolder.logoButton.setImageResource(thumbnailSet[position])
        views.add(viewHolder)
        viewHolder.selectionButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val ans = async { writeCompanyName(position) }
                if(ans.await() == "error"){
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context,"Error setting image logo", Toast.LENGTH_LONG).show()
                    }
                }
            }

            //Set all views to red x image
            for (item in views) {
                item.selectionButton.setImageResource(R.drawable.x)
            }

            //Set selected view to green check image
            viewHolder.selectionButton.setImageResource(R.drawable.check)
        }

        //When scrolling through recyclerview only view that corresponds to shared preferences value is checked
        if (position == sharedPref.getInt("ai.tradesman.logoCell",-1)) {
            val  selectedCell = sharedPref.getInt("ai.tradesman.logoCell",-1)
            if (selectedCell != -1) {
                viewHolder.selectionButton.setImageResource(R.drawable.check)
            }
            else {viewHolder.selectionButton.setImageResource(R.drawable.x)}
        }  else {
            viewHolder.selectionButton.setImageResource(R.drawable.x)
        }

        //Show enlarged preview of template in window
        viewHolder.logoButton.setOnClickListener {
            previewImageView.setImageResource(previewSet[position])
            previewView.visibility = View.VISIBLE
        }

    }

    override fun getItemCount(): Int {
        return thumbnailSet.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logoButton: ImageButton
        val selectionButton: ImageButton
        init {
            // Define click listener for the ViewHolder's View
            logoButton = view.findViewById(R.id.thumbnailLogoButton)
            selectionButton = view.findViewById(R.id.selectionLogoButton)
        }
    }

    fun writeCompanyName(position:Int):String{
        //Get Logo and save file to app specific folder
        val path = context.filesDir?.canonicalPath.toString()
        if (Files.exists(Path(path + "/logo.png"))) {
            Files.delete(Path(path + "/logo.png"))
        }

        /*
        val file = context.assets.open("logo" + position + ".png")
        file.use {Files.copy(it, Path(path + "/logo.png"), StandardCopyOption.REPLACE_EXISTING)}
        */

        val editor = sharedPref.edit()
        //Set logo in fragment
        val companyName: String = sharedPref.getString("ai.tradesman.company","").toString()
        //Write company name onto logo
        val result = LogoName().writeName(companyName,context,position)
        if (result == "success") {
            editor.putString("ai.tradesman.logo",path + "/logo.png")
            //Set image logo
            Handler(Looper.getMainLooper()).post {
                logoImage.setImageBitmap(BitmapFactory.decodeFile(path + "/logo.png"));
            }

            //Save selected to shared preferences
            editor.putInt("ai.tradesman.logoCell",position)
            editor.apply()
            editor.commit()
            return "success"
        }
        else{return "error"}
    }

}