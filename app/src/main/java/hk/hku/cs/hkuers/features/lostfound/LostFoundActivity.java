package hk.hku.cs.hkuers.features.lostfound;

import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.models.LostFound;

public class LostFoundActivity extends AppCompatActivity implements LostFoundAdapter.OnItemClickListener {
    private static final String TAG = "LostFoundActivity";
    private RecyclerView recyclerView;
    private LostFoundAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText searchInput;
    private Spinner categorySpinner;
    private FloatingActionButton fabAdd;
    
    private FirebaseFirestore db;
    private String currentUserId;
    private List<LostFound> allItems;
    private String currentCategory = "All";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_found);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        searchInput = findViewById(R.id.searchInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        fabAdd = findViewById(R.id.fabAdd);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LostFoundAdapter(this, this);
        recyclerView.setAdapter(adapter);
        
        // Setup category spinner
        setupCategorySpinner();
        
        // Setup search
        setupSearch();
        
        // Setup refresh
        swipeRefresh.setOnRefreshListener(this::loadItems);
        
        // Setup FAB
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateLostFoundActivity.class);
            startActivity(intent);
        });
        
        // Initial load
        loadItems();
    }

    private void setupCategorySpinner() {
        String[] categories = {"全部", "电子产品", "书籍文具", "生活用品", "证件卡片", "其他"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                R.layout.item_spinner, categories);
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        categorySpinner.setAdapter(spinnerAdapter);
        
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = categories[position];
                filterItems();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                filterItems();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadItems() {
        swipeRefresh.setRefreshing(true);
        
        db.collection("lost_found")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allItems = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        LostFound item = document.toObject(LostFound.class);
                        item.setId(document.getId());
                        allItems.add(item);
                    }
                    filterItems();
                    swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading items", e);
                    Toast.makeText(this, "Error loading items: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                });
    }

    private void filterItems() {
        if (allItems == null) return;
        
        List<LostFound> filteredItems = new ArrayList<>();
        for (LostFound item : allItems) {
            boolean matchesCategory = currentCategory.equals("All") ||
                    item.getCategory().equals(currentCategory);
            boolean matchesSearch = searchQuery.isEmpty() ||
                    item.getTitle().toLowerCase().contains(searchQuery) ||
                    item.getDescription().toLowerCase().contains(searchQuery) ||
                    item.getLocation().toLowerCase().contains(searchQuery) ||
                    item.getContact().toLowerCase().contains(searchQuery);
            
            if (matchesCategory && matchesSearch) {
                filteredItems.add(item);
            }
        }
        
        adapter.setItems(filteredItems);
    }

    @Override
    public void onItemClick(LostFound item) {
        Intent intent = new Intent(this, LostFoundDetailActivity.class);
        intent.putExtra("item_id", item.getId());
        startActivity(intent);
    }
} 