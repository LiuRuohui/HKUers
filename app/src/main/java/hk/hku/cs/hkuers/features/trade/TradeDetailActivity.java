package hk.hku.cs.hkuers.features.trade;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import hk.hku.cs.hkuers.R;

public class TradeDetailActivity extends AppCompatActivity {
    private static final String TAG = "TradeDetailActivity";
    
    public static Intent newIntent(Context context, String itemId) {
        Intent intent = new Intent(context, TradeDetailActivity.class);
        intent.putExtra("tradeId", itemId);
        return intent;
    }

    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView priceTextView;
    private TextView sellerTextView;
    private TextView categoryTextView;
    private MaterialButton contactButton;
    private MaterialButton favoriteButton;
    private MaterialButton shareButton;
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TradeItem tradeItem;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_trade_detail);

            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();

            initializeViews();
            setupToolbar();
            
            String tradeId = getIntent().getStringExtra("tradeId");
            Log.d(TAG, "onCreate: tradeId = " + tradeId);
            
            if (tradeId != null && !tradeId.isEmpty()) {
                loadTradeItem(tradeId);
                checkFavoriteStatus(tradeId);
            } else {
                Log.e(TAG, "No tradeId provided in intent");
                Toast.makeText(this, "Error: No item ID provided", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        titleTextView = findViewById(R.id.tvItemName);
        descriptionTextView = findViewById(R.id.tvDescription);
        priceTextView = findViewById(R.id.tvPrice);
        sellerTextView = findViewById(R.id.tvSellerName);
        categoryTextView = findViewById(R.id.tvCategory);
        contactButton = findViewById(R.id.btnContactSeller);
        favoriteButton = findViewById(R.id.btnFavorite);
        shareButton = findViewById(R.id.btnShare);

        contactButton.setOnClickListener(v -> contactSeller());
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        shareButton.setOnClickListener(v -> shareItem());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void checkFavoriteStatus(String tradeId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.getUid())
                .collection("favorites")
                .document(tradeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isFavorite = documentSnapshot.exists();
                    updateFavoriteButton();
                });
        }
    }

    private void toggleFavorite() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        String tradeId = tradeItem.getId();
        if (tradeId == null) {
            Log.e(TAG, "Cannot favorite item: tradeId is null");
            Toast.makeText(this, "Error: Cannot identify item", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Toggle favorite for item ID: " + tradeId);

        if (isFavorite) {
            // Remove from favorites
            db.collection("users")
                .document(currentUser.getUid())
                .collection("favorites")
                .document(tradeId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    isFavorite = false;
                    updateFavoriteButton();
                    Snackbar.make(favoriteButton, R.string.favorite_removed, Snackbar.LENGTH_SHORT).show();
                    Log.d(TAG, "Item removed from favorites: " + tradeId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing from favorites: " + e.getMessage(), e);
                    Toast.makeText(this, "Error removing from favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } else {
            // Add to favorites - ensure we have the complete item with ID
            if (tradeItem != null) {
                // Make sure item has proper ID set
                tradeItem.setId(tradeId);
                
                // Create a copy of the item's data to store in favorites
                // This ensures we don't lose any fields
                db.collection("users")
                    .document(currentUser.getUid())
                    .collection("favorites")
                    .document(tradeId)
                    .set(tradeItem)
                    .addOnSuccessListener(aVoid -> {
                        isFavorite = true;
                        updateFavoriteButton();
                        Snackbar.make(favoriteButton, R.string.favorite_added, Snackbar.LENGTH_SHORT).show();
                        Log.d(TAG, "Item added to favorites: " + tradeId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding to favorites: " + e.getMessage(), e);
                        Toast.makeText(this, "Error adding to favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            } else {
                Log.e(TAG, "Cannot favorite: tradeItem is null");
                Toast.makeText(this, "Error: Item data is missing", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateFavoriteButton() {
        favoriteButton.setIconResource(isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
    }

    private void shareItem() {
        if (tradeItem == null) return;

        String shareText = String.format("%s\nPrice: $%.2f\n%s",
            tradeItem.getTitle(),
            tradeItem.getPrice(),
            tradeItem.getDescription());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share item"));
    }

    private void contactSeller() {
        if (tradeItem != null) {
            // TODO: Implement actual contact functionality
            Toast.makeText(this, "Contact seller: " + tradeItem.getSellerName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent upIntent = new Intent(this, TradeListActivity.class);
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            upIntent.putExtra("fromDetail", true);
            startActivity(upIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent();
        intent.putExtra("refreshList", true);
        setResult(RESULT_OK, intent);
    }

    private void loadTradeItem(String tradeId) {
        Log.d(TAG, "Loading trade item with ID: " + tradeId);
        try {
            DocumentReference docRef = db.collection("trades").document(tradeId);
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    try {
                        // 记录接收到的所有字段，用于调试
                        Log.d(TAG, "Document data: " + documentSnapshot.getData());
                        
                        tradeItem = documentSnapshot.toObject(TradeItem.class);
                        if (tradeItem != null) {
                            tradeItem.setId(tradeId);
                            
                            // 处理timestamp或createTime字段
                            Object timestampObj = documentSnapshot.get("timestamp");
                            if (timestampObj != null) {
                                if (timestampObj instanceof Long) {
                                    long timestamp = (Long) timestampObj;
                                    Log.d(TAG, "Timestamp from Firestore (numeric): " + timestamp);
                                    tradeItem.setTimestamp(timestamp);
                                } else if (timestampObj instanceof Timestamp) {
                                    Timestamp firestoreTimestamp = (Timestamp) timestampObj;
                                    Log.d(TAG, "Timestamp from Firestore (Timestamp): " + firestoreTimestamp);
                                    tradeItem.setCreateTime(firestoreTimestamp);
                                } else {
                                    Log.d(TAG, "Timestamp is an unsupported type: " + timestampObj.getClass().getName());
                                }
                            } else {
                                // 检查createTime字段
                                Object createTimeObj = documentSnapshot.get("createTime");
                                if (createTimeObj != null) {
                                    if (createTimeObj instanceof Timestamp) {
                                        Timestamp firestoreTimestamp = (Timestamp) createTimeObj;
                                        Log.d(TAG, "CreateTime from Firestore: " + firestoreTimestamp);
                                        tradeItem.setCreateTime(firestoreTimestamp);
                                    } else {
                                        Log.d(TAG, "CreateTime is an unsupported type: " + createTimeObj.getClass().getName());
                                    }
                                } else {
                                    Log.d(TAG, "No timestamp or createTime field found in document");
                                }
                            }
                            
                            updateUI();
                        } else {
                            Log.e(TAG, "Failed to convert document to TradeItem");
                            Toast.makeText(this, "Error: Failed to load item details", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing document: " + e.getMessage(), e);
                        Toast.makeText(this, "Error processing item data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Log.e(TAG, "Document does not exist for ID: " + tradeId);
                    Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error loading trade item: " + e.getMessage(), e);
                Toast.makeText(this, "Failed to load item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in loadTradeItem: " + e.getMessage(), e);
            Toast.makeText(this, "Error accessing database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateUI() {
        try {
            if (tradeItem == null) {
                Log.e(TAG, "tradeItem is null in updateUI");
                Toast.makeText(this, "Error: Item data is missing", Toast.LENGTH_SHORT).show();
                return;
            }

            // 设置标题
            titleTextView.setText(tradeItem.getTitle());
            
            // 设置描述，确保非空
            String description = tradeItem.getDescription();
            if (description == null || description.isEmpty()) {
                description = "No description provided";
            }
            descriptionTextView.setText(description);
            
            // 设置价格
            priceTextView.setText(String.format("$%.2f", tradeItem.getPrice()));
            
            // 设置卖家名称，确保非空
            String sellerName = tradeItem.getSellerName();
            if (sellerName == null || sellerName.isEmpty()) {
                sellerName = "Anonymous";
            }
            sellerTextView.setText(sellerName);
            
            // 设置类别，确保非空
            String category = tradeItem.getCategory();
            if (category == null || category.isEmpty()) {
                category = "Other";
            }
            categoryTextView.setText(category);
            
            Log.d(TAG, "UI updated with item: " + tradeItem.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
        }
    }
} 