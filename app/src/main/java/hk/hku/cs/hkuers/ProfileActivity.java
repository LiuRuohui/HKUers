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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
    private static final String SERVER_URL = "http://10.0.2.2:5000"; // Android模拟器访问本地服务器
    private static final int REQUEST_STORAGE_PERMISSION = 100;

    private EditText etName, etEmail;
    private Button btnSave, btnChangeAvatar;
    private ImageView ivAvatar;
    private FirebaseFirestore db;
    private String userId;
    private Uri selectedImageUri;
    private OkHttpClient client;

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
                    Toast.makeText(this, "需要图片访问权限来选择头像", Toast.LENGTH_SHORT).show();
                }
            });

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        btnSave = findViewById(R.id.btnSave);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        ivAvatar = findViewById(R.id.ivAvatar);
        
        db = FirebaseFirestore.getInstance();
        client = new OkHttpClient();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 加载当前用户信息
        loadUserProfile();

        btnSave.setOnClickListener(v -> saveProfile());
        
        btnChangeAvatar.setOnClickListener(v -> checkAndRequestPermission());
    }

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == 
                    PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 12及以下
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == 
                    PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
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
                        String name = documentSnapshot.getString("uname");
                        String email = documentSnapshot.getString("email");
                        String avatarUrl = documentSnapshot.getString("avatar_url");
                        
                        etName.setText(name);
                        etEmail.setText(email);
                        
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            // 使用Glide加载头像
                            Glide.with(this)
                                    .load(SERVER_URL + "/image/" + avatarUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .error(R.drawable.default_avatar)
                                    .into(ivAvatar);
                        }
                    }
                });
    }

    private void saveProfile() {
        String newName = etName.getText().toString().trim();
        
        // 如果有选择新头像，先上传头像
        if (selectedImageUri != null) {
            uploadAvatar(newName);
        } else {
            // 只更新名称
            updateUserProfile(newName, null);
        }
    }
    
    private void uploadAvatar(String newName) {
        try {
            // 获取图片并转换
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();
            
            // 构建multipart请求体
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "avatar.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageData))
                    .build();
                    
            Request request = new Request.Builder()
                    .url(SERVER_URL + "/upload")
                    .post(requestBody)
                    .build();
                    
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> 
                        Toast.makeText(ProfileActivity.this, 
                                "上传失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        // 解析返回的JSON获取文件名
                        String filename = parseFilename(responseData);
                        // 更新用户资料
                        runOnUiThread(() -> updateUserProfile(newName, filename));
                    } else {
                        runOnUiThread(() -> 
                            Toast.makeText(ProfileActivity.this, 
                                    "上传失败，服务器返回错误", Toast.LENGTH_SHORT).show());
                    }
                }
            });
            
        } catch (IOException e) {
            Toast.makeText(this, "图片处理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private String parseFilename(String jsonResponse) {
        // 简单解析，实际项目中应使用JSON解析库
        if (jsonResponse.contains("\"filename\":")) {
            int start = jsonResponse.indexOf("\"filename\":") + "\"filename\":".length();
            int end = jsonResponse.indexOf(",", start);
            if (end == -1) end = jsonResponse.indexOf("}", start);
            String filename = jsonResponse.substring(start, end).trim();
            // 移除可能的引号
            return filename.replace("\"", "");
        }
        return null;
    }
    
    private void updateUserProfile(String newName, String avatarFilename) {
        // 构建更新数据
        if (avatarFilename != null) {
            // 更新头像和名称
            db.collection("users").document(userId)
                    .update(
                            "uname", newName,
                            "avatar_url", avatarFilename
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "资料更新成功", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "保存失败: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show());
        } else {
            // 仅更新名称
            db.collection("users").document(userId)
                    .update("uname", newName)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "资料更新成功", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "保存失败: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show());
        }
    }
}