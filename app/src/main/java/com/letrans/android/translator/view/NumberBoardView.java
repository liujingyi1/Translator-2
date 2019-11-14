package com.letrans.android.translator.view;

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.letrans.android.translator.R;

public class NumberBoardView {
    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private ViewGroup rootView;
    private EditText ed;

    public NumberBoardView(Activity activity, KeyboardView keyboardView, EditText editText) {
        this.ed = editText;

        keyboard = new Keyboard(activity, R.xml.number_keyboard);

        this.keyboardView = keyboardView;
        keyboardView.setKeyboard(keyboard);
        keyboardView.setEnabled(true);
        keyboardView.setPreviewEnabled(false);
        keyboardView.setOnKeyboardActionListener(onKeyboardActionListener);

        rootView = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
    }

    KeyboardView.OnKeyboardActionListener onKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {
        @Override
        public void swipeUp() {
        }

        @Override
        public void swipeRight() {
        }

        @Override
        public void swipeLeft() {
        }

        @Override
        public void swipeDown() {
        }

        @Override
        public void onText(CharSequence text) {
        }

        @Override
        public void onRelease(int primaryCode) {
        }

        @Override
        public void onPress(int primaryCode) {
        }

        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            Editable editable = ed.getText();
            int start = ed.getSelectionStart();
            if (primaryCode == Keyboard.KEYCODE_CANCEL) {// 完成
            } else if (primaryCode == Keyboard.KEYCODE_DONE) {
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, Keyboard.KEYCODE_DONE);
                ed.dispatchKeyEvent(keyEvent);
            } else if (primaryCode == Keyboard.KEYCODE_DELETE) {// 回退
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL);
                ed.dispatchKeyEvent(keyEvent);
            } else {
                String str = Character.toString((char) primaryCode);
                editable.insert(start, str);
            }
        }
    };
    public void showKeyboard() {
        keyboardView.setVisibility(View.VISIBLE);
    }

    public void hideKeyboard() {
        keyboardView.setVisibility(View.GONE);
        mInstance = null;
    }

    private boolean isWord(String str) {
        return str.matches("[a-zA-Z]");
    }

    private static NumberBoardView mInstance;

    public static NumberBoardView shared(Activity activity, KeyboardView keyboardView, EditText edit) {
        if (mInstance == null) {
            mInstance = new NumberBoardView(activity, keyboardView, edit);
        }
        mInstance.ed = edit;
        return mInstance;
    }

    public void release() {
        keyboardView.setOnKeyboardActionListener(null);
        keyboardView = null;
        keyboard = null;
        rootView = null;
        ed = null;
    }
}
