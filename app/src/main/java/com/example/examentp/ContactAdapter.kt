package com.example.examentp

import android.graphics.Color
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.examentp.data.Contact

class ContactAdapter(
    private var contacts: List<Contact>,
    private var fontSizeSp: Float = 16f,
    private var textColorInt: Int = Color.BLACK,
    private var isCallEnabled: Boolean = false,
    private val onCallClick: (String) -> Unit,
    private val onFavoriteClick: (Long, Boolean) -> Unit,
    private val onClick: (Long) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivContactPhoto)
        val tvName: TextView = view.findViewById(R.id.tvContactName)
        val tvPhone: TextView = view.findViewById(R.id.tvContactPhone)
        val btnFavorite: ImageButton = view.findViewById(R.id.btnFavoriteItem)
        val btnCall: ImageButton = view.findViewById(R.id.btnCallItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.tvName.text = "${contact.lastName} ${contact.firstName}"
        holder.tvPhone.text = contact.phoneNumber

        holder.tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
        holder.tvPhone.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
        holder.tvName.setTextColor(textColorInt)

        if (contact.isFavorite) {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_on)
        } else {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_off)
        }

        holder.btnFavorite.setOnClickListener {
            onFavoriteClick(contact.id, !contact.isFavorite)
        }

        if (isCallEnabled) {
            holder.btnCall.visibility = View.VISIBLE
            holder.btnCall.setOnClickListener {
                onCallClick(contact.phoneNumber)
            }
        } else {
            holder.btnCall.visibility = View.GONE
        }

        if (!contact.photoUri.isNullOrEmpty()) {
            try {
                holder.ivPhoto.setImageURI(Uri.parse(contact.photoUri))
            } catch (e: Exception) {
                holder.ivPhoto.setImageResource(R.drawable.ic_person_placeholder)
            }
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_person_placeholder)
        }

        holder.itemView.setOnClickListener {
            onClick(contact.id)
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateData(
        newContacts: List<Contact>,
        newFontSizeSp: Float = fontSizeSp,
        newTextColorInt: Int = textColorInt,
        newIsCallEnabled: Boolean = isCallEnabled
    ) {
        contacts = newContacts
        fontSizeSp = newFontSizeSp
        textColorInt = newTextColorInt
        isCallEnabled = newIsCallEnabled
        notifyDataSetChanged()
    }
}