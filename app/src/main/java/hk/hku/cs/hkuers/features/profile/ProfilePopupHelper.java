package hk.hku.cs.hkuers.features.profile;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.firestore.DocumentSnapshot;

import de.hdodenhof.circleimageview.CircleImageView;
import hk.hku.cs.hkuers.R;

/**
 * 用户资料浮窗助手类，用于显示用户资料的简略浮窗
 */
public class ProfilePopupHelper {

    private static final String TAG = "ProfilePopupHelper";
    private static final String SERVER_URL = "http://10.0.2.2:9000";
    
    private Context context;
    private PopupWindow popupWindow;
    
    // UI组件
    private CircleImageView ivPopupAvatar;
    private TextView tvPopupUsername;
    private TextView tvPopupEmail;
    private TextView tvPopupDepartment;
    private TextView tvPopupProgramme;
    private TextView tvPopupYearOfEntry;
    private TextView tvPopupSignature;
    
    // 用户数据
    private String userId;
    private DocumentSnapshot userDoc;
    private String chatRoomId;
    private String chatRoomName;
    
    // 背景遮罩
    private View backgroundDimmer;
    
    public ProfilePopupHelper(Context context) {
        this.context = context;
        initPopupWindow();
    }
    
    /**
     * 初始化弹出窗口
     */
    private void initPopupWindow() {
        // 创建视图
        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_user_profile, null);
        
        // 初始化控件
        ivPopupAvatar = popupView.findViewById(R.id.ivPopupAvatar);
        tvPopupUsername = popupView.findViewById(R.id.tvPopupUsername);
        tvPopupEmail = popupView.findViewById(R.id.tvPopupEmail);
        tvPopupDepartment = popupView.findViewById(R.id.tvPopupDepartment);
        tvPopupProgramme = popupView.findViewById(R.id.tvPopupProgramme);
        tvPopupYearOfEntry = popupView.findViewById(R.id.tvPopupYearOfEntry);
        tvPopupSignature = popupView.findViewById(R.id.tvPopupSignature);
        
        // 创建背景遮罩
        backgroundDimmer = new View(context);
        backgroundDimmer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        backgroundDimmer.setBackgroundColor(0x99000000); // 半透明黑色
        
        // 创建PopupWindow - 使用MATCH_PARENT宽度，但有边距
        popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        
        // 设置动画和背景
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        popupWindow.setElevation(16f);
        popupWindow.setOutsideTouchable(true);
        
        // 设置点击外部区域关闭浮窗
        popupWindow.setOnDismissListener(() -> {
            if (backgroundDimmer.getParent() != null) {
                ((ViewGroup) backgroundDimmer.getParent()).removeView(backgroundDimmer);
            }
        });
    }
    
    /**
     * 显示用户资料浮窗
     * @param anchorView 锚点视图，浮窗将显示在此视图附近
     * @param userId 用户ID
     * @param userDoc 用户文档对象，包含用户数据
     * @param chatRoomId 聊天室ID
     * @param chatRoomName 聊天室名称
     */
    public void showProfilePopup(View anchorView, String userId, DocumentSnapshot userDoc, 
                                String chatRoomId, String chatRoomName) {
        this.userId = userId;
        this.userDoc = userDoc;
        this.chatRoomId = chatRoomId;
        this.chatRoomName = chatRoomName;
        
        // 填充数据
        if (userDoc != null) {
            // 设置用户名
            String username = userDoc.getString("uname");
            if (username == null || username.isEmpty()) {
                username = userDoc.getString("name");
            }
            tvPopupUsername.setText(username != null ? username : "未知用户");
            
            // 设置邮箱
            String email = userDoc.getString("email");
            if (email != null && !email.isEmpty()) {
                tvPopupEmail.setText(email);
                tvPopupEmail.setVisibility(View.VISIBLE);
            } else {
                tvPopupEmail.setVisibility(View.GONE);
            }
            
            // 设置院系
            String department = userDoc.getString("department");
            if (department != null && !department.isEmpty()) {
                tvPopupDepartment.setText(department);
                tvPopupDepartment.setVisibility(View.VISIBLE);
            } else {
                tvPopupDepartment.setVisibility(View.GONE);
            }
            
            // 设置专业
            String programme = userDoc.getString("programme");
            if (programme != null && !programme.isEmpty()) {
                tvPopupProgramme.setText(programme);
                tvPopupProgramme.setVisibility(View.VISIBLE);
            } else {
                tvPopupProgramme.setVisibility(View.GONE);
            }
            
            // 设置入学年份
            String yearOfEntry = userDoc.getString("year_of_entry");
            if (yearOfEntry != null && !yearOfEntry.isEmpty()) {
                tvPopupYearOfEntry.setText("入学年份: " + yearOfEntry);
                tvPopupYearOfEntry.setVisibility(View.VISIBLE);
            } else {
                tvPopupYearOfEntry.setVisibility(View.GONE);
            }
            
            // 设置个性签名
            String signature = userDoc.getString("signature");
            if (signature != null && !signature.isEmpty()) {
                tvPopupSignature.setText("\"" + signature + "\"");
                tvPopupSignature.setVisibility(View.VISIBLE);
            } else {
                tvPopupSignature.setVisibility(View.GONE);
            }
            
            // 加载头像
            String avatarUrl = userDoc.getString("avatar_url");
            if (avatarUrl != null && !avatarUrl.isEmpty() && !"default".equals(avatarUrl)) {
                // 确保头像URL正确（添加avatar/前缀如果没有）
                if (!avatarUrl.startsWith("avatar/")) {
                    avatarUrl = "avatar/" + avatarUrl;
                }
                
                // 使用统一的服务器URL格式
                String imageUrl = SERVER_URL + "/image/" + avatarUrl;
                // 添加随机参数避免缓存问题
                String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
                imageUrl = imageUrl + "?nocache=" + uniqueParam;
                
                // 日志记录
                Log.d(TAG, "加载头像URL: " + imageUrl);
                
                // 使用Glide加载头像
                try {
                    final String finalImageUrl = imageUrl;
                    Glide.with(context)
                            .load(finalImageUrl)
                            .placeholder(R.drawable.default_avatar)
                            .error(R.drawable.default_avatar)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, 
                                                           Object model, 
                                                           Target<Drawable> target, 
                                                           boolean isFirstResource) {
                                    Log.e(TAG, "头像加载失败: " + e.getMessage(), e);
                                    return false;
                                }
                                
                                @Override
                                public boolean onResourceReady(Drawable resource, 
                                                              Object model, 
                                                              Target<Drawable> target, 
                                                              DataSource dataSource, 
                                                              boolean isFirstResource) {
                                    Log.d(TAG, "头像加载成功");
                                    return false;
                                }
                            })
                            .into(ivPopupAvatar);
                } catch (Exception e) {
                    Log.e(TAG, "Glide加载头像异常: " + e.getMessage(), e);
                    ivPopupAvatar.setImageResource(R.drawable.default_avatar);
                }
            } else {
                // 使用默认头像
                ivPopupAvatar.setImageResource(R.drawable.default_avatar);
            }
        } else {
            // 用户文档不存在，显示默认值
            tvPopupUsername.setText("用户信息不可用");
            tvPopupEmail.setVisibility(View.GONE);
            tvPopupDepartment.setVisibility(View.GONE);
            tvPopupProgramme.setVisibility(View.GONE);
            tvPopupYearOfEntry.setVisibility(View.GONE);
            tvPopupSignature.setVisibility(View.GONE);
            ivPopupAvatar.setImageResource(R.drawable.default_avatar);
        }
        
        try {
            // 添加背景遮罩
            if (context instanceof Activity) {
                ViewGroup rootView = ((Activity) context).findViewById(android.R.id.content);
                if (backgroundDimmer.getParent() == null) {
                    rootView.addView(backgroundDimmer);
                }
                
                // 设置背景点击事件为关闭浮窗
                backgroundDimmer.setOnClickListener(v -> dismissPopup());
            }
            
            // 在屏幕中央显示
            popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
            
            Log.d(TAG, "浮窗显示成功");
        } catch (Exception e) {
            Log.e(TAG, "显示浮窗失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 关闭浮窗
     */
    public void dismissPopup() {
        // 移除背景遮罩
        if (backgroundDimmer != null && backgroundDimmer.getParent() != null) {
            ((ViewGroup) backgroundDimmer.getParent()).removeView(backgroundDimmer);
        }
        
        // 关闭浮窗
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }
    
    /**
     * 判断浮窗是否正在显示
     */
    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }
    
    /**
     * 获取当前上下文
     */
    public Context getContext() {
        return context;
    }
} 