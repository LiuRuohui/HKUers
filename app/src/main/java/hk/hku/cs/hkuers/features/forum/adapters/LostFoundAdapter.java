package hk.hku.cs.hkuers.features.forum.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.models.LostFoundItem;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LostFoundAdapter extends RecyclerView.Adapter<LostFoundAdapter.ViewHolder> {
    private Context context;
    private List<LostFoundItem> items;
    private OkHttpClient client;
    private Handler mainHandler;

    public LostFoundAdapter(Context context, List<LostFoundItem> items) {
        this.context = context;
        this.items = items;
        this.mainHandler = new Handler(Looper.getMainLooper());

        // 初始化 OkHttpClient
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lost_found, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LostFoundItem item = items.get(position);

        // 设置文本内容
        holder.titleView.setText(item.getTitle());
        holder.descriptionView.setText(item.getDescription());
        holder.locationView.setText("Location: " + item.getLocation());
        holder.dateView.setText("Date: " + item.getDate());
        holder.typeView.setText("Type: " + item.getType());
        holder.categoryView.setText("Category: " + item.getCategory());

        // 处理解决状态
        if (item.isResolved()) {
            holder.resolvedView.setText("Resolved");
            holder.resolvedView.setVisibility(View.VISIBLE);
        } else {
            holder.resolvedView.setVisibility(View.GONE);
        }

        // 加载图片
        loadImage(item.getImageUrl(), holder.imageView, holder.progressBar);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder 类
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleView;
        TextView descriptionView;
        TextView locationView;
        TextView dateView;
        TextView typeView;
        TextView categoryView;
        TextView resolvedView;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            titleView = itemView.findViewById(R.id.title);
            descriptionView = itemView.findViewById(R.id.description);
            locationView = itemView.findViewById(R.id.location);
            dateView = itemView.findViewById(R.id.date);
            typeView = itemView.findViewById(R.id.type);
            categoryView = itemView.findViewById(R.id.category);
            resolvedView = itemView.findViewById(R.id.resolved);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    // 图片加载方法
    private void loadImage(String imageUrl, ImageView imageView, ProgressBar progressBar) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.placeholder);
            progressBar.setVisibility(View.GONE);
            return;
        }

        // 添加时间戳和随机参数，确保不使用缓存
        String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
        String fullImageUrl = "http://10.0.2.2:9000/image/" + imageUrl + "?nocache=" + uniqueParam;

        // 显示加载进度条
        progressBar.setVisibility(View.VISIBLE);

        Request request = new Request.Builder()
                .url(fullImageUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    imageView.setImageResource(R.drawable.error_placeholder);
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (InputStream inputStream = response.body().byteStream()) {
                        final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        mainHandler.post(() -> {
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap);
                            } else {
                                imageView.setImageResource(R.drawable.error_placeholder);
                            }
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                } else {
                    mainHandler.post(() -> {
                        imageView.setImageResource(R.drawable.error_placeholder);
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });
    }
}
