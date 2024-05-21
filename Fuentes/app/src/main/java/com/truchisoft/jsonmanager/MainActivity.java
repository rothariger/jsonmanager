package com.truchisoft.jsonmanager;

import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.truchisoft.fileselector.FileOperation;
import com.truchisoft.fileselector.FileSelector;
import com.truchisoft.fileselector.OnHandleFileListener;
import com.truchisoft.jsonmanager.fragments.EditorFragment;
import com.truchisoft.jsonmanager.fragments.FileListFragment;
import com.truchisoft.jsonmanager.utils.FileUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, com.truchisoft.jsonmanager.fragments.EditorFragment.OnFragmentInteractionListener {
    DrawerLayout _drawer;
    private static final int STORAGE_PERMISSION_CODE = 101;

    @SuppressLint("UseSupportActionBar")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
        onPostCreate();
    }

    private void onPostCreate() {
        com.truchisoft.jsonmanager.data.StaticData.setFiles(com.truchisoft.jsonmanager.utils.PrefManager.getFileData(this));
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        _drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, _drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        _drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.compareTo(Intent.ACTION_VIEW) == 0) {
            File f = new File(intent.getData().getPath());
            moveToEditor(f);
        }
    }

    private void moveToEditor(File f) {
        if (f.exists()) {
            if (!com.truchisoft.jsonmanager.utils.FileUtils.FileExists(f))
                com.truchisoft.jsonmanager.utils.FileUtils.AddFileToPrefs(f);
            android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment, com.truchisoft.jsonmanager.fragments.EditorFragment.newInstance(f.getAbsolutePath()));
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        if (id == R.id.nav_home) {
            transaction.replace(R.id.fragment, new FileListFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        } else if (id == R.id.nav_newfile) {
            transaction.replace(R.id.fragment, EditorFragment.newInstance(null));
            transaction.addToBackStack(null);
            transaction.commit();
        } else if (id == R.id.nav_openfile) {
            new FileSelector(this, FileOperation.LOAD, new OnHandleFileListener() {
                @Override
                public void handleFile(String filePath) {
                    File f = new File(filePath);
                    if (f.exists()) {
                        if (!FileUtils.FileExists(f)) FileUtils.AddFileToPrefs(f);
                        moveToEditor(f);
                    }
                }
            }, FileUtils.FileFilter).show();
        }

        _drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPostCreate();
            }
        }
    }
}
