package hk.hku.cs.hkuers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final String SERVER_URL = "http://10.0.2.2:5000";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

    // UI组件
    private EditText etDepartment, etProgramme, etYearOfEntry, etName, etSignature, etEmail;
    private ImageView ivAvatar, ivEditAvatar;
    private Button btnEdit, btnSave, btnCancel;
    private ImageButton btnBack;

    // 数据
    private FirebaseFirestore db;
    private String userId;
    private Uri selectedImageUri;
    private OkHttpClient client;
    private DocumentSnapshot originalUserData;
    private boolean isEditMode = false;
    private String oldAvatarUrl;

    // Activity结果处理器
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        Glide.with(this).load(selectedImageUri).into(ivAvatar);
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
        
        db = FirebaseFirestore.getInstance();
        client = new OkHttpClient();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadUserProfile();
    }

    private void initViews() {
        // 顶部栏
        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        
        // 头像
        ivAvatar = findViewById(R.id.ivAvatar);
        ivEditAvatar = findViewById(R.id.ivEditAvatar);
        
        // 个人信息字段
        etEmail = findViewById(R.id.etEmail);
        etDepartment = findViewById(R.id.etDepartment);
        etProgramme = findViewById(R.id.etProgramme);
        etYearOfEntry = findViewById(R.id.etYearOfEntry);
        etName = findViewById(R.id.etName);
        etSignature = findViewById(R.id.etSignature);
    }

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
    
    private int getColorResource(int colorId) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                getResources().getColor(colorId, null) : 
                getResources().getColor(colorId);
    }
    
    private void setFieldsEnabled(boolean enabled) {
        etDepartment.setEnabled(enabled);
        etProgramme.setEnabled(enabled);
        etYearOfEntry.setEnabled(enabled);
        etName.setEnabled(enabled);
        etSignature.setEnabled(enabled);
        etEmail.setEnabled(false); // 邮箱始终不可编辑
    }

    private void cancelEdit() {
        if (originalUserData != null) {
            updateUIWithUserData(originalUserData);
        }
        selectedImageUri = null;
        setEditMode(false);
    }

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

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void loadUserProfile() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        originalUserData = documentSnapshot;
                        
                        // 检查是否有旧头像需要删除（如果头像URL发生了变化）
                        String newAvatarUrl = documentSnapshot.getString("avatar_url");
                        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty() && 
                            !Objects.equals(newAvatarUrl, oldAvatarUrl)) {
                            deleteOldAvatar(oldAvatarUrl);
                        }
                        
                        // 保存当前头像URL，以便下次更新时比较
                        oldAvatarUrl = newAvatarUrl;
                        
                        updateUIWithUserData(documentSnapshot);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Loading profile failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load user profile", e);
                });
    }
    
    private void updateUIWithUserData(DocumentSnapshot documentSnapshot) {
        // 填充UI字段
        populateTextField(etName, documentSnapshot.getString("uname"));
        populateTextField(etEmail, documentSnapshot.getString("email"));
        populateTextField(etDepartment, documentSnapshot.getString("department"));
        populateTextField(etProgramme, documentSnapshot.getString("programme"));
        populateTextField(etYearOfEntry, documentSnapshot.getString("year_of_entry"));
        populateTextField(etSignature, documentSnapshot.getString("signature"));
        
        // 加载头像
        String avatarUrl = documentSnapshot.getString("avatar_url");
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(SERVER_URL + "/image/" + avatarUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.default_avatar);
        }
    }
    
    private void populateTextField(EditText field, String value) {
        field.setText(value != null ? value : "");
    }

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
    
    private void uploadAvatarAndUpdateProfile() {
        try {
            // 转换图片为字节数组
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();
            
            // 构建请求体
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "avatar.jpg",
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
                        runOnUiThread(() -> updateUserProfile(filename));
                    } else {
                        handleNetworkError("Upload failed, server error", null);
                    }
                }
            });
            
        } catch (IOException e) {
            Toast.makeText(this, "Image processing failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            setEditMode(false);
        }
    }
    
    private void handleNetworkError(String message, Exception e) {
        runOnUiThread(() -> {
            String errorMsg = e != null ? message + ": " + e.getMessage() : message;
            Toast.makeText(ProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            setEditMode(false);
        });
    }
    
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
                    loadUserProfile(); // 重新加载用户数据
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to update user profile", e);
                });
    }
    
    private void deleteOldAvatar(String avatarUrl) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("filename", avatarUrl);
            
            RequestBody requestBody = RequestBody.create(JSON, jsonBody.toString());
            Request request = new Request.Builder()
                    .url(SERVER_URL + "/delete")
                    .post(requestBody)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to delete old avatar: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String status = response.isSuccessful() ? "success" : "failed with code " + response.code();
                    Log.d(TAG, "Old avatar deletion: " + status);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing delete request: " + e.getMessage());
        }
    }
}