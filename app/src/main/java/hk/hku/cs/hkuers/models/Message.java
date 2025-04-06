package hk.hku.cs.hkuers.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class Message {
    private String senderId;     // 发送者ID
    private String senderEmail;  // 发送者邮箱
    private String text;         // 消息内容
    @ServerTimestamp
    private Timestamp timestamp; // 时间戳（Firestore自动填充）

    // 必须有无参构造函数（Firestore反序列化需要）
    public Message() {}

    public Message(String senderId, String senderEmail, String text, Timestamp timestamp) {
        this.senderId = senderId;
        this.senderEmail = senderEmail;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getter & Setter
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}