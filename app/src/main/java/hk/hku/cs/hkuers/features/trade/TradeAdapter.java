package hk.hku.cs.hkuers.features.trade;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
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
            
            // 获取商品图片
            View itemView = holder.itemView;
            if (itemView != null) {
                Context context = itemView.getContext();
                ImageView imageView = itemView.findViewById(R.id.imageView);
                ProgressBar progressBar = itemView.findViewById(R.id.imageLoadingProgress);
                
                if (imageView != null && progressBar != null) {
                    // 显示加载状态
                    progressBar.setVisibility(View.VISIBLE);
                    
                    // 加载图片
                    String imageUrl = item.getImageUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        // 处理不同类型的图片URI
                        if (imageUrl.startsWith("android.resource://")) {
                            // 处理本地drawable资源URI
                            try {
                                Uri resourceUri = Uri.parse(imageUrl);
                                Glide.with(context)
                                    .load(resourceUri)
                                    .apply(new RequestOptions()
                                        .placeholder(R.drawable.default_avatar)
                                        .error(R.drawable.default_avatar))
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(imageView);
                                
                                progressBar.setVisibility(View.GONE);
                                Log.d(TAG, "Loaded local resource image: " + imageUrl);
                            } catch (Exception e) {
                                Log.e(TAG, "Error loading local resource image", e);
                                progressBar.setVisibility(View.GONE);
                                imageView.setImageResource(R.drawable.default_avatar);
                            }
                        } else if (imageUrl.startsWith("content://")) {
                            // 处理设备相册的Content URI
                            try {
                                Uri contentUri = Uri.parse(imageUrl);
                                Glide.with(context)
                                    .load(contentUri)
                                    .apply(new RequestOptions()
                                        .placeholder(R.drawable.default_avatar)
                                        .error(R.drawable.default_avatar))
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(imageView);
                                
                                progressBar.setVisibility(View.GONE);
                                Log.d(TAG, "Loaded content URI image: " + imageUrl);
                            } catch (Exception e) {
                                Log.e(TAG, "Error loading content URI image", e);
                                progressBar.setVisibility(View.GONE);
                                imageView.setImageResource(R.drawable.default_avatar);
                            }
                        } else if (imageUrl.startsWith("file://")) {
                            // 处理本地文件URI (从应用私有存储读取)
                            try {
                                Uri fileUri = Uri.parse(imageUrl);
                                File file = new File(fileUri.getPath());
                                Log.d(TAG, "Loading file URI: " + fileUri.getPath() + ", exists: " + file.exists());
                                
                                if (file.exists()) {
                                    Glide.with(context)
                                        .load(file)
                                        .apply(new RequestOptions()
                                            .placeholder(R.drawable.default_avatar)
                                            .error(R.drawable.default_avatar))
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .into(imageView);
                                    
                                    progressBar.setVisibility(View.GONE);
                                    Log.d(TAG, "Loaded file image: " + imageUrl);
                                } else {
                                    Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
                                    progressBar.setVisibility(View.GONE);
                                    imageView.setImageResource(R.drawable.default_avatar);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error loading file URI image", e);
                                progressBar.setVisibility(View.GONE);
                                imageView.setImageResource(R.drawable.default_avatar);
                            }
                        } else if (imageUrl.startsWith("http")) {
                            // 远程URL（如Firebase Storage的下载链接）
                            try {
                                Glide.with(context)
                                    .load(imageUrl)
                                    .apply(new RequestOptions()
                                        .placeholder(R.drawable.default_avatar)
                                        .error(R.drawable.default_avatar))
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(imageView);
                                
                                progressBar.setVisibility(View.GONE);
                                Log.d(TAG, "加载Firebase Storage图片: " + imageUrl);
                            } catch (Exception e) {
                                Log.e(TAG, "加载远程图片失败", e);
                                progressBar.setVisibility(View.GONE);
                                imageView.setImageResource(R.drawable.default_avatar);
                            }
                        } else {
                            // 其他未知格式的URI，显示默认图片
                            progressBar.setVisibility(View.GONE);
                            imageView.setImageResource(R.drawable.default_avatar);
                            Log.w(TAG, "Unknown image URI format: " + imageUrl);
                        }
                    } else {
                        // 无图片URL，显示默认图片
                        progressBar.setVisibility(View.GONE);
                        imageView.setImageResource(R.drawable.default_avatar);
                    }
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