package hk.hku.cs.hkuers.features.forum.models;

import java.util.Date;

public class LostFoundItem {
    private String id;
    private String title;
    private String description;
    private String location;
    private Date date;
    private String contact;
    private String type; // "lost" or "found"
    private String imageUrl;
    private String userId;
    private String userName;
    private boolean isResolved; // 是否已解决
    private Date resolvedDate; // 解决日期
    private String resolvedBy; // 解决人ID
    private String resolvedByName; // 解决人姓名
    private String category; // 物品类别
    private String reward; // 酬谢信息

    public LostFoundItem(String id, String title, String description, String location,
                         Date date, String contact, String type, String imageUrl,
                         String userId, String userName, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.date = date;
        this.contact = contact;
        this.type = type;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.userName = userName;
        this.category = category;
        this.isResolved = false;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public Date getDate() { return date; }
    public String getContact() { return contact; }
    public String getType() { return type; }
    public String getImageUrl() { return imageUrl; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public boolean isResolved() { return isResolved; }
    public Date getResolvedDate() { return resolvedDate; }
    public String getResolvedBy() { return resolvedBy; }
    public String getResolvedByName() { return resolvedByName; }
    public String getCategory() { return category; }
    public String getReward() { return reward; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setDate(Date date) { this.date = date; }
    public void setContact(String contact) { this.contact = contact; }
    public void setType(String type) { this.type = type; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setResolved(boolean resolved) { this.isResolved = resolved; }
    public void setResolvedDate(Date resolvedDate) { this.resolvedDate = resolvedDate; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public void setResolvedByName(String resolvedByName) { this.resolvedByName = resolvedByName; }
    public void setCategory(String category) { this.category = category; }
    public void setReward(String reward) { this.reward = reward; }

    // 标记为已解决
    public void markAsResolved(String resolvedBy, String resolvedByName) {
        this.isResolved = true;
        this.resolvedDate = new Date();
        this.resolvedBy = resolvedBy;
        this.resolvedByName = resolvedByName;
    }

    // 转换为Map用于数据库存储
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("description", description);
        map.put("location", location);
        map.put("date", date);
        map.put("contact", contact);
        map.put("type", type);
        map.put("imageUrl", imageUrl);
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("isResolved", isResolved);
        map.put("resolvedDate", resolvedDate);
        map.put("resolvedBy", resolvedBy);
        map.put("resolvedByName", resolvedByName);
        map.put("category", category);
        map.put("reward", reward);
        return map;
    }
}
