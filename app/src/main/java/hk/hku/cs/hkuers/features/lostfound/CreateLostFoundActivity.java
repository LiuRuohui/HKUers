package hk.hku.cs.hkuers.features.lostfound;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.Timestamp;

import java.util.UUID;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.models.LostFound;

public class CreateLostFoundActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText locationInput;
    private TextInputEditText contactInput;
    private Spinner categorySpinner;
    private RadioGroup typeRadioGroup;
    private ImageView imageView;
    private Button selectImageButton;
    private Button submitButton;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String currentUserId;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_lost_found);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        locationInput = findViewById(R.id.locationInput);
        contactInput = findViewById(R.id.contactInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        typeRadioGroup = findViewById(R.id.typeRadioGroup);
        imageView = findViewById(R.id.imageView);
        selectImageButton = findViewById(R.id.selectImageButton);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);

        // 设置默认联系方式
        contactInput.setText("u3638173@connect.hku.hk");

        // Setup category spinner
        setupCategorySpinner();

        // Setup image selection
        selectImageButton.setOnClickListener(v -> openImagePicker());

        // Setup submit button
        submitButton.setOnClickListener(v -> submitItem());
    }

    private void setupCategorySpinner() {
        String[] categories = {"其他", "Electronics", "Books", "Clothing"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK 
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(imageView);
        }
    }

    private void submitItem() {
        if (validateInputs()) {
            String title = titleInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();
            String contact = contactInput.getText().toString().trim();
            String category = categorySpinner.getSelectedItem().toString();
            String type = typeRadioGroup.getCheckedRadioButtonId() == R.id.radioLost ? "lost" : "found";

            progressBar.setVisibility(View.VISIBLE);
            submitButton.setEnabled(false);

            if (imageUri != null) {
                // Upload image first
                String imageFileName = "lost_found/" + UUID.randomUUID().toString() + ".jpg";
                StorageReference imageRef = storage.getReference().child(imageFileName);
                
                imageRef.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            // Get the download URL
                            imageRef.getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        String imageUrl = uri.toString();
                                        createItem(title, description, location, category, type, imageUrl, contact);
                                    })
                                    .addOnFailureListener(e -> handleError(e));
                        })
                        .addOnFailureListener(e -> handleError(e));
            } else {
                // No image selected, use default image
                createItem(title, description, location, category, type, "forum/placeholder.png", contact);
            }
        }
    }

    private boolean validateInputs() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String contact = contactInput.getText().toString().trim();
        
        if (title.isEmpty()) {
            titleInput.setError("Title is required");
            return false;
        }
        
        if (description.isEmpty()) {
            descriptionInput.setError("Description is required");
            return false;
        }
        
        if (location.isEmpty()) {
            locationInput.setError("Location is required");
            return false;
        }
        
        if (contact.isEmpty()) {
            contactInput.setError("Contact is required");
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(contact).matches()) {
            contactInput.setError("Please enter a valid email address");
            return false;
        }
        
        return true;
    }

    private void createItem(String title, String description,
                          String location, String category, String type, 
                          String imageUrl, String contact) {
        LostFound item = new LostFound(
            currentUserId,  // userId
            "匿名用户",  // userName
            title,
            description,
            location,
            imageUrl,
            category,
            type,
            contact,
            Timestamp.now()
        );

        String itemId = UUID.randomUUID().toString();
        item.setId(itemId);

        db.collection("lost_found")
                .document(itemId)
                .set(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item created successfully",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> handleError(e));
    }

    private void handleError(Exception e) {
        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);
        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
} 