package hk.hku.cs.hkuers.features.forum.models;

import java.util.Map;
import java.util.HashMap;

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

    public Post() {
        // 空的构造函数是必需的，用于 Firestore 的反序列化
    }

    public Post(String id, String title, String content, String author, String authorId, String timestamp, String likes, String comments, String boardType) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.authorId = authorId;
        this.timestamp = timestamp;
        this.likes = likes;
        this.comments = comments;
        this.boardType = boardType;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getLikes() { return likes; }
    public void setLikes(String likes) { this.likes = likes; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getBoardType() { return boardType; }
    public void setBoardType(String boardType) { this.boardType = boardType; }

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
        return map;
    }
}
