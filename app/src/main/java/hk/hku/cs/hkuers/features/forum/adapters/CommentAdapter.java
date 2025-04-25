package hk.hku.cs.hkuers.features.forum.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.models.Comment;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final Context context;
    private final List<Comment> commentList;
    private final String currentUserId;
    private final OnCommentActionListener listener;

    public interface OnCommentActionListener {
        void onCommentLike(Comment comment);
        void onCommentLongClick(Comment comment);
        void onAuthorClick(String authorId, String authorName);
    }

    public CommentAdapter(Context context, List<Comment> commentList, String currentUserId, OnCommentActionListener listener) {
        this.context = context;
        this.commentList = commentList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        // 设置评论作者信息
        holder.authorTextView.setText(comment.getAuthorName());

        // 设置评论时间（相对时间格式，如"5分钟前"）
        try {
            String relativeTime = getRelativeTimeSpan(comment.getTimestamp());
            holder.dateTextView.setText(relativeTime);
        } catch (Exception e) {
            holder.dateTextView.setText(comment.getTimestamp());
        }

        // 设置评论内容
        holder.contentTextView.setText(comment.getContent());

        // 加载作者头像（如果有）
        if (comment.getAuthorAvatar() != null && !comment.getAuthorAvatar().isEmpty()) {
            Glide.with(context)
                    .load(comment.getAuthorAvatar())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(holder.avatarImageView);
        } else {
            holder.avatarImageView.setImageResource(R.drawable.default_avatar);
        }

        // 当前用户的评论显示不同的背景
        if (currentUserId != null && currentUserId.equals(comment.getAuthorId())) {
            holder.itemView.setBackgroundResource(R.drawable.bg_my_comment);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_comment);
        }

        // 设置长按事件（删除评论等操作）
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onCommentLongClick(comment);
                return true;
            }
            return false;
        });

        // 设置作者点击事件（查看作者信息）
        holder.authorLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAuthorClick(comment.getAuthorId(), comment.getAuthorName());
            }
        });

        // 更新点赞按钮状态
        boolean isLiked = comment.isLikedBy(currentUserId);
        holder.likeButton.setImageResource(isLiked ? R.drawable.ic_thumb_up_filled : R.drawable.ic_thumb_up);

        holder.likeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCommentLike(comment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView, dateTextView, contentTextView;
        ImageView avatarImageView;
        View authorLayout;
        ImageButton likeButton;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.commentAuthor);
            dateTextView = itemView.findViewById(R.id.commentDate);
            contentTextView = itemView.findViewById(R.id.commentContent);
            avatarImageView = itemView.findViewById(R.id.commentAuthorAvatar);
            authorLayout = itemView.findViewById(R.id.authorLayout);
            likeButton = itemView.findViewById(R.id.button_like);
        }
    }

    /**
     * 获取相对时间（如"5分钟前"）
     */
    private String getRelativeTimeSpan(String timestamp) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = sdf.parse(timestamp);
        long time = date.getTime();
        long now = System.currentTimeMillis();

        // 使用 Android 的 DateUtils 格式化相对时间
        return DateUtils.getRelativeTimeSpanString(
                time, now, DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE).toString();
    }

    /**
     * 添加评论到列表
     */
    public void addComment(Comment comment) {
        commentList.add(comment);
        notifyItemInserted(commentList.size() - 1);
    }

    /**
     * 删除评论
     */
    public void removeComment(int position) {
        if (position >= 0 && position < commentList.size()) {
            commentList.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * 更新评论列表
     */
    public void updateComments(List<Comment> newComments) {
        commentList.clear();
        commentList.addAll(newComments);
        notifyDataSetChanged();
    }
}
