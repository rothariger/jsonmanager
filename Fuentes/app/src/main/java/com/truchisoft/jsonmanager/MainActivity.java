package com.truchisoft.jsonmanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.truchisoft.jsonmanager.data.StaticData;
import com.truchisoft.jsonmanager.fragments.EditorFragment;
import com.truchisoft.jsonmanager.fragments.FileListFragment;
import com.truchisoft.jsonmanager.print.PrintConfig;
import com.truchisoft.jsonmanager.utils.PrefManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, EditorFragment.OnFragmentInteractionListener {
    DrawerLayout _drawer;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int REQUEST_CODE_OPEN_FILE = 102;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @SuppressLint("UseSupportActionBar")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrintConfig.initDefault(getAssets(), "fonts/materialiconfont.otf");

        onPostCreate();

        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE }, STORAGE_PERMISSION_CODE);
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
        if (action != null && action.compareTo(Intent.ACTION_VIEW) == 0) { // Add null check for action
            Uri uri = intent.getData();
            if (uri != null) { // Add null check for uri
                moveToEditor(uri);
            }
        }
    }

    private String getDisplayNameFromUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        String displayName = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (displayName == null) {
            displayName = uri.getLastPathSegment();
            if (displayName == null || displayName.isEmpty()) {
                displayName = "untitled.json"; // Default if everything else fails
            }
        }
        return displayName;
    }

    private void moveToEditor(Uri uri) {
        if (uri == null) {
            return; // Or handle error
        }
        String displayName = getDisplayNameFromUri(uri);
        // Assuming FileUtils.AddUriToPrefs will be the new method signature after refactoring FileUtils in a later step.
        // For now, we adapt to what AddPathToPrefs does, but using display name and the uri.
        com.truchisoft.jsonmanager.utils.FileUtils.AddPathToPrefs(displayName, uri); // This will be updated when FileUtils is refactored

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // Assuming EditorFragment.newInstance will be changed to accept (String displayName, Uri uri)
        transaction.replace(R.id.fragment, com.truchisoft.jsonmanager.fragments.EditorFragment.newInstance(displayName, uri));
        transaction.addToBackStack(null);
        transaction.commit();
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
            transaction.replace(R.id.fragment, EditorFragment.newInstance(null, null));
            transaction.addToBackStack(null);
            transaction.commit();
        } else if (id == R.id.nav_openfile) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityResultLauncher.launch(intent);
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
            boolean readPermissionGranted = false;
            if (grantResults.length > 0 && permissions.length > 0) { // Check permissions array as well
                for (int i = 0; i < permissions.length; i++) {
                    if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i])) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            readPermissionGranted = true;
                        }
                        break; // Found the permission we care about
                    }
                }
            }

            if (readPermissionGranted) {
                Toast.makeText(this, "Read Storage permission granted.", Toast.LENGTH_SHORT).show();
                // onPostCreate(); // Calling onPostCreate again might be too much, e.g., re-setting content view.
                // Consider if a more targeted refresh is needed, or if initial load handles it.
                // For now, let's assume the app can function or will prompt for file opening.
                // If onPostCreate() is essential for basic app operation even after denial, then it must be called.
                // Given its current content (UI setup, intent handling), it's probably okay to call.
                onPostCreate(); 
            } else {
                // Permission was denied.
                Toast.makeText(this, "Read Storage permission is required to access files.", Toast.LENGTH_LONG).show();
                if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // User selected "Don't ask again" or policy prohibits asking again.
                    // Guide user to app settings.
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    // Check if intent can be resolved to avoid ActivityNotFoundException
                    if (intent.resolveActivity(getPackageManager()) != null) {
                         Toast.makeText(this, "Permission permanently denied. Please enable it in app settings.", Toast.LENGTH_LONG).show();
                         // Consider showing a dialog that explains this and then launching the intent on positive button click.
                         // For now, direct launch for simplicity in this subtask.
                         // startActivity(intent); // Launching settings might be too abrupt without more context/dialog.
                         // For this subtask, just show a longer toast.
                    } else {
                         Toast.makeText(this, "Permission permanently denied. Please enable it in app settings (cannot open settings automatically).", Toast.LENGTH_LONG).show();
                    }
                } else {
                    // User denied but did not select "Don't ask again".
                    // Can show rationale and re-request if appropriate, or just inform them.
                    // For now, the Toast above is the main feedback.
                }
                // App might have limited functionality or should guide user to open files via SAF picker
                // which doesn't always need this specific permission.
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    ActivityResultLauncher<Intent> startActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent intent = result.getData();
                if (intent != null) {
                    Uri uri = intent.getData();
                    if (uri != null) { // Add null check for uri
                        moveToEditor(uri); // Call the new single-argument moveToEditor
                    }
                }
            });

}


