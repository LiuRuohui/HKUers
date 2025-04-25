package hk.hku.cs.hkuers.features.forum;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.models.LostFoundItem;

public class NewLostFoundActivity extends AppCompatActivity {
    private static final String TAG = "NewLostFoundActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_PERMISSIONS = 100;

    private EditText titleEditText;
    private EditText descriptionEditText;
    private EditText locationEditText;
    private EditText contactEditText;
    private ImageView imageView;
    private MaterialButton btnAddImage;
    private String currentPhotoPath;
    private Uri imageUri;
    private String type; // "lost" or "found"
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_lost_found);

        // Get type (lost or found)
        type = getIntent().getStringExtra("type");
        if (type == null) {
            type = "lost"; // Default to lost
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        titleEditText = findViewById(R.id.etTitle);
        descriptionEditText = findViewById(R.id.etDescription);
        locationEditText = findViewById(R.id.etLocation);
        contactEditText = findViewById(R.id.etContact);
        imageView = findViewById(R.id.ivImage);
        btnAddImage = findViewById(R.id.btnAddImage);

        // Set add image button click event
        btnAddImage.setOnClickListener(v -> checkPermissionsAndShowImagePicker());

        // Set title
        setTitle(type.equals("lost") ? "Post Lost Item" : "Post Found Item");

        // Log to confirm activity creation
        Log.d(TAG, "NewLostFoundActivity created: type = " + type);
    }

    private void checkPermissionsAndShowImagePicker() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // Check storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            // Request permissions
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            // Have permissions, show picker
            showImagePickerDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                showImagePickerDialog();
            } else {
                Toast.makeText(this, "Permissions are required to select images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showImagePickerDialog() {
        // Create selection dialog: take photo or choose from gallery
        String[] options = {"Take Photo", "Choose from Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Take photo
                dispatchTakePictureIntent();
            } else {
                // Choose from gallery
                openImagePicker();
            }
        });
        builder.show();
        Log.d(TAG, "Image picker dialog shown");
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
        Log.d(TAG, "Opening image picker");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_lost_found, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveLostFoundItem();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveLostFoundItem() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String contact = contactEditText.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || location.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : "anonymous";
        String userName = currentUser != null ? (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous User") : "Anonymous User";

        // Show loading toast
        Toast.makeText(this, "Publishing...", Toast.LENGTH_SHORT).show();

        // Create unique ID
        String itemId = UUID.randomUUID().toString();

        // If there's an image, upload it first
        if (imageUri != null) {
            // Upload image to Firebase Storage
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child("lost_found_images/" + itemId + ".jpg");

            Log.d(TAG, "Uploading image: " + imageUri.toString());

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Image upload success");
                        // Get image download URL
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            Log.d(TAG, "Image download URL: " + uri.toString());
                            // Image upload successful, create and save item info
                            createAndSaveLostFoundItem(itemId, title, description, location, contact, uri.toString(), userId, userName);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Image upload failed", e);
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Image upload failed, still create item but without image
                        createAndSaveLostFoundItem(itemId, title, description, location, contact, "", userId, userName);
                    });
        } else {
            // No image, create item directly
            Log.d(TAG, "No image to upload");
            createAndSaveLostFoundItem(itemId, title, description, location, contact, "", userId, userName);
        }
    }

    private void createAndSaveLostFoundItem(String itemId, String title, String description,
                                            String location, String contact, String imageUrl,
                                            String userId, String userName) {
        // Create item data map
        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("id", itemId);
        itemMap.put("title", title);
        itemMap.put("description", description);
        itemMap.put("location", location);
        itemMap.put("date", new Date());
        itemMap.put("contact", contact);
        itemMap.put("type", type);
        itemMap.put("imageUrl", imageUrl);
        itemMap.put("userId", userId);
        itemMap.put("userName", userName);
        itemMap.put("category", "Other");

        // Save to Firestore
        Log.d(TAG, "Saving lost/found item to Firestore");
        db.collection("lost_found")
                .document(itemId)
                .set(itemMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Document saved successfully");
                    Toast.makeText(this, "Published successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding document", e);
                    Toast.makeText(this, "Publication failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
            }
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this,
                        "hk.hku.cs.hkuers.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                Log.d(TAG, "Camera intent dispatched");
            }
        } else {
            Log.d(TAG, "No app can handle camera intent");
            Toast.makeText(this, "No app can handle camera operation", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        Log.d(TAG, "Created image file: " + currentPhotoPath);
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Log.d(TAG, "Camera result received");
                // Camera photo has been saved to imageUri
                imageView.setImageURI(imageUri);
                // Update UI display
                imageView.setVisibility(android.view.View.VISIBLE);
                btnAddImage.setText("Change Image");
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Log.d(TAG, "Gallery result received");
                // Photo selected from gallery
                imageUri = data.getData();
                Log.d(TAG, "Selected image URI: " + imageUri);
                imageView.setImageURI(imageUri);
                // Update UI display
                imageView.setVisibility(android.view.View.VISIBLE);
                btnAddImage.setText("Change Image");
            }
        } else {
            Log.d(TAG, "Result not OK");
        }
    }
}
