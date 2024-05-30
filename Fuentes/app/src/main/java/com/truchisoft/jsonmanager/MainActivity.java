package com.truchisoft.jsonmanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.truchisoft.fileselector.FileOperation;
import com.truchisoft.fileselector.FileSelector;
import com.truchisoft.fileselector.OnHandleFileListener;
import com.truchisoft.jsonmanager.data.StaticData;
import com.truchisoft.jsonmanager.fragments.EditorFragment;
import com.truchisoft.jsonmanager.fragments.FileListFragment;
import com.truchisoft.jsonmanager.print.PrintConfig;
import com.truchisoft.jsonmanager.utils.FileUtils;
import com.truchisoft.jsonmanager.utils.PrefManager;

import java.io.File;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, EditorFragment.OnFragmentInteractionListener {
    DrawerLayout _drawer;
    private static final int STORAGE_PERMISSION_CODE = 101;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @SuppressLint("UseSupportActionBar")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrintConfig.initDefault(getAssets(), "fonts/materialiconfont.otf");

        onPostCreate();
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:com.truchisoft.jsonmanager"));
            startActivityResultLauncher.launch(intent);
        }

        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    private void onPostCreate() {
        StaticData.setFiles(PrefManager.getFileData(this));
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
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment, com.truchisoft.jsonmanager.fragments.EditorFragment.newInstance(f.getAbsolutePath()));
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    private void moveToEditor(String path) {
        if (!path.isEmpty()) {
            com.truchisoft.jsonmanager.utils.FileUtils.AddPathToPrefs(path);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment, com.truchisoft.jsonmanager.fragments.EditorFragment.newInstance(path));
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

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

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
                        if (!FileUtils.FileExists(f))
                            FileUtils.AddFileToPrefs(f);
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

    final private Context currentCTX = this;
    ActivityResultLauncher<Intent> startActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Intent intent = result.getData();
                    if (intent != null) {
                        Uri uri = intent.getData();
//                        File f = new File(getRealPathFromURI(currentCTX, uri));
//                        moveToEditor(f);
                        DocumentFile df = DocumentFile.fromSingleUri(currentCTX, uri);
                        moveToEditor(intent.getData().toString());
                    }
                }
            });

    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
}


