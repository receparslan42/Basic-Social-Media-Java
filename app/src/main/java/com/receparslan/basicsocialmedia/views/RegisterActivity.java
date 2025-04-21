package com.receparslan.basicsocialmedia.views;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.receparslan.basicsocialmedia.R;
import com.receparslan.basicsocialmedia.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    // Firebase Auth
    private FirebaseAuth mAuth;

    // EditTexts for user information
    private EditText nameEditText;
    private EditText surnameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText passwordAgainEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Initialize view binding and set the content view
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inflate the layout for this activity
        nameEditText = binding.nameEditText;
        surnameEditText = binding.surnameEditText;
        emailEditText = binding.emailEditText;
        passwordEditText = binding.passwordEditText;
        passwordAgainEditText = binding.passwordAgainEditText;

        binding.saveButton.setOnClickListener(this::setSaveButton); // Set the save button

        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Auth

        checkConnection(); // Check the internet connection every 3 seconds
    }

    // Method to register the user
    public void setSaveButton(View view) {
        // Get the user information
        String name = nameEditText.getText().toString();
        String surname = surnameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String passwordAgain = passwordAgainEditText.getText().toString();

        // Check if the fields are empty
        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty() || passwordAgain.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_LONG).show();
        } else if (!password.equals(passwordAgain)) {
            // Check if the passwords match
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_LONG).show();
        } else {
            // Register the user
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Update the user profile
                    FirebaseUser user = mAuth.getCurrentUser();
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(name + " " + surname).build();
                    if (user != null)
                        user.updateProfile(profileUpdates);

                    new AlertDialog.Builder(RegisterActivity.this)
                            .setTitle("User Registered")
                            .setMessage("User registered successfully. Please log in !")
                            .setPositiveButton("OK", (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
                                loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(loginIntent);
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    Toast.makeText(this, "User registration failed, please try again!", Toast.LENGTH_LONG).show(); // Show an error message
                }
            });
        }
    }

    // Method to check the internet connection
    private void checkConnection() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ConnectivityManager connectivityManager = (ConnectivityManager) binding.getRoot().getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

                if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
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