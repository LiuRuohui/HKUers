package hk.hku.cs.hkuers.features.forum;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.adapters.LostFoundAdapter;
import hk.hku.cs.hkuers.features.forum.models.LostFoundItem;

// First, necessary imports
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.google.firebase.firestore.DocumentSnapshot;

public class LostFoundActivity extends AppCompatActivity {
    private static final String TAG = "LostFoundActivity";

    private RecyclerView recyclerView;
    private LostFoundAdapter adapter;
    private List<LostFoundItem> lostFoundItems;
    private List<LostFoundItem> filteredList;
    private Spinner spinnerCategory;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputEditText searchEditText;
    private FloatingActionButton fabNewLostFound;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormatter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_found);

        try {
            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance();

            // Initialize date formatter
            dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            // Initialize lists
            lostFoundItems = new ArrayList<>();
            filteredList = new ArrayList<>();

            // Initialize Firebase
            db = FirebaseFirestore.getInstance();

            // Initialize SwipeRefreshLayout
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setOnRefreshListener(this::refreshData);
            }

            // Initialize search box
            searchEditText = findViewById(R.id.searchEditText);
            if (searchEditText != null) {
                searchEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        filterBySearchText(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
            }

            // Initialize RecyclerView - use correct ID
            recyclerView = findViewById(R.id.recyclerView);
            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView not found! Check your layout ID");
                Toast.makeText(this, "Error: RecyclerView not found", Toast.LENGTH_SHORT).show();
                return; // Exit early if key view is missing
            }

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new LostFoundAdapter(this, filteredList); // Add this as Context parameter
            recyclerView.setAdapter(adapter);

            // Initialize category dropdown
            spinnerCategory = findViewById(R.id.spinnerCategory);
            if (spinnerCategory != null) {
                // Set up spinner
                String[] itemTypes = {"All", "Phone", "Wallet", "Keys", "Cards", "Other"};
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, itemTypes);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(spinnerAdapter);

                spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        // Safely call filter method
                        filterItems(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Do nothing
                    }
                });
            } else {
                Log.e(TAG, "spinnerCategory not found in layout!");
            }

            // Initialize floating action button
            fabNewLostFound = findViewById(R.id.fabAdd);
            if (fabNewLostFound != null) {
                fabNewLostFound.setOnClickListener(v -> showAddPostDialog());
            } else {
                Log.e(TAG, "FAB not found in layout!");
            }

            // Load initial data
            loadLostFoundItems();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LostFoundAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);
    }

    private void refreshData() {
        // Refresh data
        loadLostFoundItems();

        // End refresh animation
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void filterBySearchText(String query) {
        if (query == null || query.isEmpty()) {
            // If search box is empty, use category filter
            if (spinnerCategory != null) {
                filterItems(spinnerCategory.getSelectedItemPosition());
            } else {
                // If no category selector, show all items
                filteredList.clear();
                filteredList.addAll(lostFoundItems);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
            return;
        }

        // Perform search filtering
        List<LostFoundItem> searchResults = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (LostFoundItem item : lostFoundItems) {
            // Search in title, description and location
            if ((item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerCaseQuery)) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerCaseQuery)) ||
                    (item.getLocation() != null && item.getLocation().toLowerCase().contains(lowerCaseQuery))) {
                searchResults.add(item);
            }
        }

        // Update list
        filteredList.clear();
        filteredList.addAll(searchResults);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void loadLostFoundItems() {
        try {
            // Show loading indicator
            if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(true);
            }

            // Clear existing data
            lostFoundItems.clear();

            // Get data from Firestore
            db.collection("lostFoundItems")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (DocumentSnapshot document : queryDocumentSnapshots) {
                                LostFoundItem item = document.toObject(LostFoundItem.class);
                                if (item != null) {
                                    lostFoundItems.add(item);
                                }
                            }

                            // Apply current filter
                            if (searchEditText != null && searchEditText.getText() != null &&
                                    !searchEditText.getText().toString().isEmpty()) {
                                // If there's search text, filter by search text
                                filterBySearchText(searchEditText.getText().toString());
                            } else if (spinnerCategory != null) {
                                // Otherwise filter by category
                                filterItems(spinnerCategory.getSelectedItemPosition());
                            } else {
                                // If no filter, show all items
                                filteredList.clear();
                                filteredList.addAll(lostFoundItems);
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        } else {
                            Log.d(TAG, "No documents found");
                            // Add sample data if no data
                            addSampleData();
                        }

                        // Hide loading indicator
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting documents: ", e);
                        Toast.makeText(this, "Error loading items", Toast.LENGTH_SHORT).show();
                        // Add sample data on error
                        addSampleData();
                        // Hide loading indicator
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error loading items", e);
            Toast.makeText(this, "Error loading items", Toast.LENGTH_SHORT).show();
            // Ensure loading indicator is hidden
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    private void addSampleData() {
        Log.d(TAG, "Adding sample data");
        // Add some sample data items for testing
        try {
            lostFoundItems.clear();
            filteredList.clear();
            LostFoundItem item1 = new LostFoundItem(
                    "item1",
                    "Lost iPhone 13",
                    "Black iPhone 13 with blue case, has visible scratches",
                    "Library 1st floor",
                    new Date(),
                    "john@example.com",
                    "lost", // Type: lost
                    "", // No image yet
                    "user123",
                    "John",
                    "Phone" // Category
            );
            lostFoundItems.add(item1);
            Log.d(TAG, "Added item1: " + item1.getTitle());

            LostFoundItem item2 = new LostFoundItem(
                    "item2",
                    "Found Student ID Card",
                    "Found a student ID card, name: Li Ming, student ID: 2021xxxx",
                    "Student Center",
                    new Date(),
                    "mary@example.com",
                    "found", // Type: found
                    "", // No image yet
                    "user456",
                    "Mary",
                    "Cards" // Category
            );
            lostFoundItems.add(item2);
            Log.d(TAG, "Added item2: " + item2.getTitle());

            LostFoundItem item3 = new LostFoundItem(
                    "item3",
                    "Lost Wallet",
                    "Black leather wallet with small amount of cash and bank cards",
                    "Canteen",
                    new Date(),
                    "tom@example.com",
                    "lost", // Type: lost
                    "", // No image yet
                    "user789",
                    "Tom",
                    "Wallet" // Category
            );
            lostFoundItems.add(item3);
            Log.d(TAG, "Added item3: " + item3.getTitle());

            // Update filteredList
            filteredList.addAll(lostFoundItems);
            Log.d(TAG, "Updated filteredList with " + filteredList.size() + " items");

            // Notify adapter
            if (adapter != null) {
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Notified adapter of data change");
            } else {
                Log.e(TAG, "Adapter is null!");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding sample data: " + e.getMessage(), e);
        }
    }

    private void showAddPostDialog() {
        try {
            // Check if user is logged in
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Please login before posting", Toast.LENGTH_SHORT).show();
                // TODO: Redirect to login page if needed
                return;
            }

            // Create dialog layout
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Post Lost & Found Item");

            // Use custom layout
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_lost_found, null);
            builder.setView(dialogView);

            // Get dialog controls
            final EditText etTitle = dialogView.findViewById(R.id.etTitle);
            final EditText etDescription = dialogView.findViewById(R.id.etDescription);
            final EditText etLocation = dialogView.findViewById(R.id.etLocation);
            final EditText etContactInfo = dialogView.findViewById(R.id.etContactInfo);
            final Button btnDate = dialogView.findViewById(R.id.btnDate);
            final RadioGroup rgType = dialogView.findViewById(R.id.rgType);
            final Spinner spinnerItemCategory = dialogView.findViewById(R.id.spinnerItemCategory);

            // Set up category dropdown
            String[] categories = {"Phone", "Wallet", "Keys", "Cards", "Other"};
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, categories);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerItemCategory.setAdapter(categoryAdapter);

            // Set up date selection
            selectedDate = Calendar.getInstance();
            btnDate.setText(dateFormatter.format(selectedDate.getTime()));
            btnDate.setOnClickListener(v -> {
                showDatePickerDialog(btnDate);
            });

            // Prefill contact info if logged-in user has email
            if (currentUser.getEmail() != null) {
                etContactInfo.setText(currentUser.getEmail());
            }

            // Set dialog buttons
            builder.setPositiveButton("Post", null); // Set to null, will override later
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            // Create and show dialog
            AlertDialog dialog = builder.create();

            // Override Positive button click to prevent empty field submission
            dialog.setOnShowListener(dialogInterface -> {
                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(view -> {
                    // Validate all required fields
                    if (validateFields(etTitle, etDescription, etLocation, etContactInfo, rgType, spinnerItemCategory)) {
                        // Collect form data
                        String title = etTitle.getText().toString().trim();
                        String description = etDescription.getText().toString().trim();
                        String location = etLocation.getText().toString().trim();
                        String contactInfo = etContactInfo.getText().toString().trim();
                        String category = spinnerItemCategory.getSelectedItem().toString();

                        // Get type (lost/found)
                        String type = "lost"; // Default to lost
                        int selectedTypeId = rgType.getCheckedRadioButtonId();
                        if (selectedTypeId != -1) {
                            RadioButton selectedType = dialog.findViewById(selectedTypeId);
                            if (selectedType != null && "Found Item".equals(selectedType.getText().toString())) {
                                type = "found";
                            }
                        }

                        // Create and save item
                        saveNewItem(title, description, location, contactInfo, category, type);

                        // Close dialog
                        dialog.dismiss();
                    }
                });
            });

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing add post dialog: " + e.getMessage(), e);
            Toast.makeText(this, "Cannot create posting dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void filterItems(int position) {
        try {
            // Safety check
            if (spinnerCategory == null) {
                Log.d(TAG, "spinnerCategory is null, using all items");
                filteredList.clear();
                filteredList.addAll(lostFoundItems);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                return;
            }

            filteredList.clear();

            if (position >= 0 && position < spinnerCategory.getCount()) {
                String selectedCategory = spinnerCategory.getItemAtPosition(position).toString();

                for (LostFoundItem item : lostFoundItems) {
                    // Add item when first option "All" or category matches
                    if (position == 0 || "All".equals(selectedCategory) ||
                            selectedCategory.equals(item.getCategory())) {
                        filteredList.add(item);
                    }
                }
            } else {
                // Invalid position, show all items
                filteredList.addAll(lostFoundItems);
            }

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtering items: " + e.getMessage(), e);
            // On exception, show all items
            filteredList.clear();
            filteredList.addAll(lostFoundItems);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    // Show date picker dialog
    private void showDatePickerDialog(final Button dateButton) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    dateButton.setText(dateFormatter.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // Validate form fields
    private boolean validateFields(EditText etTitle, EditText etDescription,
                                   EditText etLocation, EditText etContactInfo,
                                   RadioGroup rgType, Spinner spinnerItemCategory) {
        boolean valid = true;

        // Check title
        if (etTitle.getText().toString().trim().isEmpty()) {
            etTitle.setError("Please enter a title");
            valid = false;
        }

        // Check description
        if (etDescription.getText().toString().trim().isEmpty()) {
            etDescription.setError("Please enter a description");
            valid = false;
        }

        // Check location
        if (etLocation.getText().toString().trim().isEmpty()) {
            etLocation.setError("Please enter a location");
            valid = false;
        }

        // Check contact info
        if (etContactInfo.getText().toString().trim().isEmpty()) {
            etContactInfo.setError("Please enter contact information");
            valid = false;
        }

        // Check type selection
        if (rgType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select item type (Lost/Found)", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;
    }

    // Save new item to Firestore
    private void saveNewItem(String title, String description, String location,
                             String contactInfo, String category, String type) {
        try {
            // Show loading message
            Toast.makeText(this, "Posting...", Toast.LENGTH_SHORT).show();

            // Get current user info
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }


            // Create a unique ID
            String itemId = UUID.randomUUID().toString();

            // Create new LostFoundItem object
            LostFoundItem newItem = new LostFoundItem(
                    itemId,
                    title,
                    description,
                    location,
                    selectedDate.getTime(), // Use selected date
                    contactInfo,
                    type,
                    "", // No image URL yet
                    user.getUid(),
                    user.getDisplayName() != null ? user.getDisplayName() : "Anonymous User",
                    category
            );

            // Save to Firestore
            db.collection("lostFoundItems")
                    .document(itemId)
                    .set(newItem)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(LostFoundActivity.this, "Posted successfully!", Toast.LENGTH_SHORT).show();
                        // Refresh data
                        loadLostFoundItems();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding document", e);
                        Toast.makeText(LostFoundActivity.this, "Posting failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving new item: " + e.getMessage(), e);
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
