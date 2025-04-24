package hk.hku.cs.hkuers.features.forum;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.models.Post;

public class NewForumBoardActivity extends AppCompatActivity {
    private static final String TAG = "NewForumBoardActivity";
    private EditText titleEditText;
    private EditText contentEditText;
    private String boardType;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_forum_post);

        // 获取版块类型
        boardType = getIntent().getStringExtra("board_type");
        if (boardType == null) {
            boardType = "discussion"; // 默认为讨论版块
        }

        // 初始化Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 初始化视图
        titleEditText = findViewById(R.id.etTitle);
        contentEditText = findViewById(R.id.etContent);

        // 设置标题
        setTitle("发布新帖子");

        Button btnPublish = findViewById(R.id.btnPublish);
        btnPublish.setOnClickListener(v -> savePost());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_forum_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            savePost();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void savePost() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "请填写所有必填字段", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查用户是否已登录
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "请先登录再发布帖子", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建新的Post对象
        Post post = new Post(
                UUID.randomUUID().toString(), // 生成唯一ID
                title,
                content,
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "匿名用户",
                currentUser.getUid(), // 用户ID
                new java.util.Date().toString(), // 转换为字符串
                "0", // 初始点赞数
                "0", // 初始评论数
                boardType
        );

        // 添加日志
        Log.d(TAG, "Saving post to Firestore: " + post.toString());

        // 保存到Firestore
        db.collection("forum_posts")
                .add(post.toMap())  // 使用 toMap() 方法将 Post 对象转换为 Map
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Post saved successfully with ID: " + documentReference.getId());
                    Toast.makeText(this, "发布成功", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK, new Intent().putExtra("refresh", true));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding document", e);
                    Toast.makeText(this, "发布失败: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
