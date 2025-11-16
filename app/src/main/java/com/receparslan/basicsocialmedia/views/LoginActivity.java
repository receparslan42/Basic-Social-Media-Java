package com.receparslan.basicsocialmedia.views;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.receparslan.basicsocialmedia.R;
import com.receparslan.basicsocialmedia.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    // View binding
    private ActivityLoginBinding binding;

    // Firebase Auth
    private FirebaseAuth mAuth;

    // EditTexts for user information
    private EditText emailEditText;
    private EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Initialize view binding and set the content view
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inflate the layout for this activity
        emailEditText = binding.emailEditText;
        passwordEditText = binding.passwordEditText;

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();

        // Set the buttons' onClickListeners
        binding.loginButton.setOnClickListener(view -> setLoginButton());
        binding.registerButton.setOnClickListener(view -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        checkConnection(); // Check the internet connection
    }

    // Sign in with email and password
    private void setLoginButton() {
        // Get the user information
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // Check if the fields are empty
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Email and password cannot be empty!", Toast.LENGTH_LONG).show(); // Show a toast message if the fields are empty
        } else {
            // Sign in with email and password
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign in success, redirect user to the main page
                    loginIntent();
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(LoginActivity.this, "User not found. Please check your e-mail and password!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Method to redirect user to the main page
    private void loginIntent() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    // Check the internet connection in every 3 seconds
    private void checkConnection() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ConnectivityManager connectivityManager = (ConnectivityManager) binding.getRoot().getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

                if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    builder.setTitle("No Internet Connection");
                    builder.setMessage("Please check your internet connection and try again.");
                    builder.setPositiveButton("OK", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        finish();
                    });
                    if (!isFinishing())
                        builder.show();
                }
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(runnable);
    }
}