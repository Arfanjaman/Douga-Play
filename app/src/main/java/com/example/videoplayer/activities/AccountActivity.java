package com.example.videoplayer.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.videoplayer.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AccountActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextView userNameTextView;
    private TextView userEmailTextView;
    private Button signOutButton;
    private Button signInButton;
    private Button updateUsernameButton;
    private Button updatePasswordButton;
    private Button deleteAccountButton;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        userNameTextView = findViewById(R.id.user_name);
        userEmailTextView = findViewById(R.id.user_email);
        signOutButton = findViewById(R.id.sign_out_button);
        signInButton = findViewById(R.id.sign_in_button);
        updateUsernameButton = findViewById(R.id.update_username_button);
        updatePasswordButton = findViewById(R.id.update_password_button);
        deleteAccountButton = findViewById(R.id.delete_account_button);

        if (currentUser != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
            updateUI(currentUser);
        } else {
            updateUI(null);
        }

        signOutButton.setOnClickListener(v -> {
            auth.signOut();
            updateUI(null);
        });

        signInButton.setOnClickListener(v -> showSignInDialog());

        updateUsernameButton.setOnClickListener(v -> showUpdateUsernameDialog());

        updatePasswordButton.setOnClickListener(v -> showUpdatePasswordDialog());

        deleteAccountButton.setOnClickListener(v -> deleteAccount());
    }

    private void showSignInDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sign_in, null);
        builder.setView(dialogView);

        EditText emailEditText = dialogView.findViewById(R.id.email);
        EditText passwordEditText = dialogView.findViewById(R.id.password);

        builder.setPositiveButton("Sign In", (dialog, which) -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            if (!email.isEmpty() && !password.isEmpty()) {
                signInUser(email, password);
            } else {
                Toast.makeText(AccountActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        TextView dontHaveAccount = dialogView.findViewById(R.id.dont_have_account);
        dontHaveAccount.setOnClickListener(v -> {
            startActivity(new Intent(AccountActivity.this, RegisterActivity.class));
        });

        builder.create().show();
    }

    private void signInUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser currentUser = auth.getCurrentUser();
                if (currentUser != null) {
                    databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
                }
                updateUI(currentUser);
                Toast.makeText(AccountActivity.this, "Sign In successful", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AccountActivity.this, "Sign In failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUpdateUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_username, null);
        builder.setView(dialogView);

        EditText currentPasswordEditText = dialogView.findViewById(R.id.current_password);
        EditText newUsernameEditText = dialogView.findViewById(R.id.new_username);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String currentPassword = currentPasswordEditText.getText().toString().trim();
            String newUsername = newUsernameEditText.getText().toString().trim();
            if (!currentPassword.isEmpty() && !newUsername.isEmpty()) {
                updateUsername(currentPassword, newUsername);
            } else {
                Toast.makeText(AccountActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void updateUsername(String currentPassword, String newUsername) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(newUsername).build())
                            .addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    databaseReference.child("username").setValue(newUsername);
                                    Toast.makeText(AccountActivity.this, "Username updated", Toast.LENGTH_SHORT).show();
                                    updateUI(user);
                                } else {
                                    Toast.makeText(AccountActivity.this, "Failed to update username", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(AccountActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showUpdatePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_password, null);
        builder.setView(dialogView);

        EditText currentPasswordEditText = dialogView.findViewById(R.id.current_password);
        EditText newPasswordEditText = dialogView.findViewById(R.id.new_password);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String currentPassword = currentPasswordEditText.getText().toString().trim();
            String newPassword = newPasswordEditText.getText().toString().trim();
            if (!currentPassword.isEmpty() && !newPassword.isEmpty()) {
                updatePassword(currentPassword, newPassword);
            } else {
                Toast.makeText(AccountActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void updatePassword(String currentPassword, String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userUid = user.getUid(); // Get the currently logged-in user's UID
            String email = user.getEmail();

            if (email == null) {
                Toast.makeText(AccountActivity.this, "User email not found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Reauthenticate the user with their current password
            AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // If reauthentication is successful, update the password
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            // Update the password in the Realtime Database for the current UID
                            DatabaseReference userRef = FirebaseDatabase.getInstance()
                                    .getReference("Users")
                                    .child(userUid); // Use the correct UID

                            userRef.child("password").setValue(newPassword).addOnCompleteListener(dbTask -> {
                                if (dbTask.isSuccessful()) {
                                    userRef.child("passwordUpdatedAt").setValue(System.currentTimeMillis());
                                    Toast.makeText(AccountActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(AccountActivity.this, "Failed to update password in database", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(AccountActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(AccountActivity.this, "Authentication failed. Please check your current password.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(AccountActivity.this, "No user is signed in", Toast.LENGTH_SHORT).show();
        }
    }


    private void deleteAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Delete the user from Firebase Authentication
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Remove the user data from the Realtime Database
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
                    DatabaseReference favouritesRef = FirebaseDatabase.getInstance().getReference("FavouriteVideos").child(user.getUid());
                    DatabaseReference recentVideosRef = FirebaseDatabase.getInstance().getReference("PlayedVideos").child(user.getUid());
                    DatabaseReference watchHistoryRef = FirebaseDatabase.getInstance().getReference("SearchesPerUser").child(user.getUid());

                    userRef.removeValue().addOnCompleteListener(dbTask -> {
                        if (dbTask.isSuccessful()) {
                            favouritesRef.removeValue().addOnCompleteListener(favTask -> {
                                if (favTask.isSuccessful()) {
                                    recentVideosRef.removeValue().addOnCompleteListener(recentTask -> {
                                        if (recentTask.isSuccessful()) {
                                            watchHistoryRef.removeValue().addOnCompleteListener(historyTask -> {
                                                if (historyTask.isSuccessful()) {
                                                    Toast.makeText(AccountActivity.this, "Account and all related data deleted", Toast.LENGTH_SHORT).show();
                                                    updateUI(null);
                                                } else {
                                                    Toast.makeText(AccountActivity.this, "Failed to delete watch history data", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            Toast.makeText(AccountActivity.this, "Failed to delete recent videos data", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    Toast.makeText(AccountActivity.this, "Failed to delete favourite videos data", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(AccountActivity.this, "Failed to delete user data from database", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(AccountActivity.this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(AccountActivity.this, "No user is signed in", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            String userName = user.getDisplayName();
            String userEmail = user.getEmail();

            userNameTextView.setText(userName);
            userEmailTextView.setText(userEmail);
            signOutButton.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.GONE);
            updateUsernameButton.setVisibility(View.VISIBLE);
            updatePasswordButton.setVisibility(View.VISIBLE);
            deleteAccountButton.setVisibility(View.VISIBLE);
        } else {
            userNameTextView.setText("");
            userEmailTextView.setText("");
            signOutButton.setVisibility(View.GONE);
            signInButton.setVisibility(View.VISIBLE);
            updateUsernameButton.setVisibility(View.GONE);
            updatePasswordButton.setVisibility(View.GONE);
            deleteAccountButton.setVisibility(View.GONE);
        }
    }
}