package hk.hku.cs.hkuers.features.trade;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;

@IgnoreExtraProperties
public class TradeItem implements Serializable {
    private static final String TAG = "TradeItem";
    
    private String id;
    private String title;
    private String description;
    private double price;
    private String sellerId;
    private String sellerName;
    private Timestamp createTime;
    private String category;
    private String status;
    private String imageUrl;
    private long timestamp; // 添加timestamp字段用于兼容旧数据
    private boolean isFavorite;

    // 必须有一个空的默认构造函数，供Firebase序列化使用
    public TradeItem() {
        Log.d(TAG, "创建空的 TradeItem 对象");
        this.createTime = Timestamp.now();
        this.status = "available";
        this.price = 0.0;
        this.isFavorite = false;
    }

    public TradeItem(String title, String description, double price, 
                    String sellerId, String sellerName, String category) {
        Log.d(TAG, "创建 TradeItem 对象: " + title);
        this.title = title;
        this.description = description;
        this.price = price;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.category = category;
        this.createTime = Timestamp.now();
        this.timestamp = System.currentTimeMillis();
        this.status = "available";
        this.isFavorite = false;
    }

    // Getters and Setters
    @Exclude
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName != null ? sellerName : "Anonymous";
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    // 支持字段名createTime或timestamp
    public Timestamp getCreateTime() {
        // 如果createTime为空但timestamp有值，则通过timestamp创建一个Timestamp
        if (createTime == null && timestamp > 0) {
            return new Timestamp(timestamp / 1000, (int)((timestamp % 1000) * 1000000));
        }
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
        // 同时更新timestamp字段以保持一致性
        if (createTime != null) {
            this.timestamp = createTime.getSeconds() * 1000 + createTime.getNanoseconds() / 1000000;
        }
    }

    // 添加对timestamp字段的支持
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        // 同时更新createTime字段以保持一致性
        if (this.createTime == null) {
            this.createTime = new Timestamp(timestamp / 1000, (int)((timestamp % 1000) * 1000000));
        }
    }

    public String getCategory() {
        return category != null ? category : "Other";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status != null ? status : "available";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean getIsFavorite() {
        return isFavorite;
    }
    
    public void setIsFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    @Override
    public String toString() {
        return "TradeItem{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", sellerId='" + sellerId + '\'' +
                ", sellerName='" + (sellerName != null ? sellerName : "Anonymous") + '\'' +
                ", createTime=" + createTime +
                ", timestamp=" + timestamp +
                ", category='" + category + '\'' +
                ", status='" + status + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", isFavorite=" + isFavorite +
                '}';
    }
} 