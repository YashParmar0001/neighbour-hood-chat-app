package com.example.android.neighbourhood

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.android.neighbourhood.adapters.FriendAdapter
import com.example.android.neighbourhood.contracts.MyOpenDocumentContract
import com.example.android.neighbourhood.databinding.FragmentFriendBinding
import com.example.android.neighbourhood.model.FriendlyMessage
import com.example.android.neighbourhood.observers.MyButtonObserver
import com.example.android.neighbourhood.observers.MyScrollToBottomObserver
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.onesignal.OSDeviceState
import com.onesignal.OneSignal
import org.json.JSONObject


class FriendFragment : Fragment() {
    private val args: FriendFragmentArgs by navArgs()

    private var _binding: FragmentFriendBinding? = null
    private lateinit var manager: WrapManager

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var chatRef: DatabaseReference

    private lateinit var chat: String
    private lateinit var adapter: FriendAdapter

    private lateinit var device: OSDeviceState

    private val binding get() = _binding

    private val openDocument = registerForActivityResult(MyOpenDocumentContract()) { uri ->
        uri?.let { onImageSelected(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFriendBinding.inflate(inflater, container, false)

        auth = Firebase.auth
        // Initializing realtime database
        db = Firebase.database

        // Initialize device state
        device = OneSignal.getDeviceState()!!

        val userEmail = auth.currentUser?.email
        val friendEmail = args.email
        if (userEmail != null) {
            chat = if (userEmail.length > friendEmail.length) {
                userEmail.replace(".", ",") +
                        "_to_" + friendEmail.replace(".", ",")
            } else {
                friendEmail.replace(".", ",") +
                        "_to_" + userEmail.replace(".", ",")
            }
        } else {
            startActivity(Intent(requireContext(), SignInActivity::class.java))
        }

        // Reference of user chat with friend
        chatRef = db.reference.child("chats").child(chat)

        // For recyclerview
        val options = FirebaseRecyclerOptions.Builder<FriendlyMessage>()
            .setQuery(chatRef, FriendlyMessage::class.java)
            .build()
        adapter = FriendAdapter(options, userEmail)
        manager = WrapManager(this.requireContext())
        manager.stackFromEnd = true
        _binding?.messageRecyclerView?.layoutManager = manager
        _binding?.messageRecyclerView?.adapter = adapter

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var chat = ""
        val userEmail = auth.currentUser?.email
        val friendEmail = args.email
        if (userEmail != null) {
            chat = if (userEmail.length > friendEmail.length) {
                userEmail.replace(".", ",") +
                        "_to_" + friendEmail.replace(".", ",")
            } else {
                friendEmail.replace(".", ",") +
                        "_to_" + userEmail.replace(".", ",")
            }
        } else {
            startActivity(Intent(requireContext(), SignInActivity::class.java))
        }
        // Reference of user chat with friend
        val chatRef = db.reference.child("chats").child(chat)

        // Set friend details on top
        binding?.friendName?.text = args.name
        binding?.friendPhoto?.let { loadImageIntoView(it, args.photoUrl) }

        // Scroll down when a new message arrives
        // See MyScrollToBottomObserver for details
        binding?.let { MyScrollToBottomObserver(it.messageRecyclerView, adapter, manager) }?.let {
            adapter.registerAdapterDataObserver(
                it
            )
        }

        // Disable the send button when there's no text in the input field
        // See MyButtonObserver for details
        binding?.messageEditText?.addTextChangedListener(binding?.let { MyButtonObserver(it.sendButton) })

        // When the send button is clicked send a text message
        binding?.sendButton?.setOnClickListener {
            val text = binding!!.messageEditText.text.toString()
            // Get friend id to send notification
            val friendId = args.userIdOS
            Log.d("FriendFragment", friendId)

            val friendlyMessage = FriendlyMessage(
                text,
                getUserName(),
                userEmail,
                getPhotoUrl(),
                null
            )
            chatRef.push().setValue(friendlyMessage)
            binding!!.messageEditText.setText("")
            // Send notification
            sendNotification(text, getUserName().toString(), friendId, "")
        }

        // When the image button is clicked, launch the image picker
        binding?.addMessageImageView?.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }
    }

    override fun onPause() {
        adapter.stopListening()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // Clear all notifications
        OneSignal.clearOneSignalNotifications()
        adapter.startListening()
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

    private fun onImageSelected(uri: Uri) {
        Log.d("FriendFragment", "Uri: $uri")
        val user = auth.currentUser
        val tempMessage =
            FriendlyMessage(null, getUserName(), getUserEmail(), getPhotoUrl(), LOADING_IMAGE_URL)
        chatRef
            .push()
            .setValue(
                tempMessage,
                DatabaseReference.CompletionListener { databaseError, databaseReference ->
                    if (databaseError != null) {
                        Log.w(
                            "FriendFragment", "Unable to write message to database.",
                            databaseError.toException()
                        )
                        return@CompletionListener
                    }

                    // Build a StorageReference and then upload the file
                    val key = databaseReference.key
                    val storageReference = Firebase.storage
                        .getReference(user!!.uid)
                        .child(key!!)
                        .child(uri.lastPathSegment!!)
                    putImageInStorage(storageReference, uri, key)
                })
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri, key: String?) {
        // First upload the image to Cloud Storage
        storageReference.putFile(uri)
            .addOnSuccessListener(
                this.requireActivity()
            ) { taskSnapshot -> // After the image loads, get a public downloadUrl for the image
                // and add it to the message.
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        val friendlyMessage =
                            FriendlyMessage(
                                null,
                                getUserName(),
                                getUserEmail(),
                                getPhotoUrl(),
                                uri.toString()
                            )
                        chatRef
                            .child(key!!)
                            .setValue(friendlyMessage)
                        // Send notification
                        sendNotification(
                            "Photo",
                            getUserName().toString(),
                            args.userIdOS,
                            uri.toString(),
                        )
                    }
            }
            .addOnFailureListener(this.requireActivity()) { e ->
                Log.w(
                    "FriendFragment",
                    "Image upload task was unsuccessful.",
                    e
                )
            }
    }

    private fun getUserEmail(): String {
        val user = auth.currentUser
        if (user != null) {
            return user.email.toString()
        }
        return "anonymous"
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

    private fun loadImageIntoView(view: ImageView, url: String) {
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
                        "FriendFragment",
                        "Getting download url was not successful.",
                        e
                    )
                }
        } else {
            Glide.with(view.context).load(url).into(view)
        }
    }

    // Function to send notification
    private fun sendNotification(message: String, heading: String, id: String, picture: String) {
        val notification = JSONObject(
            "{" +
                    "'contents': {'en':'$message'}, " +
                    "'include_player_ids': ['$id'], " +
                    "'headings': {'en':'$heading'}," +
                    "'big_picture': '$picture'," +
                    "'data': {'email': '${getUserEmail()}'," +
                    "'name': '${getUserName()}'," +
                    "'photoUrl': '${getPhotoUrl()}'," +
                    "'userIdOS': '${device.userId}'}}"
        )
        val handler = ApplicationClass.Handler()
        Log.d("FriendFragment", notification.toString())
        OneSignal.postNotification(notification, handler)
    }

    companion object {
        const val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
    }

}