package hk.hku.cs.hkuers.features.forum;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.models.LostFoundItem;

public class NewLostFoundActivity extends AppCompatActivity {
    private static final String TAG = "NewLostFoundActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private EditText titleEditText;
    private EditText descriptionEditText;
    private EditText locationEditText;
    private EditText contactEditText;
    private ImageView imageView;
    private String currentPhotoPath;
    private String type; // "lost" or "found"
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_lost_found);

        // 获取类型（失物或招领）
        type = getIntent().getStringExtra("type");
        if (type == null) {
            type = "lost"; // 默认为失物
        }

        // 初始化Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // 初始化视图
        titleEditText = findViewById(R.id.etTitle);
        descriptionEditText = findViewById(R.id.etDescription);
        locationEditText = findViewById(R.id.etLocation);
        contactEditText = findViewById(R.id.etContact);
        imageView = findViewById(R.id.ivImage);

        // 设置标题
        setTitle(type.equals("lost") ? "发布失物信息" : "发布招领信息");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_lost_found, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveLostFoundItem();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveLostFoundItem() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String contact = contactEditText.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || location.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "请填写所有必填字段", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建新的LostFoundItem
        LostFoundItem item = new LostFoundItem(
                null, // id will be set by Firestore
                title,
                description,
                location,
                new Date(),
                contact,
                type,
                currentPhotoPath,
                "user1", // TODO: 使用实际用户ID
                "张三", // TODO: 使用实际用户名
                "其他" // TODO: 添加分类选择
        );

        // 保存到Firestore
        db.collection("lost_found")
                .add(item.toMap())
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "发布成功", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding document", e);
                    Toast.makeText(this, "发布失败: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "hk.hku.cs.hkuers.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
