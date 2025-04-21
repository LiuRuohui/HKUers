package hk.hku.cs.hkuers.features.trade;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import hk.hku.cs.hkuers.R;

public class EditTradeActivity extends AppCompatActivity {
    
    public static Intent newIntent(Context context, String tradeId) {
        Intent intent = new Intent(context, EditTradeActivity.class);
        intent.putExtra("tradeId", tradeId);
        return intent;
    }

    private static final String[] CATEGORIES = {
        "Electronics",
        "Books",
        "Clothing",
        "Sports",
        "Home & Living",
        "Beauty & Health",
        "Others"
    };

    private String tradeId;
    private TradeItem tradeItem;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextInputEditText titleEditText;
    private TextInputEditText descriptionEditText;
    private TextInputEditText priceEditText;
    private MaterialAutoCompleteTextView categorySpinner;
    private ExtendedFloatingActionButton saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_trade);

        tradeId = getIntent().getStringExtra("tradeId");
        if (tradeId == null) {
            Toast.makeText(this, "无效的商品ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 检查用户是否登录
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupSpinners();
        loadTradeItem();

        saveButton.setOnClickListener(v -> saveTradeItem());
    }

    private void initializeViews() {
        titleEditText = findViewById(R.id.editTextTitle);
        descriptionEditText = findViewById(R.id.editTextDescription);
        priceEditText = findViewById(R.id.editTextPrice);
        categorySpinner = findViewById(R.id.spinnerCategory);
        saveButton = findViewById(R.id.fabSave);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupSpinners() {
        // 设置类别下拉菜单
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, R.layout.dropdown_item_dark, CATEGORIES);
        categorySpinner.setAdapter(categoryAdapter);
    }

    private void loadTradeItem() {
        db.collection("trades").document(tradeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tradeItem = documentSnapshot.toObject(TradeItem.class);
                        if (tradeItem != null) {
                            tradeItem.setId(tradeId);
                            
                            // 检查当前用户是否是商品发布者
                            if (!tradeItem.getSellerId().equals(auth.getCurrentUser().getUid())) {
                                Toast.makeText(this, "您无权编辑此商品", Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }
                            
                            fillFormWithData();
                        }
                    } else {
                        Toast.makeText(this, "商品不存在", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void fillFormWithData() {
        titleEditText.setText(tradeItem.getTitle());
        descriptionEditText.setText(tradeItem.getDescription());
        priceEditText.setText(String.valueOf(tradeItem.getPrice()));
        
        // 设置类别
        String category = tradeItem.getCategory();
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(category)) {
                categorySpinner.setText(CATEGORIES[i], false);
                break;
            }
        }
    }

    private void saveTradeItem() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String priceStr = priceEditText.getText().toString().trim();
        String category = categorySpinner.getText().toString().trim();
        
        // 验证输入
        if (title.isEmpty()) {
            titleEditText.setError("请输入标题");
            return;
        }
        
        if (description.isEmpty()) {
            descriptionEditText.setError("请输入描述");
            return;
        }
        
        if (priceStr.isEmpty()) {
            priceEditText.setError("请输入价格");
            return;
        }
        
        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                priceEditText.setError("价格必须大于零");
                return;
            }
        } catch (NumberFormatException e) {
            priceEditText.setError("无效的价格");
            return;
        }
        
        if (category.isEmpty()) {
            Toast.makeText(this, "请选择类别", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 更新商品信息
        tradeItem.setTitle(title);
        tradeItem.setDescription(description);
        tradeItem.setPrice(price);
        tradeItem.setCategory(category);
        
        // 保存到Firestore
        DocumentReference docRef = db.collection("trades").document(tradeId);
        docRef.set(tradeItem)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}