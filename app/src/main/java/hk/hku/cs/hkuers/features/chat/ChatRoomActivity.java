package hk.hku.cs.hkuers.features.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import hk.hku.cs.hkuers.MainActivity;
import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.courses.CourseSearchActivity;
import hk.hku.cs.hkuers.features.map.MapActivity;
import hk.hku.cs.hkuers.features.marketplace.MarketplaceActivity;
import hk.hku.cs.hkuers.models.Message;
import hk.hku.cs.hkuers.features.chat.MemberListActivity;
import com.bumptech.glide.Glide;

public class ChatRoomActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText etMessage;
    private TextView tvChatRoomName, tvAnnouncement;
    private Button btnSend;
    private BottomNavigationView bottomNavigation;
    private ImageButton btnBack, btnGroupInfo;
    private LinearLayout announcementLayout;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    
    private String chatRoomId, chatRoomName, creatorId;
    private boolean isCreator = false;
    
    private FirestoreRecyclerAdapter<Message, RecyclerView.ViewHolder> adapter;
    
    // Message type constants
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SYSTEM = 3;
    private static final int VIEW_TYPE_ANNOUNCEMENT = 4;
    
    // Server URL constant
    private static final String SERVER_URL = "http://10.0.2.2:9000";
    
    // Flag variable indicating whether Activity is finishing
    private boolean isFinishing = false;
    
    private static final int REQUEST_CODE_VIEW_MEMBERS = 1001;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Add log
        android.util.Log.d("ChatRoomActivity", "onCreate: Starting to create ChatRoomActivity");
        
        setContentView(R.layout.activity_chat_room);
        
        // Set system status bar color to fixed dark color (consistent with top bar)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#1E1E1E"));
        }
        
        // Set status bar space height
        View statusBarSpace = findViewById(R.id.statusBarSpace);
        if (statusBarSpace != null) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                int statusBarHeight = getResources().getDimensionPixelSize(resourceId);
                ViewGroup.LayoutParams params = statusBarSpace.getLayoutParams();
                params.height = statusBarHeight;
                statusBarSpace.setLayoutParams(params);
                android.util.Log.d("ChatRoomActivity", "Setting status bar height: " + statusBarHeight);
            }
        }
        
        // Test if page is displaying correctly
        String displayName = getIntent().getStringExtra("chatRoomName");
        if (displayName != null) {
            Toast.makeText(this, "Loading chat room: " + displayName, Toast.LENGTH_SHORT).show();
        }
        
        // If the entire page is black, it may be a theme issue, try setting background color
        findViewById(android.R.id.content).setBackgroundColor(getResources().getColor(android.R.color.white));
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize views
        initViews();
        
        // Get chat room ID and name from intent
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        chatRoomName = getIntent().getStringExtra("chatRoomName");
        
        // Add log
        android.util.Log.d("ChatRoomActivity", "Received parameters: chatRoomId=" + chatRoomId + ", chatRoomName=" + chatRoomName);
        
        if (chatRoomId == null) {
            Toast.makeText(this, "Chat room ID cannot be empty", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Set chat room name
        if (chatRoomName != null) {
            tvChatRoomName.setText(chatRoomName);
        } else {
            // If chat room name is empty, use the first few characters of ID
            tvChatRoomName.setText("Chat Room-" + chatRoomId.substring(0, Math.min(6, chatRoomId.length())));
        }
        
        // Load chat room information
        loadChatRoomInfo();
        
        // Set up message adapter
        setupMessageAdapter();
        
        // Set button click listeners
        setupClickListeners();
        
        // Update user read status
        updateUserReadStatus();

        // Directly set bottom navigation bar, don't use a separate method
        if (bottomNavigation != null) {
            android.util.Log.d("ChatRoomActivity", "onCreate: Directly setting BottomNavigationView");
            try {
                // Set Chat option as selected
                bottomNavigation.setSelectedItemId(R.id.navigation_chat);
                
                // Use new API to set listener - using anonymous inner class instead of lambda expression
                bottomNavigation.setOnItemSelectedListener(new com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                        android.util.Log.d("ChatRoomActivity", "Bottom navigation click: id=" + item.getItemId() + ", title=" + item.getTitle());
                        
                        int itemId = item.getItemId();
                        
                        if (itemId == R.id.navigation_chat) {
                            // Return to chat list
                            android.util.Log.d("ChatRoomActivity", "User clicked Chat option");
                            directNavigateTo(ChatListActivity.class);
                            return true;
                        } else if (itemId == R.id.navigation_forum) {
                            android.util.Log.d("ChatRoomActivity", "User clicked Forum option");
                            Toast.makeText(ChatRoomActivity.this, "Forum feature coming soon", Toast.LENGTH_SHORT).show();
                            return true;
                        } else if (itemId == R.id.navigation_dashboard) {
                            android.util.Log.d("ChatRoomActivity", "User clicked Dashboard option");
                            directNavigateTo(MainActivity.class);
                            return true;
                        } else if (itemId == R.id.navigation_courses) {
                            android.util.Log.d("ChatRoomActivity", "User clicked Courses option");
                            directNavigateTo(CourseSearchActivity.class);
                            return true;
                        } else if (itemId == R.id.navigation_marketplace) {
                            android.util.Log.d("ChatRoomActivity", "User clicked Marketplace option");
                            directNavigateTo(MarketplaceActivity.class);
                            return true;
                        }
                        return false;
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "Failed to set BottomNavigationView: " + e.getMessage(), e);
            }
        } else {
            android.util.Log.e("ChatRoomActivity", "onCreate: BottomNavigationView is null, trying to find regular buttons");
            
            // Try setting legacy bottom navigation buttons
            try {
                Button btnChat = findViewById(R.id.btnChat);
                Button btnMap = findViewById(R.id.btnMap);
                Button btnProfile = findViewById(R.id.btnProfile);
                Button btnCourses = findViewById(R.id.btnCourses);
                Button btnMarketplace = findViewById(R.id.btnMarketplace);
                
                if (btnChat != null) {
                    btnChat.setOnClickListener(v -> {
                        android.util.Log.d("ChatRoomActivity", "User clicked regular button Chat");
                        directNavigateTo(ChatListActivity.class);
                    });
                }
                
                if (btnMap != null) {
                    btnMap.setOnClickListener(v -> {
                        android.util.Log.d("ChatRoomActivity", "User clicked regular button Map");
                        directNavigateTo(MapActivity.class);
                    });
                }
                
                if (btnProfile != null) {
                    btnProfile.setOnClickListener(v -> {
                        android.util.Log.d("ChatRoomActivity", "User clicked regular button Profile");
                        directNavigateTo(MainActivity.class);
                    });
                }
                
                if (btnCourses != null) {
                    btnCourses.setOnClickListener(v -> {
                        android.util.Log.d("ChatRoomActivity", "User clicked regular button Courses");
                        directNavigateTo(CourseSearchActivity.class);
                    });
                }
                
                if (btnMarketplace != null) {
                    btnMarketplace.setOnClickListener(v -> {
                        android.util.Log.d("ChatRoomActivity", "User clicked regular button Marketplace");
                        directNavigateTo(MarketplaceActivity.class);
                    });
                }
                
                android.util.Log.d("ChatRoomActivity", "Legacy bottom navigation buttons set");
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "Failed to set legacy bottom navigation buttons: " + e.getMessage(), e);
            }
        }
        
        // Add log
        android.util.Log.d("ChatRoomActivity", "onCreate: ChatRoomActivity creation completed");
    }
    
    private void initViews() {
        // Add log
        android.util.Log.d("ChatRoomActivity", "initViews: Starting to initialize views");
        
        recyclerView = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        tvChatRoomName = findViewById(R.id.tvChatRoomName);
        tvAnnouncement = findViewById(R.id.tvAnnouncement);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnGroupInfo = findViewById(R.id.btnGroupInfo);
        announcementLayout = findViewById(R.id.announcementLayout);
        
        // Bottom navigation bar - add more detailed logs
        try {
            android.util.Log.d("ChatRoomActivity", "Finding bottom navigation bar, Android version: " + 
                    android.os.Build.VERSION.SDK_INT);
            
            // Check if include_bottom_navigation layout is loaded
            View legacyNavigation = findViewById(R.id.layout_bottom_navigation);
            if (legacyNavigation != null) {
                android.util.Log.d("ChatRoomActivity", "Found legacy bottom navigation bar layout: layout_bottom_navigation");
                
                // Initialize legacy navigation buttons
                Button btnChat = findViewById(R.id.btnChat);
                Button btnMap = findViewById(R.id.btnMap);
                Button btnProfile = findViewById(R.id.btnProfile);
                Button btnCourses = findViewById(R.id.btnCourses);
                Button btnMarketplace = findViewById(R.id.btnMarketplace);
                
                if (btnChat != null) android.util.Log.d("ChatRoomActivity", "Found chat button");
                if (btnMap != null) android.util.Log.d("ChatRoomActivity", "Found map button");
                if (btnProfile != null) android.util.Log.d("ChatRoomActivity", "Found profile button");
                if (btnCourses != null) android.util.Log.d("ChatRoomActivity", "Found courses button");
                if (btnMarketplace != null) android.util.Log.d("ChatRoomActivity", "Found marketplace button");
            }
            
            // Check BottomNavigationView
            bottomNavigation = findViewById(R.id.bottom_navigation);
            if (bottomNavigation != null) {
                android.util.Log.d("ChatRoomActivity", "Successfully found BottomNavigationView: " + bottomNavigation.getClass().getName());
                try {
                    // Check if menu resources are correctly loaded
                    android.view.Menu menu = bottomNavigation.getMenu();
                    android.util.Log.d("ChatRoomActivity", "Bottom navigation bar menu item count: " + menu.size());
                    for (int i = 0; i < menu.size(); i++) {
                        android.view.MenuItem item = menu.getItem(i);
                        android.util.Log.d("ChatRoomActivity", "Menu item " + i + ": id=" + item.getItemId() + ", title=" + item.getTitle());
                    }
                } catch (Exception e) {
                    android.util.Log.e("ChatRoomActivity", "Failed to check bottom navigation bar menu: " + e.getMessage());
                }
            } else {
                android.util.Log.e("ChatRoomActivity", "BottomNavigationView not found, please check ID in XML layout");
            }
            
            // Output all bottom navigation related components
            android.util.Log.d("ChatRoomActivity", "-- Checking bottom navigation related components --");
            
            // Try finding all root-level views
            android.view.ViewGroup rootView = (android.view.ViewGroup) findViewById(android.R.id.content);
            dumpViewHierarchy(rootView, 0);
            
        } catch (Exception e) {
            android.util.Log.e("ChatRoomActivity", "Failed to initialize bottom navigation bar: " + e.getMessage(), e);
        }
        
        // Set RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Add log
        android.util.Log.d("ChatRoomActivity", "initViews: Views initialization completed");
    }
    
    // Helper method for debugging view hierarchy
    private void dumpViewHierarchy(android.view.ViewGroup viewGroup, int depth) {
        String indent = "";
        for (int i = 0; i < depth; i++) {
            indent += "  ";
        }
        
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            android.view.View child = viewGroup.getChildAt(i);
            String id = "";
            try {
                if (child.getId() != -1) {
                    id = getResources().getResourceEntryName(child.getId());
                }
            } catch (Exception e) {
                id = "unknown-id";
            }
            
            android.util.Log.d("ViewHierarchy", indent + "View: " + child.getClass().getSimpleName() 
                    + ", id: " + id
                    + ", visibility: " + (child.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN/GONE"));
            
            if (child instanceof android.view.ViewGroup) {
                dumpViewHierarchy((android.view.ViewGroup) child, depth + 1);
            }
        }
    }
    
    private void loadChatRoomInfo() {
        // Add log
        android.util.Log.d("ChatRoomActivity", "loadChatRoomInfo: Starting to load chat room info, chatRoomId=" + chatRoomId);
        
        db.collection("chat_rooms").document(chatRoomId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                // Check if Activity is finishing
                if (isFinishing || isFinishing()) return;
                
                android.util.Log.d("ChatRoomActivity", "loadChatRoomInfo: Successfully retrieved chat room data");
                
                if (documentSnapshot.exists()) {
                    // Record entire data for debugging
                    android.util.Log.d("ChatRoomActivity", "Chat room data: " + documentSnapshot.getData());
                    
                    // Get group creator ID
                    creatorId = documentSnapshot.getString("creator_id");
                    isCreator = creatorId != null && creatorId.equals(currentUser.getUid());
                    
                    android.util.Log.d("ChatRoomActivity", "Chat room creator ID: " + creatorId + ", Current user is creator: " + isCreator);
                    
                    // Get chat room name (compatible with different field names)
                    String dbChatName = null;
                    // Try different field names
                    if (documentSnapshot.contains("chat_name")) {
                        dbChatName = documentSnapshot.getString("chat_name");
                        android.util.Log.d("ChatRoomActivity", "Getting chat room name from chat_name field: " + dbChatName);
                    } else if (documentSnapshot.contains("name")) {
                        dbChatName = documentSnapshot.getString("name");
                        android.util.Log.d("ChatRoomActivity", "Getting chat room name from name field: " + dbChatName);
                    }
                    
                    // If a name is retrieved from the database and the current name is empty or different from the database, update
                    if (dbChatName != null && !dbChatName.isEmpty() && 
                        (chatRoomName == null || !chatRoomName.equals(dbChatName))) {
                        chatRoomName = dbChatName;
                        tvChatRoomName.setText(chatRoomName);
                        android.util.Log.d("ChatRoomActivity", "Updating chat room name to: " + chatRoomName);
                    }
                    
                    // Load group announcement
                    String announcement = documentSnapshot.getString("announcement");
                    if (announcement != null && !announcement.isEmpty()) {
                        announcementLayout.setVisibility(View.VISIBLE);
                        tvAnnouncement.setText(announcement);
                        android.util.Log.d("ChatRoomActivity", "Showing group announcement: " + announcement);
                    } else {
                        announcementLayout.setVisibility(View.GONE);
                        android.util.Log.d("ChatRoomActivity", "No group announcement");
                    }
                    
                    // Get and set color indicator color
                    String colorCode = documentSnapshot.getString("color_code");
                    if (colorCode != null && !colorCode.isEmpty()) {
                        try {
                            int color = android.graphics.Color.parseColor(colorCode);
                            // Set color indicator color
                            View colorIndicator = findViewById(R.id.colorIndicator);
                            if (colorIndicator != null) {
                                colorIndicator.setBackgroundColor(color);
                                android.util.Log.d("ChatRoomActivity", "Setting color indicator color: " + colorCode);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("ChatRoomActivity", "Failed to parse color code: " + e.getMessage());
                        }
                    } else {
                        android.util.Log.d("ChatRoomActivity", "No chat room color found, using default color");
                        // If no color is set, generate a fixed color based on chat room ID
                        if (chatRoomId != null && !chatRoomId.isEmpty()) {
                            View colorIndicator = findViewById(R.id.colorIndicator);
                            if (colorIndicator != null) {
                                int colorIndex = Math.abs(chatRoomId.hashCode()) % ChatListActivity.ChatGroupViewHolder.GROUP_COLORS.length;
                                int color = ChatListActivity.ChatGroupViewHolder.GROUP_COLORS[colorIndex];
                                colorIndicator.setBackgroundColor(color);
                            }
                        }
                    }
                } else {
                    android.util.Log.e("ChatRoomActivity", "Chat room data does not exist!");
                }
            })
            .addOnFailureListener(e -> {
                // Check if Activity is finishing
                if (isFinishing || isFinishing()) return;
                
                android.util.Log.e("ChatRoomActivity", "Failed to load chat room info: " + e.getMessage());
                Toast.makeText(this, "Failed to load chat room info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void setupMessageAdapter() {
        // Add log
        android.util.Log.d("ChatRoomActivity", "setupMessageAdapter: Starting to set up message adapter");
        
        Query query = db.collection("chat_rooms").document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);
        
        android.util.Log.d("ChatRoomActivity", "Message query path: chat_rooms/" + chatRoomId + "/messages");
        
        FirestoreRecyclerOptions<Message> options = new FirestoreRecyclerOptions.Builder<Message>()
                .setQuery(query, Message.class)
                .build();
        
        adapter = new FirestoreRecyclerAdapter<Message, RecyclerView.ViewHolder>(options) {
            @Override
            public int getItemViewType(int position) {
                Message message = getItem(position);
                String senderId = message.getSenderId();
                String type = message.getType();
                
                // Add null value check to avoid NullPointerException
                if ("system".equals(senderId)) {
                    return VIEW_TYPE_SYSTEM;
                } else if (type != null && "announcement".equals(type)) {
                    return VIEW_TYPE_ANNOUNCEMENT;
                } else if (senderId != null && senderId.equals(currentUser.getUid())) {
                    return VIEW_TYPE_SENT;
                } else {
                    return VIEW_TYPE_RECEIVED;
                }
            }
            
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                
                switch (viewType) {
                    case VIEW_TYPE_SENT:
                        return new SentMessageViewHolder(
                                inflater.inflate(R.layout.item_message_sent, parent, false));
                    case VIEW_TYPE_RECEIVED:
                        return new ReceivedMessageViewHolder(
                                inflater.inflate(R.layout.item_message_received, parent, false));
                    case VIEW_TYPE_SYSTEM:
                        return new SystemMessageViewHolder(
                                inflater.inflate(R.layout.item_message_system, parent, false));
                    case VIEW_TYPE_ANNOUNCEMENT:
                        return new AnnouncementViewHolder(
                                inflater.inflate(R.layout.item_message_announcement, parent, false));
                    default:
                        return new ReceivedMessageViewHolder(
                                inflater.inflate(R.layout.item_message_received, parent, false));
                }
            }
            
            @Override
            protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull Message model) {
                android.util.Log.d("ChatRoomActivity", "Binding message: " + model.getText() + ", Type: " + model.getType());
                
                switch (holder.getItemViewType()) {
                    case VIEW_TYPE_SENT:
                        ((SentMessageViewHolder) holder).bind(model);
                        break;
                    case VIEW_TYPE_RECEIVED:
                        ((ReceivedMessageViewHolder) holder).bind(model);
                        break;
                    case VIEW_TYPE_SYSTEM:
                        ((SystemMessageViewHolder) holder).bind(model);
                        break;
                    case VIEW_TYPE_ANNOUNCEMENT:
                        ((AnnouncementViewHolder) holder).bind(model);
                        break;
                }
            }
            
            @Override
            public void onDataChanged() {
                android.util.Log.d("ChatRoomActivity", "onDataChanged: Data updated, message count: " + getItemCount());
                if (getItemCount() == 0) {
                    android.util.Log.d("ChatRoomActivity", "No messages");
                }
            }
        };
        
        recyclerView.setAdapter(adapter);
        android.util.Log.d("ChatRoomActivity", "setupMessageAdapter: Message adapter setup completed");
    }
    
    private void setupClickListeners() {
        // Back button - use a generic safe exit method
        btnBack.setOnClickListener(v -> safeFinishActivity());
        
        // Group info button
        btnGroupInfo.setOnClickListener(v -> {
            if (isFinishing || isFinishing()) return;
            showGroupInfoDialog();
        });
        
        // Send button
        btnSend.setOnClickListener(v -> {
            if (isFinishing || isFinishing()) return;
            sendMessage();
        });
        
        // Request focus and show keyboard when clicking input field
        etMessage.setOnClickListener(v -> {
            etMessage.requestFocus();
            showKeyboard(etMessage);
        });
    }
    
    // Modify safeFinishActivity method to directly jump to ChatListActivity
    private void safeFinishActivity() {
        // Already finishing, avoid repeated calls
        if (isFinishing || isFinishing()) return;
        
        // Add log
        android.util.Log.d("ChatRoomActivity", "Safe finish Activity - Directly jump to ChatListActivity");
        
        // Set flag indicating Activity is finishing
        isFinishing = true;
        
        // Stop listener
        if (adapter != null) {
            try {
                adapter.stopListening();
                android.util.Log.d("ChatRoomActivity", "Stopped adapter listener");
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "Failed to stop adapter listener: " + e.getMessage());
            }
        }
        
        // Clear adapter and recycle views, completely release resources
        recyclerView.setAdapter(null);
        
        // Immediately jump to ChatListActivity
        Intent intent = new Intent(this, ChatListActivity.class);
        // Use FLAG_ACTIVITY_NEW_TASK and FLAG_ACTIVITY_CLEAR_TOP to clear task stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        
        // Use simple animation
        overridePendingTransition(0, 0);
        
        // Directly call finish()
        finish();
        
        android.util.Log.d("ChatRoomActivity", "Started ChatListActivity and finished current Activity");
    }
    
    private void showGroupInfoDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_group_info, null);
        
        TextView tvGroupName = view.findViewById(R.id.tvGroupName);
        Button btnViewMembers = view.findViewById(R.id.btnViewMembers);
        Button btnPublishAnnouncement = view.findViewById(R.id.btnPublishAnnouncement);
        Button btnLeaveGroup = view.findViewById(R.id.btnLeaveGroup);
        
        tvGroupName.setText(chatRoomName);
        
        // Only group creator can publish announcement
        btnPublishAnnouncement.setVisibility(isCreator ? View.VISIBLE : View.GONE);
        
        // Change button text based on whether user is group creator
        if (isCreator) {
            btnLeaveGroup.setText("Disband Group");
        } else {
            btnLeaveGroup.setText("Leave Group");
        }
        
        // View members button
        btnViewMembers.setOnClickListener(v -> {
            dialog.dismiss();
            showMembersDialog();
        });
        
        // Publish announcement button
        btnPublishAnnouncement.setOnClickListener(v -> {
            dialog.dismiss();
            showAnnouncementDialog();
        });
        
        // Leave/disband group button
        btnLeaveGroup.setOnClickListener(v -> {
            dialog.dismiss();
            if (isCreator) {
                showDisbandGroupConfirmation();
            } else {
                showLeaveGroupConfirmation();
            }
        });
        
        dialog.setContentView(view);
        dialog.show();
    }
    
    private void showMembersDialog() {
        try {
            // Simple direct jump implementation
            Intent intent = new Intent(this, MemberListActivity.class);
            intent.putExtra("chat_room_id", chatRoomId);
            if (chatRoomName != null && !chatRoomName.isEmpty()) {
                intent.putExtra("chatRoomName", chatRoomName);
            }
            // Use startActivityForResult instead of startActivity
            startActivityForResult(intent, REQUEST_CODE_VIEW_MEMBERS);
            // Simple transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            android.util.Log.e("ChatRoomActivity", "Failed to jump to member list: " + e.getMessage(), e);
            Toast.makeText(this, "Cannot open member list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Handle result from member list page
        if (requestCode == REQUEST_CODE_VIEW_MEMBERS) {
            android.util.Log.d("ChatRoomActivity", "Returned from member list");
            
            // Ensure adapter starts listening
            if (adapter != null && !isFinishing && !isFinishing()) {
                try {
                    adapter.startListening();
                    android.util.Log.d("ChatRoomActivity", "Returned from member list and resumed adapter listener");
                } catch (Exception e) {
                    android.util.Log.e("ChatRoomActivity", "Failed to resume adapter listener: " + e.getMessage());
                }
            }
        }
    }
    
    private void showAnnouncementDialog() {
        if (!isCreator) {
            Toast.makeText(this, "Only group creator can publish announcements", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Use dark theme AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        View view = getLayoutInflater().inflate(R.layout.dialog_announcement, null);
        EditText etAnnouncement = view.findViewById(R.id.etAnnouncement);
        
        // Set EditText text color to black to ensure visible on light background
        etAnnouncement.setTextColor(android.graphics.Color.BLACK);
        etAnnouncement.setHintTextColor(android.graphics.Color.GRAY);
        
        // Load current announcement
        db.collection("chat_rooms").document(chatRoomId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String announcement = documentSnapshot.getString("announcement");
                    if (announcement != null) {
                        etAnnouncement.setText(announcement);
                    }
                }
            });
        
        builder.setView(view)
               .setTitle("Group Announcement")
               .setPositiveButton("Publish", (dialog, which) -> {
                   String announcementText = etAnnouncement.getText().toString().trim();
                   publishAnnouncement(announcementText);
               })
               .setNegativeButton("Cancel", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void publishAnnouncement(String announcementText) {
        if (!isCreator) {
            Toast.makeText(this, "Only group creator can publish announcements", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (announcementText.isEmpty()) {
            // Clear announcement
            db.collection("chat_rooms").document(chatRoomId)
                .update("announcement", "")
                .addOnSuccessListener(aVoid -> {
                    announcementLayout.setVisibility(View.GONE);
                    addAnnouncementMessage("Group announcement has been cleared");
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to clear announcement: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
        } else {
            // Update announcement
            db.collection("chat_rooms").document(chatRoomId)
                .update("announcement", announcementText)
                .addOnSuccessListener(aVoid -> {
                    announcementLayout.setVisibility(View.VISIBLE);
                    tvAnnouncement.setText(announcementText);
                    addAnnouncementMessage(announcementText);
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to publish announcement: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
        }
    }
    
    private void addAnnouncementMessage(String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("text", text);
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "announcement");
        
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener(documentReference -> {
                // Update chat room last message and time
                updateLastMessage("[Announcement] " + text);
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to send announcement: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    private void showLeaveGroupConfirmation() {
        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave this group?")
            .setPositiveButton("Confirm", (dialog, which) -> leaveGroup())
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showDisbandGroupConfirmation() {
        new AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Disband Group")
            .setMessage("Are you sure you want to disband this group? This action cannot be undone, and all chat history will be deleted!")
            .setPositiveButton("Confirm Disband", (dialog, which) -> disbandGroup())
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void leaveGroup() {
        String uid = currentUser.getUid();
        
        // Verify not creator to leave
        if (isCreator) {
            Toast.makeText(this, "Group creator cannot leave, only disband", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DocumentReference chatRef = db.collection("chat_rooms").document(chatRoomId);
        
        // Remove user from member list
        chatRef.get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get current member list
                    List<String> memberIds = (List<String>) documentSnapshot.get("member_ids");
                    
                    if (memberIds != null && memberIds.contains(uid)) {
                        // Remove user ID
                        memberIds.remove(uid);
                        
                        // Update member list
                        chatRef.update("member_ids", memberIds)
                            .addOnSuccessListener(aVoid -> {
                                // Add leave message
                                addUserLeftMessage();
                                
                                // Return to chat list page
                                Intent intent = new Intent(ChatRoomActivity.this, ChatListActivity.class);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> 
                                Toast.makeText(this, "Failed to leave group: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                    }
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to get group info: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    private void disbandGroup() {
        // Verify creator to disband
        if (!isCreator) {
            Toast.makeText(this, "Only group creator can disband group", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setTitle("Disbanding Group")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create();
        progressDialog.show();
        
        final DocumentReference chatRef = db.collection("chat_rooms").document(chatRoomId);
        
        // First delete all messages from chat room collection
        chatRef.collection("messages")
            .get()
            .addOnSuccessListener(messagesSnapshot -> {
                // Create batch write operation
                android.util.Log.d("ChatRoomActivity", "Deleting chat room messages, total " + messagesSnapshot.size() + " messages");
                
                // Use recursive batch delete to avoid single batch operation limit
                deleteMessagesRecursively(chatRef, messagesSnapshot.getDocuments(), 0, progressDialog);
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to disband group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    // Recursive delete messages, delete up to 500 messages at a time to avoid exceeding Firebase batch operation limit
    private void deleteMessagesRecursively(DocumentReference chatRef, 
                                          List<DocumentSnapshot> messages,
                                          int startIndex,
                                          AlertDialog progressDialog) {
        final int batchSize = 500; // Firestore each batch can have up to 500 operations
        
        // Create a new batch write
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        
        int endIndex = Math.min(startIndex + batchSize, messages.size());
        
        // Add messages to batch delete
        for (int i = startIndex; i < endIndex; i++) {
            batch.delete(messages.get(i).getReference());
        }
        
        // Submit batch delete
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                android.util.Log.d("ChatRoomActivity", "Deleted " + (endIndex - startIndex) + " messages");
                
                // Check if there are more messages to delete
                if (endIndex < messages.size()) {
                    // There are more messages, continue recursive delete
                    deleteMessagesRecursively(chatRef, messages, endIndex, progressDialog);
                } else {
                    // All messages deleted, next delete member collection
                    android.util.Log.d("ChatRoomActivity", "All messages deleted, deleting member collection");
                    deleteMembers(chatRef, progressDialog);
                }
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to delete messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    // Delete member collection
    private void deleteMembers(DocumentReference chatRef, AlertDialog progressDialog) {
        chatRef.collection("members")
            .get()
            .addOnSuccessListener(membersSnapshot -> {
                android.util.Log.d("ChatRoomActivity", "Deleting group members, total " + membersSnapshot.size() + " members");
                
                // Create batch write operation
                com.google.firebase.firestore.WriteBatch batch = db.batch();
                
                // Add all members to batch delete
                for (DocumentSnapshot memberDoc : membersSnapshot.getDocuments()) {
                    batch.delete(memberDoc.getReference());
                }
                
                // Submit batch delete
                batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("ChatRoomActivity", "All members deleted, deleting chat room document");
                        
                        // Finally delete chat room document itself
                        deleteChat(chatRef, progressDialog);
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to delete members: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to delete member collection: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    // Delete chat room document
    private void deleteChat(DocumentReference chatRef, AlertDialog progressDialog) {
        // Get chat room information for logging
        chatRef.get()
            .addOnSuccessListener(chatSnapshot -> {
                if (chatSnapshot.exists()) {
                    // Log the chat room information to be deleted
                    String chatRoomNameToDelete = chatSnapshot.getString("chat_name");
                    android.util.Log.d("ChatRoomActivity", "Preparing to delete chat room: " + chatRoomNameToDelete);
                    
                    // Delete chat room document
                    chatRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            // Close progress dialog
                            progressDialog.dismiss();
                            
                            Toast.makeText(ChatRoomActivity.this, "Group disbanded and deleted", Toast.LENGTH_SHORT).show();
                            android.util.Log.d("ChatRoomActivity", "Chat room completely deleted: " + chatRoomNameToDelete);
                            
                            // Return to chat list page
                            Intent intent = new Intent(ChatRoomActivity.this, ChatListActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Failed to delete chat room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Chat room does not exist", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to get chat room info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void addUserLeftMessage() {
        String userName = currentUser.getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = currentUser.getEmail();
        }
        
        // Create final variable for use in lambda expression
        final String finalUserName = userName;
        
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", "system");
        message.put("text", finalUserName + " left the group");
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "text");
        
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener(documentReference -> {
                // Update chat room's last message and time
                updateLastMessage(finalUserName + " left the group");
            });
    }
    
    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create message object
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("text", messageText);
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "text");
        
        // Add message to Firestore
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener(documentReference -> {
                // Clear input field
                etMessage.setText("");
                
                // Update chat room's last message and time
                updateLastMessage(messageText);
                
                // Scroll to bottom
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    private void updateLastMessage(String message) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("last_message", message);
        updates.put("last_message_time", new Timestamp(new Date()));
        
        db.collection("chat_rooms").document(chatRoomId)
            .update(updates)
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to update last message: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    private void updateUserReadStatus() {
        // Update user's last read time
        Map<String, Object> userReadStatus = new HashMap<>();
        userReadStatus.put("user_read_status." + currentUser.getUid(), new Timestamp(new Date()));
        
        db.collection("chat_rooms").document(chatRoomId)
            .update(userReadStatus)
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to update read status: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        android.util.Log.d("ChatRoomActivity", "onStart: Activity started");
        if (adapter != null) {
            adapter.startListening();
            android.util.Log.d("ChatRoomActivity", "Adapter started listening");
        }
    }
    
    @Override
    protected void onStop() {
        android.util.Log.d("ChatRoomActivity", "onStop: Activity stopped");
        // Completely release adapter resources
        if (adapter != null) {
            try {
                adapter.stopListening();
                android.util.Log.d("ChatRoomActivity", "Adapter stopped listening");
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "Failed to stop adapter listener: " + e.getMessage());
            }
        }
        super.onStop();
    }
    
    // Modify onBackPressed method to override system back key behavior
    @Override
    public void onBackPressed() {
        // Use our safe back method instead of default behavior
        safeFinishActivity();
        // Call parent method
        super.onBackPressed();
    }
    
    // Add keyboard display method
    private void showKeyboard(View view) {
        if (view.requestFocus()) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    // Sent message ViewHolder
    public class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessageText, tvTime, tvReadStatus, tvExpandCollapse;
        private CircleImageView ivUserAvatar;
        private LinearLayout messageContainer;
        private boolean isExpanded = false;
        
        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvReadStatus = itemView.findViewById(R.id.tvReadStatus);
            tvExpandCollapse = itemView.findViewById(R.id.tvExpandCollapse);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }
        
        public void bind(Message message) {
            tvMessageText.setText(message.getText());
            tvTime.setText(formatTime(message.getTimestamp()));
            
            // Dynamically get read status
            updateReadStatus(message);
            
            // Load current user avatar
            String currentUserId = currentUser.getUid();
            db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing || isFinishing()) return;
                    
                    if (documentSnapshot.exists()) {
                        // Load avatar
                        String avatarUrl = documentSnapshot.getString("avatar_url");
                        if (avatarUrl != null && !avatarUrl.isEmpty() && !"default".equals(avatarUrl)) {
                            // Ensure avatar URL is correct (add avatar/ prefix if not already there)
                            if (!avatarUrl.startsWith("avatar/")) {
                                avatarUrl = "avatar/" + avatarUrl;
                            }
                            
                            // Use uniform server URL format
                            String imageUrl = SERVER_URL + "/image/" + avatarUrl;
                            // Add random parameter to avoid caching issues
                            String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
                            imageUrl = imageUrl + "?nocache=" + uniqueParam;
                            
                            // Use Glide to load avatar
                            Glide.with(ChatRoomActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .into(ivUserAvatar);
                            
                            // Add log
                            android.util.Log.d("ChatRoomActivity", "Loading avatar URL: " + imageUrl);
                        } else {
                            // Use default avatar
                            ivUserAvatar.setImageResource(R.drawable.default_avatar);
                        }
                        
                        // Add click avatar to open user profile functionality
                        final DocumentSnapshot userDoc = documentSnapshot;
                        ivUserAvatar.setOnClickListener(v -> {
                            android.util.Log.d("ChatRoomActivity", "User clicked own avatar, opening profile page");
                            openUserProfile(currentUserId, userDoc);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing || isFinishing()) return;
                    // Use default avatar
                    ivUserAvatar.setImageResource(R.drawable.default_avatar);
                });
            
            // Handle long message collapse/expand
            if (message.getText().length() > 100) {
                tvExpandCollapse.setVisibility(View.VISIBLE);
                tvExpandCollapse.setOnClickListener(v -> {
                    // Check if Activity is finishing
                    if (isFinishing || isFinishing()) return;
                    
                    isExpanded = !isExpanded;
                    if (isExpanded) {
                        tvMessageText.setMaxLines(Integer.MAX_VALUE);
                        tvExpandCollapse.setText("Collapse");
                    } else {
                        tvMessageText.setMaxLines(5);
                        tvExpandCollapse.setText("Show more");
                    }
                });
                tvMessageText.setMaxLines(5);
            } else {
                tvExpandCollapse.setVisibility(View.GONE);
                tvMessageText.setMaxLines(Integer.MAX_VALUE);
            }
        }
        
        // Update read status
        private void updateReadStatus(Message message) {
            // Get group member count and read count
            db.collection("chat_rooms").document(chatRoomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing || isFinishing()) return;
                    
                    if (documentSnapshot.exists()) {
                        // Get member ID list
                        List<String> memberIds = (List<String>) documentSnapshot.get("member_ids");
                        
                        if (memberIds == null || memberIds.isEmpty()) {
                            tvReadStatus.setText("0/0 read");
                            return;
                        }
                        
                        // Create list of users who need to read this message (excluding sender)
                        List<String> readersToCount = new ArrayList<>();
                        String senderId = message.getSenderId();
                        if (senderId != null) {
                            for (String memberId : memberIds) {
                                // Don't include message sender in count of people needed to read
                                if (!memberId.equals(senderId)) {
                                    readersToCount.add(memberId);
                                }
                            }
                        } else {
                            // If sender ID is empty, all members need to read
                            readersToCount.addAll(memberIds);
                        }
                        
                        int totalReadersCount = readersToCount.size();
                        
                        // Get read status mapping
                        Map<String, Object> readStatusMap = (Map<String, Object>) documentSnapshot.get("user_read_status");
                        int readCount = 0;
                        
                        if (readStatusMap != null && totalReadersCount > 0) {
                            // Calculate read count (by timestamp comparison)
                            Timestamp messageTime = message.getTimestamp();
                            
                            for (String userId : readersToCount) {
                                Object userReadTimeObj = readStatusMap.get(userId);
                                if (userReadTimeObj instanceof Timestamp) {
                                    Timestamp userReadTime = (Timestamp) userReadTimeObj;
                                    // If user's last read time is later than message send time, count as read
                                    if (userReadTime.compareTo(messageTime) >= 0) {
                                        readCount++;
                                    }
                                }
                            }
                            
                            // Update UI
                            tvReadStatus.setText(readCount + "/" + totalReadersCount + " read");
                        } else {
                            // Default display
                            tvReadStatus.setText("0/" + totalReadersCount + " read");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing || isFinishing()) return;
                    tvReadStatus.setText("Unknown read status");
                    android.util.Log.e("ChatRoomActivity", "Failed to get read status: " + e.getMessage());
                });
        }
    }
    
    // Received message ViewHolder
    public class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMessageText, tvTime, tvSenderName, tvExpandCollapse;
        private CircleImageView ivUserAvatar;
        private boolean isExpanded = false;
        
        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvExpandCollapse = itemView.findViewById(R.id.tvExpandCollapse);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
        }
        
        public void bind(Message message) {
            tvMessageText.setText(message.getText());
            tvTime.setText(formatTime(message.getTimestamp()));
            
            // Get sender information - add null value check
            String senderId = message.getSenderId();
            if (senderId != null && !senderId.isEmpty() && !"system".equals(senderId)) {
                db.collection("users").document(senderId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // Check if Activity is finishing
                        if (isFinishing || isFinishing()) return;
                        
                        if (documentSnapshot.exists()) {
                            // Set username - prioritize uname field
                            String userName = documentSnapshot.getString("uname");
                            if (userName == null || userName.isEmpty()) {
                                // Backup display field
                                userName = documentSnapshot.getString("email");
                                if (userName != null && userName.contains("@")) {
                                    // Display email prefix as name
                                    userName = userName.substring(0, userName.indexOf('@'));
                                }
                            }
                            tvSenderName.setText(userName);
                            
                            // Load avatar
                            String avatarUrl = documentSnapshot.getString("avatar_url");
                            if (avatarUrl != null && !avatarUrl.isEmpty() && !"default".equals(avatarUrl)) {
                                // Ensure avatar URL is correct (add avatar/ prefix if not already there)
                                if (!avatarUrl.startsWith("avatar/")) {
                                    avatarUrl = "avatar/" + avatarUrl;
                                }
                                
                                // Use uniform server URL format
                                String imageUrl = SERVER_URL + "/image/" + avatarUrl;
                                // Add random parameter to avoid caching issues
                                String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
                                imageUrl = imageUrl + "?nocache=" + uniqueParam;
                                
                                // Add log
                                android.util.Log.d("ChatRoomActivity", "Loading other user avatar URL: " + imageUrl);
                                
                                // Use Glide to load avatar
                                Glide.with(ChatRoomActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .error(R.drawable.default_avatar)
                                    .into(ivUserAvatar);
                            } else {
                                // Use default avatar
                                ivUserAvatar.setImageResource(R.drawable.default_avatar);
                            }
                            
                            // Add click avatar to open user profile functionality
                            final String finalSenderId = senderId;
                            final DocumentSnapshot userDoc = documentSnapshot;
                            ivUserAvatar.setOnClickListener(v -> {
                                android.util.Log.d("ChatRoomActivity", "User clicked other user avatar, user ID: " + finalSenderId);
                                openUserProfile(finalSenderId, userDoc);
                            });
                        }
                    });
            } else {
                // If it's a system message or sender ID is empty
                tvSenderName.setText("System");
                ivUserAvatar.setImageResource(R.drawable.default_avatar);
            }
            
            // Handle long message collapse/expand
            if (message.getText().length() > 100) {
                tvExpandCollapse.setVisibility(View.VISIBLE);
                tvExpandCollapse.setOnClickListener(v -> {
                    // Check if Activity is finishing
                    if (isFinishing || isFinishing()) return;
                    
                    isExpanded = !isExpanded;
                    if (isExpanded) {
                        tvMessageText.setMaxLines(Integer.MAX_VALUE);
                        tvExpandCollapse.setText("Collapse");
                    } else {
                        tvMessageText.setMaxLines(5);
                        tvExpandCollapse.setText("Show more");
                    }
                });
                tvMessageText.setMaxLines(5);
            } else {
                tvExpandCollapse.setVisibility(View.GONE);
                tvMessageText.setMaxLines(Integer.MAX_VALUE);
            }
        }
    }
    
    // System message ViewHolder
    public class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSystemMessage, tvTime;
        
        public SystemMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystemMessage = itemView.findViewById(R.id.tvSystemMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
        
        public void bind(Message message) {
            tvSystemMessage.setText(message.getText());
            tvTime.setText(formatTime(message.getTimestamp()));
        }
    }
    
    // Announcement message ViewHolder
    public class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        private TextView tvAnnouncementText, tvTime, tvPublisherName, tvExpandCollapse;
        private CircleImageView ivUserAvatar;
        private boolean isExpanded = false;
        
        public AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAnnouncementText = itemView.findViewById(R.id.tvAnnouncementText);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvPublisherName = itemView.findViewById(R.id.tvPublisherName);
            tvExpandCollapse = itemView.findViewById(R.id.tvExpandCollapse);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
        }
        
        public void bind(Message message) {
            tvAnnouncementText.setText(message.getText());
            tvTime.setText(formatDate(message.getTimestamp()));
            
            // Get publisher information - add null value check
            String senderId = message.getSenderId();
            if (senderId != null && !senderId.isEmpty() && !"system".equals(senderId)) {
                db.collection("users").document(senderId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // Check if Activity is finishing
                        if (isFinishing || isFinishing()) return;
                        
                        if (documentSnapshot.exists()) {
                            // Set username - prioritize uname field
                            String userName = documentSnapshot.getString("uname");
                            if (userName == null || userName.isEmpty()) {
                                // Backup display field
                                userName = documentSnapshot.getString("email");
                                if (userName != null && userName.contains("@")) {
                                    // Display email prefix as name
                                    userName = userName.substring(0, userName.indexOf('@'));
                                }
                            }
                            tvPublisherName.setText(userName);
                            
                            // Load avatar
                            String avatarUrl = documentSnapshot.getString("avatar_url");
                            if (avatarUrl != null && !avatarUrl.isEmpty() && !"default".equals(avatarUrl)) {
                                // Ensure avatar URL is correct (add avatar/ prefix if not already there)
                                if (!avatarUrl.startsWith("avatar/")) {
                                    avatarUrl = "avatar/" + avatarUrl;
                                }
                                
                                // Use uniform server URL format
                                String imageUrl = SERVER_URL + "/image/" + avatarUrl;
                                // Add random parameter to avoid caching issues
                                String uniqueParam = System.currentTimeMillis() + "_" + Math.random();
                                imageUrl = imageUrl + "?nocache=" + uniqueParam;
                                
                                // Add log
                                android.util.Log.d("ChatRoomActivity", "Loading announcement publisher avatar URL: " + imageUrl);
                                
                                // Use Glide to load avatar
                                if (ivUserAvatar != null) {
                                    Glide.with(ChatRoomActivity.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.default_avatar)
                                        .error(R.drawable.default_avatar)
                                        .into(ivUserAvatar);
                                }
                            } else if (ivUserAvatar != null) {
                                // Use default avatar
                                ivUserAvatar.setImageResource(R.drawable.default_avatar);
                            }
                            
                            // Add click avatar to open user profile functionality
                            if (ivUserAvatar != null) {
                                final String finalSenderId = senderId;
                                final DocumentSnapshot userDoc = documentSnapshot;
                                ivUserAvatar.setOnClickListener(v -> {
                                    android.util.Log.d("ChatRoomActivity", "User clicked other user avatar, user ID: " + finalSenderId);
                                    openUserProfile(finalSenderId, userDoc);
                                });
                            }
                        }
                    });
            } else {
                // If it's a system message or sender ID is empty
                tvPublisherName.setText("System");
                if (ivUserAvatar != null) {
                    ivUserAvatar.setImageResource(R.drawable.default_avatar);
                }
            }
            
            // Handle long message collapse/expand
            if (message.getText().length() > 100) {
                tvExpandCollapse.setVisibility(View.VISIBLE);
                tvExpandCollapse.setOnClickListener(v -> {
                    // Check if Activity is finishing
                    if (isFinishing || isFinishing()) return;
                    
                    isExpanded = !isExpanded;
                    if (isExpanded) {
                        tvAnnouncementText.setMaxLines(Integer.MAX_VALUE);
                        tvExpandCollapse.setText("Collapse");
                    } else {
                        tvAnnouncementText.setMaxLines(5);
                        tvExpandCollapse.setText("Show more");
                    }
                });
                tvAnnouncementText.setMaxLines(5);
            } else {
                tvExpandCollapse.setVisibility(View.GONE);
                tvAnnouncementText.setMaxLines(Integer.MAX_VALUE);
            }
        }
    }
    
    // Format time as HH:mm
    private String formatTime(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }
    
    // Format full date
    private String formatDate(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }
    
    // Add onPause method to clean up input method manager
    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.d("ChatRoomActivity", "onPause: Activity paused");
        
        // Stop adapter listener in onPause to avoid issues on resume
        if (adapter != null) {
            try {
                adapter.stopListening();
                android.util.Log.d("ChatRoomActivity", "Stopped adapter listener in onPause");
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "Failed to stop adapter listener in onPause: " + e.getMessage());
            }
        }
    }

    // Modify onDestroy method to ensure resources are released when Activity is destroyed
    @Override
    protected void onDestroy() {
        android.util.Log.d("ChatRoomActivity", "onDestroy: Starting to destroy Activity");
        
        // Stop all asynchronous operations
        if (adapter != null) {
            try {
                adapter.stopListening();
                adapter = null; // Completely release reference
                android.util.Log.d("ChatRoomActivity", "Stopped adapter listener and cleared reference in onDestroy");
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "Failed to stop adapter listener and clear reference in onDestroy: " + e.getMessage());
            }
        }
        
        // Release bindings to avoid memory leaks and exceptions
        recyclerView.setAdapter(null);
        etMessage = null;
        tvChatRoomName = null;
        tvAnnouncement = null;
        btnSend = null;
        btnBack = null;
        btnGroupInfo = null;
        announcementLayout = null;
        bottomNavigation = null;
        
        super.onDestroy();
    }

    // Open user profile page
    private void openUserProfile(String userId, DocumentSnapshot userDoc) {
        try {
            // Create Intent
            Intent intent = new Intent(this, hk.hku.cs.hkuers.features.profile.UserProfileActivity.class);
            
            // Pass necessary user ID
            intent.putExtra("user_id", userId);
            
            // Pass chat room return information for correct return logic
            intent.putExtra("from_chat_room", "true");
            intent.putExtra("chat_room_id", chatRoomId);
            intent.putExtra("chat_room_name", chatRoomName);
            
            // If there's a document, pass more user information to reduce next page query
            if (userDoc != null) {
                // Pass user basic information
                if (userDoc.contains("uname") || userDoc.contains("name")) {
                    String username = userDoc.getString("uname");
                    if (username == null || username.isEmpty()) {
                        username = userDoc.getString("name");
                    }
                    if (username != null && !username.isEmpty()) {
                        intent.putExtra("user_name", username);
                    }
                }
                
                if (userDoc.contains("email")) {
                    intent.putExtra("user_email", userDoc.getString("email"));
                }
                
                // Add debug information to verify avatar URL exists and correct
                android.util.Log.d("ChatRoomActivity", "User document contains avatar_url: " + userDoc.contains("avatar_url"));
                if (userDoc.contains("avatar_url")) {
                    String avatarUrl = userDoc.getString("avatar_url");
                    intent.putExtra("user_avatar_url", avatarUrl);
                    android.util.Log.d("ChatRoomActivity", "Passing to UserProfileActivity avatarUrl: " + avatarUrl);
                }
                
                if (userDoc.contains("department")) {
                    intent.putExtra("user_department", userDoc.getString("department"));
                }
                
                if (userDoc.contains("programme")) {
                    intent.putExtra("user_programme", userDoc.getString("programme"));
                }
                
                if (userDoc.contains("year_of_entry")) {
                    intent.putExtra("user_year_of_entry", userDoc.getString("year_of_entry"));
                }
                
                if (userDoc.contains("signature")) {
                    intent.putExtra("user_signature", userDoc.getString("signature"));
                }
            } else {
                android.util.Log.d("ChatRoomActivity", "userDoc is null, no user data passed");
            }
            
            // Start activity
            startActivity(intent);
            
            // Add transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            
            android.util.Log.d("ChatRoomActivity", "Opened user profile page, user ID: " + userId);
        } catch (Exception e) {
            android.util.Log.e("ChatRoomActivity", "Failed to open user profile: " + e.getMessage(), e);
            Toast.makeText(this, "Unable to open user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Add direct navigation method as alternative
    private <T extends AppCompatActivity> void directNavigateTo(Class<T> targetActivity) {
        android.util.Log.d("ChatRoomActivity", "directNavigateTo: Directly navigate to " + targetActivity.getSimpleName());
        
        // Stop any ongoing operations
        if (adapter != null) {
            try {
                adapter.stopListening();
                android.util.Log.d("ChatRoomActivity", "Stopped adapter listener in directNavigateTo");
            } catch (Exception e) {
                android.util.Log.e("ChatRoomActivity", "Failed to stop adapter in directNavigateTo: " + e.getMessage());
            }
        }
        
        // Directly start activity
        try {
            Intent intent = new Intent(this, targetActivity);
            startActivity(intent);
            android.util.Log.d("ChatRoomActivity", "Directly started new activity in directNavigateTo");
            finish();
            android.util.Log.d("ChatRoomActivity", "Directly finished current activity in directNavigateTo");
        } catch (Exception e) {
            android.util.Log.e("ChatRoomActivity", "Failed to navigate in directNavigateTo: " + e.getMessage());
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reset flag
        isFinishing = false;
        android.util.Log.d("ChatRoomActivity", "onResume: Activity resumed, reset isFinishing=false");
        
        // In onResume, reset adapter to avoid IndexOutOfBoundsException on resume state
        if (recyclerView != null && recyclerView.getAdapter() == null) {
            android.util.Log.d("ChatRoomActivity", "onResume: Resetting message adapter");
            setupMessageAdapter();
        }
        
        try {
            // Ensure adapter starts listening
            if (adapter != null && !isFinishing && !isFinishing()) {
                adapter.startListening();
                android.util.Log.d("ChatRoomActivity", "Started adapter listener in onResume");
                
                // Trigger scroll to latest message
                new Handler().postDelayed(() -> {
                    if (adapter != null && adapter.getItemCount() > 0 && !isFinishing && !isFinishing()) {
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }, 300);
            }
            
            // Update user read status
            updateUserReadStatus();
        } catch (Exception e) {
            android.util.Log.e("ChatRoomActivity", "Exception in onResume: " + e.getMessage(), e);
        }
    }
} 
