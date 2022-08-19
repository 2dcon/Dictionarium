package com.fmaghi.dictionarium

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import java.io.File

class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager.beginTransaction().replace(R.id.preferenceFrame, SettingsFragment()).commit()
    }
}

class SettingsFragment: PreferenceFragmentCompat() {
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                selectFile.launch("*/*")
            }
        }

    private val selectFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null){
            val stream = context?.contentResolver?.openInputStream(uri)
            val fileName = uri.path.toString().split(':').last().split('/').last()
            Log.i("SettingsFragment", "fileName = $fileName")

            val dictDir = "${context?.filesDir.toString()}/dict"
            val dictDirFile = File(dictDir)
            val targetFile = File("$dictDir/$fileName")

            if (!dictDirFile.exists()) {
                dictDirFile.mkdir()
            }

            stream.use { input ->
                targetFile.outputStream().use { out ->
                    input?.copyTo(out)
                }
            }

//            val editor = activity?.getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE)?.edit()
//            editor?.putString(getString(R.string.preference_dictionary_path), dictFolder)
//            editor?.apply()
//            Log.i("SettingsFragment", getString(R.string.dictionary_path))
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // TODO: manage dictionaries (set default, delete)
        setPreferencesFromResource(R.xml.preference, rootKey)

        val dictPath = findPreference<Preference>("dictPath")
        dictPath?.setOnPreferenceClickListener {
            // check permission
            val permissionStatus = context?.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            if (permissionStatus == PackageManager.PERMISSION_DENIED){
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                selectFile.launch("*/*")
            }

            true
        }
    }

}
