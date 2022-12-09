package com.example.android.neighbourhood

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.android.neighbourhood.adapters.FriendListAdapter
import com.example.android.neighbourhood.databinding.FragmentFriendListBinding
import com.example.android.neighbourhood.model.Friend
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class FriendListFragment: Fragment() {
    private var _binding: FragmentFriendListBinding? = null

    //    private lateinit var manager: WrapManager
    private lateinit var manager: WrapManager

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var adapter: FriendListAdapter

    // For menu
    private lateinit var menuHost: MenuHost

    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFriendListBinding.inflate(inflater, container, false)
        Log.d("FriendListFragment", "On create called!")

        // If user is not signed in
        auth = Firebase.auth

        if (auth.currentUser == null) {
            startActivity(Intent(this.context, SignInActivity::class.java))
        } else {
            // Upload user details
            val db = Firebase.database
            val user = Firebase.auth.currentUser
            db.reference
                .child("users")
                .child(getUserEmail().replace(".", ","))
                .child("details")
                .setValue(Friend(
                    user?.displayName.toString(),
                    user?.photoUrl.toString(),
                    user?.email.toString()
                ))

            // Initialize realtime database
            val ref = db.reference
                .child("users")
                .child("${getUserEmail().replace(".", ",")}")

            // For recyclerview
            val options = FirebaseRecyclerOptions.Builder<Friend>()
                .setQuery(ref.child("friends"), Friend::class.java)
                .build()
            adapter = FriendListAdapter(options)
            manager = WrapManager(this.requireContext())
            manager.stackFromEnd = false
            _binding?.friendRecyclerView?.layoutManager = manager
            _binding?.friendRecyclerView?.adapter = adapter
        }

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FriendListFragment", "On view created called!")

        binding?.addFriendFab?.setOnClickListener {
            findNavController().navigate(R.id.action_friendListFragment_to_addFriendFragment)
        }

        binding?.userName?.text = getUserName()
        binding?.userEmail?.text = getUserEmail()
        val photoView = binding?.userPhoto
        if (photoView != null) {
            Glide.with(view.context).load(getPhotoUrl()).into(photoView)
        }

        // Creating menu
//        menuHost = requireHost() as MenuHost
//        menuHost.addMenuProvider(object : MyMenuProvider(){})
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        Log.d("FriendListFragment", "On start called!")
        // Check if user is signed in.
        if (auth.currentUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this.requireContext(), SignInActivity::class.java))
            return
        }
    }

    override fun onPause() {
        Log.d("FriendListFragment", "On pause called!")
        if (auth.currentUser != null) {
            adapter.stopListening()
        }
        super.onPause()
    }

    override fun onResume() {
        Log.d("FriendListFragment", "On resume called!")
        if (auth.currentUser != null) {
            adapter.startListening()
        }
        super.onResume()
    }

    class WrapManager(context: Context) : LinearLayoutManager(context) {
        override fun onLayoutChildren(
            recycler: RecyclerView.Recycler?,
            state: RecyclerView.State?
        ) {
            try {
                super.onLayoutChildren(recycler, state)
            } catch (e: java.lang.IndexOutOfBoundsException) {
                Log.e("FriendListFragment", "Error occurred!")
            }
        }
    }

    private fun signOut() {
        AuthUI.getInstance().signOut(this.requireContext())
        startActivity(Intent(this.context, SignInActivity::class.java))
    }

    private fun getPhotoUrl(): String? {
        val user = auth.currentUser
        return user?.photoUrl?.toString()
    }

    private fun getUserName(): String? {
        val user = auth.currentUser
        return if (user != null) {
            user.displayName
        } else "anonymous"
    }

    private fun getUserEmail(): String {
        val user = auth.currentUser
        if (user != null) {
            return user.email.toString()
        }
        return "anonymous"
    }

    open inner class MyMenuProvider: MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            Log.d("FriendListFragment", "Menu created!")
            menuInflater.inflate(R.menu.main_menu, menu)
        }

        override fun onMenuItemSelected(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.sign_out_menu -> {
                    signOut()
                    true
                }
                else -> false
            }
        }
    }
}