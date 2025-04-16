package hk.hku.cs.hkuers.models;

import com.google.firebase.Timestamp;

public class ChatGroup {
    private String groupId;          // 群组ID（Firestore文档ID）
    private String groupName;        // 群组名称（如课程代码COMP7903）
    private String lastMessage;      // 最后一条消息内容
    private Timestamp lastMessageTimestamp; // 最后消息时间
    private int unreadCount;         // 未读消息数量

    // 必须有无参构造函数
    public ChatGroup() {
        this.unreadCount = 0;
    }

    public ChatGroup(String groupId, String groupName, String lastMessage, Timestamp lastMessageTimestamp) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = 0;
    }

    public ChatGroup(String groupId, String groupName, String lastMessage, Timestamp lastMessageTimestamp, int unreadCount) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
    }

    // Getter & Setter
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    // 辅助方法：格式化时间戳
    public String getFormattedTimestamp() {
        if (lastMessageTimestamp == null) {
            return "";
        }
        return new java.text.SimpleDateFormat("HH:mm").format(lastMessageTimestamp.toDate());
    }
}