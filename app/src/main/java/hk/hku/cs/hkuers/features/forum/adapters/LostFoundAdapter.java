package hk.hku.cs.hkuers.features.forum.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.models.LostFoundItem;

public class LostFoundAdapter extends RecyclerView.Adapter<LostFoundAdapter.ViewHolder> {
    private List<LostFoundItem> lostFoundList;

    public LostFoundAdapter(List<LostFoundItem> lostFoundList) {
        this.lostFoundList = lostFoundList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lost_found, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LostFoundItem item = lostFoundList.get(position);
        holder.titleTextView.setText(item.getTitle());
        holder.descriptionTextView.setText(item.getDescription());
        holder.locationTextView.setText("地点: " + item.getLocation());
        holder.dateTextView.setText("日期: " + item.getDate());
        holder.contactTextView.setText("联系方式: " + item.getContact());
    }

    @Override
    public int getItemCount() {
        return lostFoundList.size();
    }

    public void updateList(List<LostFoundItem> newList) {
        this.lostFoundList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView locationTextView;
        TextView dateTextView;
        TextView contactTextView;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tvTitle);
            descriptionTextView = itemView.findViewById(R.id.tvDescription);
            locationTextView = itemView.findViewById(R.id.tvLocation);
            dateTextView = itemView.findViewById(R.id.tvDate);
            contactTextView = itemView.findViewById(R.id.tvContact);
        }
    }
}
