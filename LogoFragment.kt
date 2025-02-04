package ai.tradesman.logo

import ai.tradesman.R
import ai.tradesman.databinding.FragmentLogoBinding
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

class LogoFragment : Fragment() {

    private lateinit var logoViewModel: LogoViewModel
    private lateinit var binding: FragmentLogoBinding
    private lateinit var customAdapterLogo: CustomAdapterLogo

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLogoBinding.inflate(inflater, container, false)
        val view = binding.root
        logoViewModel = ViewModelProvider(this).get(LogoViewModel::class.java)

        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        logoViewModel.logo = sharedPref.getString("ai.tradesman.logo","").toString()

        //Disable Google Drive Button
        //binding.driveButton.isEnabled = false
        //binding.driveButton.isClickable = false

        //Show preview of assigned logo
        binding.logoImageView.setOnClickListener {
            binding.previewLogoIV.setImageResource(0)
            logoViewModel.logo = sharedPref.getString("ai.tradesman.logo","").toString()
            try {
                //Set image
                val uri = Uri.fromFile(File(logoViewModel.logo))
                val file = File(logoViewModel.logo);
                if (file.exists()) {
                    binding.previewLogoIV.setImageURI(uri)
                    binding.previewLogoView.visibility = View.VISIBLE
                }
            } catch (e:Exception) {
                Toast.makeText(context, "Error loading logo", Toast.LENGTH_SHORT).show()
            }
        }

        //Show logos in recyclerview
        customAdapterLogo = CustomAdapterLogo(binding.logoImageView, requireContext(),logoViewModel.thumbnail,logoViewModel.preview,requireActivity().getPreferences(Context.MODE_PRIVATE),binding.previewLogoView,binding.previewLogoIV)
        val recyclerView: RecyclerView = binding.logoSelectionRV
        recyclerView.adapter = customAdapterLogo

        //Hide assigned logo preview until view requested
        binding.previewLogoView.visibility = View.INVISIBLE
        binding.closeLogoIB.setOnClickListener {
            binding.previewLogoView.visibility = View.INVISIBLE
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logoViewModel = ViewModelProvider(this).get(LogoViewModel::class.java)
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.remove("ai.tradesman.logoCell")
        editor.apply()
        editor.commit()

        //Get info from shared preferences and display logo
        displayLogo()

        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the photo picker.
            if (uri != null) {
                //Copy file to app specific directory
                CoroutineScope(Dispatchers.IO).launch {
                    val ans = async { copyLogoFile(uri, editor) }
                    if(ans.await() == "success"){
                        //Set image
                        Handler(Looper.getMainLooper()).post {
                            binding.logoImageView.setImageURI(uri)
                        }
                    }
                    else {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context,"Error copying logo file", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        //Select image from phone for logo
        binding.phoneButton.setOnClickListener {
            //Open image picker
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }



        //Button disabled
        binding.driveButton.setOnClickListener {
            Toast.makeText(context, "Google Drive Coming Soon", Toast.LENGTH_SHORT).show()

        }

    }


    fun displayLogo(){
        if (logoViewModel.logo != "") {
            try {
                //Set image
                val uri = Uri.fromFile(File(logoViewModel.logo))
                val file = File(logoViewModel.logo);
                if (file.exists()) {
                    binding.logoImageView.setImageURI(uri)
                }
            } catch (e:Exception) {
                Toast.makeText(context, "Error loading logo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copyLogoFile(uri:Uri, editor: SharedPreferences.Editor):String{
        try {
            val path = context?.filesDir?.canonicalPath.toString()
            val ext = (DocumentFile.fromSingleUri(requireContext(), uri)?.name ?: "").split('.')[1]
            val file = context?.contentResolver?.openInputStream(uri) as InputStream
            file.use {
                Files.copy(it, Path(path + "/logo." + ext), StandardCopyOption.REPLACE_EXISTING)
            }
            editor.putString("ai.tradesman.logo",path + "/logo." + ext)
            editor.apply()
            editor.commit()

            //if (customAdapterLogo != null) {
                //customAdapterLogo.notifyDataSetChanged()
            //}

            //binding.logoImageView.setImageURI(uri)
        } catch (e:Exception) {return "failed"}
        return "success"
    }


}