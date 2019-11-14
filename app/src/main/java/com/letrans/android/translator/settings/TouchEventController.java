package com.letrans.android.translator.settings;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.HashMap;

public class TouchEventController implements SettingsActivity.DispatchTouchListener,
        View.OnFocusChangeListener, View.OnTouchListener {
    private SettingsActivity mSettingsActivity;
    private ArrayList<EditText> mEditTexts = new ArrayList<>();
    private HashMap<Integer, View> mFocusedViews = new HashMap<>();
    private HashMap<Integer, View> mTouchedViews = new HashMap<>();

    private InputMethodManager mInputMethodManager;

    public TouchEventController(SettingsActivity settingsActivity) {
        mSettingsActivity = settingsActivity;
        mSettingsActivity.addDispatchTouchListener(this);
        mInputMethodManager = ((InputMethodManager) settingsActivity
                .getSystemService(Context.INPUT_METHOD_SERVICE));
    }

    @Override
    public void afterDispatchTouchEvent() {
        if (mTouchedViews.isEmpty() && !mFocusedViews.isEmpty()) {
            int viewId = mFocusedViews.keySet().iterator().next();
            View view = mFocusedViews.get(viewId);
            if (mInputMethodManager != null && mInputMethodManager.isActive(view)) {
                mInputMethodManager.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
            }
        }
    }

    @Override
    public void beforeDispatchTouchEvent() {
        mTouchedViews.clear();
    }

    public void onDestroyView() {
        mSettingsActivity.removeDispatchTouchListener(this);
        mEditTexts.clear();
        mFocusedViews.clear();
        mTouchedViews.clear();
    }

    public void addEditText(EditText editText) {
        mEditTexts.add(editText);
        editText.setOnFocusChangeListener(this);
        editText.setOnTouchListener(this);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (mEditTexts.contains(v)) {
            if (hasFocus) {
                mFocusedViews.put(v.getId(), v);
            } else {
                mFocusedViews.remove(v.getId());
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mTouchedViews.clear();
        if (mEditTexts.contains(v)) {
            mTouchedViews.put(v.getId(), v);
        }
        return false;
    }
}
