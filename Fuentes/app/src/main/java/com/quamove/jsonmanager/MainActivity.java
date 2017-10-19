package com.quamove.jsonmanager;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.quamove.fileselector.FileOperation;
import com.quamove.fileselector.FileSelector;
import com.quamove.fileselector.OnHandleFileListener;
import com.quamove.jsonmanager.data.StaticData;
import com.quamove.jsonmanager.fragments.EditorFragment;
import com.quamove.jsonmanager.fragments.FileListFragment;
import com.quamove.jsonmanager.utils.FileUtils;
import com.quamove.jsonmanager.utils.PrefManager;

import java.io.File;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, EditorFragment.OnFragmentInteractionListener {
    DrawerLayout _drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StaticData.setFiles(PrefManager.getFileData(this));
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        _drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                _drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
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
            if (!FileUtils.FileExists(f))
                FileUtils.AddFileToPrefs(f);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment, EditorFragment.newInstance(f.getAbsolutePath()));
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        switch (id) {
            case R.id.nav_home:
                transaction.replace(R.id.fragment, new FileListFragment());
                transaction.addToBackStack(null);
                transaction.commit();
                break;
            case R.id.nav_newfile:
                transaction.replace(R.id.fragment, EditorFragment.newInstance(null));
                transaction.addToBackStack(null);
                transaction.commit();
                break;
            case R.id.nav_openfile:
                new FileSelector(this, FileOperation.LOAD, new OnHandleFileListener() {
                    @Override
                    public void handleFile(String filePath) {
                        File f = new File(filePath);
                        if (f.exists()) {
                            if (!FileUtils.FileExists(f))
                                FileUtils.AddFileToPrefs(f);
                            moveToEditor(f);
                        }
                    }
                }, FileUtils.FileFilter).show();
                break;
            default:
        }

        _drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
