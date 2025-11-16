package com.receparslan.basicsocialmedia.views;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.receparslan.basicsocialmedia.R;
import com.receparslan.basicsocialmedia.adapter.RecyclerAdapter;
import com.receparslan.basicsocialmedia.databinding.ActivityMainBinding;
import com.receparslan.basicsocialmedia.model.Post;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    // ArrayList to store the posts
    private ArrayList<Post> postArrayList;

    // RecyclerView to show the posts
    private RecyclerView recyclerView;

    // View binding
    private ActivityMainBinding binding;

    // Floating Action Buttons
    private ExtendedFloatingActionButton moreEFAB;
    private ExtendedFloatingActionButton logoutEFAB;
    private ExtendedFloatingActionButton deleteAccountEFAB;
    private ExtendedFloatingActionButton addPostEFAB;

    // Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Set the content view using view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the postArrayList
        postArrayList = new ArrayList<>();

        // Initialize Firebase Auth and get the current user
        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        // Check if the user is signed in
        if (user == null) {
            // User is not signed in, redirect to the login page
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else {
            // Get the posts from the Firestore
            getData();
        }

        // Inflate the layout for this activity
        moreEFAB = binding.moreEFAB;
        logoutEFAB = binding.logoutEFAB;
        deleteAccountEFAB = binding.deleteAccountEFAB;
        addPostEFAB = binding.addPostEFAB;

        // Initialize the RecyclerView
        recyclerView = binding.recyclerView;
        recyclerView.setAdapter(new RecyclerAdapter(postArrayList));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set the layout for the first time
        moreEFAB.shrink();
        logoutEFAB.hide();
        logoutEFAB.shrink();
        deleteAccountEFAB.hide();
        deleteAccountEFAB.shrink();
        addPostEFAB.hide();
        addPostEFAB.shrink();

        // Set the click listeners for the FABs
        addPostEFAB.setOnClickListener(view -> setAddPostEFAB());
        deleteAccountEFAB.setOnClickListener(view -> setDeleteAccountEFAB());
        logoutEFAB.setOnClickListener(view -> setLogoutEFAB());
        moreEFAB.setOnClickListener(view -> setMoreEFAB());

        // Check if the internet connection is available
        checkConnection();
    }

    // Method to redirect to the post activity
    private void setAddPostEFAB() {
        // Redirect to the add post page
        Intent intent = new Intent(MainActivity.this, PostActivity.class);
        startActivity(intent);
    }

    // Method to delete the user account
    private void setDeleteAccountEFAB() {
        // Create an EditText to get the password from the user for re-authentication
        EditText passwordEditText = new EditText(MainActivity.this);
        passwordEditText.setHint(R.string.confirm_password);
        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordEditText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // Create an AlertDialog to confirm the account deletion
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete your account?");
        builder.setView(passwordEditText);
        builder.setPositiveButton("Yes", (dialogInterface, i) -> {
            // Re-authenticate the user and delete the account
            user.reauthenticate(EmailAuthProvider.getCredential(Objects.requireNonNull(user.getEmail()), String.valueOf(passwordEditText.getText()))).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Delete user's posts
                    firebaseFirestore.collection("Posts").whereEqualTo("email", user.getEmail()).get().addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                            for (DocumentSnapshot documentSnapshot : task2.getResult().getDocuments()) {
                                String imageUrl = documentSnapshot.getString("imageUrl");
                                String path = "images/" + Objects.requireNonNull(imageUrl).substring(imageUrl.indexOf("%2F") + 3, imageUrl.indexOf(".jpg")) + ".jpg";
                                storageReference.child(path).delete().addOnSuccessListener(voidTask -> documentSnapshot.getReference().delete());
                            }
                        }
                    });

                    // Delete the user account
                    user.delete().addOnCompleteListener(voidTask -> {
                        if (voidTask.isSuccessful()) {
                            // Account deleted successfully
                            Toast.makeText(MainActivity.this, "Account deleted successfully", Toast.LENGTH_LONG).show();

                            // Redirect to the login page
                            logoutIntent();
                        } else {
                            // Account deletion failed
                            Toast.makeText(MainActivity.this, "Account deletion failed, try again later!", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    // Re-authentication failed due to incorrect password
                    Toast.makeText(MainActivity.this, "Re-authentication failed, please check your password and try again later!", Toast.LENGTH_LONG).show();
                }
            });
            dialogInterface.dismiss();
        });
        builder.setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.show();
    }

    // Method to logout the user
    private void setLogoutEFAB() {
        logoutIntent(); // Redirect to the login page

        mAuth.signOut(); // Sign out the user
    }

    // Method to handle the click event of the more FAB
    private void setMoreEFAB() {
        // Check if the FAB is extended
        if (moreEFAB.isExtended()) {
            // Shrink the FAB and hide the other FABs
            moreEFAB.shrink();
            moreEFAB.setIcon(AppCompatResources.getDrawable(this, android.R.drawable.ic_input_add));
            logoutEFAB.hide();
            logoutEFAB.shrink();
            deleteAccountEFAB.hide();
            deleteAccountEFAB.shrink();
            addPostEFAB.hide();
            addPostEFAB.shrink();
        } else {
            // Extend the FAB and show the other FABs
            moreEFAB.extend();
            moreEFAB.setIcon(AppCompatResources.getDrawable(this, android.R.drawable.ic_delete));
            logoutEFAB.show();
            logoutEFAB.extend();
            deleteAccountEFAB.show();
            deleteAccountEFAB.extend();
            addPostEFAB.show();
            addPostEFAB.extend();
        }
    }

    // Method to redirect user to the login page
    private void logoutIntent() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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

    // Method to get the posts from the Firestore
    private void getData() {
        firebaseFirestore.collection("Posts").orderBy("date", Query.Direction.DESCENDING).addSnapshotListener((value, error) -> {
            // Check if there is an error
            if (error == null && value != null) {
                // Get the posts from the Firestore and add them to the postArrayList
                for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                    Map<String, Object> data = documentSnapshot.getData();

                    if (data != null) {
                        Post post = new Post();
                        post.setDisplayName((String) data.get("displayName"));
                        post.setEmail((String) data.get("email"));
                        post.setComment((String) data.get("comment"));
                        post.setImageUri(Uri.parse((String) data.get("imageUrl")));

                        // Set the date of the post
                        Timestamp ts = ((Timestamp) data.get("date"));
                        if (ts != null)
                            post.setDate(new java.sql.Timestamp(ts.toDate().getTime()).toString().split("\\.")[0]);

                        postArrayList.add(post);

                        // Notify the adapter that the data has changed
                        if (recyclerView.getAdapter() != null)
                            recyclerView.getAdapter().notifyItemInserted(postArrayList.size() - 1);
                    }
                }
            }
        });
    }
}