package com.letrans.android.translator.settings.about;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.BaseDialog;
import com.letrans.android.translator.settings.SystemProxy;

public class AboutFragment extends Fragment implements View.OnClickListener {
    private DeviceInfoSettings mDeviceInfoSettings;
    private FeedBackSettings mFeedBackSettings;
    private UserProtocol mUserProtocol;

    private TextView mFeedbackBtn;
    private TextView mUserProtocolBtn;
    private TextView mRefactoryView;

    private BaseDialog mDialog;

    private FragmentManager mFragmentManager;

    /*@Override
    public void onAttachFragment(Fragment childFragment) {
        if (childFragment instanceof FeedBackSettings) {
            mFeedBackSettings = (FeedBackSettings) childFragment;
        } else if (childFragment instanceof UserProtocol) {
            mUserProtocol = (UserProtocol) childFragment;
        }
    }*/

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentManager = getFragmentManager();

        mDeviceInfoSettings = new DeviceInfoSettings(getContext());
        mDeviceInfoSettings.setFragmentManager(mFragmentManager);

        // If use getChildFragmentManager(), then press back key can not pop back stack
        if (savedInstanceState != null) {
            Fragment fragment = mFragmentManager.findFragmentById(R.id.new_container);
            if (fragment != null) {
                if (fragment instanceof FeedBackSettings) {
                    mFeedBackSettings = (FeedBackSettings) fragment;
                } else if (fragment instanceof UserProtocol) {
                    mUserProtocol = (UserProtocol) fragment;
                }
            }
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.setting_about, container, false);
        mDeviceInfoSettings.onCreateView(v);
        mFeedbackBtn = (TextView) v.findViewById(R.id.feedback_btn);
        mFeedbackBtn.setOnClickListener(this);
        mUserProtocolBtn = (TextView) v.findViewById(R.id.user_protocol_btn);
        mUserProtocolBtn.setOnClickListener(this);
        mRefactoryView = (TextView) v.findViewById(R.id.refactory_button);
        mRefactoryView.setOnClickListener(this);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        mDeviceInfoSettings.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mDeviceInfoSettings.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDeviceInfoSettings.onPause();
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mDeviceInfoSettings.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isDestroyed()) {
            return;
        }
        int count = mFragmentManager.getBackStackEntryCount();
        for (int i = 0; i < count; i++) {
            mFragmentManager.popBackStack();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.refactory_button:
                BaseDialog.Builder builder = new BaseDialog.Builder(getContext());
                builder.setTitle(R.string.reset_factory_label)
                        .setMessage(R.string.reset_factory_confirm_msg)
                        .setPositiveButton(android.R.string.ok)
                        .setNegativeButton(android.R.string.cancel)
                        .setButtonListener(new BaseDialog.OnButtonClickListener() {
                            @Override
                            public void onClick(int which) {
                                switch (which) {
                                    case BaseDialog.BUTTON_POSITIVE:
                                        if (!ActivityManager.isUserAMonkey()) {
                                            new EraseDeviceTask().execute();
                                        }
                                        break;
                                    case BaseDialog.BUTTON_NEGATIVE:
                                        break;
                                }
                            }
                        });
                mDialog = builder.build();
                mDialog.show(mFragmentManager, "confirm_dialog");
                break;
            case R.id.feedback_btn:
                showFeedbackFragment();
                break;
            case R.id.user_protocol_btn:
                showUserProtocolFragment();
                break;
        }
    }

    private void showFeedbackFragment() {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        mFeedBackSettings = new FeedBackSettings();
        fragmentTransaction.add(R.id.new_container, mFeedBackSettings);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void showUserProtocolFragment() {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        mUserProtocol = new UserProtocol();
        fragmentTransaction.add(R.id.new_container, mUserProtocol);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    class EraseDeviceTask extends AsyncTask<Void, Void, Void> {
        int mOldOrientation;
        BaseDialog mBaseDialog;

        @Override
        protected Void doInBackground(Void... voids) {
            SystemProxy.getInstance().eraseDevice();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mBaseDialog.dismiss();
            if (getActivity() != null) {
                getActivity().setRequestedOrientation(mOldOrientation);
            }
        }

        @Override
        protected void onPreExecute() {
            mBaseDialog = getBaseDialog();
            mBaseDialog.show(mFragmentManager, "erase_progress_dialog");

            // need to prevent orientation changes as we're about to go into
            // a long IO request, so we won't be able to access inflate resources on flash
            mOldOrientation = getActivity().getRequestedOrientation();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }

        private BaseDialog getBaseDialog() {
            BaseDialog.Builder builder = new BaseDialog.Builder(getContext());
            builder.setTitle(R.string.master_clear_progress_title);
            View view = LayoutInflater.from(getContext()).inflate(
                    R.layout.progress_dialog_layout, null);
            TextView textView = (TextView) view.findViewById(R.id.progress_text);
            textView.setText(R.string.master_clear_progress_text);
            builder.setContentView(view);
            BaseDialog baseDialog = builder.build();
            baseDialog.setCancelable(false);
            return baseDialog;
        }
    }
}
