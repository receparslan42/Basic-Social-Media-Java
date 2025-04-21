package com.receparslan.basicsocialmedia.views;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.receparslan.basicsocialmedia.R;
import com.receparslan.basicsocialmedia.databinding.ActivityPostBinding;
import com.receparslan.basicsocialmedia.model.Post;

import java.util.HashMap;
import java.util.UUID;

public class PostActivity extends AppCompatActivity {

    // View Binding
    private ActivityPostBinding binding;

    // Firebase
    private FirebaseFirestore firebaseFirestore;
    private StorageReference storageReference;
    private FirebaseUser user;

    // Views
    private ImageView selectedImageView;
    private EditText commentEditText;

    // Model
    private Post post;

    // Activity Result Launcher for selecting image from gallery
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Intent intentFromResult = result.getData();
            if (intentFromResult != null) {
                post.setImageUri(intentFromResult.getData());
                if (post.getImageUri() != null) {
                    selectedImageView.setImageURI(post.getImageUri());
                }
            }
        }
    });

    // Activity Result Launcher for requesting permission
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            // Permission is granted
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        } else {
            // Permission is denied
            Toast.makeText(this, "Permission is required to access the gallery.", Toast.LENGTH_LONG).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Initialize view binding
        binding = ActivityPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        user = FirebaseAuth.getInstance().getCurrentUser();

        post = new Post(); // Initialize post

        // Inflate views
        selectedImageView = binding.selectImageView;
        commentEditText = binding.commentEditText;

        selectedImageView.setOnClickListener(this::setSelectedImageView); // Set onClickListener for selected image

        binding.uploadButton.setOnClickListener(this::setUploadButton); // Set onClickListener for upload button

        checkConnection(); // Check the internet connection every 3 seconds
    }

    // Method to select image from gallery
    private void setSelectedImageView(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission(view, android.Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            requestPermission(view, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    // Method to upload the post
    private void setUploadButton(View view) {
        // Check if an image is selected
        if (post.getImageUri() == null) {
            Snackbar.make(view, "Please select an image", Snackbar.LENGTH_LONG).setAction("Select Image", this::setSelectedImageView).show();
        } else {
            // Check if a comment is entered
            post.setComment(commentEditText.getText().toString());

            if (post.getComment().isEmpty()) {
                Snackbar.make(view, "Please enter a comment", Snackbar.LENGTH_LONG).show();
            } else {
                // Upload the post
                String imageName = "images/" + UUID.randomUUID() + ".jpg"; // Create a unique image name

                // Upload the image to Firebase Storage
                storageReference.child(imageName).putFile(post.getImageUri()).addOnCompleteListener(taskSnapshot -> {
                    if (taskSnapshot.isSuccessful()) {
                        // Get the download URL of the image
                        storageReference.child(imageName).getDownloadUrl().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Create a post data
                                HashMap<String, Object> postData = new HashMap<>();
                                postData.put("displayName", user.getDisplayName());
                                postData.put("email", user.getEmail());
                                postData.put("date", FieldValue.serverTimestamp());
                                postData.put("comment", post.getComment());
                                postData.put("imageUrl", task.getResult());

                                // Upload the post data to Firestore
                                firebaseFirestore.collection("Posts").add(postData).addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        Intent intent = new Intent(PostActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(PostActivity.this, task2.getException() != null ? task2.getException().getMessage() : "Post could not be uploaded", Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                Toast.makeText(PostActivity.this, task.getException() != null ? task.getException().getMessage() : "Post could not be uploaded", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Toast.makeText(PostActivity.this, taskSnapshot.getException() != null ? taskSnapshot.getException().getMessage() : "Post could not be uploaded", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    // Method to request permission to access the gallery
    private void requestPermission(View view, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        } else if (shouldShowRequestPermissionRationale(permission)) {
            Snackbar.make(view, "Permission is required to access the gallery.", Snackbar.LENGTH_LONG).setAction("Allow", v -> requestPermissionLauncher.launch(permission)).show();
        } else {
            requestPermissionLauncher.launch(permission);
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(PostActivity.this);
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