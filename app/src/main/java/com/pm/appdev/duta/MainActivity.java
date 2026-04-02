package com.pm.appdev.duta;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pm.appdev.duta.Common.NodeNames;
import com.pm.appdev.duta.chats.ChatFragment;
import com.pm.appdev.duta.findfriends.FindFriendsFragment;
import com.pm.appdev.duta.notifications.NotificationCheckManager;
import com.pm.appdev.duta.notifications.NotificationSnapshot;
import com.pm.appdev.duta.profile.ProfileActivity;
import com.pm.appdev.duta.requests.RequestsFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Toolbar toolbar;
    private ImageView iconCamera, iconSearch, iconMore;
    private boolean doubleBackPressed = false;
    private NotificationCheckManager notificationCheckManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(decorView);
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        tabLayout = findViewById(R.id.tabMain);
        viewPager = findViewById(R.id.vpMain);

        iconCamera = findViewById(R.id.icon_camera);
        iconSearch = findViewById(R.id.icon_search);
        iconMore = findViewById(R.id.icon_more);

        iconCamera.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Camera Clicked", Toast.LENGTH_SHORT).show()
        );

        iconSearch.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Search Clicked", Toast.LENGTH_SHORT).show()
        );

        iconMore.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "More Options Clicked", Toast.LENGTH_SHORT).show()
        );

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            DatabaseReference databaseReferenceUsers = FirebaseDatabase.getInstance().getReference()
                    .child(NodeNames.USERS).child(firebaseAuth.getCurrentUser().getUid());

            databaseReferenceUsers.child(NodeNames.ONLINE).setValue(true);
            databaseReferenceUsers.child(NodeNames.ONLINE).onDisconnect().setValue(false);
        }

        setViewPager();
        setupNotificationChecks();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleCustomBackPress();
            }
        });
    }

    private void setupNotificationChecks() {
        notificationCheckManager = new NotificationCheckManager(this, new NotificationCheckManager.Listener() {
            @Override
            public void onNotificationSnapshot(@NonNull NotificationSnapshot snapshot) {
                updateTabBadge(0, snapshot.getUnreadChatCount());
                updateTabBadge(1, snapshot.getPendingRequestCount());
            }

            @Override
            public void onCheckError(@NonNull String reason, boolean retryable) {
                Log.w(TAG, "Notification check error. retryable=" + retryable + " reason=" + reason);
            }

            @Override
            public void onAuthRequired() {
                Log.w(TAG, "Notification check stopped: user is not authenticated");
                clearTabBadge(0);
                clearTabBadge(1);
            }
        });
    }

    private void setViewPager() {
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_chat));
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_requests));
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_findfriends));

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });
    }

    private void updateTabBadge(int tabIndex, int count) {
        TabLayout.Tab tab = tabLayout.getTabAt(tabIndex);
        if (tab == null) {
            return;
        }

        if (count <= 0) {
            tab.removeBadge();
            return;
        }

        tab.getOrCreateBadge().setVisible(true);
        tab.getOrCreateBadge().setMaxCharacterCount(2);
        tab.getOrCreateBadge().setNumber(Math.min(count, 99));
    }

    private void clearTabBadge(int tabIndex) {
        TabLayout.Tab tab = tabLayout.getTabAt(tabIndex);
        if (tab != null) {
            tab.removeBadge();
        }
    }

    public class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new ChatFragment();
                case 1:
                    return new RequestsFragment();
                case 2:
                    return new FindFriendsFragment();
                default:
                    return new ChatFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.mnuProfile) {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleCustomBackPress() {
        if (tabLayout.getSelectedTabPosition() > 0) {
            tabLayout.selectTab(tabLayout.getTabAt(0));
        } else {
            if (doubleBackPressed) {
                finishAffinity();
            } else {
                doubleBackPressed = true;
                Toast.makeText(this, R.string.press_back_to_exit, Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(() -> doubleBackPressed = false, 2000);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (notificationCheckManager != null) {
            notificationCheckManager.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notificationCheckManager != null) {
            notificationCheckManager.stop();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(decorView);
            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }
}
