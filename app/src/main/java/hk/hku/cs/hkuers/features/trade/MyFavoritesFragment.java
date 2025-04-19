package hk.hku.cs.hkuers.features.trade;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import hk.hku.cs.hkuers.R;

public class MyFavoritesFragment extends Fragment {
    private static final String TAG = "MyFavoritesFragment";
    private RecyclerView recyclerView;
    private CircularProgressIndicator progressIndicator;
    private TradeAdapter adapter;
    private List<TradeItem> items = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            Log.d(TAG, "onCreateView: 创建MyFavoritesFragment视图");
            View view = inflater.inflate(R.layout.fragment_trade_list, container, false);
            
            recyclerView = view.findViewById(R.id.recyclerView);
            progressIndicator = view.findViewById(R.id.progressIndicator);
            
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            
            Log.d(TAG, "onCreateView: 创建TradeAdapter");
            adapter = new TradeAdapter(items, this::onItemClick);
            recyclerView.setAdapter(adapter);

            db = FirebaseFirestore.getInstance();
            
            return view;
        } catch (Exception e) {
            Log.e(TAG, "onCreateView: 创建视图时发生错误", e);
            Toast.makeText(getContext(), "初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return new View(getContext()); // 返回空视图防止崩溃
        }
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            Log.d(TAG, "onViewCreated: 视图创建完成，加载收藏数据");
            loadFavorites();
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated: 加载数据时发生错误", e);
            Toast.makeText(getContext(), "加载数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        try {
            Log.d(TAG, "onResume: 刷新收藏数据");
            loadFavorites();
        } catch (Exception e) {
            Log.e(TAG, "onResume: 刷新数据时发生错误", e);
        }
    }

    private void loadFavorites() {
        try {
            Log.d(TAG, "loadFavorites: 开始加载收藏数据");
            // 显示加载指示器
            progressIndicator.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "loadFavorites: 用户未登录");
                progressIndicator.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String userId = currentUser.getUid();
            Log.d(TAG, "loadFavorites: 用户ID=" + userId);
            
            // 直接从用户的favorites集合获取收藏的商品数据
            db.collection("users")
                .document(userId)
                .collection("favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    try {
                        Log.d(TAG, "loadFavorites: 获取到收藏列表，数量=" + queryDocumentSnapshots.size());
                        items.clear();
                        
                        if (queryDocumentSnapshots.isEmpty()) {
                            // 如果没有收藏项，直接隐藏进度指示器
                            progressIndicator.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            Toast.makeText(getContext(), "暂无收藏的商品", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // 直接从favorites文档转换为TradeItem对象
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            try {
                                String tradeId = doc.getId();
                                Log.d(TAG, "loadFavorites: 处理收藏商品，ID=" + tradeId);
                                
                                TradeItem item = doc.toObject(TradeItem.class);
                                if (item != null) {
                                    // 确保ID已设置
                                    item.setId(tradeId);
                                    
                                    // 检查必要的字段是否存在
                                    if (item.getTitle() == null || item.getTitle().isEmpty()) {
                                        Log.w(TAG, "loadFavorites: 商品缺少标题，ID=" + tradeId);
                                        // 如果收藏的商品数据不完整，可以选择跳过或者标记
                                        item.setTitle("Unknown Item");
                                    }
                                    
                                    // 添加到列表
                                    items.add(item);
                                    Log.d(TAG, "loadFavorites: 添加商品 " + item.getTitle() + " 到列表");
                                } else {
                                    Log.e(TAG, "loadFavorites: 无法将文档转换为TradeItem，ID=" + tradeId);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "loadFavorites: 处理单个商品时出错", e);
                            }
                        }
                        
                        // 更新适配器
                        adapter.notifyDataSetChanged();
                        
                        // 隐藏加载指示器
                        progressIndicator.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        
                        // 显示结果
                        if (items.isEmpty()) {
                            Toast.makeText(getContext(), "暂无收藏的商品", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "loadFavorites: 成功加载 " + items.size() + " 个收藏商品");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "loadFavorites: 处理收藏列表时发生错误", e);
                        progressIndicator.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(), "处理收藏数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // 隐藏加载指示器
                    Log.e(TAG, "loadFavorites: 获取收藏列表失败", e);
                    progressIndicator.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    
                    Toast.makeText(getContext(), "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "loadFavorites: 加载过程中发生错误", e);
            if (progressIndicator != null) {
                progressIndicator.setVisibility(View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            Toast.makeText(getContext(), "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void onItemClick(TradeItem item) {
        try {
            Log.d(TAG, "onItemClick: 点击商品，ID=" + item.getId());
            // 跳转到详情页
            Intent intent = new Intent(getContext(), TradeDetailActivity.class);
            intent.putExtra("tradeId", item.getId());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "onItemClick: 点击处理时发生错误", e);
            Toast.makeText(getContext(), "打开详情失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 