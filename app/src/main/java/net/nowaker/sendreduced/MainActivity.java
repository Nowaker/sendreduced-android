package net.nowaker.sendreduced;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/** Launcher screen: lists profiles, lets the user add, edit and delete them. */
public class MainActivity extends AppCompatActivity {

    private final List<Profile> profiles = new ArrayList<>();
    private ProfileAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView list = findViewById(R.id.profileList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProfileAdapter(profiles, this::editProfile);
        list.setAdapter(adapter);

        FloatingActionButton add = findViewById(R.id.addProfile);
        add.setOnClickListener(v -> editProfile(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        profiles.clear();
        profiles.addAll(Store.loadProfiles(this));
        adapter.notifyDataSetChanged();
    }

    private void editProfile(Profile profile) {
        Intent intent = new Intent(this, ProfileEditActivity.class);
        if (profile != null) {
            intent.putExtra(ProfileEditActivity.EXTRA_ID, profile.id);
        }
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_include_direct).setChecked(Store.isIncludeDirect(this));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_include_direct) {
            boolean value = !item.isChecked();
            item.setChecked(value);
            Store.setIncludeDirect(this, value);
            return true;
        } else if (id == R.id.action_licenses) {
            startActivity(new Intent(this, ShowLicense.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
