package hk.hku.cs.hkuers.features.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import hk.hku.cs.hkuers.R;

public class UserProfileActivity extends AppCompatActivity {
    
    private static final String TAG = "UserProfileActivity";
    private static final String SERVER_URL = "http://10.0.2.2:9000"; // 修改为与ChatRoomActivity一致
    
    // Firebase
    private FirebaseFirestore db;
    
    // UI组件
    private ImageButton btnBack;
    private ImageView ivAvatar;
    private TextView tvUsername;
    private TextView tvEmail;
    private TextView tvDepartment;
    private TextView tvProgramme;
    private TextView tvYearOfEntry;
    private TextView tvSignature;
    
    // 数据
    private String userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        
        // 设置系统状态栏颜色为固定的深色（与顶部栏一致）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#1E1E1E"));
        }
        
        // 设置状态栏空间高度
        View statusBarSpace = findViewById(R.id.statusBarSpace);
        if (statusBarSpace != null) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                int statusBarHeight = getResources().getDimensionPixelSize(resourceId);
                android.view.ViewGroup.LayoutParams params = statusBarSpace.getLayoutParams();
                params.height = statusBarHeight;
                statusBarSpace.setLayoutParams(params);
            }
        }
        
        // 初始化Firebase
        db = FirebaseFirestore.getInstance();
        
        // 获取传递的用户ID
        userId = getIntent().getStringExtra("user_id");
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "无效的用户ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化UI组件
        initViews();
        
        // 加载用户信息
        loadUserProfile();
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvProgramme = findViewById(R.id.tvProgramme);
        tvYearOfEntry = findViewById(R.id.tvYearOfEntry);
        tvSignature = findViewById(R.id.tvSignature);
        
        // 设置返回按钮
        btnBack.setOnClickListener(v -> finish());
    }
    
    private void loadUserProfile() {
        // 尝试使用Intent获取数据
        Intent intent = getIntent();
        boolean hasAllData = false;
        
        // 检查是否有直接传递的用户数据
        if (intent.hasExtra("user_name") && intent.hasExtra("user_email")) {
            // 使用Intent中的数据
            String username = intent.getStringExtra("user_name");
            String email = intent.getStringExtra("user_email");
            String department = intent.getStringExtra("user_department");
            String programme = intent.getStringExtra("user_programme");
            String yearOfEntry = intent.getStringExtra("user_year_of_entry");
            String signature = intent.getStringExtra("user_signature");
            String avatarUrl = intent.getStringExtra("user_avatar_url");
            
            // 添加调试日志，查看接收到的avatarUrl
            android.util.Log.d(TAG, "从Intent接收到avatarUrl: " + avatarUrl);
            
            // 更新UI
            updateUIWithIntentData(username, email, department, programme, yearOfEntry, signature, avatarUrl);
            hasAllData = true;
        }
        
        // 如果没有完整的数据，则从Firestore加载
        if (!hasAllData) {
            android.util.Log.d(TAG, "从Firestore加载用户ID: " + userId);
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(this::updateUI)
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "加载用户信息失败", Toast.LENGTH_SHORT).show();
                        android.util.Log.e(TAG, "加载用户信息失败: " + e.getMessage(), e);
                        finish();
                    });
        }
    }
    
    private void updateUI(DocumentSnapshot document) {
        if (document.exists()) {
            // 设置用户名
            String username = document.getString("uname");
            tvUsername.setText(username != null ? username : "未知用户");
            
            // 设置邮箱
            String email = document.getString("email");
            if (email != null && !email.isEmpty()) {
                tvEmail.setText(email);
                tvEmail.setVisibility(View.VISIBLE);
            } else {
                tvEmail.setVisibility(View.GONE);
            }
            
            // 设置院系
            String department = document.getString("department");
            if (department != null && !department.isEmpty()) {
                tvDepartment.setText(department);
                tvDepartment.setVisibility(View.VISIBLE);
            } else {
                tvDepartment.setVisibility(View.GONE);
            }
            
            // 设置专业
            String programme = document.getString("programme");
            if (programme != null && !programme.isEmpty()) {
                tvProgramme.setText(programme);
                tvProgramme.setVisibility(View.VISIBLE);
            } else {
                tvProgramme.setVisibility(View.GONE);
            }
            
            // 设置入学年份
            String yearOfEntry = document.getString("year_of_entry");
            if (yearOfEntry != null && !yearOfEntry.isEmpty()) {
                tvYearOfEntry.setText("入学年份: " + yearOfEntry);
                tvYearOfEntry.setVisibility(View.VISIBLE);
            } else {
                tvYearOfEntry.setVisibility(View.GONE);
            }
            
            // 设置个性签名
            String signature = document.getString("signature");
            if (signature != null && !signature.isEmpty()) {
                tvSignature.setText("\"" + signature + "\"");
                tvSignature.setVisibility(View.VISIBLE);
            } else {
                tvSignature.setVisibility(View.GONE);
            }
            
            // 设置头像
            String avatarUrl = document.getString("avatar_url");
            android.util.Log.d(TAG, "从Firestore获取的avatarUrl: " + avatarUrl);
            
            if (avatarUrl != null && !avatarUrl.isEmpty() && !"default".equals(avatarUrl)) {
                // 确保头像URL正确（添加avatar/前缀如果没有）
                if (!avatarUrl.startsWith("avatar/")) {
                    avatarUrl = "avatar/" + avatarUrl;
                    android.util.Log.d(TAG, "添加前缀后的avatarUrl: " + avatarUrl);
                }
                
                // 构建完整的URL
                String imageUrl = SERVER_URL + "/image/" + avatarUrl;
                // 添加随机参数避免缓存问题
                String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
                imageUrl = imageUrl + "?nocache=" + uniqueParam;
                
                android.util.Log.d(TAG, "加载用户头像URL: " + imageUrl);
                
                final String finalImageUrl = imageUrl;
                // 确保Glide在主线程中加载
                runOnUiThread(() -> {
                    try {
                        Glide.with(UserProfileActivity.this)
                                .load(finalImageUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                        android.util.Log.e(TAG, "头像加载失败: " + e.getMessage(), e);
                                        return false;
                                    }
                                    
                                    @Override
                                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                        android.util.Log.d(TAG, "头像加载成功");
                                        return false;
                                    }
                                })
                                .centerCrop()
                                .into(ivAvatar);
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Glide加载头像异常: " + e.getMessage(), e);
                        ivAvatar.setImageResource(R.drawable.default_avatar);
                    }
                });
            } else {
                android.util.Log.d(TAG, "使用默认头像，avatarUrl为空或default");
                ivAvatar.setImageResource(R.drawable.default_avatar);
            }
        } else {
            Toast.makeText(this, "用户不存在", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void updateUIWithIntentData(String username, String email, String department, 
                                        String programme, String yearOfEntry, String signature, String avatarUrl) {
        // 设置用户名
        tvUsername.setText(username != null ? username : "未知用户");
        
        // 设置邮箱
        if (email != null && !email.isEmpty()) {
            tvEmail.setText(email);
            tvEmail.setVisibility(View.VISIBLE);
        } else {
            tvEmail.setVisibility(View.GONE);
        }
        
        // 设置院系
        if (department != null && !department.isEmpty()) {
            tvDepartment.setText(department);
            tvDepartment.setVisibility(View.VISIBLE);
        } else {
            tvDepartment.setVisibility(View.GONE);
        }
        
        // 设置专业
        if (programme != null && !programme.isEmpty()) {
            tvProgramme.setText(programme);
            tvProgramme.setVisibility(View.VISIBLE);
        } else {
            tvProgramme.setVisibility(View.GONE);
        }
        
        // 设置入学年份
        if (yearOfEntry != null && !yearOfEntry.isEmpty()) {
            tvYearOfEntry.setText("入学年份: " + yearOfEntry);
            tvYearOfEntry.setVisibility(View.VISIBLE);
        } else {
            tvYearOfEntry.setVisibility(View.GONE);
        }
        
        // 设置个性签名
        if (signature != null && !signature.isEmpty()) {
            tvSignature.setText("\"" + signature + "\"");
            tvSignature.setVisibility(View.VISIBLE);
        } else {
            tvSignature.setVisibility(View.GONE);
        }
        
        // 设置头像
        if (avatarUrl != null && !avatarUrl.isEmpty() && !"default".equals(avatarUrl)) {
            android.util.Log.d(TAG, "原始avatarUrl: " + avatarUrl);
            
            // 确保头像URL正确（添加avatar/前缀如果没有）
            if (!avatarUrl.startsWith("avatar/")) {
                avatarUrl = "avatar/" + avatarUrl;
                android.util.Log.d(TAG, "添加前缀后的avatarUrl: " + avatarUrl);
            }
            
            // 构建完整的URL
            String imageUrl = SERVER_URL + "/image/" + avatarUrl;
            // 添加随机参数避免缓存问题
            String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
            imageUrl = imageUrl + "?nocache=" + uniqueParam;
            
            android.util.Log.d(TAG, "从Intent加载用户头像URL: " + imageUrl);
            
            final String finalImageUrl = imageUrl;
            // 确保Glide在主线程中加载
            runOnUiThread(() -> {
                try {
                    Glide.with(UserProfileActivity.this)
                            .load(finalImageUrl)
                            .placeholder(R.drawable.default_avatar)
                            .error(R.drawable.default_avatar)
                            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                                @Override
                                public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                    android.util.Log.e(TAG, "Intent传递的头像加载失败: " + e.getMessage(), e);
                                    return false;
                                }
                                
                                @Override
                                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                    android.util.Log.d(TAG, "Intent传递的头像加载成功");
                                    return false;
                                }
                            })
                            .centerCrop()
                            .into(ivAvatar);
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Glide加载Intent传递的头像异常: " + e.getMessage(), e);
                    ivAvatar.setImageResource(R.drawable.default_avatar);
                }
            });
        } else {
            android.util.Log.d(TAG, "使用默认头像，avatarUrl为空或default");
            ivAvatar.setImageResource(R.drawable.default_avatar);
        }
    }
} 