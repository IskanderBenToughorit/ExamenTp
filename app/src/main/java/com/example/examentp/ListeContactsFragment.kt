package com.example.examentp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.examentp.data.PreferencesManager
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ListeContactsFragment : Fragment(R.layout.fragment_liste_contacts) {

    private val viewModel: ContactViewModel by activityViewModels()
    private lateinit var adapter: ContactAdapter
    private lateinit var prefsManager: PreferencesManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefsManager = PreferencesManager(requireContext())

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            title = "Mes contacts"
            setDisplayHomeAsUpEnabled(false)
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.add(Menu.NONE, 102, Menu.NONE, "Paramètres")
                    .setIcon(android.R.drawable.ic_menu_preferences)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == 102) {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container_view, ParametresFragment())
                        .addToBackStack(null)
                        .commit()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rvContacts = view.findViewById<RecyclerView>(R.id.rvContacts)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAddContact)

        adapter = ContactAdapter(
            contacts = emptyList(),
            fontSizeSp = prefsManager.fontSizeSp,
            textColorInt = prefsManager.textColorInt,
            isCallEnabled = prefsManager.isCallFromListEnabled,
            onCallClick = { phone ->
                if (phone.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(requireContext(), "Aucune application de téléphone disponible", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Numéro de téléphone vide", Toast.LENGTH_SHORT).show()
                }
            },
            onFavoriteClick = { contactId, _ ->
                viewModel.toggleFavorite(contactId)
            },
            onClick = { contactId ->
                viewModel.selectContact(contactId)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_view, DetailContactFragment())
                    .addToBackStack(null)
                    .commit()
            }
        )

        rvContacts.layoutManager = LinearLayoutManager(requireContext())
        rvContacts.adapter = adapter

        fabAdd.setOnClickListener {
            viewModel.selectContact(null)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, DetailContactFragment())
                .addToBackStack(null)
                .commit()
        }

        viewModel.filteredContacts.observe(viewLifecycleOwner) { contacts ->
            adapter.updateData(
                newContacts = contacts,
                newFontSizeSp = prefsManager.fontSizeSp,
                newTextColorInt = prefsManager.textColorInt,
                newIsCallEnabled = prefsManager.isCallFromListEnabled
            )
            if (contacts.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvContacts.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvContacts.visibility = View.VISIBLE
            }
        }

        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            if (etSearch.text.toString() != query) {
                etSearch.setText(query)
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadContacts()
        val currentContacts = viewModel.filteredContacts.value ?: emptyList()
        adapter.updateData(
            newContacts = currentContacts,
            newFontSizeSp = prefsManager.fontSizeSp,
            newTextColorInt = prefsManager.textColorInt,
            newIsCallEnabled = prefsManager.isCallFromListEnabled
        )
    }
}