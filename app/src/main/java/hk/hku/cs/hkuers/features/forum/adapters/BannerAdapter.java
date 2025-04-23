package hk.hku.cs.hkuers.features.forum.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.models.BannerItem;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    private List<BannerItem> bannerItems;

    public BannerAdapter(List<BannerItem> bannerItems) {
        this.bannerItems = bannerItems;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        BannerItem item = bannerItems.get(position);
        holder.title.setText(item.getTitle());
        holder.image.setImageResource(item.getImageResId());
    }

    @Override
    public int getItemCount() {
        return bannerItems.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;

        BannerViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.bannerImage);
            title = itemView.findViewById(R.id.bannerTitle);
        }
    }
}
