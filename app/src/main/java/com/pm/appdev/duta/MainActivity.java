package com.pm.appdev.duta;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }

    private void setViewPager() {
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_chat));
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_requests));
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_findfriends));

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

//        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
//        viewPager.setAdapter(adapter);

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

    private static class ViewPagerAdapter extends FragmentPagerAdapter {
        public ViewPagerAdapter(@NonNull FragmentManager fm, Lifecycle behavior) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
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
        public int getCount() {
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
        if (item.getItemId() == R.id.mnuProfile) {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean doubleBackPressed = false;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
}


//  package com.pm.appdev.duta;
//
//import android.os.Bundle;
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.Handler;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentPagerAdapter;
//import androidx.viewpager.widget.ViewPager;
//
//import com.google.android.material.tabs.TabLayout;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.pm.appdev.duta.Common.NodeNames;
//import com.pm.appdev.duta.chats.ChatFragment;
//import com.pm.appdev.duta.findfriends.FindFriendsFragment;
//import com.pm.appdev.duta.profile.ProfileActivity;
//import com.pm.appdev.duta.requests.RequestsFragment;
//
//  public class MainActivity extends AppCompatActivity {
//
//      private TabLayout tabLayout;
//      private ViewPager viewPager;
//
//      @Override
//      protected void onCreate(Bundle savedInstanceState) {
//          super.onCreate(savedInstanceState);
//          setContentView(R.layout.activity_main);
//
//          tabLayout = findViewById(R.id.tabMain);
//          viewPager = findViewById(R.id.vpMain);
//
//          FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
//
//
//          DatabaseReference databaseReferenceUsers = FirebaseDatabase.getInstance().getReference()
//                  .child(NodeNames.USERS).child(firebaseAuth.getCurrentUser().getUid());
//
//          databaseReferenceUsers.child(NodeNames.ONLINE).setValue(true);
//          databaseReferenceUsers.child(NodeNames.ONLINE).onDisconnect().setValue(false);
//
//          setViewPager();
//
//      }
//
//      class Adapter extends FragmentPagerAdapter {
//
//          public Adapter(@NonNull FragmentManager fm, int behavior) {
//              super(fm, behavior);
//          }
//
//          @NonNull
//          @Override
//          public Fragment getItem(int position) {
//              switch (position)
//              {
//                  case 0:
//                      ChatFragment chatFragment = new ChatFragment();
//                      return  chatFragment;
//                  case 1:
//                      RequestsFragment requestsFragment = new RequestsFragment();
//                      return  requestsFragment;
//                  case 2:
//                      FindFriendsFragment findFriendsFragment = new FindFriendsFragment();
//                      return  findFriendsFragment;
//              }
//              return null;
//          }
//
//          @Override
//          public int getCount() {
//              return tabLayout.getTabCount();
//          }
//      }
//
//
//      private void setViewPager(){
//
//          tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_chat));
//          tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_requests));
//          tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_findfriends));
//
//          tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
//
//          Adapter  adapter = new Adapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
//          viewPager.setAdapter(adapter);
//
//          tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
//              @Override
//              public void onTabSelected(TabLayout.Tab tab) {
//                  viewPager.setCurrentItem(tab.getPosition());
//              }
//
//              @Override
//              public void onTabUnselected(TabLayout.Tab tab) {
//
//              }
//
//              @Override
//              public void onTabReselected(TabLayout.Tab tab) {
//
//              }
//          });
//
//
//          viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
//      }
//
//
//      @Override
//      public boolean onCreateOptionsMenu(Menu menu) {
//          getMenuInflater().inflate(R.menu.menu_main, menu);
//          return super.onCreateOptionsMenu(menu);
//      }
//
//      @Override
//      public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//          int id = item.getItemId();
//
//          if(id==R.id.mnuProfile)
//          {
//              startActivity(new Intent(MainActivity.this, ProfileActivity.class));
//          }
//          return super.onOptionsItemSelected(item);
//      }
//
//      private  boolean doubleBackPressed = false;
//
//      @Override
//      public void onBackPressed() {
//          // super.onBackPressed();
//
//          super.onBackPressed();
//          if(tabLayout.getSelectedTabPosition()>0)
//          {
//              tabLayout.selectTab(tabLayout.getTabAt(0));
//          }
//          else
//          {
//              if(doubleBackPressed)
//              {
//                  finishAffinity();
//              }
//              else
//              {
//                  doubleBackPressed=true;
//                  Toast.makeText(this, R.string.press_back_to_exit, Toast.LENGTH_SHORT).show();
//                  //delay
//                  Handler handler = new Handler();
//                  handler.postDelayed(new Runnable() {
//                      @Override
//                      public void run() {
//                          doubleBackPressed=false;
//                      }
//                  }, 2000);
//
//              }
//          }
//      }
//  }