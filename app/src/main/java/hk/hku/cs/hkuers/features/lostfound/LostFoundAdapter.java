package hk.hku.cs.hkuers.features.lostfound;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.models.LostFound;

public class LostFoundAdapter extends RecyclerView.Adapter<LostFoundAdapter.ViewHolder> {
    private static final String TAG = "LostFoundAdapter";
    private List<LostFound> items;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(LostFound item);
    }

    public LostFoundAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.items = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lost_found, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LostFound item = items.get(position);
        holder.titleText.setText(item.getTitle());
        holder.descriptionText.setText(item.getDescription());
        holder.locationText.setText(item.getLocation());
        holder.categoryText.setText(item.getCategory());
        holder.typeText.setText(item.getType());
        holder.contactText.setText(item.getContact());
        holder.timestampText.setText(formatTimestamp(item.getDate()));

        // Load image using Glide
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            if (item.getImageUrl().startsWith("forum/")) {
                // Load from resources
                String resourceName = item.getImageUrl()
                    .replace("forum/", "")
                    .replace(".png", "")
                    .replace(".jpg", "");
                int resourceId = context.getResources().getIdentifier(
                    resourceName,
                    "drawable",
                    context.getPackageName()
                );
                if (resourceId != 0) {
                    Glide.with(context)
                        .load(resourceId)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(holder.imageView);
                } else {
                    holder.imageView.setImageResource(R.drawable.placeholder_image);
                }
            } else {
                // Load from URL
                Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.imageView);
            }
        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_image);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<LostFound> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void addItem(LostFound item) {
        this.items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void updateItem(LostFound item) {
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(item.getId())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            items.set(index, item);
            notifyItemChanged(index);
        }
    }

    private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleText, descriptionText, locationText, timestampText, typeText, categoryText, contactText;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            titleText = itemView.findViewById(R.id.title);
            descriptionText = itemView.findViewById(R.id.description);
            locationText = itemView.findViewById(R.id.location);
            timestampText = itemView.findViewById(R.id.date);
            typeText = itemView.findViewById(R.id.type);
            categoryText = itemView.findViewById(R.id.category);
            contactText = itemView.findViewById(R.id.contact);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
} 