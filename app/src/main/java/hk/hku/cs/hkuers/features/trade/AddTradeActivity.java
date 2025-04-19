package hk.hku.cs.hkuers.features.trade;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
    
    // 添加图片选择的ActivityResultLauncher
    private ActivityResultLauncher<String> imagePickerLauncher;
    
    // 添加权限请求的ActivityResultLauncher
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_add_trade);

            db = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();
            auth = FirebaseAuth.getInstance();
            
            // 初始化图片选择器和权限请求
            setupImagePicker();
            setupPermissionLauncher();

            initViews();
            setupToolbar();
            setupCategoryDropdown();
            
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
        
        // 添加图片卡片点击事件
        imageCardView.setOnClickListener(v -> {
            try {
                // 显示图片选择对话框
                showSimpleImageSelectionDialog();
            } catch (Exception e) {
                Log.e(TAG, "显示图片选择对话框失败", e);
                Toast.makeText(AddTradeActivity.this, "无法显示图片选择对话框: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 设置图片选择器
     */
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        // 记录原始URI
                        Log.d(TAG, "Original image URI: " + uri);
                        
                        // 将图片复制到应用私有目录
                        File privateDir = new File(getFilesDir(), "product_images");
                        if (!privateDir.exists()) {
                            privateDir.mkdirs();
                        }
                        
                        // 创建目标文件，使用时间戳确保唯一
                        String fileName = "img_" + System.currentTimeMillis() + ".jpg";
                        File destinationFile = new File(privateDir, fileName);
                        
                        // 复制文件内容
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        FileOutputStream outputStream = new FileOutputStream(destinationFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        outputStream.close();
                        inputStream.close();
                        
                        // 使用私有文件URI
                        Uri privateUri = Uri.fromFile(destinationFile);
                        Log.d(TAG, "Copied to private URI: " + privateUri);
                        selectedImageUri = privateUri;
                        
                        // 显示图片
                        productImageView.setImageURI(selectedImageUri);
                        productImageView.setVisibility(View.VISIBLE);
                        uploadImageLayout.setVisibility(View.GONE);
                        
                        Toast.makeText(this, "图片已选择", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error copying image to private storage: " + e.getMessage(), e);
                        // 如果复制失败，回退到使用原始URI
                        selectedImageUri = uri;
                        productImageView.setImageURI(uri);
                        productImageView.setVisibility(View.VISIBLE);
                        uploadImageLayout.setVisibility(View.GONE);
                        Toast.makeText(this, "图片已选择（原始方式）", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }
    
    /**
     * 设置权限请求处理
     */
    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // 如果权限被授予，打开图片选择器
                    openImagePicker();
                } else {
                    Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    /**
     * 检查和请求图片访问权限
     */
    private void checkAndRequestPermission() {
        // Android 13+ (API 33+) 使用 READ_MEDIA_IMAGES
        // 低版本使用 READ_EXTERNAL_STORAGE
        String permission;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            // 已有权限，直接打开图片选择器
            openImagePicker();
        } else {
            // 请求权限
            requestPermissionLauncher.launch(permission);
        }
    }
    
    /**
     * 打开图片选择器
     */
    private void openImagePicker() {
        try {
            imagePickerLauncher.launch("image/*");
        } catch (Exception e) {
            Log.e(TAG, "Error launching image picker", e);
            Toast.makeText(this, "无法打开图片选择器: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 设置默认本地图片 (保留作为备选方案)
     */
    private void setDefaultLocalImage() {
        try {
            // 设置本地资源图片
            productImageView.setImageResource(R.drawable.mac);
            productImageView.setVisibility(View.VISIBLE);
            uploadImageLayout.setVisibility(View.GONE);
            
            // 记录已选择本地图片的URI
            selectedImageUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.mac);
            Log.d(TAG, "Local image resource set: " + selectedImageUri);
        } catch (Exception e) {
            Log.e(TAG, "Error setting local image", e);
        }
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
            
            // 检查是否选择了图片
            if (selectedImageUri == null) {
                // 提示用户选择图片
                showImageSelectionDialog(title, description, price, category);
                return;
            }
            
            // 如果已经选择了图片，继续发布商品流程
            continueAddTrade(title, description, price, category);
            
        } catch (Exception e) {
            Log.e(TAG, "添加商品失败", e);
            Toast.makeText(this, "添加商品失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            loadingOverlay.setVisibility(View.GONE);
            submitButton.setEnabled(true);
        }
    }
    
    /**
     * 显示图片选择对话框
     */
    private void showImageSelectionDialog(String title, String description, double price, String category) {
        String[] options = new String[]{"从设备相册选择", "使用应用内图片", "使用默认图片 (mac.jpeg)"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择图片来源")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // 从设备相册选择
                            Toast.makeText(this, "图片将直接使用本地URI，不会上传到服务器", Toast.LENGTH_SHORT).show();
                            checkAndRequestPermission();
                            break;
                        case 1:
                            // 使用应用内图片
                            showDrawableResourcesDialog(title, description, price, category);
                            break;
                        case 2:
                            // 使用默认图片
                            setDefaultLocalImage();
                            continueAddTrade(title, description, price, category);
                            break;
                    }
                })
                .setCancelable(true)
                .show();
    }
    
    /**
     * 显示drawable资源选择对话框
     */
    private void showDrawableResourcesDialog(String title, String description, double price, String category) {
        // 可用的资源名称列表 (不包含扩展名)
        final String[] drawableResources = {"mac", "default_avatar", "ic_empty_list", "ic_person"};
        final int[] drawableResourceIds = {
                R.drawable.mac,
                R.drawable.default_avatar,
                R.drawable.ic_empty_list,
                R.drawable.ic_person
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择应用资源图片")
                .setItems(drawableResources, (dialog, which) -> {
                    // 设置选中的drawable资源
                    int resourceId = drawableResourceIds[which];
                    productImageView.setImageResource(resourceId);
                    productImageView.setVisibility(View.VISIBLE);
                    uploadImageLayout.setVisibility(View.GONE);
                    
                    // 记录已选择的资源URI
                    selectedImageUri = Uri.parse("android.resource://" + getPackageName() + "/" + resourceId);
                    Log.d(TAG, "Selected drawable resource: " + drawableResources[which] + ", URI: " + selectedImageUri);
                    Toast.makeText(this, "已选择图片: " + drawableResources[which], Toast.LENGTH_SHORT).show();
                    
                    // 继续添加商品流程
                    continueAddTrade(title, description, price, category);
                })
                .setCancelable(true)
                .show();
    }
    
    /**
     * 继续添加商品的流程
     */
    private void continueAddTrade(String title, String description, double price, String category) {
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

        // 准备要上传的数据
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
        
        // 上传图片并创建商品
        uploadImageAndCreateTrade(tradeData);
    }

    private void uploadImageAndCreateTrade(Map<String, Object> tradeData) {
        try {
            if (selectedImageUri != null) {
                // 检查是否是应用资源URI
                String scheme = selectedImageUri.getScheme();
                if (scheme != null && scheme.equals("android.resource")) {
                    // 对于应用资源，直接使用URI
                    String imageUrl = selectedImageUri.toString();
                    tradeData.put("imageUrl", imageUrl);
                    Log.d(TAG, "使用应用资源图片URI: " + imageUrl);
                    createTradeInFirestore(tradeData);
                } else if (scheme != null && scheme.equals("content")) {
                    // 对于content类型的URI（从相册选择的图片），也直接使用
                    String imageUrl = selectedImageUri.toString();
                    tradeData.put("imageUrl", imageUrl);
                    Log.d(TAG, "使用设备相册图片URI: " + imageUrl);
                    createTradeInFirestore(tradeData);
                } else {
                    // 为其他类型的URI，如有需要可以添加额外处理
                    String imageUrl = selectedImageUri.toString();
                    tradeData.put("imageUrl", imageUrl);
                    Log.d(TAG, "使用其他格式图片URI: " + imageUrl);
                    createTradeInFirestore(tradeData);
                }
            } else {
                // 如果没有选择图片，设置为空字符串
                tradeData.put("imageUrl", "");
                Log.d(TAG, "没有选择图片，使用空图片URL");
                createTradeInFirestore(tradeData);
            }
        } catch (Exception e) {
            Log.e(TAG, "处理图片时出错", e);
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    resultIntent.putExtra("refreshList", true);
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

    /**
     * 显示简单的图片选择对话框（不带商品信息参数）
     */
    private void showSimpleImageSelectionDialog() {
        String[] options = new String[]{"从设备相册选择", "使用应用内图片", "使用默认图片 (mac.jpeg)"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择图片来源")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // 从设备相册选择
                            Toast.makeText(this, "选择图片", Toast.LENGTH_SHORT).show();
                            checkAndRequestPermission();
                            break;
                        case 1:
                            // 使用应用内图片
                            showSimpleDrawableResourcesDialog();
                            break;
                        case 2:
                            // 使用默认图片
                            setDefaultLocalImage();
                            break;
                    }
                })
                .setCancelable(true)
                .show();
    }
    
    /**
     * 显示简单的drawable资源选择对话框（不带商品信息参数）
     */
    private void showSimpleDrawableResourcesDialog() {
        // 可用的资源名称列表 (不包含扩展名)
        final String[] drawableResources = {"mac", "default_avatar", "ic_empty_list", "ic_person"};
        final int[] drawableResourceIds = {
                R.drawable.mac,
                R.drawable.default_avatar,
                R.drawable.ic_empty_list,
                R.drawable.ic_person
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择应用资源图片")
                .setItems(drawableResources, (dialog, which) -> {
                    // 设置选中的drawable资源
                    int resourceId = drawableResourceIds[which];
                    productImageView.setImageResource(resourceId);
                    productImageView.setVisibility(View.VISIBLE);
                    uploadImageLayout.setVisibility(View.GONE);
                    
                    // 记录已选择的资源URI
                    selectedImageUri = Uri.parse("android.resource://" + getPackageName() + "/" + resourceId);
                    Log.d(TAG, "Selected drawable resource: " + drawableResources[which] + ", URI: " + selectedImageUri);
                    Toast.makeText(this, "已选择图片: " + drawableResources[which], Toast.LENGTH_SHORT).show();
                })
                .setCancelable(true)
                .show();
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