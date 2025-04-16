package hk.hku.cs.hkuers.models;

import com.google.firebase.Timestamp;

/**
 * 聊天室成员关系模型类
 * 用于表示用户与聊天室之间的关系
 */
public class ChatRoomMember {
    private String id;          // 文档ID
    private String userId;      // 用户ID
    private String chatRoomId;  // 聊天室ID
    private Timestamp joinedAt; // 加入时间
    private boolean isAdmin;    // 是否为管理员
    private Timestamp lastReadTime; // 最后阅读时间
    private long readMessageCount;  // 已读消息数量

    // 无参构造函数（Firestore需要）
    public ChatRoomMember() {}

    // 构造函数
    public ChatRoomMember(String userId, String chatRoomId, Timestamp joinedAt, boolean isAdmin) {
        this.userId = userId;
        this.chatRoomId = chatRoomId;
        this.joinedAt = joinedAt;
        this.isAdmin = isAdmin;
        this.lastReadTime = joinedAt;
        this.readMessageCount = 0;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public Timestamp getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(Timestamp lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public long getReadMessageCount() {
        return readMessageCount;
    }

    public void setReadMessageCount(long readMessageCount) {
        this.readMessageCount = readMessageCount;
    }
} 