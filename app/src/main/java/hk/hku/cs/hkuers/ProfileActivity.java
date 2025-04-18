package hk.hku.cs.hkuers;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    // 常量定义
    private static final String TAG = "ProfileActivity";
    private static final String SERVER_URL = "http://10.0.2.2:5000";
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");
    private static final int MAX_AVATAR_LOAD_RETRIES = 3;

    // UI组件
    private EditText etDepartment, etProgramme, etYearOfEntry, etName, etSignature, etEmail;
    private ImageView ivAvatar, ivEditAvatar;
    private Button btnEdit, btnSave, btnCancel;
    private ImageButton btnBack;
    private ProgressBar pbAvatarLoading;
    
    // 数据和状态
    private FirebaseFirestore db;
    private String userId;
    private Uri selectedImageUri;
    private OkHttpClient client;
    private DocumentSnapshot originalUserData;
    private boolean isEditMode = false;
    private int avatarLoadRetryCount = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Activity结果处理器
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            ivAvatar.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            Log.e(TAG, "Error loading selected image", e);
                            Toast.makeText(this, "Failed to load selected image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openImagePicker();
                } else {
                    Toast.makeText(this, "Permission needed to select an avatar", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupListeners();
        initNetwork();
        
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadUserProfile();
    }

    /**
     * 初始化界面元素
     */
    private void initViews() {
        // 顶部栏
        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        
        // 头像
        ivAvatar = findViewById(R.id.ivAvatar);
        ivEditAvatar = findViewById(R.id.ivEditAvatar);
        
        // 添加加载进度条
        FrameLayout avatarContainer = (FrameLayout) ivAvatar.getParent();
        pbAvatarLoading = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.CENTER;
        avatarContainer.addView(pbAvatarLoading, params);
        pbAvatarLoading.setVisibility(View.GONE);
        
        // 个人信息字段
        etEmail = findViewById(R.id.etEmail);
        etDepartment = findViewById(R.id.etDepartment);
        etProgramme = findViewById(R.id.etProgramme);
        etYearOfEntry = findViewById(R.id.etYearOfEntry);
        etName = findViewById(R.id.etName);
        etSignature = findViewById(R.id.etSignature);
    }

    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnEdit.setOnClickListener(v -> setEditMode(true));
        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> cancelEdit());
        
        // 头像点击监听
        View.OnClickListener avatarClickListener = v -> {
            if (isEditMode) {
                checkAndRequestPermission();
            }
        };
        ivEditAvatar.setOnClickListener(avatarClickListener);
        ivAvatar.setOnClickListener(avatarClickListener);
    }
    
    /**
     * 初始化网络客户端
     */
    private void initNetwork() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 切换编辑模式
     */
    private void setEditMode(boolean editMode) {
        isEditMode = editMode;
        
        // 修改按钮可见性
        btnEdit.setVisibility(editMode ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(editMode ? View.VISIBLE : View.GONE);
        btnCancel.setVisibility(editMode ? View.VISIBLE : View.GONE);
        ivEditAvatar.setVisibility(editMode ? View.VISIBLE : View.GONE);
        
        // 更改输入框状态
        setFieldsEnabled(editMode);
        
        // Email始终不可编辑，但在编辑模式下改变文本颜色为橙色以示区分
        int emailTextColor = editMode ? 
                getColorResource(android.R.color.holo_orange_light) : 
                getColorResource(android.R.color.white);
        etEmail.setTextColor(emailTextColor);
    }
    
    /**
     * 获取颜色资源，兼容不同Android版本
     */
    private int getColorResource(int colorId) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                getResources().getColor(colorId, null) : 
                getResources().getColor(colorId);
    }
    
    /**
     * 设置输入字段是否可编辑
     */
    private void setFieldsEnabled(boolean enabled) {
        etDepartment.setEnabled(enabled);
        etProgramme.setEnabled(enabled);
        etYearOfEntry.setEnabled(enabled);
        etName.setEnabled(enabled);
        etSignature.setEnabled(enabled);
        etEmail.setEnabled(false); // 邮箱始终不可编辑
    }

    /**
     * 取消编辑，恢复原始数据
     */
    private void cancelEdit() {
        if (originalUserData != null) {
            updateUIWithUserData(originalUserData);
        }
        selectedImageUri = null;
        setEditMode(false);
    }

    /**
     * 检查并请求存储权限
     */
    private void checkAndRequestPermission() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : 
                Manifest.permission.READ_EXTERNAL_STORAGE;
                
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    /**
     * 打开图片选择器
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    //-----------------------------------
    // 数据加载和显示
    //-----------------------------------

    /**
     * 从Firebase加载用户资料
     */
    private void loadUserProfile() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        originalUserData = documentSnapshot;
                        updateUIWithUserData(documentSnapshot);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Loading profile failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load user profile", e);
                });
    }
    
    /**
     * 使用文档数据更新UI
     */
    private void updateUIWithUserData(DocumentSnapshot documentSnapshot) {
        // 填充UI字段
        populateTextField(etName, documentSnapshot.getString("uname"));
        populateTextField(etEmail, documentSnapshot.getString("email"));
        populateTextField(etDepartment, documentSnapshot.getString("department"));
        populateTextField(etProgramme, documentSnapshot.getString("programme"));
        populateTextField(etYearOfEntry, documentSnapshot.getString("year_of_entry"));
        populateTextField(etSignature, documentSnapshot.getString("signature"));
        
        // 加载头像，重置重试计数
        avatarLoadRetryCount = 0;
        loadAvatar(documentSnapshot.getString("avatar_url"));
    }
    
    /**
     * 加载头像图片
     */
    private void loadAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            ivAvatar.setImageResource(R.drawable.default_avatar);
            return;
        }
        
        // 添加时间戳和随机参数，确保不使用缓存
        String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
        String imageUrl = SERVER_URL + "/image/" + avatarUrl + "?nocache=" + uniqueParam;
        
        Log.d(TAG, "Loading avatar from: " + imageUrl);
        
        // 显示加载进度条
        pbAvatarLoading.setVisibility(View.VISIBLE);
        
        // 使用OkHttp直接下载图片
        Request request = new Request.Builder()
                .url(imageUrl)
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Avatar download failed", e);
                retryOrShowDefault();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (InputStream inputStream = response.body().byteStream()) {
                        // 将InputStream转换为Bitmap
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        
                        if (bitmap != null) {
                            // 在主线程设置图片
                            mainHandler.post(() -> {
                                ivAvatar.setImageBitmap(bitmap);
                                pbAvatarLoading.setVisibility(View.GONE);
                                avatarLoadRetryCount = 0;
                            });
                        } else {
                            Log.e(TAG, "Failed to decode bitmap from stream");
                            retryOrShowDefault();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing avatar image", e);
                        retryOrShowDefault();
                    }
                } else {
                    Log.e(TAG, "Server returned error: " + response.code());
                    retryOrShowDefault();
                }
            }
            
            private void retryOrShowDefault() {
                mainHandler.post(() -> {
                    // 检查是否需要重试
                    if (avatarLoadRetryCount < MAX_AVATAR_LOAD_RETRIES) {
                        avatarLoadRetryCount++;
                        Log.d(TAG, "Retrying avatar load, attempt " + avatarLoadRetryCount);
                        // 稍后重试
                        mainHandler.postDelayed(() -> loadAvatar(avatarUrl), 500);
                    } else {
                        // 达到最大重试次数，显示默认头像
                        Log.e(TAG, "Failed to load avatar after " + MAX_AVATAR_LOAD_RETRIES + " attempts");
                        ivAvatar.setImageResource(R.drawable.default_avatar);
                        pbAvatarLoading.setVisibility(View.GONE);
                    }
                });
            }
        });
    }
    
    /**
     * 填充文本字段的辅助方法
     */
    private void populateTextField(EditText field, String value) {
        field.setText(value != null ? value : "");
    }

    //-----------------------------------
    // 保存和上传功能
    //-----------------------------------

    /**
     * 保存个人资料
     */
    private void saveProfile() {
        String newName = etName.getText().toString().trim();
        
        // 验证必填字段
        if (newName.isEmpty()) {
            Toast.makeText(this, "Nickname cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 如果有选择新头像，先上传头像，否则直接更新资料
        if (selectedImageUri != null) {
            uploadAvatarAndUpdateProfile();
        } else {
            updateUserProfile(null);
        }
    }
    
    /**
     * 上传头像并更新个人资料
     */
    private void uploadAvatarAndUpdateProfile() {
        try {
            // 转换图片为字节数组
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();
            
            // 使用用户ID作为文件名前缀，确保唯一性
            String avatarFilename = "avatar_" + userId + ".jpg";
            
            // 构建请求体
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", avatarFilename,
                            RequestBody.create(MEDIA_TYPE_JPEG, imageData))
                    .addFormDataPart("type", "avatar")
                    .build();
                    
            Request request = new Request.Builder()
                    .url(SERVER_URL + "/upload")
                    .post(requestBody)
                    .build();
                    
            Toast.makeText(this, "Uploading avatar...", Toast.LENGTH_SHORT).show();
            
            // 发送请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    handleNetworkError("Upload failed", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        String filename = parseFilename(responseData);
                        
                        if (filename != null) {
                            Log.d(TAG, "Avatar uploaded successfully: " + filename);
                            runOnUiThread(() -> updateUserProfile(filename));
                        } else {
                            handleNetworkError("Failed to get filename from server", null);
                        }
                    } else {
                        handleNetworkError("Upload failed, server error: " + response.code(), null);
                    }
                }
            });
            
        } catch (IOException e) {
            Toast.makeText(this, "Image processing failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            setEditMode(false);
        }
    }
    
    /**
     * 处理网络错误
     */
    private void handleNetworkError(String message, Exception e) {
        runOnUiThread(() -> {
            String errorMsg = e != null ? message + ": " + e.getMessage() : message;
            Toast.makeText(ProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            Log.e(TAG, errorMsg, e);
            setEditMode(false);
        });
    }
    
    /**
     * 从JSON响应中解析文件名
     */
    private String parseFilename(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            return json.optString("filename", null);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing filename from JSON: " + e.getMessage());
            
            // 回退到简单的字符串解析方法
            if (jsonResponse.contains("\"filename\":")) {
                int start = jsonResponse.indexOf("\"filename\":") + "\"filename\":".length();
                int end = jsonResponse.indexOf(",", start);
                if (end == -1) end = jsonResponse.indexOf("}", start);
                String filename = jsonResponse.substring(start, end).trim();
                return filename.replace("\"", "");
            }
            return null;
        }
    }
    
    /**
     * 更新用户个人资料
     */
    private void updateUserProfile(String avatarFilename) {
        // 收集表单数据
        Map<String, Object> updates = new HashMap<>();
        updates.put("uname", etName.getText().toString().trim());
        updates.put("department", etDepartment.getText().toString().trim());
        updates.put("programme", etProgramme.getText().toString().trim());
        updates.put("year_of_entry", etYearOfEntry.getText().toString().trim());
        updates.put("signature", etSignature.getText().toString().trim());
        
        // 如果有新头像，也更新头像URL
        if (avatarFilename != null) {
            updates.put("avatar_url", avatarFilename);
        }
        
        // 更新到Firestore
        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    setEditMode(false);
                    
                    // 如果更新了头像，立即加载新头像展示
                    if (avatarFilename != null) {
                        // 短暂延迟确保服务器处理完成
                        mainHandler.postDelayed(() -> loadAvatar(avatarFilename), 500);
                    }
                    
                    // 重新加载用户数据以更新originalUserData，方便后续编辑取消操作
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to update user profile", e);
                });
    }
}