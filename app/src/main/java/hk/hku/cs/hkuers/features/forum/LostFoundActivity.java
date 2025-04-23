package hk.hku.cs.hkuers.features.forum;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hk.hku.cs.hkuers.R;
import hk.hku.cs.hkuers.features.forum.adapters.LostFoundAdapter;
import hk.hku.cs.hkuers.features.forum.models.LostFoundItem;

public class LostFoundActivity extends AppCompatActivity {
    private static final String TAG = "LostFoundActivity";

    private RecyclerView recyclerView;
    private LostFoundAdapter adapter;
    private List<LostFoundItem> lostFoundList;
    private List<LostFoundItem> filteredList;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_found);

        try {
            // 初始化Firebase
            db = FirebaseFirestore.getInstance();

            // 初始化列表
            lostFoundList = new ArrayList<>();
            filteredList = new ArrayList<>();

            // 初始化RecyclerView
            recyclerView = findViewById(R.id.rvLostFound);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new LostFoundAdapter(filteredList);
            recyclerView.setAdapter(adapter);

            // 设置下拉刷新
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            swipeRefreshLayout.setOnRefreshListener(this::loadLostFoundItems);

            // 设置TabLayout
            tabLayout = findViewById(R.id.tabLayout);
            setupTabLayout();

            // 设置发布按钮
            FloatingActionButton fabNewLostFound = findViewById(R.id.fabNewLostFound);
            fabNewLostFound.setOnClickListener(v -> openNewLostFoundActivity());

            // 加载数据
            loadLostFoundItems();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "加载失物招领页面时出错: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lost_found, menu);

        // 设置搜索功能
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterItems(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterItems(newText);
                return true;
            }
        });

        return true;
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterItems(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadLostFoundItems() {
        swipeRefreshLayout.setRefreshing(true);

        db.collection("lost_found")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefreshLayout.setRefreshing(false);

                    if (task.isSuccessful()) {
                        lostFoundList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LostFoundItem item = document.toObject(LostFoundItem.class);
                            lostFoundList.add(item);
                        }
                        filterItems(tabLayout.getSelectedTabPosition());
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(this, "加载数据失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterItems(int position) {
        filteredList.clear();
        String searchQuery = searchView != null ? searchView.getQuery().toString() : "";

        for (LostFoundItem item : lostFoundList) {
            boolean matchesType = position == 0 ||
                    (position == 1 && item.getType().equals("lost")) ||
                    (position == 2 && item.getType().equals("found"));

            boolean matchesSearch = searchQuery.isEmpty() ||
                    item.getTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                    item.getDescription().toLowerCase().contains(searchQuery.toLowerCase());

            if (matchesType && matchesSearch) {
                filteredList.add(item);
            }
        }

        adapter.updateList(filteredList);
    }

    private void filterItems(String query) {
        filterItems(tabLayout.getSelectedTabPosition());
    }

    private void openNewLostFoundActivity() {
        Intent intent = new Intent(this, NewLostFoundActivity.class);
        startActivityForResult(intent, REQUEST_CODE_NEW_LOST_FOUND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_NEW_LOST_FOUND && resultCode == RESULT_OK) {
            loadLostFoundItems(); // 刷新列表
        }
    }

    private static final int REQUEST_CODE_NEW_LOST_FOUND = 1;
}
