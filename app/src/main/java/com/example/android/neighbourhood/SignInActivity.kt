package com.example.android.neighbourhood

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.example.android.neighbourhood.databinding.ActivitySignInBinding
import com.example.android.neighbourhood.model.Friend
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

private const val TAG = "SignInActivity"

class SignInActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding

    private val signIn: ActivityResultLauncher<Intent> =
        registerForActivityResult(FirebaseAuthUIActivityResultContract(), this::onSignInResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()

        if (Firebase.auth.currentUser == null) {
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setIsSmartLockEnabled(true)
                .setLogo(R.mipmap.ic_launcher)
                .setAvailableProviders(listOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                ))
                .build()

            signIn.launch(signInIntent)
        }else {
            goToMainActivity()
        }
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        Log.d(TAG, "${result.resultCode} | $RESULT_OK")
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Sign in successful!")
            Toast.makeText(this, "Sign in successful", Toast.LENGTH_LONG).show()
//            val db = Firebase.database
//            val user = Firebase.auth.currentUser
//            db.reference
//                .child("users")
//                .child(user?.email.toString().replace(".", ","))
//                .child("details")
//                .setValue(Friend(
//                    user?.displayName.toString(),
//                    user?.photoUrl.toString(),
//                    user?.email.toString()
//                ))
//            Log.d("SignIn", "Value set")
            goToMainActivity()
        } else {
            Toast.makeText(
                this,
                "There was an error signing in",
                Toast.LENGTH_LONG).show()

            val response = result.idpResponse
            if (response == null) {
                Log.w(TAG, "Sign in canceled")
            } else {
                Log.w(TAG, "Sign in error", response.error)
            }
        }
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}