package hk.hku.cs.hkuers.features.profile;

import android.content.Context;
import android.view.View;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * 用户资料浮窗管理器 - 单例模式
 * 可以在应用的任何地方轻松调用用户资料浮窗
 */
public class UserProfileManager {

    private static UserProfileManager instance;
    private ProfilePopupHelper popupHelper;
    private FirebaseFirestore db;
    
    // 私有构造函数
    private UserProfileManager() {
        db = FirebaseFirestore.getInstance();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized UserProfileManager getInstance() {
        if (instance == null) {
            instance = new UserProfileManager();
        }
        return instance;
    }
    
    /**
     * 通过用户ID显示用户资料浮窗
     * 
     * @param context 上下文
     * @param anchorView 锚点视图
     * @param userId 用户ID
     * @param chatRoomId 聊天室ID（可选，如不在聊天室中可传null）
     * @param chatRoomName 聊天室名称（可选，如不在聊天室中可传null）
     */
    public void showUserProfile(Context context, View anchorView, String userId, 
                              String chatRoomId, String chatRoomName) {
        // 确保上下文有效
        if (context == null || anchorView == null) return;
        
        // 初始化或更新弹窗助手
        if (popupHelper == null || !context.equals(popupHelper.getContext())) {
            popupHelper = new ProfilePopupHelper(context);
        }
        
        // 如果已有用户数据则直接显示
        if (popupHelper.isShowing()) {
            popupHelper.dismissPopup();
        }
        
        // 从Firestore加载用户数据
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // 显示用户资料浮窗
                    popupHelper.showProfilePopup(
                            anchorView, userId, documentSnapshot, chatRoomId, chatRoomName);
                }
            })
            .addOnFailureListener(e -> {
                // 处理错误情况
                android.util.Log.e("UserProfileManager", "加载用户数据失败: " + e.getMessage(), e);
            });
    }
    
    /**
     * 通过用户文档显示用户资料浮窗（适用于已有用户数据的情况）
     * 
     * @param context 上下文
     * @param anchorView 锚点视图
     * @param userId 用户ID
     * @param userDoc 用户文档对象
     * @param chatRoomId 聊天室ID（可选，如不在聊天室中可传null）
     * @param chatRoomName 聊天室名称（可选，如不在聊天室中可传null）
     */
    public void showUserProfile(Context context, View anchorView, String userId, 
                              DocumentSnapshot userDoc, String chatRoomId, String chatRoomName) {
        // 确保上下文有效
        if (context == null || anchorView == null) return;
        
        // 初始化或更新弹窗助手
        if (popupHelper == null || !context.equals(popupHelper.getContext())) {
            popupHelper = new ProfilePopupHelper(context);
        }
        
        // 如果已有浮窗正在显示，先关闭
        if (popupHelper.isShowing()) {
            popupHelper.dismissPopup();
        }
        
        // 显示用户资料浮窗
        popupHelper.showProfilePopup(
                anchorView, userId, userDoc, chatRoomId, chatRoomName);
    }
    
    /**
     * 关闭当前显示的浮窗
     */
    public void dismissUserProfile() {
        if (popupHelper != null) {
            popupHelper.dismissPopup();
        }
    }
    
    /**
     * 判断浮窗是否正在显示
     */
    public boolean isProfileShowing() {
        return popupHelper != null && popupHelper.isShowing();
    }
} 