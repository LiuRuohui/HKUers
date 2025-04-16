package hk.hku.cs.hkuers.features.profile;

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
    
    // Firebase
    private FirebaseFirestore db;
    
    // UI组件
    private ImageButton btnBack;
    private ImageView ivAvatar;
    private TextView tvUsername;
    private TextView tvEmail;
    private TextView tvDepartment;
    
    // 数据
    private String userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        
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
        
        // 设置返回按钮
        btnBack.setOnClickListener(v -> finish());
    }
    
    private void loadUserProfile() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(this::updateUI)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "加载用户信息失败", Toast.LENGTH_SHORT).show();
                    finish();
                });
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
            
            // 设置头像
            String avatarUrl = document.getString("avatar");
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .centerCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.default_avatar);
            }
        } else {
            Toast.makeText(this, "用户不存在", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
} 