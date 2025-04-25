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

import java.util.HashMap;
import java.util.Map;
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
            boardType = "campus_life"; // 默认为校园生活版块
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

        // 根据不同的版块类型保存到不同的集合
        if ("lostfound".equals(boardType)) {
            // 保存到失物招领集合
            Map<String, Object> lostFoundItem = new HashMap<>();
            lostFoundItem.put("title", title);
            lostFoundItem.put("description", content);
            lostFoundItem.put("author", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "匿名用户");
            lostFoundItem.put("authorId", currentUser.getUid());
            lostFoundItem.put("timestamp", new java.util.Date().toString());
            lostFoundItem.put("status", "active"); // 状态：活跃/已解决

            // 添加日志
            Log.d(TAG, "Saving lost and found item to Firestore: " + lostFoundItem);

            // 保存到Firestore
            db.collection("lostFoundItems")
                    .add(lostFoundItem)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Lost and found item saved successfully with ID: " + documentReference.getId());
                        Toast.makeText(this, "发布成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK, new Intent().putExtra("refresh", true));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding lost and found item", e);
                        Toast.makeText(this, "发布失败: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // 保存到论坛帖子集合
            Post post = new Post(
                    UUID.randomUUID().toString(), // 生成唯一ID
                    title,
                    content,
                    currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "匿名用户",
                    currentUser.getUid(), // 用户ID
                    new java.util.Date().toString(), // 时间戳
                    boardType // 版块类型
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
}
