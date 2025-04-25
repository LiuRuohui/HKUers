package hk.hku.cs.hkuers.features.courses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hk.hku.cs.hkuers.MainActivity;
import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.chat.ChatListActivity;
import hk.hku.cs.hkuers.features.chat.ChatRoomActivity;
import hk.hku.cs.hkuers.features.forum.ForumActivity;
import hk.hku.cs.hkuers.features.map.MapActivity;
import hk.hku.cs.hkuers.features.marketplace.MarketplaceActivity;
import hk.hku.cs.hkuers.models.ChatGroup;
import hk.hku.cs.hkuers.models.Course;

public class CourseSearchActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView tvEmptyCourseState;
    private BottomNavigationView bottomNavigation;
    private FirebaseFirestore db;
    private FirestoreRecyclerAdapter<Course, CourseSearchActivity.CourseViewHolder> adapter;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerCourses);
        tvEmptyCourseState = findViewById(R.id.tvEmptyCourseState);
        ImageButton btnAddCourse = findViewById(R.id.btnAddCourse);
        ImageButton btnBack = findViewById(R.id.btnBack);
        
        // 初始化底部导航
        bottomNavigation = findViewById(R.id.bottom_navigation);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnBack.setOnClickListener(v -> {
            bottomNavigation.setSelectedItemId(R.id.navigation_dashboard);
            finish();
        });

        btnAddCourse.setOnClickListener(v -> showAddCourseDialog());

        setupBottomNavigation();

        loadCourses();
    }

    private void showAddCourseDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_course, null);
        TextInputLayout tilCourseId       = dialogView.findViewById(R.id.tilCourseId);
        TextInputLayout tilCourseClass    = dialogView.findViewById(R.id.tilCourseClass);
        TextInputLayout tilCourseSemester = dialogView.findViewById(R.id.tilCourseSemester);
        TextInputEditText etCourseId      = dialogView.findViewById(R.id.addCourseId);
        TextInputEditText etCourseClass   = dialogView.findViewById(R.id.addCourseClass);
        TextInputEditText etCourseSemester= dialogView.findViewById(R.id.addCourseSemester);

        // 先创建但不设置点击事件
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Add Course")
                .setView(dialogView)
                .setPositiveButton("Add", null)    // 先传 null
                .setNegativeButton("Cancel", null)
                .show();

        // 再拿到按钮，自己做点击拦截
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String courseId       = etCourseId.getText().toString().trim();
            String courseClass    = etCourseClass.getText().toString().trim();
            String courseSemester = etCourseSemester.getText().toString().trim();

            boolean valid = true;

            if (courseId.isEmpty()) {
                tilCourseId.setError("Course ID is required");
                valid = false;
            } else {
                tilCourseId.setError(null);
            }
            if (courseClass.isEmpty()) {
                tilCourseClass.setError("Class is required");
                valid = false;
            } else {
                tilCourseClass.setError(null);
            }
            if (courseSemester.isEmpty()) {
                tilCourseSemester.setError("Semester is required");
                valid = false;
            } else {
                tilCourseSemester.setError(null);
            }

            if (valid) {
                addCourse(courseId, courseSemester, courseClass);
                dialog.dismiss();
            }
        });
    }

    private void addCourse(String courseId, String courseSemester, String courseClass) {
        db.collection("courses")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("courseSemester", courseSemester)
                .whereEqualTo("courseClass", courseClass)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentReference existingCourseRef = queryDocumentSnapshots.getDocuments().get(0).getReference();
                        String existingCourseIdFireStore = existingCourseRef.getId();
                        joinExistingCourse(existingCourseIdFireStore);
//                        addUserToCourse(existingCourseIdFireStore);
                    }
                    else {
                        AddNewCourseInternal(courseId, courseSemester, courseClass);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failure:  " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void joinExistingCourse(String courseIdFireStore) {
        db.collection("courses").document(courseIdFireStore)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> studentIds = (List<String>) documentSnapshot.get("studentIds");

                        if (studentIds != null && studentIds.contains(currentUser.getUid())) {
                            Toast.makeText(this, "You already add this course", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        studentIds.add(currentUser.getUid());

                        documentSnapshot.getReference().update("studentIds", studentIds)
                                .addOnSuccessListener(aVoid -> {
                                    addUserToCourse(courseIdFireStore);
                                });

                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failure: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );

        DocumentReference userRef = db
                .collection("users")
                .document(currentUser.getUid());
    }

    private void loadCourses() {
        Query query = db.collection("courses")
                .whereArrayContains("studentIds", currentUser.getUid());

        FirestoreRecyclerOptions<Course> options = new FirestoreRecyclerOptions.Builder<Course>()
                .setQuery(query, Course.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Course, CourseViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CourseViewHolder holder, int position, @NonNull Course model) {
                DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);

                String courseIdFireStore = snapshot.getId();
                model.setCourseIdFireStore(courseIdFireStore);

                String idFromDb = snapshot.getString("courseId");
                model.setCourseId((idFromDb == null || idFromDb.isEmpty()) ? "Unknown" : idFromDb);

                String semesterFromDb = snapshot.getString("courseSemester");
                model.setCourseSemester((semesterFromDb == null || semesterFromDb.isEmpty()) ? "Unknown" : semesterFromDb);

                String classFromDb = snapshot.getString("courseClass");
                model.setCourseClass((classFromDb == null || classFromDb.isEmpty()) ? "Unknown" : classFromDb);

                holder.bind(model);
            }

            @NonNull
            @Override
            public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_course, parent, false);
                return new CourseViewHolder(view);
            }

            @Override
            public void onDataChanged() {
                int itemCount = getItemCount();
                android.util.Log.d("ChatListActivity", "Load successfully, totally " + itemCount + " courses");

                if (itemCount == 0) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyCourseState.setVisibility(View.VISIBLE);
                    android.util.Log.d("CoursesActivity", "Courses unavailable");
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmptyCourseState.setVisibility(View.GONE);
                    android.util.Log.d("CoursesActivity", "Courses available, show lists");
                }
            }
        };

        recyclerView.setAdapter(adapter);
        android.util.Log.d("CoursesActivity", "Adapter set");
    }

    private void setupBottomNavigation() {
        // 设置选中Courses选项
        bottomNavigation.setSelectedItemId(R.id.navigation_courses);
        
        // 设置导航点击监听
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_chat) {
                startActivity(new Intent(this, ChatListActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_forum) {
                startActivity(new Intent(this, ForumActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_dashboard) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_courses) {
                // 已经在课程页面，不需要操作
                return true;
            } else if (itemId == R.id.navigation_marketplace) {
                startActivity(new Intent(this, MarketplaceActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCourseId, tvCourseSemester, tvCourseClass;
        private final View colorIndicator;
        private final ImageButton btnDelete;
        public static final int[] COURSE_COLORS = {
                0xFF4CAF50,
                0xFF2196F3,
                0xFFE91E63,
                0xFFFF9800,
                0xFF9C27B0,
                0xFF00BCD4,
                0xFFFF5722,
                0xFF607D8B,
                0xFFFFC107,
                0xFF795548,
                0xFF673AB7,
                0xFF8BC34A,
                0xFF3F51B5,
                0xFFFF4081,
                0xFF009688
        };
        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseId = itemView.findViewById(R.id.tvCourseId);
            tvCourseSemester = itemView.findViewById(R.id.tvCourseSemester);
            tvCourseClass = itemView.findViewById(R.id.tvCourseClass);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
            btnDelete = itemView.findViewById(R.id.btnDeleteCourse);
        }
        public void bind(Course course) {
            tvCourseId.setText(course.getCourseId());
            tvCourseSemester.setText(course.getCourseSemester());
            tvCourseClass.setText(course.getCourseClass());

            String courseIdFireStore = course.getCourseIdFireStore();
            if (courseIdFireStore != null && !courseIdFireStore.isEmpty()) {
                int colorIndex = Math.abs(courseIdFireStore.hashCode()) % COURSE_COLORS.length;
                colorIndicator.setBackgroundColor(COURSE_COLORS[colorIndex]);
            } else {
                colorIndicator.setBackgroundColor(COURSE_COLORS[0]);
            }

            // 添加整个课程项的点击事件，提示用户是否加入课程群聊
            itemView.setOnClickListener(v -> {
                Context context = itemView.getContext();
                if (context instanceof CourseSearchActivity) {
                    CourseSearchActivity activity = (CourseSearchActivity) context;
                    activity.checkAndPromptJoinCourseChat(
                            course.getCourseId(), 
                            course.getCourseClass(), 
                            course.getCourseSemester()
                    );
                }
            });

            btnDelete.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(itemView.getContext())
                        .setTitle("Delete confirmation")
                        .setMessage("Are you sure you want to delete the course?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            String docId = course.getCourseIdFireStore();
                            FirebaseFirestore.getInstance()
                                    .collection("courses")
                                    .document(docId)
                                    .update("studentIds", FieldValue.arrayRemove(
                                            FirebaseAuth.getInstance().getCurrentUser().getUid()))
                                    .addOnSuccessListener(aVoid -> Toast.makeText(
                                            itemView.getContext(), "Delete course successfully", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(
                                            itemView.getContext(), "Course deletion failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void AddNewCourseInternal(String courseId, String courseSemester, String courseClass) {
        String courseIdFireStore = db.collection("courses").document().getId();

        Map<String, Object> courseData = new HashMap<>();
        courseData.put("courseId", courseId);
        courseData.put("courseSemester", courseSemester);
        courseData.put("courseClass", courseClass);
//        courseData.put("student_id", currentUser.getUid());

        int colorIndex = Math.abs(courseIdFireStore.hashCode()) % CourseViewHolder.COURSE_COLORS.length;
        String colorCode =  String.format("#%06X", (0XFFFFFF & CourseViewHolder.COURSE_COLORS[colorIndex]));
        courseData.put("color_code", colorCode);

        List<String> studentIds = new ArrayList<>();
        studentIds.add(currentUser.getUid());
        courseData.put("studentIds", studentIds);

        db.collection("courses").document(courseIdFireStore)
                .set(courseData)
                .addOnSuccessListener(documentReference -> {
                    addUserToCourse(courseIdFireStore);

                    Toast.makeText(this, "Add Course Successfully", Toast.LENGTH_SHORT).show();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failure: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void addUserToCourse(String courseIdFireStore) {
        Map<String, Object> studentData = new HashMap<>();
        studentData.put("user_id", currentUser.getUid());
        studentData.put("email", currentUser.getEmail());
        studentData.put("name", currentUser.getDisplayName());
        studentData.put("joined_at", new Timestamp(new Date()));

        db.collection("courses")
                .document(courseIdFireStore)
                .collection("students")
                .document(currentUser.getUid())
                .set(studentData)
                .addOnSuccessListener(aVoid -> {
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(currentUser.getUid())
                            .update("courses", FieldValue.arrayUnion(courseIdFireStore))
                            .addOnSuccessListener(__ -> {
                                Toast.makeText(this, "Course added successfully",
                                                Toast.LENGTH_SHORT).show();
                                
                                // Get course details to check/create course chat
                                db.collection("courses").document(courseIdFireStore)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String courseId = documentSnapshot.getString("courseId");
                                            String courseClass = documentSnapshot.getString("courseClass");
                                            String courseSemester = documentSnapshot.getString("courseSemester");
                                            
                                            if (courseId != null && courseClass != null && courseSemester != null) {
                                                // Check if course chat exists, create if not
                                                checkAndCreateCourseChat(courseId, courseClass, courseSemester);
                                            }
                                        }
                                    });
                            })
                            .addOnFailureListener(e -> Toast.makeText(this,
                                                    "Failed to update course: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add course: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
    
    /**
     * Check if a course chat exists and create it if not
     * @param courseId Course ID
     * @param courseClass Course class
     * @param courseSemester Course semester
     */
    private void checkAndCreateCourseChat(String courseId, String courseClass, String courseSemester) {
        // Generate chat room name format: courseId-class-semester
        String chatName = courseId + "-" + courseClass + "-" + courseSemester;
        
        // Check if the chat room already exists
        db.collection("chat_rooms")
            .whereEqualTo("chat_name", chatName)
            .whereEqualTo("group_type", ChatGroup.TYPE_COURSE)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    // Course chat already exists, check if user is already a member
                    DocumentReference existingChatRef = queryDocumentSnapshots.getDocuments().get(0).getReference();
                    String existingChatId = existingChatRef.getId();
                    
                    // Check if user is already a member
                    existingChatRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            List<String> memberIds = (List<String>) documentSnapshot.get("member_ids");
                            
                            if (memberIds != null && !memberIds.contains(currentUser.getUid())) {
                                // User is not a member, automatically add them to the chat
                                joinCourseChatGroup(existingChatId, chatName);
                            }
                        }
                    });
                } else {
                    // Course chat doesn't exist, create one
                    createCourseChatGroup(courseId, courseClass, courseSemester);
                }
            });
    }
    
    /**
     * Check if a course chat exists and prompt the user to join
     * @param courseId Course ID
     * @param courseClass Course class
     * @param courseSemester Course semester
     */
    public void checkAndPromptJoinCourseChat(String courseId, String courseClass, String courseSemester) {
        // Generate chat room name format: courseId-class-semester
        String chatName = courseId + "-" + courseClass + "-" + courseSemester;
        
        // Check if the chat room already exists
        db.collection("chat_rooms")
            .whereEqualTo("chat_name", chatName)
            .whereEqualTo("group_type", ChatGroup.TYPE_COURSE)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    // Course chat already exists, check if user is already a member
                    DocumentReference existingChatRef = queryDocumentSnapshots.getDocuments().get(0).getReference();
                    String existingChatId = existingChatRef.getId();
                    
                    // Check if user is already a member
                    existingChatRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            List<String> memberIds = (List<String>) documentSnapshot.get("member_ids");
                            
                            if (memberIds != null && memberIds.contains(currentUser.getUid())) {
                                // User is already a member, ask if they want to enter the chat
                                new AlertDialog.Builder(this)
                                    .setTitle("Course Chat")
                                    .setMessage("Would you like to enter the chat room for " + courseId + "?")
                                    .setPositiveButton("Enter", (dialog, which) -> {
                                        openChatActivity(existingChatId, chatName);
                                    })
                                    .setNegativeButton("Not now", null)
                                    .show();
                            } else {
                                // User is not a member, ask if they want to join
                                new AlertDialog.Builder(this)
                                    .setTitle("Join Course Chat")
                                    .setMessage("Would you like to join the chat room for " + courseId + "?")
                                    .setPositiveButton("Join", (dialog, which) -> {
                                        joinCourseChatGroup(existingChatId, chatName);
                                    })
                                    .setNegativeButton("No thanks", null)
                                    .show();
                            }
                        }
                    });
                } else {
                    // This case shouldn't happen anymore since we create chats automatically
                    // But just in case, we'll handle it
                    new AlertDialog.Builder(this)
                        .setTitle("Course Chat Not Found")
                        .setMessage("No chat room found for this course. Creating one now...")
                        .setPositiveButton("OK", (dialog, which) -> {
                            createCourseChatGroup(courseId, courseClass, courseSemester);
                        })
                        .show();
                }
            });
    }
    
    /**
     * 创建课程群聊
     * @param courseId 课程ID
     * @param courseClass 课程班级
     * @param courseSemester 课程学期
     */
    private void createCourseChatGroup(String courseId, String courseClass, String courseSemester) {
        // 生成聊天室名称 格式: 课程-班级-学期
        String chatName = courseId + "-" + courseClass + "-" + courseSemester;
        
        // 获取当前时间
        Timestamp now = new Timestamp(new Date());
        
        // 生成唯一ID
        String chatId = db.collection("chat_rooms").document().getId();
        
        // 在Firestore中创建聊天室文档
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chat_id", chatId);
        chatData.put("chat_name", chatName);
        chatData.put("courseId", courseId);
        chatData.put("courseClass", courseClass);
        chatData.put("courseSemester", courseSemester);
        chatData.put("creator_id", "system"); // 系统创建，没有群主
        chatData.put("created_at", now);
        chatData.put("is_active", true);
        chatData.put("group_type", ChatGroup.TYPE_COURSE); // 设置为课程群聊类型
        
        // 设置最后消息时间为创建时间，确保聊天室能在列表查询中显示
        chatData.put("last_message_time", now);
        chatData.put("last_message", "Course chat created. Welcome to discuss!");
        
        // 为聊天室生成一个随机颜色代码
        int colorIndex = Math.abs(chatId.hashCode()) % 15; // 与Chat列表颜色保持一致
        String colorCode = String.format("#%06X", (0xFFFFFF & ChatListActivity.ChatGroupViewHolder.GROUP_COLORS[colorIndex]));
        chatData.put("color_code", colorCode);
        
        // 初始化成员列表
        List<String> memberIds = new ArrayList<>();
        memberIds.add(currentUser.getUid());
        chatData.put("member_ids", memberIds);
        
        // 初始化消息计数
        chatData.put("message_count", 0L);
        
        // 初始化用户读取状态
        Map<String, Object> userReadStatus = new HashMap<>();
        userReadStatus.put(currentUser.getUid(), now);
        chatData.put("user_read_status", userReadStatus);
        
        // 初始化读取计数
        Map<String, Object> readCounts = new HashMap<>();
        readCounts.put(currentUser.getUid(), 0L);
        chatData.put("read_counts", readCounts);

        // 保存聊天室数据
        db.collection("chat_rooms").document(chatId)
                .set(chatData)
                .addOnSuccessListener(documentReference -> {
                    // 将用户添加到成员列表
                    addUserToChatGroup(chatId, chatName);
                    
                    // 添加一条初始消息
                    addInitialMessage(chatId, courseId, courseClass, courseSemester);
                    
                    // 提示用户
                    Toast.makeText(this, "Course chat created successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> 
                    android.util.Log.e("CourseSearchActivity", "Failed to create course chat: " + e.getMessage())
                );
    }
    
    /**
     * 加入课程群聊
     * @param chatId 聊天室ID
     * @param chatName 聊天室名称
     */
    private void joinCourseChatGroup(String chatId, String chatName) {
        // 检查用户是否已经是群组成员
        db.collection("chat_rooms").document(chatId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> memberIds = (List<String>) documentSnapshot.get("member_ids");
                    
                    if (memberIds != null && memberIds.contains(currentUser.getUid())) {
                        // 用户已经是成员，直接提示
                        Toast.makeText(this, "You are already a member of this chat", Toast.LENGTH_SHORT).show();
                        
                        // 询问是否立即进入聊天
                        new AlertDialog.Builder(this)
                            .setTitle("Enter Chat")
                            .setMessage("Would you like to enter the course chat now?")
                            .setPositiveButton("Enter", (dialog, which) -> {
                                openChatActivity(chatId, chatName);
                            })
                            .setNegativeButton("Later", null)
                            .show();
                        return;
                    }
                    
                    // 用户不是成员，添加用户到成员列表
                    memberIds.add(currentUser.getUid());
                    
                    // 更新聊天室文档
                    documentSnapshot.getReference().update("member_ids", memberIds)
                        .addOnSuccessListener(aVoid -> {
                            // 添加用户详细信息到成员子集合
                            addUserToChatGroup(chatId, chatName);
                            
                            // 更新用户读取状态
                            Timestamp now = new Timestamp(new Date());
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("user_read_status." + currentUser.getUid(), now);
                            
                            // 获取当前消息计数
                            Long messageCount = documentSnapshot.getLong("message_count");
                            if (messageCount == null) messageCount = 0L;
                            
                            // 更新用户读取计数
                            updates.put("read_counts." + currentUser.getUid(), messageCount);
                            
                            documentSnapshot.getReference().update(updates)
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "Successfully joined course chat", Toast.LENGTH_SHORT).show();
                                    
                                    // 添加用户加入的系统消息
                                    addUserJoinMessage(chatId);
                                    
                                    // 询问是否立即进入聊天
                                    new AlertDialog.Builder(this)
                                        .setTitle("Enter Chat")
                                        .setMessage("Would you like to enter the course chat now?")
                                        .setPositiveButton("Enter", (dialog, which) -> {
                                            openChatActivity(chatId, chatName);
                                        })
                                        .setNegativeButton("Later", null)
                                        .show();
                                });
                        });
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to join course chat: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }
    
    /**
     * 将用户添加到群聊成员列表
     * @param chatId 聊天室ID
     * @param chatName 聊天室名称
     */
    private void addUserToChatGroup(String chatId, String chatName) {
        // 添加用户详细信息到聊天室成员子集合
        Map<String, Object> memberData = new HashMap<>();
        memberData.put("user_id", currentUser.getUid());
        memberData.put("email", currentUser.getEmail());
        memberData.put("display_name", currentUser.getDisplayName());
        memberData.put("joined_at", new Timestamp(new Date()));
        memberData.put("is_admin", false);  // 课程群聊中用户不是管理员
        memberData.put("is_active", true);
        
        // 保存到聊天室的成员子集合
        db.collection("chat_rooms")
            .document(chatId)
            .collection("members")
            .document(currentUser.getUid())
            .set(memberData);
    }
    
    /**
     * 添加用户加入的系统消息
     * @param chatId 聊天室ID
     */
    private void addUserJoinMessage(String chatId) {
        String userName = currentUser.getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = currentUser.getEmail();
        }
        
        // 创建final变量以便在lambda表达式中使用
        final String finalUserName = userName;
        
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", "system");
        message.put("text", finalUserName + " joined the chat");
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "text");
        
        db.collection("chat_rooms").document(chatId)
          .collection("messages")
          .add(message)
          .addOnSuccessListener(documentReference -> {
              // 更新聊天室的消息计数和最后消息时间
              db.collection("chat_rooms").document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long currentCount = documentSnapshot.getLong("message_count");
                        if (currentCount == null) currentCount = 0L;
                        
                        documentSnapshot.getReference().update(
                            "message_count", currentCount + 1,
                            "last_message", finalUserName + " joined the chat",
                            "last_message_time", new Timestamp(new Date())
                        );
                    }
                });
          });
    }
    
    /**
     * 添加初始消息到聊天室
     * @param chatId 聊天室ID
     * @param courseId 课程ID
     * @param courseClass 课程班级
     * @param courseSemester 课程学期
     */
    private void addInitialMessage(String chatId, String courseId, String courseClass, String courseSemester) {
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", "system");
        message.put("text", courseId + "-" + courseClass + "-" + courseSemester + " course chat created. Welcome to discuss!");
        message.put("timestamp", new Timestamp(new Date()));
        message.put("type", "text");
        
        db.collection("chat_rooms").document(chatId)
          .collection("messages")
          .add(message)
          .addOnSuccessListener(documentReference -> {
              // 更新聊天室的消息计数和最后消息时间
              db.collection("chat_rooms").document(chatId)
                .update(
                    "message_count", 1L,
                    "last_message", courseId + "-" + courseClass + "-" + courseSemester + " course chat created. Welcome to discuss!",
                    "last_message_time", new Timestamp(new Date())
                );
          });
    }
    
    /**
     * 打开聊天页面
     * @param chatId 聊天室ID
     * @param chatName 聊天室名称
     */
    private void openChatActivity(String chatId, String chatName) {
        Intent intent = new Intent(CourseSearchActivity.this, ChatRoomActivity.class);
        intent.putExtra("chatRoomId", chatId);
        intent.putExtra("chatRoomName", chatName);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}


