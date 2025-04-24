package hk.hku.cs.hkuers.features.forum.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.models.Post;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<Post> postList;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        if (post != null) {
            holder.titleTextView.setText(post.getTitle() != null ? post.getTitle() : "");
            holder.authorTextView.setText(post.getAuthor() != null ? post.getAuthor() : "");
            holder.publishTimeTextView.setText(post.getTimestamp() != null ? post.getTimestamp() : "");
            holder.replyCountTextView.setText(post.getComments() != null ? post.getComments() : "0");
            holder.readCountTextView.setText(post.getLikes() != null ? post.getLikes() : "0");
            // 移除 categoryTextView 的引用，因为布局中没有这个视图
        }
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView authorTextView;
        TextView publishTimeTextView;
        TextView replyCountTextView;
        TextView readCountTextView;

        PostViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.postTitle);
            authorTextView = itemView.findViewById(R.id.postAuthor);
            publishTimeTextView = itemView.findViewById(R.id.postDate);
            replyCountTextView = itemView.findViewById(R.id.postReplyCount);
            readCountTextView = itemView.findViewById(R.id.postViewsCount);
        }
    }
}
