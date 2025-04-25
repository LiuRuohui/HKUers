package hk.hku.cs.hkuers.models;

import com.google.firebase.Timestamp;

public class LostFound {
    private String id;
    private String userId;
    private String userName;
    private String title;
    private String description;
    private String location;
    private String imageUrl;
    private String category;
    private String type; // "lost" or "found"
    private String contact;
    private Timestamp date;
    private boolean resolved;

    public LostFound() {
        // Required empty constructor for Firestore
    }

    public LostFound(String userId, String userName, String title, String description, 
                    String location, String imageUrl, String category, String type, 
                    String contact, Timestamp date) {
        this.userId = userId;
        this.userName = userName;
        this.title = title;
        this.description = description;
        this.location = location;
        this.imageUrl = imageUrl;
        this.category = category;
        this.type = type;
        this.contact = contact;
        this.date = date;
        this.resolved = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
} 