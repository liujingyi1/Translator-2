package com.letrans.android.translator.settings.about;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.letrans.android.translator.R;

public class UserProtocol extends Fragment {
    private Context mContext;

    private TextView mContentView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        mContext = getContext();
        View v = inflater.inflate(R.layout.setting_user_protocol, null);
        mContentView = (TextView) v.findViewById(R.id.protocol_content);
        return v;
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
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
