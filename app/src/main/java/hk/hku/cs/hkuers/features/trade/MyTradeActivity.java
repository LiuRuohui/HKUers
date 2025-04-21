package hk.hku.cs.hkuers.features.trade;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

import hk.hku.cs.hkuers.R;

public class MyTradeActivity extends AppCompatActivity {
    private static final String TAG = "MyTradeActivity";
    private static final String[] TAB_TITLES = new String[]{"My Posted Items", "My Favorited Items"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "onCreate: 开始创建MyTradeActivity");
            setContentView(R.layout.activity_my_trade);

            // 检查用户是否登录
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Log.d(TAG, "onCreate: 用户未登录，结束活动");
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "onCreate: 设置工具栏");
            // 设置工具栏
            setSupportActionBar(findViewById(R.id.toolbar));
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            Log.d(TAG, "onCreate: 设置ViewPager和TabLayout");
            // 设置ViewPager2和TabLayout
            ViewPager2 viewPager = findViewById(R.id.viewPager);
            TabLayout tabLayout = findViewById(R.id.tabLayout);

            Log.d(TAG, "onCreate: 创建适配器");
            viewPager.setAdapter(new FragmentStateAdapter(this) {
                @Override
                public int getItemCount() {
                    return TAB_TITLES.length;
                }

                @Override
                public Fragment createFragment(int position) {
                    Log.d(TAG, "createFragment: 创建Fragment，位置=" + position);
                    Fragment fragment = position == 0 ? new MyPostsFragment() : new MyFavoritesFragment();
                    Log.d(TAG, "createFragment: 创建了" + (position == 0 ? "MyPostsFragment" : "MyFavoritesFragment"));
                    return fragment;
                }
            });

            Log.d(TAG, "onCreate: 设置TabLayoutMediator");
            new TabLayoutMediator(tabLayout, viewPager,
                    (tab, position) -> tab.setText(TAB_TITLES[position])).attach();
            
            Log.d(TAG, "onCreate: MyTradeActivity创建完成");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: 创建过程中发生错误", e);
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        try {
            Log.d(TAG, "onBackPressed: 返回上一页面");
            finish();
        } catch (Exception e) {
            Log.e(TAG, "onBackPressed: 返回时发生错误", e);
            super.onBackPressed();
        }
    }
} 