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
    private View loadingView;
    
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
            
            // 排序处理
            final String sortOption = sortSpinner != null ? sortSpinner.getText().toString() : "Newest First";
            if (sortOption.equals("Price: Low to High")) {
                query = query.orderBy("price", Query.Direction.ASCENDING);
            } else if (sortOption.equals("Price: High to Low")) {
                query = query.orderBy("price", Query.Direction.DESCENDING);
            } else if (sortOption.equals("Oldest First")) {
                query = query.orderBy("createTime", Query.Direction.ASCENDING);
            } else {
                // Default to newest first
                query = query.orderBy("createTime", Query.Direction.DESCENDING);
            }
            
            Log.d(TAG, "Query prepared with search: " + finalSearchText + ", category: " + finalSelectedCategory + ", sort: " + sortOption);
            
            // 执行查询
            query.get().addOnCompleteListener(task -> {
                try {
                    if (task.isSuccessful()) {
                        // 创建一个临时列表以存储完成卖家信息加载的商品
                        List<TradeItem> tempItems = new ArrayList<>();
                        List<QueryDocumentSnapshot> documents = new ArrayList<>();
                        
                        // 先收集所有文档
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            documents.add(document);
                        }
                        
                        Log.d(TAG, "Query returned " + documents.size() + " documents");
                        
                        // 如果没有商品，直接显示空状态
                        if (documents.isEmpty()) {
                            fallbackItems.clear();
                            if (fallbackAdapter != null) {
                                fallbackAdapter.notifyDataSetChanged();
                            }
                            
                            // 使用在方法开始已经定义的loadingView变量
                            if (loadingView != null) {
                                loadingView.setVisibility(View.GONE);
                            }
                            
                            // 显示空状态
                            View emptyView = findViewById(R.id.emptyView);
                            if (emptyView != null) {
                                emptyView.setVisibility(View.VISIBLE);
                            }
                            return;
                        }
                        
                        // 使用计数器来跟踪加载完成的商品数量
                        final int[] completedCounter = {0};
                        final int totalCount = documents.size();

                        // 处理每个文档
                        for (QueryDocumentSnapshot document : documents) {
                            try {
                                // 获取文档字段
                                String id = document.getId();
                                String title = document.getString("title");
                                String description = document.getString("description");
                                
                                Double priceObj = document.getDouble("price");
                                double price = priceObj != null ? priceObj : 0.0;
                                
                                String sellerId = document.getString("sellerId");
                                String sellerName = document.getString("sellerName");
                                String category = document.getString("category");
                                String status = document.getString("status");
                                String imageUrl = document.getString("imageUrl");
                                
                                // 尝试获取createTime
                                Timestamp createTime = null;
                                Object createTimeObj = document.get("createTime");
                                if (createTimeObj instanceof Timestamp) {
                                    createTime = (Timestamp) createTimeObj;
                                } else if (document.contains("timestamp")) {
                                    Long timestamp = document.getLong("timestamp");
                                    if (timestamp != null) {
                                        createTime = new Timestamp(new Date(timestamp));
                                    }
                                }
                                
                                // 如果没有createTime，使用当前时间
                                if (createTime == null) {
                                    createTime = Timestamp.now();
                                }
                                
                                // 创建TradeItem对象并设置所有字段
                                TradeItem item = new TradeItem();
                                item.setId(id);
                                item.setTitle(title);
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
                                        completedCounter[0]++;
                                        checkAndUpdateUI(completedCounter[0], totalCount, tempItems);
                                        continue; // 跳过不匹配的项目
                                    }
                                }
                                
                                // 查询卖家真实名称
                                if (sellerId != null && !sellerId.isEmpty()) {
                                    // 从users集合中查询卖家信息
                                    db.collection("users").document(sellerId).get()
                                        .addOnSuccessListener(userDoc -> {
                                            String userName = null;
                                            if (userDoc.exists()) {
                                                // 优先使用uname字段
                                                userName = userDoc.getString("uname");
                                                
                                                // 如果uname为空，尝试使用displayName字段
                                                if (userName == null || userName.isEmpty()) {
                                                    userName = userDoc.getString("displayName");
                                                }
                                                
                                                // 如果找到了用户名，更新商品的sellerName
                                                if (userName != null && !userName.isEmpty()) {
                                                    item.setSellerName(userName);
                                                    Log.d(TAG, "Updated seller name for item " + id + ": " + userName);
                                                }
                                            }
                                            
                                            // 添加到临时列表
                                            tempItems.add(item);
                                            completedCounter[0]++;
                                            
                                            // 检查是否所有商品都已处理完毕
                                            checkAndUpdateUI(completedCounter[0], totalCount, tempItems);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error fetching seller info for ID " + sellerId + ": " + e.getMessage());
                                            
                                            // 即使获取卖家信息失败，也添加商品到列表
                                            tempItems.add(item);
                                            completedCounter[0]++;
                                            
                                            // 检查是否所有商品都已处理完毕
                                            checkAndUpdateUI(completedCounter[0], totalCount, tempItems);
                                        });
                                } else {
                                    // 如果没有sellerId，直接添加到列表
                                    tempItems.add(item);
                                    completedCounter[0]++;
                                    
                                    // 检查是否所有商品都已处理完毕
                                    checkAndUpdateUI(completedCounter[0], totalCount, tempItems);
                                }
                                
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing document: " + e.getMessage(), e);
                                completedCounter[0]++;
                                checkAndUpdateUI(completedCounter[0], totalCount, tempItems);
                            }
                        }
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        
                        // 使用在方法开始已经定义的loadingView变量
                        if (loadingView != null) {
                            loadingView.setVisibility(View.GONE);
                        }
                        
                        // 显示空状态
                        View emptyView = findViewById(R.id.emptyView);
                        if (emptyView != null) {
                            emptyView.setVisibility(View.VISIBLE);
                        }
                        
                        Toast.makeText(TradeListActivity.this, "Failed to load trades: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in query completion: " + e.getMessage(), e);
                    
                    // 使用在方法开始已经定义的loadingView变量
                    if (loadingView != null) {
                        loadingView.setVisibility(View.GONE);
                    }
                    
                    // 显示空状态
                    View emptyView = findViewById(R.id.emptyView);
                    if (emptyView != null) {
                        emptyView.setVisibility(View.VISIBLE);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadAllTradesManually: " + e.getMessage(), e);
            
            // 使用在方法开始已经定义的loadingView变量
            if (loadingView != null) {
                loadingView.setVisibility(View.GONE);
            }
            
            // 显示空状态
            View emptyView = findViewById(R.id.emptyView);
            if (emptyView != null) {
                emptyView.setVisibility(View.VISIBLE);
            }
            
            Toast.makeText(this, "Error loading trades: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 检查是否所有商品都已处理，如果是，更新UI
     */
    private void checkAndUpdateUI(int completed, int total, List<TradeItem> tempItems) {
        if (completed >= total) {
            // 所有商品都处理完毕
            // 更新列表数据
            fallbackItems.clear();
            fallbackItems.addAll(tempItems);
            
            // 刷新UI
            if (fallbackAdapter != null) {
                fallbackAdapter.notifyDataSetChanged();
                Log.d(TAG, "Updated fallback adapter with " + fallbackItems.size() + " items");
            }
            
            // 隐藏加载状态
            if (loadingView != null) {
                loadingView.setVisibility(View.GONE);
            }
            
            // 如果没有商品，显示空状态
            if (fallbackItems.isEmpty()) {
                View emptyView = findViewById(R.id.emptyView);
                if (emptyView != null) {
                    emptyView.setVisibility(View.VISIBLE);
                }
            } else {
                View emptyView = findViewById(R.id.emptyView);
                if (emptyView != null) {
                    emptyView.setVisibility(View.GONE);
                }
            }
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
            
            // 初始化loadingView
            loadingView = findViewById(R.id.loadingProgressBar);
            
            // 设置toolbar的返回按钮点击事件
            androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setNavigationOnClickListener(v -> {
                    // 返回到主界面
                    Intent intent = new Intent(TradeListActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            }
            
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
                    R.layout.dropdown_item_dark,
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
                    R.layout.dropdown_item_dark,
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