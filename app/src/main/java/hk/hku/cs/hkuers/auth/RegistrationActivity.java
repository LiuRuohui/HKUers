package hk.hku.cs.hkuers.auth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import hk.hku.cs.hkuers.R;

public class RegistrationActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (!isValidHKUEmail(email)) {
                    Toast.makeText(RegistrationActivity.this, "Must use @connect.hku.hk email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isValidPassword(password)) {
                    Toast.makeText(RegistrationActivity.this, "Password should contain at least 8 digits, including numbers and letters", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 创建用户
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // 发送验证邮件
                                mAuth.getCurrentUser().sendEmailVerification();
                                // 存储用户到Firestore
                                saveUserToFirestore(email);
                                Toast.makeText(RegistrationActivity.this, "Registration successful! Please check verify your email", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                            } else {
                                Toast.makeText(RegistrationActivity.this, "Registration failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    // 验证HKU邮箱格式
    private boolean isValidHKUEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@connect\\.hku\\.hk$";
        return Pattern.compile(regex).matcher(email).matches();
    }

    // 验证密码格式
    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$";
        return Pattern.compile(regex).matcher(password).matches();
    }

    // 存储用户到Firestore
    private void saveUserToFirestore(String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = mAuth.getCurrentUser().getUid();
        
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("email", email);
        user.put("uname", email.substring(0, email.indexOf('@'))); // 默认用户名为邮箱前缀
        user.put("avatar_url", "default"); // 使用默认头像
        user.put("courses", new ArrayList<String>());
        user.put("created_at", System.currentTimeMillis()); // 添加创建时间戳

        db.collection("users").document(uid)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(RegistrationActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // 跳转到登录页面
    public void navigateToLogin(View view) {
        startActivity(new Intent(this, LoginActivity.class));
    }
}