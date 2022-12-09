package com.example.android.neighbourhood

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.android.neighbourhood.databinding.FragmentAddFriendBinding
import com.example.android.neighbourhood.databinding.FragmentFriendListBinding
import com.example.android.neighbourhood.model.Friend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AddFriendFragment: Fragment() {
    private var _binding: FragmentAddFriendBinding? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var ref: DatabaseReference

    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddFriendBinding.inflate(inflater, container, false)
        auth = Firebase.auth
        db = Firebase.database
        ref = db.reference
            .child("users")
            .child("${getUserEmail().replace(".", ",")}")

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.addFriendButton?.setOnClickListener {
            val email = binding!!.friendEmail.text.toString().trim()
            context?.let { it1 -> addFriend(ref, email.replace(".", ","), it1) }
            findNavController().popBackStack()
        }
    }

    private fun addFriend(ref: DatabaseReference, email: String, context: Context) {
        if (auth.currentUser?.email.toString().replace(".", ",") == email) {
            Toast.makeText(context, "You're your friend already!", Toast.LENGTH_LONG).show()
            return
        }
        var friend: Friend?
        val friendRef = db.reference.child("users").child(email)

        friendRef.child("details")
            .get().addOnSuccessListener {
                val value = it.getValue<Friend>()
                if (value != null) {
                    friend = Friend(
                        value.name,
                        value.photoUrl,
                        value.userEmail
                    )
                    // Add friend to current user
                    ref.child("friends").child(email).setValue(friend)
                    // Add current user to friend
                    friendRef.child("friends").child(getUserEmail().replace(".", ",")).setValue(
                        Friend(
                            auth.currentUser?.displayName.toString(),
                            auth.currentUser?.photoUrl.toString(),
                            auth.currentUser?.email.toString()
                        )
                    )
                    Toast.makeText(context, "Friend added", Toast.LENGTH_LONG).show()
//                    Log.d("AddFriend", "Friend added")
                } else {
//                    Log.d("AddFriend", "Doesn't use app")
                    Toast.makeText(context, "Doesn't use app", Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to add friend!", Toast.LENGTH_LONG).show()
            }
    }

    private fun getUserEmail(): String {
        val user = auth.currentUser
        if (user != null) {
            return user.email.toString()
        }
        return "anonymous"
    }
}