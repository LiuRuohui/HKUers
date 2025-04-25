package hk.hku.cs.hkuers.features.lostfound;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.models.LostFound;

public class LostFoundDetailActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView titleText;
    private TextView descriptionText;
    private TextView locationText;
    private TextView dateText;
    private TextView typeText;
    private TextView categoryText;
    private TextView contactText;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String currentUserId;
    private String itemId;
    private LostFound item;
    private DocumentReference itemRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_found_detail);

        // Get item ID from intent
        itemId = getIntent().getStringExtra("item_id");
        if (itemId == null) {
            Toast.makeText(this, "Error: Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        itemRef = db.collection("lost_found").document(itemId);

        // Initialize views
        imageView = findViewById(R.id.imageView);
        titleText = findViewById(R.id.titleText);
        descriptionText = findViewById(R.id.descriptionText);
        locationText = findViewById(R.id.locationText);
        dateText = findViewById(R.id.dateText);
        typeText = findViewById(R.id.typeText);
        categoryText = findViewById(R.id.categoryText);
        contactText = findViewById(R.id.contactText);
        progressBar = findViewById(R.id.progressBar);

        // Load item details
        loadItem();
    }

    private void loadItem() {
        progressBar.setVisibility(View.VISIBLE);

        itemRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    item = documentSnapshot.toObject(LostFound.class);
                    if (item != null) {
                        item.setId(documentSnapshot.getId());
                        displayItem();
                    } else {
                        Toast.makeText(this, "Error: Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading item: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    finish();
                });
    }

    private void displayItem() {
        // Set title in action bar
        setTitle(item.getType().equals("lost") ? "Lost Item" : "Found Item");

        // Load image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            if (item.getImageUrl().startsWith("forum/")) {
                int resourceId = getResources().getIdentifier(
                    item.getImageUrl().replace("forum/", "").replace(".png", ""),
                    "drawable",
                    getPackageName()
                );
                if (resourceId != 0) {
                    Glide.with(this)
                            .load(resourceId)
                            .into(imageView);
                } else {
                    imageView.setImageResource(R.drawable.placeholder_image);
                }
            } else {
                Glide.with(this)
                        .load(item.getImageUrl())
                        .into(imageView);
            }
        } else {
            imageView.setImageResource(R.drawable.placeholder_image);
        }

        // Set text fields
        titleText.setText(item.getTitle());
        descriptionText.setText(item.getDescription());
        locationText.setText(getString(R.string.location_format, item.getLocation()));
        typeText.setText(getString(R.string.type_format,
                item.getType().substring(0, 1).toUpperCase() + item.getType().substring(1)));
        categoryText.setText(getString(R.string.category_format, item.getCategory()));
        contactText.setText(getString(R.string.contact_format, item.getContact()));

        // Format and set date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateStr = sdf.format(item.getDate().toDate());
        dateText.setText(getString(R.string.date_format, dateStr));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (item != null && item.getUserId().equals(currentUserId)) {
            getMenuInflater().inflate(R.menu.menu_lost_found_detail, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            showDeleteDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> deleteItem())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteItem() {
        progressBar.setVisibility(View.VISIBLE);

        itemRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
} 