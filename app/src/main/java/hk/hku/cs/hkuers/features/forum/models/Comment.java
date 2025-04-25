package hk.hku.cs.hkuers.features.forum.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Comment {
    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String content;
    private String timestamp;
    private int likesCount; // 评论点赞数量
    private List<String> likedBy;

    public Comment() {
        // 空构造函数，Firebase需要
        this.likedBy = new ArrayList<>();
        this.likesCount = 0;
    }

    public Comment(String authorId, String authorName, String content, String timestamp) {
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
        this.timestamp = timestamp;
        this.likedBy = new ArrayList<>();
        this.likesCount = 0;
    }

    /**
     * 转换为Map，用于Firestore存储
     */
    public Map<String, Object> toMap() {
        Map<String, Object> commentMap = new HashMap<>();
        commentMap.put("authorId", authorId);
        commentMap.put("authorName", authorName);
        commentMap.put("content", content);
        commentMap.put("timestamp", timestamp);
        commentMap.put("likesCount", likesCount);
        if (authorAvatar != null) {
            commentMap.put("authorAvatar", authorAvatar);
        }
        return commentMap;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public List<String> getLikedBy() {
        return likedBy;
    }

    public void setLikedBy(List<String> likedBy) {
        this.likedBy = likedBy;
    }

    // 检查用户是否已经点赞
    public boolean isLikedBy(String userId) {
        return likedBy != null && likedBy.contains(userId);
    }

    // 添加点赞
    public void addLike(String userId) {
        if (!isLikedBy(userId)) {
            likedBy.add(userId);
            likesCount++;
        }
    }

    // 取消点赞
    public void removeLike(String userId) {
        if (isLikedBy(userId)) {
            likedBy.remove(userId);
            likesCount--;
        }
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id='" + id + '\'' +
                ", authorId='" + authorId + '\'' +
                ", authorName='" + authorName + '\'' +
                ", content='" + content + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", likesCount=" + likesCount +
                '}';
    }
}
