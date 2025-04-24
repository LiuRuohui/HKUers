package hk.hku.cs.hkuers.features.forum;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.adapters.LostFoundAdapter;
import hk.hku.cs.hkuers.features.forum.models.LostFoundItem;

// 首先，添加必要的导入
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.google.firebase.firestore.DocumentSnapshot;

public class LostFoundActivity extends AppCompatActivity {
    private static final String TAG = "LostFoundActivity";

    private RecyclerView recyclerView;
    private LostFoundAdapter adapter;
    private List<LostFoundItem> lostFoundItems;
    private List<LostFoundItem> filteredList;
    private Spinner spinnerCategory;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputEditText searchEditText;
    private FloatingActionButton fabNewLostFound;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormatter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_found);

        try {
            // 初始化Firebase Auth
            mAuth = FirebaseAuth.getInstance();

            // 初始化日期格式化器
            dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            // 初始化列表
            lostFoundItems = new ArrayList<>();
            filteredList = new ArrayList<>();

            // 初始化Firebase
            db = FirebaseFirestore.getInstance();

            // 初始化SwipeRefreshLayout
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setOnRefreshListener(this::refreshData);
            }

            // 初始化搜索框
            searchEditText = findViewById(R.id.searchEditText);
            if (searchEditText != null) {
                searchEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        filterBySearchText(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
            }

            // 初始化RecyclerView - 使用正确的ID
            recyclerView = findViewById(R.id.rvLostFound);
            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView not found! Check your layout ID");
                Toast.makeText(this, "Error: RecyclerView not found", Toast.LENGTH_SHORT).show();
                return; // 如果关键视图丢失，提前退出
            }

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new LostFoundAdapter(this, filteredList); // 添加 this 作为 Context 参数
            recyclerView.setAdapter(adapter);

            // 初始化分类下拉菜单
            spinnerCategory = findViewById(R.id.spinnerCategory);
            if (spinnerCategory != null) {
                // 设置spinner
                String[] itemTypes = {"全部", "手机", "钱包", "钥匙", "卡片", "其他"};
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, itemTypes);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(spinnerAdapter);

                spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        // 安全地调用过滤方法
                        filterItems(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // 不做任何处理
                    }
                });
            } else {
                Log.e(TAG, "spinnerCategory not found in layout!");
            }

            // 初始化浮动操作按钮
            fabNewLostFound = findViewById(R.id.fabNewLostFound);
            if (fabNewLostFound != null) {
                fabNewLostFound.setOnClickListener(v -> showAddPostDialog());
            } else {
                Log.e(TAG, "FAB not found in layout!");
            }

            // 加载初始数据
            loadLostFoundItems();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LostFoundAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);
    }

    private void refreshData() {
        // 刷新数据
        loadLostFoundItems();

        // 结束刷新动画
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void filterBySearchText(String query) {
        if (query == null || query.isEmpty()) {
            // 如果搜索框为空，使用分类过滤
            if (spinnerCategory != null) {
                filterItems(spinnerCategory.getSelectedItemPosition());
            } else {
                // 如果没有分类选择器，显示所有项目
                filteredList.clear();
                filteredList.addAll(lostFoundItems);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
            return;
        }

        // 执行搜索过滤
        List<LostFoundItem> searchResults = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (LostFoundItem item : lostFoundItems) {
            // 在标题、描述和地点中搜索
            if ((item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerCaseQuery)) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerCaseQuery)) ||
                    (item.getLocation() != null && item.getLocation().toLowerCase().contains(lowerCaseQuery))) {
                searchResults.add(item);
            }
        }

        // 更新列表
        filteredList.clear();
        filteredList.addAll(searchResults);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void loadLostFoundItems() {
        try {
            // 显示加载指示器
            if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(true);
            }

            // 清除现有数据
            lostFoundItems.clear();

            // 从 Firestore 获取数据
            db.collection("lostFoundItems")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (DocumentSnapshot document : queryDocumentSnapshots) {
                                LostFoundItem item = document.toObject(LostFoundItem.class);
                                if (item != null) {
                                    lostFoundItems.add(item);
                                }
                            }

                            // 应用当前过滤器
                            if (searchEditText != null && searchEditText.getText() != null &&
                                    !searchEditText.getText().toString().isEmpty()) {
                                // 如果有搜索文本，按搜索文本过滤
                                filterBySearchText(searchEditText.getText().toString());
                            } else if (spinnerCategory != null) {
                                // 否则按分类过滤
                                filterItems(spinnerCategory.getSelectedItemPosition());
                            } else {
                                // 如果没有过滤器，显示所有项目
                                filteredList.clear();
                                filteredList.addAll(lostFoundItems);
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        } else {
                            Log.d(TAG, "No documents found");
                            // 如果没有数据，添加示例数据
                            addSampleData();
                        }

                        // 隐藏加载指示器
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting documents: ", e);
                        Toast.makeText(this, "Error loading items", Toast.LENGTH_SHORT).show();
                        // 发生错误时添加示例数据
                        addSampleData();
                        // 隐藏加载指示器
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error loading items", e);
            Toast.makeText(this, "Error loading items", Toast.LENGTH_SHORT).show();
            // 确保隐藏加载指示器
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    private void addSampleData() {
        Log.d(TAG, "Adding sample data");
        // 添加几个样本数据项进行测试
        try {
            lostFoundItems.clear();
            filteredList.clear();
            LostFoundItem item1 = new LostFoundItem(
                    "item1",
                    "丢失iPhone 13手机",
                    "黑色iPhone 13，蓝色手机壳，有明显划痕",
                    "图书馆一楼",
                    new Date(),
                    "john@example.com",
                    "lost", // 类型：丢失
                    "", // 暂无图片
                    "user123",
                    "张三",
                    "手机" // 分类
            );
            lostFoundItems.add(item1);
            Log.d(TAG, "Added item1: " + item1.getTitle());

            LostFoundItem item2 = new LostFoundItem(
                    "item2",
                    "捡到学生证",
                    "捡到一张学生证，姓名李明，学号2021xxxx",
                    "学生中心",
                    new Date(),
                    "mary@example.com",
                    "found", // 类型：拾得
                    "", // 暂无图片
                    "user456",
                    "李四",
                    "卡片" // 分类
            );
            lostFoundItems.add(item2);
            Log.d(TAG, "Added item2: " + item2.getTitle());

            LostFoundItem item3 = new LostFoundItem(
                    "item3",
                    "丢失钱包",
                    "黑色皮质钱包，内有少量现金和银行卡",
                    "饭堂",
                    new Date(),
                    "tom@example.com",
                    "lost", // 类型：丢失
                    "", // 暂无图片
                    "user789",
                    "王五",
                    "钱包" // 分类
            );
            lostFoundItems.add(item3);
            Log.d(TAG, "Added item3: " + item3.getTitle());

            // 更新 filteredList
            filteredList.addAll(lostFoundItems);
            Log.d(TAG, "Updated filteredList with " + filteredList.size() + " items");

            // 通知适配器
            if (adapter != null) {
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Notified adapter of data change");
            } else {
                Log.e(TAG, "Adapter is null!");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding sample data: " + e.getMessage(), e);
        }
    }

    private void showAddPostDialog() {
        try {
            // 检查用户是否已登录
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "请先登录再发布信息", Toast.LENGTH_SHORT).show();
                // TODO: 如果需要，跳转到登录页面
                return;
            }

            // 创建对话框布局
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("发布失物招领信息");

            // 使用自定义布局
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_lost_found, null);
            builder.setView(dialogView);

            // 获取对话框控件
            final EditText etTitle = dialogView.findViewById(R.id.etTitle);
            final EditText etDescription = dialogView.findViewById(R.id.etDescription);
            final EditText etLocation = dialogView.findViewById(R.id.etLocation);
            final EditText etContactInfo = dialogView.findViewById(R.id.etContactInfo);
            final Button btnDate = dialogView.findViewById(R.id.btnDate);
            final RadioGroup rgType = dialogView.findViewById(R.id.rgType);
            final Spinner spinnerItemCategory = dialogView.findViewById(R.id.spinnerItemCategory);

            // 设置分类下拉框
            String[] categories = {"手机", "钱包", "钥匙", "卡片", "其他"};
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, categories);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerItemCategory.setAdapter(categoryAdapter);

            // 设置日期选择
            selectedDate = Calendar.getInstance();
            btnDate.setText(dateFormatter.format(selectedDate.getTime()));
            btnDate.setOnClickListener(v -> {
                showDatePickerDialog(btnDate);
            });

            // 如果已登录用户有联系信息，预填充
            if (currentUser.getEmail() != null) {
                etContactInfo.setText(currentUser.getEmail());
            }

            // 设置对话框按钮
            builder.setPositiveButton("发布", null); // 暂时设置为null，稍后重写
            builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

            // 创建并显示对话框
            AlertDialog dialog = builder.create();

            // 重写Positive按钮点击事件，以防空字段提交
            dialog.setOnShowListener(dialogInterface -> {
                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(view -> {
                    // 验证所有必填字段
                    if (validateFields(etTitle, etDescription, etLocation, etContactInfo, rgType, spinnerItemCategory)) {
                        // 收集表单数据
                        String title = etTitle.getText().toString().trim();
                        String description = etDescription.getText().toString().trim();
                        String location = etLocation.getText().toString().trim();
                        String contactInfo = etContactInfo.getText().toString().trim();
                        String category = spinnerItemCategory.getSelectedItem().toString();

                        // 获取类型 (lost/found)
                        String type = "lost"; // 默认为丢失
                        int selectedTypeId = rgType.getCheckedRadioButtonId();
                        if (selectedTypeId != -1) {
                            RadioButton selectedType = dialog.findViewById(selectedTypeId);
                            if (selectedType != null && "拾得物品".equals(selectedType.getText().toString())) {
                                type = "found";
                            }
                        }

                        // 创建并保存项目
                        saveNewItem(title, description, location, contactInfo, category, type);

                        // 关闭对话框
                        dialog.dismiss();
                    }
                });
            });

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing add post dialog: " + e.getMessage(), e);
            Toast.makeText(this, "无法创建发布对话框: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void filterItems(int position) {
        try {
            // 安全检查
            if (spinnerCategory == null) {
                Log.d(TAG, "spinnerCategory is null, using all items");
                filteredList.clear();
                filteredList.addAll(lostFoundItems);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                return;
            }

            filteredList.clear();

            if (position >= 0 && position < spinnerCategory.getCount()) {
                String selectedCategory = spinnerCategory.getItemAtPosition(position).toString();

                for (LostFoundItem item : lostFoundItems) {
                    // 首个选项"全部"或类别匹配时添加项目
                    if (position == 0 || "全部".equals(selectedCategory) ||
                            selectedCategory.equals(item.getCategory())) {
                        filteredList.add(item);
                    }
                }
            } else {
                // 无效位置，显示所有项目
                filteredList.addAll(lostFoundItems);
            }

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtering items: " + e.getMessage(), e);
            // 发生异常时，显示所有项目
            filteredList.clear();
            filteredList.addAll(lostFoundItems);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    // 显示日期选择器对话框
    private void showDatePickerDialog(final Button dateButton) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    dateButton.setText(dateFormatter.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // 验证表单字段
    private boolean validateFields(EditText etTitle, EditText etDescription,
                                   EditText etLocation, EditText etContactInfo,
                                   RadioGroup rgType, Spinner spinnerItemCategory) {
        boolean valid = true;

        // 检查标题
        if (etTitle.getText().toString().trim().isEmpty()) {
            etTitle.setError("请输入标题");
            valid = false;
        }

        // 检查描述
        if (etDescription.getText().toString().trim().isEmpty()) {
            etDescription.setError("请输入描述");
            valid = false;
        }

        // 检查地点
        if (etLocation.getText().toString().trim().isEmpty()) {
            etLocation.setError("请输入地点");
            valid = false;
        }

        // 检查联系方式
        if (etContactInfo.getText().toString().trim().isEmpty()) {
            etContactInfo.setError("请输入联系方式");
            valid = false;
        }

        // 检查类型选择
        if (rgType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "请选择物品类型（丢失/拾得）", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;
    }

    // 保存新项目到Firestore
    private void saveNewItem(String title, String description, String location,
                             String contactInfo, String category, String type) {
        try {
            // 显示加载提示
            Toast.makeText(this, "正在发布...", Toast.LENGTH_SHORT).show();

            // 获取当前用户信息
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
                return;
            }


            // 创建一个唯一ID
            String itemId = UUID.randomUUID().toString();

            // 创建新的LostFoundItem对象
            LostFoundItem newItem = new LostFoundItem(
                    itemId,
                    title,
                    description,
                    location,
                    selectedDate.getTime(), // 使用所选日期
                    contactInfo,
                    type,
                    "", // 暂无图片URL
                    user.getUid(),
                    user.getDisplayName() != null ? user.getDisplayName() : "匿名用户",
                    category
            );

            // 保存到Firestore
            db.collection("lostFoundItems")
                    .document(itemId)
                    .set(newItem)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(LostFoundActivity.this, "发布成功！", Toast.LENGTH_SHORT).show();
                        // 刷新数据
                        loadLostFoundItems();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding document", e);
                        Toast.makeText(LostFoundActivity.this, "发布失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving new item: " + e.getMessage(), e);
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
