package com.example.android.neighbourhood.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.android.neighbourhood.databinding.FriendViewBinding
import com.example.android.neighbourhood.model.Friend
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.bumptech.glide.Glide
import com.example.android.neighbourhood.FriendListFragment
import com.example.android.neighbourhood.FriendListFragmentDirections
import com.example.android.neighbourhood.MainActivity
import com.example.android.neighbourhood.R

private const val TAG = "FriendListAdapter"
class FriendListAdapter(
    options: FirebaseRecyclerOptions<Friend>,
): FirebaseRecyclerAdapter<Friend, ViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.friend_view, parent, false)
        val binding = FriendViewBinding.bind(view)
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Friend) {
        (holder as FriendViewHolder).bind(model)
        holder.itemView.setOnClickListener {
            Log.d("Friend", "Clicked: ${model.userEmail.toString()}")

            // Navigate to friend fragment with email of friend
            val action = FriendListFragmentDirections
                .actionFriendListFragmentToFriendFragment(
                    model.userEmail.toString(),
                    model.name,
                    model.photoUrl.toString()
                )
            holder.itemView.findNavController().navigate(action)
        }
    }

    inner class FriendViewHolder(private val binding: FriendViewBinding): ViewHolder(binding.root) {
        fun bind(item: Friend) {
            binding.friendName.text = item.name

            if (item.photoUrl != null) {
                loadImageIntoView(binding.friendPhoto, item.photoUrl)
            } else {
                binding.friendPhoto.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }
    }

    fun loadImageIntoView(view: ImageView, url: String) {
        if (url.startsWith("gs://")) {
            val storageReference = Firebase.storage.getReferenceFromUrl(url)
            storageReference.downloadUrl
                .addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    Glide.with(view.context)
                        .load(downloadUrl)
                        .into(view)
                }
                .addOnFailureListener { e ->
                    Log.w(
                        TAG,
                        "Getting download url was not successful.",
                        e
                    )
                }
        } else {
            Glide.with(view.context).load(url).into(view)
        }
    }

}