package hk.hku.cs.hkuers.features.trade;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hk.hku.cs.hkuers.MainActivity;
import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.auth.LoginActivity;

public class TradeListActivity extends AppCompatActivity {
    private static final String TAG = "TradeListActivity";
    private RecyclerView recyclerView;
    private FirestoreRecyclerAdapter<TradeItem, TradeItemViewHolder> adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private EditText searchEditText;
    private MaterialAutoCompleteTextView categorySpinner;
    private MaterialAutoCompleteTextView sortSpinner;
    private FloatingActionButton fabAdd;
    private FloatingActionButton fabMyTrade;
    private FloatingActionButton fabMyFavorites;
    private boolean isShowingFavorites = false;
    
    // 添加一个普通RecyclerView.Adapter作为备用方案
    private TradeAdapter fallbackAdapter;
    private List<TradeItem> fallbackItems = new ArrayList<>();

    private static final String[] CATEGORIES = {
        "All Categories",
        "Electronics",
        "Books",
        "Clothing",
        "Sports",
        "Home & Living",
        "Beauty & Health",
        "Others"
    };

    private static final String[] SORT_OPTIONS = {
        "Newest First",
        "Oldest First",
        "Price: Low to High",
        "Price: High to Low"
    };

    private static final int ADD_TRADE_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception", throwable);
            Toast.makeText(this, "发生错误: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            
            // 返回主界面
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_trade_list);

            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();

            // 初始化视图
            initializeViews();
            setupSpinners();
            setupSearch();
            
            // 检查用户是否登录
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                // 用户未登录，跳转到登录页面
                Log.d(TAG, "User not logged in, navigating to login screen");
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            
            // 初始化RecyclerView和适配器
            setupRecyclerView();
            
            // 初始显示所有商品
            loadAllTradesManually();
            
            // 标记为已经初始化，避免重复加载
            getIntent().putExtra("isInitialized", true);

        } catch (Exception e) {
            Log.e(TAG, "初始化失败", e);
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void loadAllTradesManually() {
        try {
            // 清空原有数据，避免重复
            if (fallbackItems != null) {
                fallbackItems.clear();
                
                // 如果适配器存在，先通知UI清空数据
                if (fallbackAdapter != null) {
                    fallbackAdapter.notifyDataSetChanged();
                }
            }
            
            // 检查用户登录状态
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Log.d(TAG, "User not logged in, can't load trades manually");
                return;
            }
            
            // 显示加载状态
            View loadingView = findViewById(R.id.loadingProgressBar);
            if (loadingView != null) {
                loadingView.setVisibility(View.VISIBLE);
            }
            
            // 获取当前搜索文本、分类和排序选项
            String searchText = "";
            String selectedCategory = "All Categories";
            
            if (searchEditText != null) {
                searchText = searchEditText.getText().toString().toLowerCase();
            }
            
            if (categorySpinner != null && categorySpinner.getText() != null && 
                !categorySpinner.getText().toString().isEmpty()) {
                selectedCategory = categorySpinner.getText().toString();
            }
            
            final String finalSearchText = searchText;
            final String finalSelectedCategory = selectedCategory;
            
            // 手动从Firestore获取所有交易
            Query query = db.collection("trades");
            
            // 只有当选择了特定分类且不是"All Categories"时才过滤
            if (!finalSelectedCategory.equals("All Categories") && !finalSelectedCategory.isEmpty()) {
                query = query.whereEqualTo("category", finalSelectedCategory);
                Log.d(TAG, "Filtering by category: " + finalSelectedCategory);
            } else {
                Log.d(TAG, "Not filtering by category, showing all items");
            }
            
            query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    try {
                        Log.d(TAG, "Manual query returned " + queryDocumentSnapshots.size() + " documents");
                        
                        // 如果没有数据，打印日志
                        if (queryDocumentSnapshots.isEmpty()) {
                            Log.d(TAG, "No items found in Firestore");
                        }
                        
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                String id = document.getId();
                                String title = document.getString("title");
                                String description = document.getString("description");
                                String sellerId = document.getString("sellerId");
                                String sellerName = document.getString("sellerName");
                                String category = document.getString("category");
                                String status = document.getString("status");
                                String imageUrl = document.getString("imageUrl");
                                double price = 0;
                                
                                // 安全地尝试获取价格
                                try {
                                    if (document.contains("price")) {
                                        if (document.get("price") instanceof Double) {
                                            price = document.getDouble("price");
                                        } else if (document.get("price") instanceof Long) {
                                            price = document.getLong("price");
                                        } else {
                                            price = 0;
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing price for document " + id, e);
                                    price = 0;
                                }
                                
                                // 处理时间戳 - 考虑两种格式
                                Timestamp createTime = null;
                                try {
                                    // 尝试获取Timestamp类型
                                    if (document.contains("createTime")) {
                                        if (document.get("createTime") instanceof Timestamp) {
                                            createTime = document.getTimestamp("createTime");
                                        } else if (document.get("createTime") instanceof Long) {
                                            // 转换长整型为Timestamp
                                            long timeMillis = document.getLong("createTime");
                                            createTime = new Timestamp(new Date(timeMillis));
                                        }
                                    } else if (document.contains("timestamp")) {
                                        // 尝试从timestamp字段获取
                                        long timeMillis = document.getLong("timestamp");
                                        createTime = new Timestamp(new Date(timeMillis));
                                    }
                                    
                                    if (createTime == null) {
                                        // 如果都没有，使用当前时间
                                        createTime = Timestamp.now();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing timestamp for document " + id, e);
                                    createTime = Timestamp.now();
                                }
                                
                                // 创建交易项
                                TradeItem item = new TradeItem();
                                item.setId(id);
                                item.setTitle(title != null ? title : "Unknown title");
                                item.setDescription(description);
                                item.setPrice(price);
                                item.setSellerId(sellerId);
                                item.setSellerName(sellerName != null ? sellerName : "Anonymous");
                                item.setCategory(category);
                                item.setStatus(status);
                                item.setImageUrl(imageUrl);
                                item.setCreateTime(createTime);
                                
                                // 应用搜索过滤
                                if (!finalSearchText.isEmpty()) {
                                    boolean matchesSearch = false;
                                    if (title != null && title.toLowerCase().contains(finalSearchText)) {
                                        matchesSearch = true;
                                    } else if (description != null && description.toLowerCase().contains(finalSearchText)) {
                                        matchesSearch = true;
                                    } else if (category != null && category.toLowerCase().contains(finalSearchText)) {
                                        matchesSearch = true;
                                    }
                                    
                                    if (!matchesSearch) {
                                        continue; // 跳过不匹配的项目
                                    }
                                }
                                
                                // 添加到列表
                                fallbackItems.add(item);
                                Log.d(TAG, "Added manual item: " + item.getTitle());
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing document " + document.getId(), e);
                                // 继续处理下一个文档
                            }
                        }
                        
                        // 应用排序
                        applySortingToFallbackItems();
                        
                        // 通知适配器更新
                        if (fallbackAdapter != null) {
                            fallbackAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Notified fallback adapter, items: " + fallbackItems.size());
                        }
                        
                        // 隐藏加载状态
                        View loadingView2 = findViewById(R.id.loadingProgressBar);
                        if (loadingView2 != null) {
                            loadingView2.setVisibility(View.GONE);
                        }
                        
                        // 更新空视图状态
                        View emptyView = findViewById(R.id.emptyView);
                        if (emptyView != null) {
                            emptyView.setVisibility(fallbackItems.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing manual query results", e);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading trades manually", e);
                    
                    // 隐藏加载状态
                    View loadingView2 = findViewById(R.id.loadingProgressBar);
                    if (loadingView2 != null) {
                        loadingView2.setVisibility(View.GONE);
                    }
                    
                    // 显示空视图
                    View emptyView = findViewById(R.id.emptyView);
                    if (emptyView != null) {
                        emptyView.setVisibility(View.VISIBLE);
                    }
                    
                    Toast.makeText(this, "加载商品列表失败", Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadAllTradesManually", e);
        }
    }
    
    private void applySortingToFallbackItems() {
        try {
            String sortOption = "Newest First"; // 默认排序
            
            if (sortSpinner != null && sortSpinner.getText() != null) {
                sortOption = sortSpinner.getText().toString();
            }
            
            switch (sortOption) {
                case "Newest First":
                    fallbackItems.sort((a, b) -> {
                        if (a.getCreateTime() == null) return 1;
                        if (b.getCreateTime() == null) return -1;
                        return -a.getCreateTime().compareTo(b.getCreateTime()); // 降序，最新的在前
                    });
                    break;
                case "Oldest First":
                    fallbackItems.sort((a, b) -> {
                        if (a.getCreateTime() == null) return -1;
                        if (b.getCreateTime() == null) return 1;
                        return a.getCreateTime().compareTo(b.getCreateTime()); // 升序，最旧的在前
                    });
                    break;
                case "Price: Low to High":
                    fallbackItems.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                    break;
                case "Price: High to Low":
                    fallbackItems.sort((a, b) -> -Double.compare(a.getPrice(), b.getPrice()));
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying sorting", e);
        }
    }
    
    private void setupFallbackAdapter() {
        try {
            fallbackAdapter = new TradeAdapter(fallbackItems, this::onItemClick);
            
            // 如果recyclerView不为空，设置备用适配器
            if (recyclerView != null) {
                recyclerView.setAdapter(fallbackAdapter);
                Log.d(TAG, "Set fallback adapter to RecyclerView");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setupFallbackAdapter", e);
        }
    }

    private void initializeViews() {
        try {
            recyclerView = findViewById(R.id.recyclerView);
            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView not found in layout");
                Toast.makeText(this, "布局加载错误", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            // 初始化搜索框
            searchEditText = findViewById(R.id.searchEditText);
            categorySpinner = findViewById(R.id.categorySpinner);
            sortSpinner = findViewById(R.id.sortSpinner);
            
            fabAdd = findViewById(R.id.fabAdd);
            if (fabAdd != null) {
                fabAdd.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(TradeListActivity.this, AddTradeActivity.class);
                        startActivityForResult(intent, ADD_TRADE_REQUEST_CODE);
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching AddTradeActivity", e);
                        Toast.makeText(this, "无法打开添加交易页面: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            fabMyTrade = findViewById(R.id.fabMyTrade);
            if (fabMyTrade != null) {
                fabMyTrade.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(this, MyTradeActivity.class));
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching MyTradeActivity", e);
                        Toast.makeText(this, "无法打开我的交易页面: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            fabMyFavorites = findViewById(R.id.fabMyFavorites);
            if (fabMyFavorites != null) {
                fabMyFavorites.setOnClickListener(v -> {
                    try {
                        toggleFavorites();
                    } catch (Exception e) {
                        Log.e(TAG, "Error toggling favorites", e);
                        Toast.makeText(this, "无法切换收藏显示: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "初始化视图失败", e);
            Toast.makeText(this, "初始化视图失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSpinners() {
        try {
            // Setup category spinner
            if (categorySpinner != null) {
                ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    CATEGORIES
                );
                categorySpinner.setAdapter(categoryAdapter);
                
                // 设置默认值为全部类别
                categorySpinner.setText(CATEGORIES[0], false);
                
                categorySpinner.setOnItemClickListener((parent, view, position, id) -> {
                    Log.d(TAG, "Category selected: " + CATEGORIES[position]);
                    updateQuery();
                });
            }

            // Setup sort spinner
            if (sortSpinner != null) {
                ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    SORT_OPTIONS
                );
                sortSpinner.setAdapter(sortAdapter);
                
                // 设置默认值为最新优先
                sortSpinner.setText(SORT_OPTIONS[0], false);
                
                sortSpinner.setOnItemClickListener((parent, view, position, id) -> {
                    Log.d(TAG, "Sort option selected: " + SORT_OPTIONS[position]);
                    updateQuery();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "设置下拉框失败", e);
        }
    }

    private void setupSearch() {
        try {
            if (searchEditText != null) {
                searchEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        Log.d(TAG, "Search text changed: " + s.toString());
                        updateQuery();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "设置搜索框失败", e);
        }
    }

    private void setupRecyclerView() {
        try {
            // 直接使用自定义适配器，因为它可以处理所有数据格式
            fallbackAdapter = new TradeAdapter(fallbackItems, this::onItemClick);
            
            if (recyclerView != null) {
                recyclerView.setAdapter(fallbackAdapter);
                Log.d(TAG, "Initial RecyclerView setup complete with adapter");
            } else {
                Log.e(TAG, "RecyclerView is null during setupRecyclerView");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView: " + e.getMessage(), e);
            Toast.makeText(this, "Error setting up item list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        
        // 如果是从详情页面返回，刷新数据
        if (intent != null && intent.getBooleanExtra("fromDetail", false)) {
            updateQuery();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
            
            // 检查是否需要刷新数据
            boolean needRefresh = false;
            Intent intent = getIntent();
            
            // 如果是初始化后首次onResume，不需要刷新
            if (intent.getBooleanExtra("isInitialized", false) && !intent.getBooleanExtra("hasResumed", false)) {
                // 标记已经执行过onResume
                intent.putExtra("hasResumed", true);
                Log.d(TAG, "First resume after initialization, skipping refresh");
            }
            // 如果是从详情页面返回，需要刷新
            else if (intent.getBooleanExtra("fromDetail", false)) {
                needRefresh = true;
                Log.d(TAG, "Returned from detail view, refreshing data");
            }
            // 如果添加了新商品，需要刷新
            else if (intent.getBooleanExtra("refreshList", false)) {
                needRefresh = true;
                Log.d(TAG, "Refresh requested, refreshing data");
            }
            
            if (needRefresh) {
                // 显示加载指示器
                View loadingView = findViewById(R.id.loadingProgressBar);
                if (loadingView != null) {
                    loadingView.setVisibility(View.VISIBLE);
                }
                
                // 暂时隐藏空视图
                View emptyView = findViewById(R.id.emptyView);
                if (emptyView != null) {
                    emptyView.setVisibility(View.GONE);
                }
                
                // 刷新商品数据
                updateQuery();
                Log.d(TAG, "onResume called, refreshing data");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    private void updateQuery() {
        try {
            // 检查用户是否登录
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "Current user is null, cannot perform query");
                return;
            }
            
            // 使用备用方案加载和过滤数据
            loadAllTradesManually();
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating query: " + e.getMessage(), e);
            Toast.makeText(this, "Error updating items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // 显示空视图
            View emptyView = findViewById(R.id.emptyView);
            if (emptyView != null) {
                emptyView.setVisibility(View.VISIBLE);
            }
            
            // 隐藏加载指示器
            View loadingView = findViewById(R.id.loadingProgressBar);
            if (loadingView != null) {
                loadingView.setVisibility(View.GONE);
            }
        }
    }

    private void toggleFavorites() {
        try {
            isShowingFavorites = !isShowingFavorites;
            
            if (isShowingFavorites) {
                // 显示收藏列表
                Toast.makeText(this, "显示我的收藏", Toast.LENGTH_SHORT).show();
                // 更新工具栏标题
                androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
                if (toolbar != null) {
                    toolbar.setTitle(R.string.my_favorites);
                }
                // 禁用分类和搜索功能
                if (searchEditText != null) searchEditText.setEnabled(false);
                if (categorySpinner != null) categorySpinner.setEnabled(false);
                if (sortSpinner != null) sortSpinner.setEnabled(false);
            } else {
                // 恢复显示所有商品
                Toast.makeText(this, "显示所有商品", Toast.LENGTH_SHORT).show();
                // 恢复工具栏标题
                androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
                if (toolbar != null) {
                    toolbar.setTitle(R.string.trade_market);
                }
                // 启用分类和搜索功能
                if (searchEditText != null) searchEditText.setEnabled(true);
                if (categorySpinner != null) categorySpinner.setEnabled(true);
                if (sortSpinner != null) sortSpinner.setEnabled(true);
            }
            
            // 更新查询
            updateQuery();
        } catch (Exception e) {
            Log.e(TAG, "Error toggling favorites", e);
            Toast.makeText(this, "Error toggling favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // 从添加商品界面返回时，刷新商品列表
        if (requestCode == ADD_TRADE_REQUEST_CODE) {
            Log.d("TradeListActivity", "Returned from AddTradeActivity, refreshing data");
            // 显示加载状态
            View loadingView = findViewById(R.id.loadingProgressBar);
            if (loadingView != null) {
                loadingView.setVisibility(View.VISIBLE);
            }
            // 刷新商品数据
            updateQuery();
        }
    }

    public void onItemClick(TradeItem item) {
        try {
            if (item != null && item.getId() != null) {
                Log.d(TAG, "Clicked on trade item with ID: " + item.getId());
                Intent intent = new Intent(this, TradeDetailActivity.class);
                intent.putExtra("tradeId", item.getId());
                startActivity(intent);
            } else {
                Log.e(TAG, "Cannot open trade details: item or item ID is null");
                Toast.makeText(this, "Cannot open item details", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onItemClick", e);
            Toast.makeText(this, "Error opening item details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to handle similar category names
    private boolean isSimilarCategory(String category1, String category2) {
        if (category1 == null || category2 == null) return false;
        
        // Convert to lowercase for comparison
        String cat1 = category1.toLowerCase();
        String cat2 = category2.toLowerCase();
        
        // Define mappings between Chinese and English categories
        if ((cat1.contains("电子") || cat1.contains("electronic")) && cat2.contains("electronic")) return true;
        if ((cat1.contains("书籍") || cat1.contains("book")) && cat2.contains("book")) return true;
        if ((cat1.contains("家居") || cat1.contains("home")) && cat2.contains("home")) return true;
        if ((cat1.contains("服装") || cat1.contains("cloth")) && cat2.contains("cloth")) return true;
        if ((cat1.contains("美妆") || cat1.contains("beauty")) && cat2.contains("beauty")) return true;
        if ((cat1.contains("运动") || cat1.contains("sport")) && cat2.contains("sport")) return true;
        if ((cat1.contains("其他") || cat1.contains("other")) && cat2.contains("other")) return true;
        
        return false;
    }
} 