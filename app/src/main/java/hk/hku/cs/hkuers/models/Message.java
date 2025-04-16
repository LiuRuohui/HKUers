package hk.hku.cs.hkuers.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private String id;              // 消息ID
    private String senderId;        // 发送者ID（系统消息可使用"system"）
    private String text;            // 消息内容
    private String type;            // 消息类型：text, system, announcement
    @ServerTimestamp
    private Timestamp timestamp;    // 发送时间
    private Map<String, Boolean> readStatus; // 用户读取状态

    // 必须有无参构造函数（Firestore反序列化需要）
    public Message() {
        this.readStatus = new HashMap<>();
        this.type = "text"; // 默认文本类型
    }

    public Message(String senderId, String text, Timestamp timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.readStatus = new HashMap<>();
        this.type = "text"; // 默认文本类型
    }
    
    public Message(String senderId, String text, String type, Timestamp timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.type = type;
        this.timestamp = timestamp;
        this.readStatus = new HashMap<>();
    }

    // Getter & Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Boolean> getReadStatus() {
        return readStatus;
    }
    
    public void setReadStatus(Map<String, Boolean> readStatus) {
        this.readStatus = readStatus;
    }
    
    // 标记用户已读
    public void markAsReadBy(String userId) {
        if (this.readStatus == null) {
            this.readStatus = new HashMap<>();
        }
        this.readStatus.put(userId, true);
    }
    
    // 获取已读人数
    public int getReadCount() {
        if (this.readStatus == null) {
            return 0;
        }
        int count = 0;
        for (Boolean isRead : readStatus.values()) {
            if (isRead) {
                count++;
            }
        }
        return count;
    }
}