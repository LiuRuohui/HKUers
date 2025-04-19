package hk.hku.cs.hkuers.features.trade;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import hk.hku.cs.hkuers.R;

public class TradeItemViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "TradeItemViewHolder";
    
    public TextView titleTextView;
    public TextView priceTextView;
    public TextView sellerTextView;
    public CardView categoryCardView;
    public TextView categoryTextView;

    public TradeItemViewHolder(@NonNull View itemView) {
        super(itemView);
        try {
            titleTextView = itemView.findViewById(R.id.textViewTitle);
            priceTextView = itemView.findViewById(R.id.textViewPrice);
            sellerTextView = itemView.findViewById(R.id.textViewSeller);
            
            // 直接使用正确的ID查找categoryTextView
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            
            // 日志输出所有视图的状态
            Log.d(TAG, "Views initialized: " +
                    "titleTextView=" + (titleTextView != null) +
                    ", priceTextView=" + (priceTextView != null) +
                    ", sellerTextView=" + (sellerTextView != null) +
                    ", categoryTextView=" + (categoryTextView != null));
            
            if (categoryTextView == null) {
                Log.e(TAG, "CRITICAL: categoryTextView not found in layout! Check layout XML file.");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ViewHolder", e);
        }
    }
} 