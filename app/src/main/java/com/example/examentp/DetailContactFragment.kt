package com.example.examentp

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.example.examentp.data.Contact
import com.example.examentp.data.PreferencesManager
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream

class DetailContactFragment : Fragment(R.layout.fragment_detail_contact) {

    private val viewModel: ContactViewModel by activityViewModels()
    private var currentContact: Contact? = null
    private var tempPhotoUri: String? = null
    private lateinit var prefsManager: PreferencesManager

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val savedPath = saveUriToInternalStorage(it)
            if (savedPath != null) {
                tempPhotoUri = savedPath
                view?.findViewById<ImageView>(R.id.ivDetailPhoto)?.setImageURI(Uri.parse(savedPath))
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val savedPath = saveBitmapToInternalStorage(it)
            if (savedPath != null) {
                tempPhotoUri = savedPath
                view?.findViewById<ImageView>(R.id.ivDetailPhoto)?.setImageBitmap(it)
            }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(requireContext(), "Permission caméra refusée", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUriToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap): String? {
        return try {
            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefsManager = PreferencesManager(requireContext())

        val etFirstName = view.findViewById<TextInputEditText>(R.id.etFirstName)
        val etLastName = view.findViewById<TextInputEditText>(R.id.etLastName)
        val etPhone = view.findViewById<TextInputEditText>(R.id.etPhone)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etLatitude = view.findViewById<TextInputEditText>(R.id.etLatitude)
        val etLongitude = view.findViewById<TextInputEditText>(R.id.etLongitude)
        val cbFavorite = view.findViewById<MaterialCheckBox>(R.id.cbFavorite)
        val ivPhoto = view.findViewById<ImageView>(R.id.ivDetailPhoto)
        val btnGallery = view.findViewById<Button>(R.id.btnGallery)
        val btnCamera = view.findViewById<Button>(R.id.btnCamera)
        val btnCall = view.findViewById<Button>(R.id.btnCall)
        val btnEmail = view.findViewById<Button>(R.id.btnEmail)
        val btnShowMap = view.findViewById<Button>(R.id.btnShowMap)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        val fontSp = prefsManager.fontSizeSp
        val textColor = prefsManager.textColorInt

        etFirstName.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSp)
        etLastName.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSp)
        etPhone.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSp)
        etFirstName.setTextColor(textColor)
        etLastName.setTextColor(textColor)

        val contactId = viewModel.selectedContactId.value
        val isCreation = (contactId == null)

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            title = if (isCreation) "Nouveau contact" else "Détail du contact"
            setDisplayHomeAsUpEnabled(true)
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                if (!isCreation) {
                    menu.add(Menu.NONE, 102, Menu.NONE, "Supprimer")
                        .setIcon(android.R.drawable.ic_menu_delete)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == 102 && !isCreation) {
                    showDeleteConfirmationDialog()
                    return true
                } else if (menuItem.itemId == android.R.id.home) {
                    parentFragmentManager.popBackStack()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        if (contactId != null) {
            viewModel.getContact(contactId) { contact ->
                currentContact = contact
                contact?.let { c ->
                    etFirstName.setText(c.firstName)
                    etLastName.setText(c.lastName)
                    etPhone.setText(c.phoneNumber)
                    etEmail.setText(c.email ?: "")
                    c.latitude?.let { etLatitude.setText(it.toString()) }
                    c.longitude?.let { etLongitude.setText(it.toString()) }
                    cbFavorite.isChecked = c.isFavorite
                    tempPhotoUri = c.photoUri
                    if (!c.photoUri.isNullOrEmpty()) {
                        try {
                            ivPhoto.setImageURI(Uri.parse(c.photoUri))
                        } catch (e: Exception) {
                            ivPhoto.setImageResource(R.drawable.ic_person_placeholder)
                        }
                    } else {
                        ivPhoto.setImageResource(R.drawable.ic_person_placeholder)
                    }
                }
            }
        }

        btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                cameraLauncher.launch(null)
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnCall.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (phone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), "Aucune application de téléphone disponible", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Numéro de téléphone vide", Toast.LENGTH_SHORT).show()
            }
        }

        btnEmail.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), "Aucune application de messagerie disponible", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Adresse e-mail vide", Toast.LENGTH_SHORT).show()
            }
        }

        btnShowMap.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, CarteContactFragment())
                .addToBackStack(null)
                .commit()
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSave.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val latStr = etLatitude.text.toString().trim()
            val lonStr = etLongitude.text.toString().trim()
            val isFavorite = cbFavorite.isChecked

            if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez remplir les champs obligatoires (Prénom, Nom, Téléphone)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            var latitude: Double? = null
            var longitude: Double? = null

            if (latStr.isNotEmpty()) {
                val lat = latStr.toDoubleOrNull()
                if (lat == null || lat < -90.0 || lat > 90.0) {
                    Toast.makeText(requireContext(), "Latitude invalide (-90 à 90)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                latitude = lat
            }

            if (lonStr.isNotEmpty()) {
                val lon = lonStr.toDoubleOrNull()
                if (lon == null || lon < -180.0 || lon > 180.0) {
                    Toast.makeText(requireContext(), "Longitude invalide (-180 à 180)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                longitude = lon
            }

            val emailValue = if (email.isEmpty()) null else email

            if (currentContact == null) {
                val newContact = Contact(
                    id = 0L,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phone,
                    email = emailValue,
                    photoUri = tempPhotoUri,
                    latitude = latitude,
                    longitude = longitude,
                    isFavorite = isFavorite
                )
                viewModel.addContact(newContact) {
                    Toast.makeText(requireContext(), "Contact créé", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            } else {
                val updated = currentContact!!.copy(
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phone,
                    email = emailValue,
                    photoUri = tempPhotoUri,
                    latitude = latitude,
                    longitude = longitude,
                    isFavorite = isFavorite
                )
                viewModel.updateContact(updated) {
                    Toast.makeText(requireContext(), "Contact enregistré", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        currentContact?.let { contact ->
            AlertDialog.Builder(requireContext())
                .setTitle("Confirmation de suppression")
                .setMessage("Voulez-vous vraiment supprimer ${contact.lastName} ${contact.firstName} ?")
                .setPositiveButton("Supprimer") { _, _ ->
                    viewModel.deleteContact(contact.id) {
                        Toast.makeText(requireContext(), "Contact supprimé", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }
}