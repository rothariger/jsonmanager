package com.truchisoft.jsonmanager.fragments;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.truchisoft.fileselector.FileOperation;
import com.truchisoft.fileselector.FileSelector;
import com.truchisoft.fileselector.OnHandleFileListener;
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
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_FILE_NAME = "fileName";
    private Context fragmentContext = null;

    // TODO: Rename and change types of parameters
    private String filename;
    private Menu _currentMenu;

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
     * @param Filename
     * @return A new instance of fragment EditorFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EditorFragment newInstance(String Filename) {
        EditorFragment fragment = new EditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_NAME, Filename);
        fragment.setArguments(args);
        return fragment;
    }

    public EditorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentContext = this.getActivity();
        if (getArguments() != null) {
            filename = getArguments().getString(ARG_FILE_NAME);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.tvJson) {
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            BaseItem bi = (BaseItem) ((TreeViewList) getActivity().findViewById(R.id.tvJson)).getItemAtPosition(acmi.position);

            if (!(bi instanceof PropertyItem)) {
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.treeviewcontextmenu, menu);

                if (copyJsonValue == null || copyJsonValue.isEmpty()) {
                    menu.findItem(R.id.action_paste).setEnabled(false);
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position;
        final BaseItem bi;
        if (item.getItemId() == R.id.action_import) {
            position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            bi = (BaseItem) ((TreeViewList) getActivity().findViewById(R.id.tvJson)).getItemAtPosition(position);

            new FileSelector(getActivity(), FileOperation.LOAD, new OnHandleFileListener() {
                @Override
                public void handleFile(String filePath) {
                    File f = new File(filePath);
                    if (f.exists()) {
                        String fileContent = loadFile(filePath);
                        try {
                            loadJson(fileContent, bi);
                        } catch (JsonParseException jpe) {
                        }
                    }
                }
            }, FileUtils.FileFilter).show();
        } else if (item.getItemId() == R.id.action_importurl) {
            position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            bi = (BaseItem) ((TreeViewList) getActivity().findViewById(R.id.tvJson)).getItemAtPosition(position);
            ActionImportUrl(bi);
        } else if (item.getItemId() == R.id.action_copy) {
            position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            bi = (BaseItem) ((TreeViewList) getActivity().findViewById(R.id.tvJson)).getItemAtPosition(position);
            copyJsonValue = getTreeElements(bi).toString();
        } else if (item.getItemId() == R.id.action_paste) {
            position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            bi = (BaseItem) ((TreeViewList) getActivity().findViewById(R.id.tvJson)).getItemAtPosition(position);
            try {
                loadJson(copyJsonValue, bi);
            } catch (JsonParseException jpe) {
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

        if (id == R.id.action_prettify) {
            EditText et = (EditText) getActivity().findViewById(R.id.etEditJson);
            et.setText(getPreetyJson(et.getText().toString()));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_editor, container, false);
        registerForContextMenu(view.findViewById(R.id.tvJson));
        createTabs(view);
        createTree(view);

        if (filename != null && !filename.isEmpty()) {
            refreshTree();
            try {
                loadJson(loadFile());
            } catch (JsonParseException jpe) {
                showFileList();
            }
            _mManager.collapseChildren(null);
        } else {
            refreshTree();
            _tBuilder.addRelation(null, new ArrayItem("Root"));
        }
        return view;
    }

    private void createTree(View view) {
        TreeViewList tvl = ((TreeViewList) view.findViewById(R.id.tvJson));

        _mManager = new InMemoryTreeStateManager<>();
        _tBuilder = new TreeBuilder<>(_mManager);
        _jAdapter = new JsonAdapter(this.getActivity(), _selected, _mManager, 1);
        tvl.setAdapter(_jAdapter);
    }

    private void refreshTree() {
        _tBuilder.clear();
    }

    private void createTabs(View view) {
        TabHost tabs = (TabHost) view.findViewById(android.R.id.tabhost);
        tabs.setup();

        TabHost.TabSpec spec = tabs.newTabSpec("tabTree");
        spec.setContent(R.id.tabTree);
        spec.setIndicator("View Tree");
        tabs.addTab(spec);

        spec = tabs.newTabSpec("tabEditor");
        spec.setContent(R.id.tabEditor);
        spec.setIndicator("Edit Json");
        tabs.addTab(spec);

        tabs.setCurrentTab(0);

        tabs.setOnTabChangedListener(this);
    }

    // TODO: Rename method, update argument and hook method into UI event
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
        switch (tabId) {
            case "tabTree":
                try {
                    String jsonvalue = ((EditText) getActivity().findViewById(R.id.etEditJson)).getText().toString();
                    if (!currentJson.equals(jsonvalue)) {
                        refreshTree();
                        try {
                            loadJson(jsonvalue);
                        } catch (JsonParseException jpe) {
                        }
                    }
                    _currentMenu.findItem(R.id.action_prettify).setVisible(false);
                } catch (Exception ex) {
                    Toast.makeText(getActivity(), "Exception ocurred while trying to read the json text: \n" + ex.getCause().getMessage(), Toast.LENGTH_LONG).show();
                    TabHost tabs = (TabHost) getView().findViewById(android.R.id.tabhost);
                    tabs.setOnTabChangedListener(null);
                    tabs.setCurrentTab(1);
                    tabs.setOnTabChangedListener(this);
                }
                break;
            case "tabEditor":
                currentJson = getPreetyJson();
                ((EditText) getActivity().findViewById(R.id.etEditJson)).setText(currentJson);
                _currentMenu.findItem(R.id.action_prettify).setVisible(true);
                break;
        }
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }

    //region File Related Functions
    private String loadFile() {
        return loadFile(filename);
    }

    private String loadFile(String file) {
        if (file.contains("msf")) return FileUtils.ReadFromResource(file);
        return FileUtils.ReadFromFile(file);
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
            Toast.makeText(getActivity(), "There was an exception loading the json", Toast.LENGTH_LONG).show();
            throw new JsonParseException("There was an exception parsing the json string.");
        }

        ConvertTask(jElement, parent);
    }

    private void ConvertTask(final JsonElement jElement, final BaseItem parent) {
//        final ProgressDialog progress = ProgressDialog.show(getActivity(), "Json Manager",
//                "Please Wait...", true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
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
                        _mManager.refresh();
//                        progress.hide();
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
            Toast.makeText(getActivity(), "Exception ocurred while trying to read the json text: \n" + ex.getCause().getMessage(), Toast.LENGTH_LONG).show();
            return "";
        }
    }

    private String getPreetyJson(String json) {
        try {
            JsonElement jsonArray = getJson();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(gson.fromJson(json, JsonElement.class));
        } catch (Exception ex) {
            Toast.makeText(getActivity(), "Exception ocurred while trying to read the json text: \n" + ex.getCause().getMessage(), Toast.LENGTH_LONG).show();
            return json;
        }
    }

    private void saveJson(Boolean newFile) {
        final byte[] jData = getPreetyJson().getBytes();

        if (newFile || filename == null || filename.isEmpty()) {
            new FileSelector(fragmentContext, FileOperation.SAVE, new OnHandleFileListener() {
                @Override
                public void handleFile(String filePath) {
                    File f = FileUtils.CreateFile(filePath);
                    if (!FileUtils.FileExists(f)) FileUtils.AddFileToPrefs(f);
                    if (f.canWrite()) {
                        FileUtils.WriteToFile(f, jData);
                    }
                }
            }, FileUtils.FileFilter).show();
        } else {
            File f = new File(filename);
            if (!FileUtils.FileExists(f)) FileUtils.AddFileToPrefs(f);
            FileUtils.WriteToFile(f, jData);
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

}
