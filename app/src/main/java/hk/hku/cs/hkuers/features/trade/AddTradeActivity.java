package hk.hku.cs.hkuers.features.trade;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.auth.LoginActivity;

public class AddTradeActivity extends AppCompatActivity {

    private static final String TAG = "AddTradeActivity";
    private static final String[] CATEGORIES = {
        "Electronics",
        "Books",
        "Clothing",
        "Sports",
        "Home & Living",
        "Beauty & Health",
        "Others"
    };

    private MaterialToolbar toolbar;
    private MaterialCardView imageCardView;
    private ImageView productImageView;
    private View uploadImageLayout;
    private TextInputLayout titleInputLayout, descriptionInputLayout, priceInputLayout, categoryInputLayout;
    private TextInputEditText titleEditText, descriptionEditText, priceEditText;
    private AutoCompleteTextView categoryAutoCompleteTextView;
    private MaterialButton submitButton;
    private View loadingOverlay;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;
    private Uri selectedImageUri;

    private ActivityResultLauncher<String> getContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_add_trade);

            db = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();
            auth = FirebaseAuth.getInstance();

            initViews();
            setupToolbar();
            setupCategoryDropdown();
            setupImagePicker();
            
            // 检查用户是否登录
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                // 用户未登录，跳转到登录页面
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            submitButton.setOnClickListener(v -> addTrade());
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        imageCardView = findViewById(R.id.imageCardView);
        productImageView = findViewById(R.id.productImageView);
        uploadImageLayout = findViewById(R.id.uploadImageLayout);
        titleInputLayout = findViewById(R.id.titleInputLayout);
        descriptionInputLayout = findViewById(R.id.descriptionInputLayout);
        priceInputLayout = findViewById(R.id.priceInputLayout);
        categoryInputLayout = findViewById(R.id.categoryInputLayout);
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        priceEditText = findViewById(R.id.priceEditText);
        categoryAutoCompleteTextView = findViewById(R.id.categoryAutoCompleteTextView);
        submitButton = findViewById(R.id.submitButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        
        // 为图片卡片添加点击事件
        imageCardView.setOnClickListener(v -> {
            // 启动图片选择器
            getContent.launch("image/*");
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupCategoryDropdown() {
        List<String> categories = Arrays.asList(CATEGORIES);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, categories
        );
        categoryAutoCompleteTextView.setAdapter(adapter);
    }

    private void setupImagePicker() {
        try {
            getContent = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        productImageView.setImageURI(uri);
                        productImageView.setVisibility(View.VISIBLE);
                        uploadImageLayout.setVisibility(View.GONE);
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error setting up image picker", e);
        }
    }

    private void addTrade() {
        try {
            // 获取用户输入
            String title = titleEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();
            String priceText = priceEditText.getText().toString().trim();
            String category = categoryAutoCompleteTextView.getText().toString().trim();

            // 验证输入
            if (title.isEmpty()) {
                titleInputLayout.setError("请输入商品标题");
                return;
            }

            if (description.isEmpty()) {
                descriptionInputLayout.setError("请输入商品描述");
                return;
            }

            if (priceText.isEmpty()) {
                priceInputLayout.setError("请输入商品价格");
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceText);
            } catch (NumberFormatException e) {
                priceInputLayout.setError("请输入有效的价格");
                return;
            }

            if (category.isEmpty()) {
                categoryInputLayout.setError("请选择商品分类");
                return;
            }

            // 显示进度条
            loadingOverlay.setVisibility(View.VISIBLE);
            submitButton.setEnabled(false);

            // 获取当前用户
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                loadingOverlay.setVisibility(View.GONE);
                submitButton.setEnabled(true);
                return;
            }

            // 上传数据到Firestore
            String userId = currentUser.getUid();
            // 确保sellerName不为null
            String sellerName = "Anonymous";
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                sellerName = currentUser.getDisplayName();
            }

            // 创建与现有数据格式一致的商品数据Map
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("title", title);
            tradeData.put("description", description);
            tradeData.put("price", price);  // 确保是double类型
            tradeData.put("sellerId", userId);
            tradeData.put("sellerName", sellerName);  // 确保不为null
            tradeData.put("category", category);
            tradeData.put("status", "available");
            tradeData.put("createTime", Timestamp.now());  // 使用Firebase Timestamp
            
            // 不添加timestamp和id字段，保持与旧数据格式一致

            // 如果有图片，先上传图片
            if (selectedImageUri != null) {
                uploadImageAndCreateTrade(tradeData);
            } else {
                // 没有图片，直接创建商品
                createTradeInFirestore(tradeData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding trade", e);
            Toast.makeText(this, "添加商品失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            loadingOverlay.setVisibility(View.GONE);
            submitButton.setEnabled(true);
        }
    }

    private void uploadImageAndCreateTrade(Map<String, Object> tradeData) {
        try {
            String imageFileName = "trades/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = storage.getReference().child(imageFileName);

            UploadTask uploadTask = storageRef.putFile(selectedImageUri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // 获取图片下载地址
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // 添加图片地址到商品数据
                    tradeData.put("imageUrl", uri.toString());
                    // 创建商品
                    createTradeInFirestore(tradeData);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting download URL", e);
                    Toast.makeText(AddTradeActivity.this, "获取图片URL失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadingOverlay.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error uploading image", e);
                Toast.makeText(this, "上传图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                loadingOverlay.setVisibility(View.GONE);
                submitButton.setEnabled(true);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error uploading image", e);
            Toast.makeText(this, "上传图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            loadingOverlay.setVisibility(View.GONE);
            submitButton.setEnabled(true);
        }
    }

    private void createTradeInFirestore(Map<String, Object> tradeData) {
        try {
            db.collection("trades")
                .add(tradeData)
                .addOnSuccessListener(documentReference -> {
                    String tradeId = documentReference.getId();
                    Log.d(TAG, "Trade added with ID: " + tradeId);
                    
                    // 不要在添加后再更新id字段，保持与旧数据格式一致
                    Toast.makeText(AddTradeActivity.this, "商品发布成功", Toast.LENGTH_SHORT).show();
                    
                    // 返回结果并关闭页面
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("tradeId", tradeId);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding trade", e);
                    Toast.makeText(AddTradeActivity.this, "发布商品失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadingOverlay.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                });
        } catch (Exception e) {
            Log.e(TAG, "Error creating trade in Firestore", e);
            Toast.makeText(this, "创建商品失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            loadingOverlay.setVisibility(View.GONE);
            submitButton.setEnabled(true);
        }
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