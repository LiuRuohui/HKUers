package hk.hku.cs.hkuers.features.trade;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.List;

import hk.hku.cs.hkuers.R;

public class MyTradeAdapter extends RecyclerView.Adapter<MyTradeAdapter.MyTradeViewHolder> {
    private List<TradeItem> items;
    private OnItemClickListener listener;
    private OnEditClickListener editListener;
    private OnDeleteClickListener deleteListener;

    public interface OnItemClickListener {
        void onItemClick(TradeItem item);
    }

    public interface OnEditClickListener {
        void onEditClick(TradeItem item);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(TradeItem item);
    }

    public MyTradeAdapter(List<TradeItem> items,
                          OnItemClickListener listener,
                          OnEditClickListener editListener,
                          OnDeleteClickListener deleteListener) {
        this.items = items;
        this.listener = listener;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public MyTradeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_trade, parent, false);
        return new MyTradeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyTradeViewHolder holder, int position) {
        TradeItem item = items.get(position);
        holder.titleTextView.setText(item.getTitle());
        holder.priceTextView.setText(String.format("HK$ %.2f", item.getPrice()));
        
        // 设置分类文本
        final String category = item.getCategory() != null ? item.getCategory() : "未分类";
        // 使用多种方式设置Chip文本
        try {
            // 直接设置文本
            holder.categoryTextView.setText(category);
            // 确保内容描述也被设置
            holder.categoryTextView.setContentDescription(category);
            holder.categoryTextView.setVisibility(View.VISIBLE);
            
            // 使用延迟确保UI已更新
            holder.categoryTextView.post(() -> {
                try {
                    // 再次设置文本，以确保它显示
                    holder.categoryTextView.setText(category);
                    System.out.println("MyTradeAdapter - Category Chip Text (post): " + holder.categoryTextView.getText());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            
            // 日志输出分类文本
            System.out.println("MyTradeAdapter - Category: " + category);
            System.out.println("MyTradeAdapter - Category Chip Text: " + holder.categoryTextView.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });

        holder.editButton.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEditClick(item);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class MyTradeViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView priceTextView;
        Chip categoryTextView;
        MaterialButton editButton;
        MaterialButton deleteButton;

        MyTradeViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.textViewTitle);
            priceTextView = itemView.findViewById(R.id.textViewPrice);
            categoryTextView = itemView.findViewById(R.id.chipCategory);
            editButton = itemView.findViewById(R.id.buttonEdit);
            deleteButton = itemView.findViewById(R.id.buttonDelete);
        }
    }
} 