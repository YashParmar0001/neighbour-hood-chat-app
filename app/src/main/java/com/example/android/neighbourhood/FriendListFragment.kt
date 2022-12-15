package com.example.android.neighbourhood

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.android.neighbourhood.adapters.FriendListAdapter
import com.example.android.neighbourhood.databinding.FragmentFriendListBinding
import com.example.android.neighbourhood.model.Friend
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.onesignal.OSDeviceState
import com.onesignal.OneSignal

class FriendListFragment : Fragment() {
    private var _binding: FragmentFriendListBinding? = null

    private lateinit var manager: FriendFragment.WrapManager

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: FriendListAdapter

    // For OneSignal
    private lateinit var device: OSDeviceState

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

        // Initialize device state
        device = OneSignal.getDeviceState()!!

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
                .setValue(
                    Friend(
                        user?.displayName.toString(),
                        user?.photoUrl.toString(),
                        user?.email.toString(),
                        device.userId?.toString()
                    )
                )

            // Initialize realtime database
            val ref = db.reference
                .child("users")
                .child(getUserEmail().replace(".", ","))

            // For recyclerview
            val options = FirebaseRecyclerOptions.Builder<Friend>()
                .setQuery(ref.child("friends"), Friend::class.java)
                .build()
            adapter = FriendListAdapter(options)
            manager = FriendFragment.WrapManager(this.requireContext())
            manager.stackFromEnd = false
            _binding?.friendRecyclerView?.layoutManager = manager
            _binding?.friendRecyclerView?.adapter = adapter
        }

        // On notification clicked
        OneSignal.setNotificationOpenedHandler { result ->
            if (result.notification.title == "Testing") {
                Log.d("Friend", "Notification received")
            }else {
                val data = result.notification.additionalData
                Log.d("Friend", "Data: ${result.notification.additionalData.get("email")}")
                val action = FriendListFragmentDirections
                    .actionFriendListFragmentToFriendFragment(
                        data.get("email").toString(),
                        data.get("name").toString(),
                        data.get("photoUrl").toString(),
                        data.get("userIdOS").toString()
                    )
                findNavController().navigate(action)
            }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        Log.d("FriendListFragment", "On start called!")
        // Clear all notifications
        OneSignal.clearOneSignalNotifications()
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
}