package hk.hku.cs.hkuers.models;

import com.google.firebase.Timestamp;

public class ChatGroup {
    private String groupId;          // 群组ID（Firestore文档ID）
    private String groupName;        // 群组名称（如课程代码COMP7903）
    private String lastMessage;      // 最后一条消息内容
    private Timestamp lastMessageTimestamp; // 最后消息时间
    private int unreadCount;         // 未读消息数量
    private String groupType;        // 群组类型：NORMAL(普通群聊), COURSE(课程群聊), TRADE(购物聊天)

    // 定义常量
    public static final String TYPE_NORMAL = "NORMAL";  // 普通群聊
    public static final String TYPE_COURSE = "COURSE";  // 课程群聊
    public static final String TYPE_TRADE = "TRADE";    // 购物聊天

    // 必须有无参构造函数
    public ChatGroup() {
        this.unreadCount = 0;
        this.groupType = TYPE_NORMAL; // 默认为普通群聊
    }

    public ChatGroup(String groupId, String groupName, String lastMessage, Timestamp lastMessageTimestamp) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = 0;
        this.groupType = TYPE_NORMAL;
    }

    public ChatGroup(String groupId, String groupName, String lastMessage, Timestamp lastMessageTimestamp, int unreadCount) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
        this.groupType = TYPE_NORMAL;
    }
    
    public ChatGroup(String groupId, String groupName, String lastMessage, Timestamp lastMessageTimestamp, 
                    int unreadCount, String groupType) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
        this.groupType = groupType;
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
    
    public String getGroupType() {
        return groupType;
    }
    
    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }
    
    public boolean isNormalGroup() {
        return TYPE_NORMAL.equals(groupType);
    }
    
    public boolean isCourseGroup() {
        return TYPE_COURSE.equals(groupType);
    }
    
    public boolean isTradeGroup() {
        return TYPE_TRADE.equals(groupType);
    }

    // 辅助方法：格式化时间戳
    public String getFormattedTimestamp() {
        if (lastMessageTimestamp == null) {
            return "";
        }
        return new java.text.SimpleDateFormat("HH:mm").format(lastMessageTimestamp.toDate());
    }
}