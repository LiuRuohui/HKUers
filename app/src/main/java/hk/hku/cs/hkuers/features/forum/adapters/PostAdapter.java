package hk.hku.cs.hkuers.features.forum.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.models.Post;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> postList;
    private final String currentUserId;
    private final OnPostClickListener listener;
    private final FirebaseFirestore db;
    private boolean isProcessing = false; // 防止快速点击

    public interface OnPostClickListener {
        void onPostClick(Post post);
        void onLikeClick(Post post);
        void onCommentClick(Post post);
    }

    public PostAdapter(Context context, List<Post> postList, String currentUserId, OnPostClickListener listener) {
        this.context = context;
        this.postList = postList;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // 设置帖子内容
        holder.titleTextView.setText(post.getTitle());
        holder.authorTextView.setText(post.getAuthor());
        holder.dateTextView.setText(post.getTimestamp());
        holder.likesCountTextView.setText(post.getLikes());
        holder.replyCountTextView.setText(post.getComments());

        // 设置点赞按钮状态
        updateLikeButtonState(holder.likeButton, post);

        // 设置点赞按钮点击事件
        holder.likeButton.setOnClickListener(v -> {
            if (!isProcessing) {
                handlePostLike(post, holder);
            }
        });

        // 设置评论按钮点击事件
        holder.commentButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCommentClick(post);
            }
        });

        // 设置整个视图的点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            }
        });
    }

    private void updateLikeButtonState(ImageButton likeButton, Post post) {
        if (post.isLikedByUser(currentUserId)) {
            likeButton.setImageResource(R.drawable.ic_thumb_up_filled);
        } else {
            likeButton.setImageResource(R.drawable.ic_thumb_up);
        }
    }

    private void handlePostLike(Post post, PostViewHolder holder) {
        isProcessing = true;
        List<String> likedUsers = post.getLikedByUsers();

        // 为防止并发问题，创建副本
        final List<String> finalLikedUsers = new ArrayList<>(likedUsers);
        final String originalLikes = post.getLikes();
        final boolean wasLiked = likedUsers.contains(currentUserId);

        if (!wasLiked) {
            // 添加点赞
            likedUsers.add(currentUserId);
            int currentLikes = Integer.parseInt(post.getLikes());
            post.setLikes(String.valueOf(currentLikes + 1));
            holder.likeButton.setImageResource(R.drawable.ic_thumb_up_filled);
        } else {
            // 取消点赞
            likedUsers.remove(currentUserId);
            int currentLikes = Integer.parseInt(post.getLikes());
            post.setLikes(String.valueOf(Math.max(0, currentLikes - 1)));
            holder.likeButton.setImageResource(R.drawable.ic_thumb_up);
        }

        // 立即更新UI
        holder.likesCountTextView.setText(post.getLikes());

        // 更新Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("likes", post.getLikes());
        updates.put("likedByUsers", likedUsers);

        db.collection("forum_posts")
                .document(post.getId())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // 更新成功
                    if (listener != null) {
                        listener.onLikeClick(post);
                    }
                    isProcessing = false;
                })
                .addOnFailureListener(e -> {
                    // 更新失败，恢复状态
                    Toast.makeText(context, "操作失败，请稍后重试", Toast.LENGTH_SHORT).show();

                    // 恢复原始状态
                    likedUsers.clear();
                    likedUsers.addAll(finalLikedUsers);
                    post.setLikes(originalLikes);
                    updateLikeButtonState(holder.likeButton, post);
                    holder.likesCountTextView.setText(post.getLikes());

                    isProcessing = false;
                });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, authorTextView, dateTextView, likesCountTextView, replyCountTextView;
        ImageButton likeButton, commentButton;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.postTitle);
            authorTextView = itemView.findViewById(R.id.postAuthor);
            dateTextView = itemView.findViewById(R.id.postDate);
            likesCountTextView = itemView.findViewById(R.id.postLikesCount);
            replyCountTextView = itemView.findViewById(R.id.postReplyCount);
            likeButton = itemView.findViewById(R.id.btnLike);
            commentButton = itemView.findViewById(R.id.btnComment);
        }
    }

    public void updatePosts(List<Post> newPosts) {
        postList.clear();
        postList.addAll(newPosts);
        notifyDataSetChanged();
    }
}
