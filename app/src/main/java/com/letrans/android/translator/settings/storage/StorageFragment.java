package com.letrans.android.translator.settings.storage;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.InfoAdapter;
import com.letrans.android.translator.settings.InfoObject;
import com.letrans.android.translator.settings.ItemDivider;
import com.letrans.android.translator.settings.SystemProxy;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.FileUtils;
import com.letrans.android.translator.utils.ToastUtils;

import java.io.File;
import java.util.ArrayList;

public class StorageFragment extends Fragment implements View.OnClickListener {
    private Context mContext;
    private RadioGroup mRadioGroup;
    private TextView mSaveButton;
    private TextView mClearDataButton;
    private int mStorageType = 1;

    private RecyclerView mStorageStateView;
    private InfoAdapter mAdapter;
    private SystemProxy mSystemProxy;

    private static final String KEY_STORAGE_TYPE = "storage_type";

    private interface StorageType {
        int TYPE_NOT_SAVE = TStorageManager.STORAGE_TYPE_NOT_SAVE;
        int TYPE_SAVE_TEXT = TStorageManager.STORAGE_TYPE_SAVE_TEXT;
        int TYPE_SAVE_SOUND = TStorageManager.STORAGE_TYPE_SAVE_SOUND;
        int TYPE_SAVE_TEXT_SOUND = TStorageManager.STORAGE_TYPE_SAVE_TEXT_SOUND;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.setting_storage, container, false);

        mStorageStateView = (RecyclerView) v.findViewById(R.id.storage_state_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext,
                LinearLayoutManager.VERTICAL, false);
        mStorageStateView.setLayoutManager(linearLayoutManager);
        ItemDivider itemDivider = new ItemDivider(mContext);
        mStorageStateView.addItemDecoration(itemDivider);
        mAdapter = new InfoAdapter(mContext);
        mStorageStateView.setAdapter(mAdapter);
        mAdapter.addInfoObjects(getStorageVolume());

        mRadioGroup = (RadioGroup) v.findViewById(R.id.save_type);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.not_save:
                        mStorageType = StorageType.TYPE_NOT_SAVE;
                        break;
                    case R.id.only_save_text:
                        mStorageType = StorageType.TYPE_SAVE_TEXT;
                        break;
                    case R.id.only_save_sound:
                        mStorageType = StorageType.TYPE_SAVE_SOUND;
                        break;
                    case R.id.both_save:
                        mStorageType = StorageType.TYPE_SAVE_TEXT_SOUND;
                        break;
                }
                TStorageManager.getInstance().setStorageType(mStorageType);
            }
        });
        mSaveButton = (TextView) v.findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(this);
        mClearDataButton = (TextView) v.findViewById(R.id.clear_data_button);
        mClearDataButton.setOnClickListener(this);
        return v;
    }

    private ArrayList<InfoObject> getStorageVolume() {
        ArrayList<InfoObject> list = new ArrayList<>();
        SystemProxy.getInstance().refreshStorage();
        String[] labels = getContext().getResources().getStringArray(R.array.storage_top_columns);
        /*
        String total = Formatter.formatFileSize(getContext(),
                mSystemProxy.getInternalTotalStorageMemory());
        String available = Formatter.formatFileSize(getContext(),
                mSystemProxy.getInternalFreeStorageMemory());
        String used = Formatter.formatFileSize(getContext(),
                mSystemProxy.getInternalTotalStorageMemory() -
                        mSystemProxy.getInternalFreeStorageMemory());
        */
        long[] usage = mSystemProxy.getInternalStorageMemory();
        String total = Formatter.formatFileSize(getContext(), usage[0]);
        String used = Formatter.formatFileSize(getContext(), usage[1]);
        String available = Formatter.formatFileSize(getContext(), usage[2]);
        list.add(new InfoObject(labels[0], total));
        list.add(new InfoObject(labels[1], used));
        list.add(new InfoObject(labels[2], available));
        return list;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save_button:
                TStorageManager.getInstance().setStorageType(mStorageType);
                break;
            case R.id.clear_data_button:
                clearData();
                break;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        mSystemProxy = SystemProxy.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_STORAGE_TYPE, mStorageType);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mStorageType = savedInstanceState.getInt(KEY_STORAGE_TYPE);
        } else {
            mStorageType = TStorageManager.getInstance().getStorageType();
        }
        RadioButton radioButton;
        switch (mStorageType) {
            case StorageType.TYPE_NOT_SAVE:
                radioButton = (RadioButton) getView().findViewById(R.id.not_save);
                radioButton.setChecked(true);
                break;
            case StorageType.TYPE_SAVE_TEXT:
                radioButton = (RadioButton) getView().findViewById(R.id.only_save_text);
                radioButton.setChecked(true);
                break;
            case StorageType.TYPE_SAVE_SOUND:
                radioButton = (RadioButton) getView().findViewById(R.id.only_save_sound);
                radioButton.setChecked(true);
                break;
            case StorageType.TYPE_SAVE_TEXT_SOUND:
                radioButton = (RadioButton) getView().findViewById(R.id.both_save);
                radioButton.setChecked(true);
                break;
        }
    }

    private void clearData() {
        new CleanTask().execute((Void) null);

    }

    class CleanTask extends AsyncTask<Void, Object, Boolean> {

        @Override
        protected Boolean doInBackground(Void[] objects) {
            try {
                TStorageManager.getInstance().clean();
                FileUtils.deleteFiles(new File(AppContext.SOUND_ABSOLUTE_FILE_DIR));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (bool) {
                ToastUtils.showLong(R.string.toast_clean_data_success);
            } else {
                ToastUtils.showLong(R.string.toast_clean_data_failed);
            }
            TStorageManager.getInstance().notifyDataCleaned();
        }
    }
}
