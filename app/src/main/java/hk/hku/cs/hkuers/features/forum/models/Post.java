package hk.hku.cs.hkuers.features.forum.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Post {
    private String id;
    private String title;
    private String content;
    private String author;
    private String authorId;
    private String timestamp;
    private String likes;
    private String comments;
    private String boardType;
    private List<String> likedByUsers;
    private String imageUrl; // 新增：帖子图片URL

    // 空构造函数，用于Firestore
    public Post() {
        // 初始化默认值，避免空指针异常
        this.likes = "0";
        this.comments = "0";
        this.likedByUsers = new ArrayList<>();
    }

    // 构造函数
    public Post(String id, String title, String content, String author, String authorId,
                String timestamp, String boardType) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.authorId = authorId;
        this.timestamp = timestamp;
        this.likes = "0";
        this.comments = "0";
        this.boardType = boardType;
        this.likedByUsers = new ArrayList<>();
    }

    // 将Post对象转换为Map，用于Firestore存储
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("content", content);
        map.put("author", author);
        map.put("authorId", authorId);
        map.put("timestamp", timestamp);
        map.put("likes", likes);
        map.put("comments", comments);
        map.put("boardType", boardType);
        map.put("likedByUsers", likedByUsers);
        if (imageUrl != null) {
            map.put("imageUrl", imageUrl);
        }
        return map;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLikes() {
        return likes != null ? likes : "0";
    }

    public void setLikes(String likes) {
        this.likes = likes;
    }

    public String getComments() {
        return comments != null ? comments : "0";
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getBoardType() {
        return boardType;
    }

    public void setBoardType(String boardType) {
        this.boardType = boardType;
    }

    public List<String> getLikedByUsers() {
        if (likedByUsers == null) {
            likedByUsers = new ArrayList<>();
        }
        return likedByUsers;
    }

    public void setLikedByUsers(List<String> likedByUsers) {
        this.likedByUsers = likedByUsers;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isLikedByUser(String userId) {
        return getLikedByUsers().contains(userId);
    }

    @Override
    public String toString() {
        return "Post{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : "null") + '\'' +
                ", author='" + author + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", likes='" + likes + '\'' +
                ", comments='" + comments + '\'' +
                ", boardType='" + boardType + '\'' +
                '}';
    }
}
