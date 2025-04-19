package hk.hku.cs.hkuers.features.trade;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.chat.ChatRoomActivity;
import hk.hku.cs.hkuers.auth.LoginActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.view.ViewGroup;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class TradeDetailActivity extends AppCompatActivity {
    private static final String TAG = "TradeDetailActivity";
    
    public static Intent newIntent(Context context, String itemId) {
        Intent intent = new Intent(context, TradeDetailActivity.class);
        intent.putExtra("tradeId", itemId);
        return intent;
    }

    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView priceTextView;
    private TextView sellerTextView;
    private TextView categoryTextView;
    private MaterialButton contactButton;
    private MaterialButton favoriteButton;
    private MaterialButton shareButton;
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TradeItem tradeItem;
    private boolean isFavorite = false;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_trade_detail);

            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            storage = FirebaseStorage.getInstance();

            initializeViews();
            setupToolbar();
            
            String tradeId = getIntent().getStringExtra("tradeId");
            Log.d(TAG, "onCreate: tradeId = " + tradeId);
            
            if (tradeId != null && !tradeId.isEmpty()) {
                loadTradeItem(tradeId);
                checkFavoriteStatus(tradeId);
            } else {
                Log.e(TAG, "No tradeId provided in intent");
                Toast.makeText(this, "Error: No item ID provided", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        titleTextView = findViewById(R.id.tvItemName);
        descriptionTextView = findViewById(R.id.tvDescription);
        priceTextView = findViewById(R.id.tvPrice);
        sellerTextView = findViewById(R.id.tvSellerName);
        categoryTextView = findViewById(R.id.tvCategory);
        contactButton = findViewById(R.id.btnContactSeller);
        favoriteButton = findViewById(R.id.btnFavorite);
        shareButton = findViewById(R.id.btnShare);

        contactButton.setOnClickListener(v -> contactSeller());
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        shareButton.setOnClickListener(v -> shareItem());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void checkFavoriteStatus(String tradeId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.getUid())
                .collection("favorites")
                .document(tradeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isFavorite = documentSnapshot.exists();
                    updateFavoriteButton();
                });
        }
    }

    private void toggleFavorite() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        String tradeId = tradeItem.getId();
        if (tradeId == null) {
            Log.e(TAG, "Cannot favorite item: tradeId is null");
            Toast.makeText(this, "Error: Cannot identify item", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Toggle favorite for item ID: " + tradeId);

        if (isFavorite) {
            // Remove from favorites
            db.collection("users")
                .document(currentUser.getUid())
                .collection("favorites")
                .document(tradeId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    isFavorite = false;
                    updateFavoriteButton();
                    Snackbar.make(favoriteButton, R.string.favorite_removed, Snackbar.LENGTH_SHORT).show();
                    Log.d(TAG, "Item removed from favorites: " + tradeId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing from favorites: " + e.getMessage(), e);
                    Toast.makeText(this, "Error removing from favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } else {
            // Add to favorites - ensure we have the complete item with ID
            if (tradeItem != null) {
                // Make sure item has proper ID set
                tradeItem.setId(tradeId);
                
                // Create a copy of the item's data to store in favorites
                // This ensures we don't lose any fields
                db.collection("users")
                    .document(currentUser.getUid())
                    .collection("favorites")
                    .document(tradeId)
                    .set(tradeItem)
                    .addOnSuccessListener(aVoid -> {
                        isFavorite = true;
                        updateFavoriteButton();
                        Snackbar.make(favoriteButton, R.string.favorite_added, Snackbar.LENGTH_SHORT).show();
                        Log.d(TAG, "Item added to favorites: " + tradeId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding to favorites: " + e.getMessage(), e);
                        Toast.makeText(this, "Error adding to favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            } else {
                Log.e(TAG, "Cannot favorite: tradeItem is null");
                Toast.makeText(this, "Error: Item data is missing", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateFavoriteButton() {
        favoriteButton.setIconResource(isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
    }

    private void shareItem() {
        if (tradeItem == null) return;

        String shareText = String.format("%s\nPrice: $%.2f\n%s",
            tradeItem.getTitle(),
            tradeItem.getPrice(),
            tradeItem.getDescription());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share item"));
    }

    private void contactSeller() {
        if (tradeItem == null || tradeItem.getSellerId() == null) {
            Toast.makeText(this, "无法获取卖家信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String sellerId = tradeItem.getSellerId();
        String currentUserId = currentUser.getUid();
        
        // 不能给自己发消息
        if (sellerId.equals(currentUserId)) {
            Toast.makeText(this, "这是您自己的商品", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示进度对话框
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setTitle("正在连接聊天")
            .setMessage("请稍候...")
            .setCancelable(false)
            .create();
        progressDialog.show();
        
        // 生成私聊群组ID（两个用户ID按字母顺序排序并拼接，确保唯一性）
        String chatId;
        if (sellerId.compareTo(currentUserId) < 0) {
            chatId = "private_" + sellerId + "_" + currentUserId;
        } else {
            chatId = "private_" + currentUserId + "_" + sellerId;
        }
        
        // 检查私聊是否已存在
        db.collection("chat_rooms").document(chatId).get()
            .addOnSuccessListener(documentSnapshot -> {
                progressDialog.dismiss();
                
                if (documentSnapshot.exists()) {
                    // 聊天已存在，直接打开
                    String chatName = tradeItem.getSellerName() + " - " + tradeItem.getTitle();
                    openChatRoom(chatId, chatName);
                } else {
                    // 聊天不存在，创建新的聊天
                    createPrivateChat(chatId, sellerId);
                }
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "联系卖家失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * 创建私聊聊天室
     * @param chatId 聊天室ID
     * @param sellerId 卖家ID
     */
    private void createPrivateChat(String chatId, String sellerId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;
        
        String currentUserId = currentUser.getUid();
        String chatName = tradeItem.getSellerName() + " - " + tradeItem.getTitle();
        
        // 准备聊天室数据
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chat_name", chatName);
        chatData.put("creator_id", currentUserId);
        chatData.put("created_at", new Timestamp(new Date()));
        chatData.put("is_private", true);
        chatData.put("item_id", tradeItem.getId());
        chatData.put("item_title", tradeItem.getTitle());
        
        // 将双方都添加为成员
        List<String> memberIds = new ArrayList<>();
        memberIds.add(currentUserId);
        memberIds.add(sellerId);
        chatData.put("member_ids", memberIds);
        
        // 初始化已读状态
        Map<String, Object> readCounts = new HashMap<>();
        readCounts.put(currentUserId, 0);
        readCounts.put(sellerId, 0);
        chatData.put("read_counts", readCounts);
        
        Map<String, Object> userReadStatus = new HashMap<>();
        userReadStatus.put(currentUserId, 0);
        userReadStatus.put(sellerId, 0);
        chatData.put("user_read_status", userReadStatus);
        
        // 设置最后消息和时间
        chatData.put("last_message", "");
        chatData.put("last_message_time", new Timestamp(new Date()));
        chatData.put("message_count", 0);
        
        // 显示进度对话框
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setTitle("正在创建聊天")
            .setMessage("请稍候...")
            .setCancelable(false)
            .create();
        progressDialog.show();
        
        // 创建聊天室
        db.collection("chat_rooms").document(chatId)
            .set(chatData)
            .addOnSuccessListener(aVoid -> {
                // 添加系统消息
                addInitialMessage(chatId, sellerId, progressDialog);
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "创建聊天失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * 添加聊天初始消息
     * @param chatId 聊天室ID
     * @param sellerId 卖家ID
     * @param progressDialog 进度对话框
     */
    private void addInitialMessage(String chatId, String sellerId, AlertDialog progressDialog) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            progressDialog.dismiss();
            return;
        }
        
        // 创建系统消息
        Map<String, Object> message = new HashMap<>();
        message.put("type", "system");
        message.put("text", "关于商品 \"" + tradeItem.getTitle() + "\" 的对话已创建");
        message.put("timestamp", new Timestamp(new Date()));
        message.put("senderId", "system");
        
        // 添加消息
        db.collection("chat_rooms").document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener(documentReference -> {
                // 更新聊天室的最后消息和时间
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("last_message", "关于商品 \"" + tradeItem.getTitle() + "\" 的对话已创建");
                updateData.put("last_message_time", new Timestamp(new Date()));
                updateData.put("message_count", 1);
                
                db.collection("chat_rooms").document(chatId)
                    .update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        // 打开聊天室
                        String chatName = tradeItem.getSellerName() + " - " + tradeItem.getTitle();
                        openChatRoom(chatId, chatName);
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "更新聊天室失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "添加消息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * 打开聊天室
     * @param chatId 聊天室ID
     * @param chatName 聊天室名称
     */
    private void openChatRoom(String chatId, String chatName) {
        Intent intent = new Intent(TradeDetailActivity.this, ChatRoomActivity.class);
        intent.putExtra("chatRoomId", chatId);
        intent.putExtra("chatRoomName", chatName);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent upIntent = new Intent(this, TradeListActivity.class);
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            upIntent.putExtra("fromDetail", true);
            startActivity(upIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, TradeListActivity.class);
        intent.putExtra("refreshList", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void loadTradeItem(String tradeId) {
        Log.d(TAG, "Loading trade item with ID: " + tradeId);
        try {
            DocumentReference docRef = db.collection("trades").document(tradeId);
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    try {
                        // 记录接收到的所有字段，用于调试
                        Log.d(TAG, "Document data: " + documentSnapshot.getData());
                        
                        tradeItem = documentSnapshot.toObject(TradeItem.class);
                        if (tradeItem != null) {
                            tradeItem.setId(tradeId);
                            
                            // 处理timestamp或createTime字段
                            Object timestampObj = documentSnapshot.get("timestamp");
                            if (timestampObj != null) {
                                if (timestampObj instanceof Long) {
                                    long timestamp = (Long) timestampObj;
                                    Log.d(TAG, "Timestamp from Firestore (numeric): " + timestamp);
                                    tradeItem.setTimestamp(timestamp);
                                } else if (timestampObj instanceof Timestamp) {
                                    Timestamp firestoreTimestamp = (Timestamp) timestampObj;
                                    Log.d(TAG, "Timestamp from Firestore (Timestamp): " + firestoreTimestamp);
                                    tradeItem.setCreateTime(firestoreTimestamp);
                                }
                            } else {
                                // 检查createTime字段
                                Object createTimeObj = documentSnapshot.get("createTime");
                                if (createTimeObj != null && createTimeObj instanceof Timestamp) {
                                        Timestamp firestoreTimestamp = (Timestamp) createTimeObj;
                                        Log.d(TAG, "CreateTime from Firestore: " + firestoreTimestamp);
                                        tradeItem.setCreateTime(firestoreTimestamp);
                                }
                            }
                            
                            // 直接更新一次UI，显示已有的信息
                            updateUI();
                            
                            // 从用户集合获取卖家真实名称
                            String sellerId = tradeItem.getSellerId();
                            if (sellerId != null && !sellerId.isEmpty()) {
                                Log.d(TAG, "Looking up seller info for ID: " + sellerId);
                                
                                // 查询用户信息以获取真实姓名
                                db.collection("users").document(sellerId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            // 从用户文档中获取名称（优先使用uname字段）
                                            String userName = userDoc.getString("uname");
                                            
                                            if (userName != null && !userName.isEmpty()) {
                                                Log.d(TAG, "Found seller name: " + userName);
                                                tradeItem.setSellerName(userName);
                                                
                                                // 更新UI显示
                                                sellerTextView.setText(userName);
                                            } else {
                                                Log.d(TAG, "Seller name not found in user document");
                                            }
                                        } else {
                                            Log.d(TAG, "User document not found for seller ID: " + sellerId);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error fetching seller info: " + e.getMessage(), e);
                                    });
                            }
                        } else {
                            Log.e(TAG, "Failed to convert document to TradeItem");
                            Toast.makeText(this, "Error: Failed to load item details", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing document: " + e.getMessage(), e);
                        Toast.makeText(this, "Error processing item data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Log.e(TAG, "Document does not exist for ID: " + tradeId);
                    Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error loading trade item: " + e.getMessage(), e);
                Toast.makeText(this, "Failed to load item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in loadTradeItem: " + e.getMessage(), e);
            Toast.makeText(this, "Error accessing database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateUI() {
        try {
            if (tradeItem == null) {
                Log.e(TAG, "tradeItem is null in updateUI");
                Toast.makeText(this, "Error: Item data is missing", Toast.LENGTH_SHORT).show();
                return;
            }

            // 设置标题
            titleTextView.setText(tradeItem.getTitle());
            
            // 设置描述，确保非空
            String description = tradeItem.getDescription();
            if (description == null || description.isEmpty()) {
                description = "No description provided";
            }
            descriptionTextView.setText(description);
            
            // 设置价格
            priceTextView.setText(String.format("$%.2f", tradeItem.getPrice()));
            
            // 设置类别，确保非空
            String category = tradeItem.getCategory();
            if (category == null || category.isEmpty()) {
                category = "Other";
            }
            categoryTextView.setText(category);
            
            // 加载商品图片
            loadItemImage();
            
            Log.d(TAG, "UI updated with item: " + tradeItem.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
        }
    }

    /**
     * 加载商品图片
     */
    private void loadItemImage() {
        try {
            // 获取图片URL
            String imageUrl = tradeItem.getImageUrl();
            Log.d(TAG, "Loading product image: " + imageUrl);
            
            // 查找视图
            androidx.viewpager2.widget.ViewPager2 viewPager = findViewById(R.id.viewPager);
            ImageView singleImageView = new ImageView(this);
            singleImageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            singleImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // 处理不同类型的图片URI
                try {
                    // 尝试通用方法处理所有类型的URI
                    Uri uri = Uri.parse(imageUrl);
                    Log.d(TAG, "Parsed URI: " + uri + ", scheme: " + uri.getScheme());
                    
                    // 对file://类型的URI特殊处理 (私有存储的图片)
                    if (uri.getScheme() != null && uri.getScheme().equals("file")) {
                        Log.d(TAG, "Loading file URI: " + uri.getPath());
                        File imageFile = new File(uri.getPath());
                        if (imageFile.exists()) {
                            Log.d(TAG, "File exists, size: " + imageFile.length() + " bytes");
                        } else {
                            Log.e(TAG, "File does not exist: " + uri.getPath());
                        }
                    }
                    
                    // 使用Glide直接加载
                    Glide.with(this)
                        .load(uri)
                        .apply(new RequestOptions()
                             .diskCacheStrategy(DiskCacheStrategy.ALL)
                             .placeholder(R.drawable.default_avatar)
                             .error(R.drawable.default_avatar))
                        .into(singleImageView);
                    
                    // 将单个图片添加到视图
                    viewPager.setAdapter(new SingleImageAdapter(singleImageView));
                    Log.d(TAG, "Image loading attempt initiated");
                    
                    // 记录URI的详细信息
                    if (uri.getScheme() != null && uri.getScheme().equals("content")) {
                        Log.d(TAG, "Content URI details - Authority: " + uri.getAuthority() 
                             + ", Path: " + uri.getPath()
                             + ", Query: " + uri.getQuery());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image with URI: " + imageUrl, e);
                    singleImageView.setImageResource(R.drawable.default_avatar);
                    viewPager.setAdapter(new SingleImageAdapter(singleImageView));
                    
                    // 尝试备用方法 - 对content URI特殊处理
                    if (imageUrl.startsWith("content://")) {
                        try {
                            // 直接使用ImageView的setImageURI
                            Log.d(TAG, "Trying backup method for content URI");
                            singleImageView.setImageURI(Uri.parse(imageUrl));
                            viewPager.setAdapter(new SingleImageAdapter(singleImageView));
                        } catch (Exception e2) {
                            Log.e(TAG, "Backup method also failed for content URI", e2);
                            singleImageView.setImageResource(R.drawable.default_avatar);
                            viewPager.setAdapter(new SingleImageAdapter(singleImageView));
                        }
                    }
                    // 尝试备用方法 - 对file URI特殊处理
                    else if (imageUrl.startsWith("file://")) {
                        try {
                            Log.d(TAG, "Trying backup method for file URI");
                            File file = new File(Uri.parse(imageUrl).getPath());
                            if (file.exists()) {
                                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                                singleImageView.setImageBitmap(bitmap);
                                viewPager.setAdapter(new SingleImageAdapter(singleImageView));
                                Log.d(TAG, "Successfully loaded image from file");
                            } else {
                                Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
                                singleImageView.setImageResource(R.drawable.default_avatar);
                                viewPager.setAdapter(new SingleImageAdapter(singleImageView));
                            }
                        } catch (Exception e2) {
                            Log.e(TAG, "Backup method also failed for file URI", e2);
                            singleImageView.setImageResource(R.drawable.default_avatar);
                            viewPager.setAdapter(new SingleImageAdapter(singleImageView));
                        }
                    }
                }
            } else {
                // 没有图片URL，显示默认图片
                Log.d(TAG, "No image URL, showing default image");
                singleImageView.setImageResource(R.drawable.default_avatar);
                viewPager.setAdapter(new SingleImageAdapter(singleImageView));
            }
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in loadItemImage: " + e.getMessage(), e);
        }
    }
    
    /**
     * 单图片适配器 - 用于ViewPager2显示单个图片
     */
    private static class SingleImageAdapter extends RecyclerView.Adapter<SingleImageAdapter.ViewHolder> {
        private final View imageView;
        
        public SingleImageAdapter(View imageView) {
            this.imageView = imageView;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(imageView);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // 无需进一步操作，图片已经在构造函数中设置好
        }
        
        @Override
        public int getItemCount() {
            return 1; // 只有一个图片
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        try {
            // 检查用户是否登录，避免闪退
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                // 用户未登录，跳转到登录页面
                Log.d(TAG, "User not logged in in onResume, navigating to login screen");
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            
            // 获取商品ID并刷新数据
            String tradeId = getIntent().getStringExtra("tradeId");
            if (tradeId != null && !tradeId.isEmpty()) {
                loadTradeItem(tradeId);
                Log.d(TAG, "onResume called, refreshing data");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }
} 