package hk.hku.cs.hkuers.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户状态帮助类
 * 用于更新用户状态相关信息
 */
public class UserStatusHelper {

    private static final String TAG = "UserStatusHelper";

    /**
     * 更新用户最后活跃时间
     */
    public static void updateUserLastActive() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("last_active", new Timestamp(new Date()));

        db.collection("users").document(currentUser.getUid())
            .update(updates)
            .addOnFailureListener(e -> 
                android.util.Log.e(TAG, "更新用户活跃状态失败: " + e.getMessage())
            );
    }
} 