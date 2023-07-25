package com.simonhan.expense_tracker

import android.app.Activity.RESULT_OK
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.simonhan.expense_tracker.presentation.profile.ProfileScreen
import com.simonhan.expense_tracker.presentation.sign_in.GoogleAuthUIClient
import com.simonhan.expense_tracker.presentation.sign_in.SignInScreen
import com.simonhan.expense_tracker.presentation.sign_in.SignInViewModel
import com.simonhan.expense_tracker.ui.theme.Expense_trackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val googleAuthUIClient by lazy {
        GoogleAuthUIClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "onCreate is here ")
        setContent {
            Expense_trackerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    assembleNavigation(applicationContext, googleAuthUIClient, lifecycleScope)
                }
            }
        }
    }
}

@Composable
fun assembleNavigation(
    context: Context,
    googleAuthUIClient: GoogleAuthUIClient,
    lifecycleScope: LifecycleCoroutineScope
) {
    val navController = rememberNavController();
    NavHost(navController = navController, startDestination = "sign_in") {
        composable("sign_in") {
            val viewModel = viewModel<SignInViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(key1 = Unit) {
                if (googleAuthUIClient.getSignedInUser() != null) {
                    navController.navigate("profile");
                }
            }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult(),
                onResult = { result ->
                    if (result.resultCode == RESULT_OK) {
                        lifecycleScope.launch {
                            val signInResult = googleAuthUIClient.signInWithIntent(
                                intent = result.data ?: return@launch
                            )
                            viewModel.onSignInResult(signInResult)
                        }
                    }
                }
            )

            LaunchedEffect(key1 = state.isSignInSuccessful) {
                if (state.isSignInSuccessful) {
                    Toast.makeText(context, "Sign in successful", Toast.LENGTH_LONG).show()
                    navController.navigate("profile")
                    viewModel.resetState()
                }
            }

            SignInScreen(
                state = state,
                onSignInClick = {
                    lifecycleScope.launch {
                        try {
                            val signInIntentSender = googleAuthUIClient.signIn();
                            launcher.launch(
                                IntentSenderRequest.Builder(
                                    signInIntentSender ?: return@launch
                                ).build()
                            )
                        } catch (e: Exception) {
                            Log.i("MainActivity", "SignInScreen onSignInClick ------ 111111 ${e.printStackTrace()}")
                        }

                    }
                }
            )
        }
        composable("profile") {
            ProfileScreen(
                userData = googleAuthUIClient.getSignedInUser(),
                onSignOut = {
                    lifecycleScope.launch {
                        googleAuthUIClient.signOut()
                        Toast.makeText(context, "Signed out", Toast.LENGTH_LONG).show()
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}