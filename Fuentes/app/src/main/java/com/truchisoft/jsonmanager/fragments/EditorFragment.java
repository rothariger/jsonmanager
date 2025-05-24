package com.truchisoft.jsonmanager.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.truchisoft.jsonmanager.R;
import com.truchisoft.jsonmanager.adapters.JsonAdapter;
import com.truchisoft.jsonmanager.data.tree.ArrayItem;
import com.truchisoft.jsonmanager.data.tree.BaseItem;
import com.truchisoft.jsonmanager.data.tree.ObjectItem;
import com.truchisoft.jsonmanager.data.tree.PropertyItem;
import com.truchisoft.jsonmanager.utils.FileUtils;
import com.truchisoft.jsonmanager.utils.UrlDialog;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link EditorFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link EditorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EditorFragment extends Fragment implements TabHost.OnTabChangeListener {
    private static final String ARG_FILE_NAME = "fileName"; // Will store display name
    private static final String ARG_URI = "argURI"; // Will store the actual Uri

    private String filename; // For display purposes
    private Uri currentFileUri; // The Uri passed to the fragment
    private String currentJsonContent = ""; // For onSaveInstanceState
    private Menu _currentMenu;
    private BaseItem _bi;

    // View Caching
    private EditText etEditJson;
    private TreeViewList tvJson;
    private TabHost tabHost;
    private View progressOverlay; // This would be the ProgressBar or its container

    private static final String STATE_URI = "state_uri";
    private static final String STATE_JSON_CONTENT = "state_json_content";
    private static final String STATE_DISPLAY_NAME = "state_display_name";
    private TreeBuilder<BaseItem> _tBuilder;
    private TreeStateManager<BaseItem> _mManager;
    private JsonAdapter _jAdapter;
    private final Set<BaseItem> _selected = new HashSet<BaseItem>();
    private String copyJsonValue;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param displayName
     * @param uri
     * @return A new instance of fragment EditorFragment.
     */
    public static EditorFragment newInstance(String displayName, Uri uri) {
        EditorFragment fragment = new EditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_NAME, displayName); // ARG_FILE_NAME now stores displayName
        args.putParcelable(ARG_URI, uri);           // ARG_URI now stores the actual Uri
        fragment.setArguments(args);
        return fragment;
    }

    public EditorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filename = getArguments().getString(ARG_FILE_NAME); // This is the display name
            currentFileUri = getArguments().getParcelable(ARG_URI);
        }

        if (savedInstanceState != null) {
            filename = savedInstanceState.getString(STATE_DISPLAY_NAME, filename);
            String savedUriString = savedInstanceState.getString(STATE_URI);
            if (savedUriString != null) {
                currentFileUri = Uri.parse(savedUriString);
            }
            currentJsonContent = savedInstanceState.getString(STATE_JSON_CONTENT, "");
            // If currentJsonContent is available and we are in text mode, set it.
            // If tree mode was active, loading from currentFileUri in onCreateView will handle it.
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentFileUri != null) {
            outState.putString(STATE_URI, currentFileUri.toString());
        }
        outState.putString(STATE_DISPLAY_NAME, filename); // Save current display name

        // Try to get current text from EditText if visible and save it
        View view = getView();
        if (view != null) {
            EditText et = view.findViewById(R.id.etEditJson);
            if (et != null && et.getVisibility() == View.VISIBLE) {
                currentJsonContent = et.getText().toString();
            } else if (currentJson != null) { 
                // if text editor not visible but we have a cached json string from tree view
                 currentJsonContent = currentJson;
            }
        }
        outState.putString(STATE_JSON_CONTENT, currentJsonContent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.tvJson && tvJson != null) { // Added null check for tvJson
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            _bi = (BaseItem) tvJson.getItemAtPosition(acmi.position);

            if (!(_bi instanceof PropertyItem)) {
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.treeviewcontextmenu, menu);

                if (copyJsonValue == null || copyJsonValue.isEmpty()) {
                    menu.findItem(R.id.action_paste).setEnabled(false);
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position;
        // Ensure tvJson is not null before using it
        if (tvJson == null) return false;

        if (item.getItemId() == R.id.action_import) {
            position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            _bi = (BaseItem) tvJson.getItemAtPosition(position);

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            startActivityResultLauncher.launch(intent);
        } else if (item.getItemId() == R.id.action_importurl) {
            position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            _bi = (BaseItem) tvJson.getItemAtPosition(position);
            ActionImportUrl(_bi);
        } else if (item.getItemId() == R.id.action_copy) {
            position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            _bi = (BaseItem) tvJson.getItemAtPosition(position);
            copyJsonValue = getTreeElements(_bi).toString();
        } else if (item.getItemId() == R.id.action_paste) {
            position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            _bi = (BaseItem) tvJson.getItemAtPosition(position);
            try {
                loadJson(copyJsonValue, _bi);
            } catch (JsonParseException jpe) {
                Toast.makeText(getContext(), "Error parsing JSON for paste: " + jpe.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        return false;
    }

    private void ActionImportUrl(final BaseItem bi) {
        UrlDialog ud = new UrlDialog(getActivity(), new UrlDialog.IDialogResult() {
            @Override
            public void OnResult(String Value) {
                try {
                    loadJson(Value, bi);
                } catch (JsonParseException jpe) {
                    Toast.makeText(getContext(), "Error parsing JSON from URL: " + jpe.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        ud.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.editormenu, menu);
        _currentMenu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            saveJson(false);
            return true;
        }

        if (id == R.id.action_saveas) {
            saveJson(true);
            return true;
        }

        if (id == R.id.action_prettify && etEditJson != null) { // Added null check
            etEditJson.setText(getPreetyJson(etEditJson.getText().toString()));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etEditJson = view.findViewById(R.id.etEditJson);
        tvJson = view.findViewById(R.id.tvJson);
        tabHost = view.findViewById(android.R.id.tabhost);
        // progressOverlay = view.findViewById(R.id.progress_overlay); // Assuming R.id.progress_overlay exists in XML
        // if (progressOverlay != null) progressOverlay.setVisibility(View.GONE);


        if (tvJson != null) { // Check if tvJson was found before registering context menu
            registerForContextMenu(tvJson);
        }
        createTabs(view); // view is still needed if createTabs accesses other views directly
        createTree(view); // view is still needed if createTree accesses other views directly

        if (currentFileUri != null) {
            refreshTree();
            try {
                loadJson(loadFile(currentFileUri));
            } catch (JsonParseException jpe) {
                // If loading from URI fails, and we have saved content, try that.
                if (currentJsonContent != null && !currentJsonContent.isEmpty()) {
                    try {
                        loadJson(currentJsonContent);
                        // Consider updating filename to indicate this is restored/modified content
                        // filename = "Restored unsaved content";
                    } catch (JsonParseException jpe2) {
                        showFileList(); // Both URI and saved content failed
                    }
                } else {
                    showFileList(); // URI failed, no saved content
                }
            }
            if (_mManager != null) _mManager.collapseChildren(null); // Added null check
        } else if (currentJsonContent != null && !currentJsonContent.isEmpty()) {
            // No URI, but have saved content (e.g. new file not yet saved, or restored state)
            refreshTree();
            try {
                loadJson(currentJsonContent);
                if (filename == null || filename.isEmpty()) { // If filename wasn't restored
                    filename = "Restored Content"; // Or "New File"
                }
            } catch (JsonParseException jpe) {
                // Handle case where saved content is also invalid
                 if (_tBuilder != null) _tBuilder.addRelation(null, new ArrayItem("Root")); // Start fresh // Added null check
            }
        }
        else {
            refreshTree();
             if (_tBuilder != null) _tBuilder.addRelation(null, new ArrayItem("Root")); // Added null check
        }
    }


    @Override
    public void onDestroyView() {
        etEditJson = null;
        tvJson = null;
        tabHost = null;
        progressOverlay = null;
        super.onDestroyView(); // Standard practice to call super at the end for onDestroyView
    }

    private void createTree(View view) { // view param might be removable if tvJson is always used
        if (tvJson == null) return; // Guard against null tvJson

        _mManager = new InMemoryTreeStateManager<>();
        _tBuilder = new TreeBuilder<>(_mManager);
        _jAdapter = new JsonAdapter(this.getActivity(), _selected, _mManager, 1);
        tvJson.setAdapter(_jAdapter);
    }

    private void refreshTree() {
        if (_tBuilder != null) _tBuilder.clear(); // Added null check
    }

    private void createTabs(View view) { // view param might be removable if tabHost is always used
        if (tabHost == null) return; // Guard against null tabHost
        tabHost.setup();

        TabHost.TabSpec spec = tabHost.newTabSpec("tabTree");
        spec.setContent(R.id.tabTree);
        spec.setIndicator("View Tree");
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("tabEditor");
        spec.setContent(R.id.tabEditor);
        spec.setIndicator("Edit Json");
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);

        tabHost.setOnTabChangedListener(this);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private String currentJson;

    @Override
    public void onTabChanged(String tabId) {
        if (etEditJson == null || tabHost == null || _currentMenu == null) return; // Guard against null views

        switch (tabId) {
            case "tabTree":
                try {
                    String jsonvalue = etEditJson.getText().toString();
                    currentJsonContent = jsonvalue; // Save latest from editor
                    if (!currentJson.equals(jsonvalue)) {
                        try {
                            loadJson(jsonvalue);
                            refreshTree();
                        } catch (JsonParseException jpe) {
                            String message = "Exception occurred while trying to read the json text: \n";
                            if (jpe.getCause() != null)
                                message += jpe.getCause().getMessage();
                            else
                                message += jpe.getMessage();
                            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

                            tabHost.setOnTabChangedListener(null);
                            tabHost.setCurrentTab(1);
                            tabHost.setOnTabChangedListener(this);
                        }
                    }
                    _currentMenu.findItem(R.id.action_prettify).setVisible(false);
                } catch (Exception ex) {
                    String message = "Exception occurred while trying to read the json text: \n";
                    if (ex.getCause() != null)
                        message += ex.getCause().getMessage();
                    else
                        message += ex.getMessage();
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

                    tabHost.setOnTabChangedListener(null);
                    tabHost.setCurrentTab(1);
                    tabHost.setOnTabChangedListener(this);
                }
                break;
            case "tabEditor":
                if (currentJsonContent != null && !currentJsonContent.isEmpty()) {
                    if(etEditJson.getText().toString().isEmpty()){ // Only set if editor is empty
                        etEditJson.setText(currentJsonContent);
                    } else {
                        currentJson = getPreetyJson(); // Update currentJson from tree before switching
                        etEditJson.setText(currentJson);
                    }
                } else {
                     currentJson = getPreetyJson();
                    etEditJson.setText(currentJson);
                }
                _currentMenu.findItem(R.id.action_prettify).setVisible(true);
                break;
        }
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }

    //region File Related Functions
    private String getDisplayNameFromUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        String displayName = null;
        Context context = getContext();
        if (context != null && ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, null, null, null, null);
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


    private String loadFile(Uri uriToLoad) {
        if (uriToLoad == null) return ""; // Or throw an exception
        return FileUtils.ReadFromResource(getContext(), uriToLoad); // Pass context
    }

    private void showFileList() {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment, new FileListFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }
    //endregion

    //region Json Related Functions
    private void loadJson(String jsonstring) throws JsonParseException {
        loadJson(jsonstring, null);
    }

    private void loadJson(String jsonstring, BaseItem parent) throws JsonParseException {
        JsonElement jElement = new Gson().fromJson(jsonstring, JsonElement.class);
        if (jElement == null) {
//            Toast.makeText(getActivity(), "There was an exception loading the json", Toast.LENGTH_LONG).show();
            throw new JsonParseException("There was an exception parsing the json string.");
        }

        ConvertTask(jElement, parent);
    }

    private void ConvertTask(final JsonElement jElement, final BaseItem parent) {
        // if (progressOverlay != null) progressOverlay.setVisibility(View.VISIBLE);
        // For now, use a Toast to indicate start if no ProgressBar view is available.
        if (getContext() != null) Toast.makeText(getContext(), "Parsing JSON...", Toast.LENGTH_SHORT).show();


        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (_mManager == null || _tBuilder == null) return; // Guard clause

                if (parent == null) ConvertJsonToTree(parent, jElement, "Root");
                else {
                    if (parent instanceof ArrayItem)
                        ConvertJsonToTree(parent, jElement, String.valueOf(_mManager.getChildren(parent).size()));
                    else
                        ConvertJsonToTree(parent, jElement, "Item " + String.valueOf(_mManager.getChildren(parent).size()));
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (_mManager != null) _mManager.refresh(); // Guard clause
                        // if (progressOverlay != null) progressOverlay.setVisibility(View.GONE);
                        // For now, use a Toast to indicate end if no ProgressBar view is available.
                        if (getContext() != null) Toast.makeText(getContext(), "JSON parsing complete.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    //endregion

    //region Tree functions
    private void ConvertJsonToTree(BaseItem biParent, JsonElement jElement, String name) {
        BaseItem currentItem;
        int i = 0;
        if (jElement.isJsonArray()) {
            currentItem = new ArrayItem(String.valueOf(name));
            _tBuilder.addRelation(biParent, currentItem);
            for (JsonElement je : jElement.getAsJsonArray()) {
                ConvertJsonToTree(currentItem, je, String.valueOf(i++));
            }
        }
        if (jElement.isJsonObject()) {
            currentItem = new ObjectItem(name);
            _tBuilder.addRelation(biParent, currentItem);
            for (Map.Entry<String, JsonElement> entry : jElement.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    _tBuilder.addRelation(currentItem, new PropertyItem(entry.getKey(), entry.getValue().getAsString()));
                } else ConvertJsonToTree(currentItem, entry.getValue(), entry.getKey());
            }
        }

    }

    private String getPreetyJson() {
        JsonElement jsonArray = getJson();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            return gson.toJson(jsonArray);
        } catch (Exception ex) {
            String errorMessage = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
            if (errorMessage == null) errorMessage = "Unknown error during JSON processing.";
            Toast.makeText(getActivity(), "Exception occurred while trying to read the json text: \n" + errorMessage, Toast.LENGTH_LONG).show();
            return "";
        }
    }

    private String getPreetyJson(String json) {
        try {
            JsonElement jsonArray = getJson(); // Ensure getJson() itself is safe or wrapped
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(gson.fromJson(json, JsonElement.class));
        } catch (Exception ex) {
            String errorMessage = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
            if (errorMessage == null) errorMessage = "Unknown error during JSON processing.";
            Toast.makeText(getActivity(), "Exception occurred while trying to read the json text: \n" + errorMessage, Toast.LENGTH_LONG).show();
            return json;
        }
    }

    private void saveJson(Boolean newFile) {
        final byte[] jData = getPreetyJson().getBytes();
        currentJsonContent = new String(jData); // Update currentJsonContent with what's being saved

        if (newFile || currentFileUri == null) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            // Use current filename as suggestion if available, otherwise "newfile.json"
            String suggestedName = (filename != null && !filename.isEmpty()) ? filename : "newfile.json";
            // Ensure the suggested name ends with .json if it's a new file or filename is a placeholder
            if (!suggestedName.toLowerCase().endsWith(".json")) {
                 if (suggestedName.lastIndexOf('.') > 0) { // has extension but not json
                    suggestedName = suggestedName.substring(0, suggestedName.lastIndexOf('.')) + ".json";
                } else { // no extension
                    suggestedName = suggestedName + ".json";
                }
            }
            intent.putExtra(Intent.EXTRA_TITLE, suggestedName);
            createFileResultLauncher.launch(intent);
        } else {
            com.truchisoft.jsonmanager.utils.FileUtils.AddPathToPrefs(filename, currentFileUri); // Use display name and current URI
            FileUtils.WriteToFile(getContext(), currentFileUri, jData); // Pass context
        }
    }

    private JsonElement getJson() {
        return getTreeElements(_mManager.getChildren(null).get(0));
    }

    private JsonElement getTreeElements(BaseItem tn) {
        JsonElement jsonElement = null;
        if (tn instanceof ArrayItem) {
            jsonElement = new JsonArray();
            for (BaseItem tEach : _mManager.getChildren(tn)) {
                jsonElement.getAsJsonArray().add(getTreeElements(tEach));
            }
        } else if (tn instanceof ObjectItem) {
            jsonElement = new JsonObject();
            for (BaseItem tEach : _mManager.getChildren(tn)) {
                jsonElement.getAsJsonObject().add(tEach.name, getTreeElements(tEach));
            }
        } else if (tn instanceof PropertyItem) {
            return new JsonPrimitive(tn.getAsPropertyItem().value);
        }

        return jsonElement;
    }
    //endregion

    ActivityResultLauncher<Intent> startActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent intent = result.getData();
                if (intent != null) {
                    Uri uri = intent.getData();
                    String fileContent = loadFile(uri); // This loadFile takes URI and uses context
                    try {
                        loadJson(fileContent, _bi);
                    } catch (JsonParseException jpe) {
                        Toast.makeText(getActivity(), "There was an exception loading the json", Toast.LENGTH_LONG).show();
                    }
                }
            });

    ActivityResultLauncher<Intent> createFileResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent intent = result.getData();
                if (intent != null) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        currentFileUri = uri;
                        filename = getDisplayNameFromUri(currentFileUri); // Update filename with display name from URI
                        // Update preferences - using AddPathToPrefs with display name and URI
                        com.truchisoft.jsonmanager.utils.FileUtils.AddPathToPrefs(filename, currentFileUri);
                        try {
                            boolean success = FileUtils.WriteToFile(getContext(), currentFileUri, getPreetyJson().getBytes()); // Pass context
                            if (success) {
                                currentJsonContent = getPreetyJson(); // Update content after successful save
                                Toast.makeText(getContext(), "File saved successfully.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Error saving file.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception ex) {
                            Log.e("EditorFragment", "Error saving file via createFileResultLauncher", ex); // Keep detailed log
                            Toast.makeText(getContext(), "Error saving file: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
}
