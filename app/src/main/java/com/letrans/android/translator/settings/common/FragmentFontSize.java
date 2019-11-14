package com.letrans.android.translator.settings.common;

import android.app.Fragment;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.SystemProxy;

public class FragmentFontSize extends Fragment {
    private static final String TAG = "FragmentFontSize";
    private SeekBar mSeekBar;
    private float[] mValues;
    //protected String[] mEntries;
    protected int mInitialIndex;
    protected int mCurrentIndex;
    private ImageView mLarger;
    private ImageView mSmaller;
    private final Configuration mCurConfig = new Configuration();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setting_font_size, container, false);
        mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        Resources res = getContext().getResources();
        //mEntries = res.getStringArray(R.array.custom_entries_font_size);
        final String[] strEntryValues = res.getStringArray(R.array.entry_values_font_size);
        mValues = new float[strEntryValues.length];
        for (int i = 0; i < strEntryValues.length; ++i) {
            mValues[i] = Float.parseFloat(strEntryValues[i]);
        }
        mInitialIndex = fontSizeValueToIndex(mValues);
        //mSeekBar.setLabels(mEntries);
        final int max = Math.max(1, strEntryValues.length - 1);
        mSeekBar.setMax(max);
        mSeekBar.setProgress(mInitialIndex);
        mSeekBar.setOnSeekBarChangeListener(new onPreviewSeekBarChangeListener());
        mLarger = (ImageView) view.findViewById(R.id.id_larger);
        mLarger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int progress = mSeekBar.getProgress();
                if (progress < mSeekBar.getMax()) {
                    mSeekBar.setProgress(progress + 1);
                }
            }
        });
        mSmaller = (ImageView) view.findViewById(R.id.id_smaller);
        mSmaller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int progress = mSeekBar.getProgress();
                if (progress > 0) {
                    mSeekBar.setProgress(progress - 1);
                }
            }
        });
        return view;
    }

    private class onPreviewSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private boolean mSeekByTouch;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mCurrentIndex = progress;
            if (!mSeekByTouch) {
                commit();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mSeekByTouch = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            commit();
            mSeekByTouch = false;
        }
    }

    protected void commit() {
        SystemProxy.getInstance().updateFontScale(mValues[mCurrentIndex]);
    }

    public int fontSizeValueToIndex(float[] indices) {
        return SystemProxy.getInstance().getFontSizeIndex(indices);
    }
}
