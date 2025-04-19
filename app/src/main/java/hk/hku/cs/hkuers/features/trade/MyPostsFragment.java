package hk.hku.cs.hkuers.features.trade;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import hk.hku.cs.hkuers.R;

public class MyPostsFragment extends Fragment {
    private RecyclerView recyclerView;
    private CircularProgressIndicator progressIndicator;
    private MyTradeAdapter adapter;
    private List<TradeItem> items = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trade_list, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        progressIndicator = view.findViewById(R.id.progressIndicator);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyTradeAdapter(
                items,
                this::onItemClick,
                this::onEditClick,
                this::onDeleteClick
        );
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadMyPosts();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复时重新加载数据，以便在编辑后更新列表
        loadMyPosts();
    }

    private void loadMyPosts() {
        // 显示加载指示器
        progressIndicator.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("trades")
            .whereEqualTo("sellerId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                items.clear();
                for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                    TradeItem item = queryDocumentSnapshots.getDocuments().get(i).toObject(TradeItem.class);
                    if (item != null) {
                        item.setId(queryDocumentSnapshots.getDocuments().get(i).getId());
                        items.add(item);
                    }
                }
                
                // 手动按时间排序（如果有createTime字段）
                items.sort((item1, item2) -> {
                    try {
                        // 尝试使用createTime字段排序
                        if (item1.getCreateTime() != null && item2.getCreateTime() != null) {
                            // 降序排列（新的在前）
                            return -item1.getCreateTime().compareTo(item2.getCreateTime());
                        } else if (item1.getCreateTime() != null) {
                            return -1; // item1有时间，排前面
                        } else if (item2.getCreateTime() != null) {
                            return 1;  // item2有时间，排前面
                        } else {
                            return 0;  // 都没有时间，保持原排序
                        }
                    } catch (Exception e) {
                        // 出现异常时不改变顺序
                        return 0;
                    }
                });
                
                adapter.notifyDataSetChanged();
                
                // 隐藏加载指示器
                progressIndicator.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                
                // 如果没有数据，显示提示
                if (items.isEmpty()) {
                    Toast.makeText(getContext(), "暂无发布的商品", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                // 隐藏加载指示器
                progressIndicator.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                
                Toast.makeText(getContext(), "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void onItemClick(TradeItem item) {
        // 跳转到详情页
        Intent intent = new Intent(getContext(), TradeDetailActivity.class);
        intent.putExtra("tradeId", item.getId());
        startActivity(intent);
    }
    
    private void onEditClick(TradeItem item) {
        // 跳转到编辑页面
        Intent intent = EditTradeActivity.newIntent(getContext(), item.getId());
        startActivity(intent);
    }
    
    private void onDeleteClick(TradeItem item) {
        // 显示确认对话框
        new AlertDialog.Builder(getContext())
                .setTitle("删除商品")
                .setMessage("确定要删除商品 \"" + item.getTitle() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteTradeItem(item);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void deleteTradeItem(TradeItem item) {
        // 显示加载指示器
        progressIndicator.setVisibility(View.VISIBLE);
        
        // 从Firestore中删除商品
        db.collection("trades").document(item.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // 从列表中移除商品
                    items.remove(item);
                    adapter.notifyDataSetChanged();
                    
                    // 隐藏加载指示器
                    progressIndicator.setVisibility(View.GONE);
                    
                    Toast.makeText(getContext(), "商品已删除", Toast.LENGTH_SHORT).show();
                    
                    // 如果没有数据，显示提示
                    if (items.isEmpty()) {
                        Toast.makeText(getContext(), "暂无发布的商品", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // 隐藏加载指示器
                    progressIndicator.setVisibility(View.GONE);
                    
                    Toast.makeText(getContext(), "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
} 