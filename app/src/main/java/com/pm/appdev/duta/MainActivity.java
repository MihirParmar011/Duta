package com.pm.appdev.duta;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;


import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pm.appdev.duta.Common.NodeNames;
import com.pm.appdev.duta.chats.ChatFragment;
import com.pm.appdev.duta.findfriends.FindFriendsFragment;
import com.pm.appdev.duta.profile.ProfileActivity;
import com.pm.appdev.duta.requests.RequestsFragment;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Toolbar toolbar;
    private ImageView iconCamera, iconSearch, iconMore;
    private boolean doubleBackPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();

        // ✅ Allow layout in display cutout (notch area)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        // ✅ Extend layout fullscreen and under notch/nav bar
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

        // ✅ Make status and nav bar transparent
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        // ✅ Optional: hide system bars, reveal on swipe
        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(decorView);
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        // ✅ Handle padding if needed
        ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Optional padding logic (only if content goes under system bars)
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

//// Edge-to-edge setup
//        View decorView = getWindow().getDecorView();
//        ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, windowInsets) -> {
//            WindowInsetsCompat insets = windowInsets;
//            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//
//            // Apply padding to root view if needed
//            view.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, systemBarsInsets.bottom);
//
//            return WindowInsetsCompat.CONSUMED;
//        });
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            getWindow().setDecorFitsSystemWindows(false);
//        }
//        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
//        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
//
//        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(decorView);
//        if (controller != null) {
//            controller.setAppearanceLightStatusBars(true); // dark icons on light background
//            controller.setAppearanceLightNavigationBars(true);
//        }


        // Initialize Toolbar
        toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // Initialize UI Components
        tabLayout = findViewById(R.id.tabMain);
        viewPager = findViewById(R.id.vpMain);

        // Initialize Icons
        iconCamera = findViewById(R.id.icon_camera);
        iconSearch = findViewById(R.id.icon_search);
        iconMore = findViewById(R.id.icon_more);

        // Set Click Listeners for Icons
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
        DatabaseReference databaseReferenceUsers = FirebaseDatabase.getInstance().getReference()
                .child(NodeNames.USERS).child(firebaseAuth.getCurrentUser().getUid());

        databaseReferenceUsers.child(NodeNames.ONLINE).setValue(true);
        databaseReferenceUsers.child(NodeNames.ONLINE).onDisconnect().setValue(false);

        setViewPager();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleCustomBackPress();
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
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });
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

          if(id==R.id.mnuProfile)
          {
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