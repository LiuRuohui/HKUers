package hk.hku.cs.hkuers.features.courses;

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
import hk.hku.cs.hkuers.features.map.MapActivity;
import hk.hku.cs.hkuers.features.marketplace.MarketplaceActivity;
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
                Toast.makeText(this, "Forum feature coming soon", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(this, "Add course successfully",
                                                Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this,
                                                    "Update course failure: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failure to add course: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
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


