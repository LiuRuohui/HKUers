package hk.hku.cs.hkuers.features.trade;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hk.hku.cs.hkuers.R;

/**
 * 商品列表适配器，用于显示交易商品列表
 * 可以处理不同格式的数据
 */
public class TradeAdapter extends RecyclerView.Adapter<TradeItemViewHolder> {
    private static final String TAG = "TradeAdapter";
    private List<TradeItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TradeItem item);
    }

    public TradeAdapter(List<TradeItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
        Log.d(TAG, "Adapter created with " + (items != null ? items.size() : 0) + " items");
    }

    @NonNull
    @Override
    public TradeItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trade, parent, false);
            return new TradeItemViewHolder(view);
        } catch (Exception e) {
            Log.e(TAG, "Error creating ViewHolder", e);
            // 如果视图创建失败，创建一个简单的备用视图
            View fallbackView = new View(parent.getContext());
            fallbackView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return new TradeItemViewHolder(fallbackView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull TradeItemViewHolder holder, int position) {
        try {
            TradeItem item = items.get(position);
            Log.d(TAG, "Binding item at position " + position + ": " + item.getTitle());

            // 设置标题
            if (holder.titleTextView != null) {
                holder.titleTextView.setText(item.getTitle());
            }

            // 设置价格
            if (holder.priceTextView != null) {
                holder.priceTextView.setText(String.format("HK$ %.2f", item.getPrice()));
            }

            // 设置卖家名称
            String sellerName = item.getSellerName();
            if (sellerName == null || sellerName.isEmpty()) {
                sellerName = "Anonymous";
            }
            if (holder.sellerTextView != null) {
                holder.sellerTextView.setText(sellerName);
            }

            // 设置分类
            try {
                String category = item.getCategory();
                if (category != null && !category.isEmpty() && holder.categoryTextView != null) {
                    holder.categoryTextView.setText(category);
                    holder.categoryTextView.setVisibility(View.VISIBLE);
                } else if (holder.categoryTextView != null) {
                    holder.categoryTextView.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting category text", e);
                if (holder.categoryTextView != null) {
                    holder.categoryTextView.setVisibility(View.GONE);
                }
            }

            // 设置点击事件
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error binding data", e);
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    /**
     * 更新适配器数据
     * @param newItems 新的商品列表
     */
    public void updateItems(List<TradeItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
        Log.d(TAG, "Updated items: " + (newItems != null ? newItems.size() : 0));
    }
} 