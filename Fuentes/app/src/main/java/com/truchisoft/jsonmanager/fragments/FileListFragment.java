package com.truchisoft.jsonmanager.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.truchisoft.jsonmanager.R;
import com.truchisoft.jsonmanager.adapters.FileListAdapter;
import com.truchisoft.jsonmanager.data.StaticData;

/**
 * A placeholder fragment containing a simple view.
 */
public class FileListFragment extends Fragment {

    public FileListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final FileListAdapter fla = new FileListAdapter(this.getActivity(), StaticData.getFiles());
        View view = inflater.inflate(R.layout.fragment_filelist, container, false);
        ListView lv = (ListView) view.findViewById(R.id.lvFileList);
        final SwipeRefreshLayout srlFileList = (SwipeRefreshLayout) view.findViewById(R.id.srlFileList);
        srlFileList.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fla.setFiles(StaticData.getFiles());
                srlFileList.setRefreshing(false);
            }
        });
        lv.setAdapter(fla);

        view.findViewById(R.id.newFile).setOnClickListener(v -> {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment, EditorFragment.newInstance(null, null));
            transaction.addToBackStack(null);
            transaction.commit();
        });

        view.findViewById(R.id.openFile).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityResultLauncher.launch(intent);
        });

        return view;
    }

    ActivityResultLauncher<Intent> startActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent intent = result.getData();
                if (intent != null) {
                    Uri uri = intent.getData();
                    if (!uri.toString().isEmpty()) {
                        com.truchisoft.jsonmanager.utils.FileUtils.AddPathToPrefs(uri.toString(), uri);
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.fragment, com.truchisoft.jsonmanager.fragments.EditorFragment.newInstance(uri.toString(), uri));
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }
                }
            });
}
