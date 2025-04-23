package hk.hku.cs.hkuers.features.forum.models;

public class Post {
    private String id;
    private String title;
    private String content;
    private String author;
    private String date;
    private int replyCount;
    private int viewCount;
    private String category;

    public Post() {
        // 无参构造函数，用于Firestore反序列化
    }

    public Post(String id, String title, String content, String author, String date, int replyCount, int viewCount, String category) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.date = date;
        this.replyCount = replyCount;
        this.viewCount = viewCount;
        this.category = category;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
