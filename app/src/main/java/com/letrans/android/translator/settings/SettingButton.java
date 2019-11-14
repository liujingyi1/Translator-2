package com.letrans.android.translator.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.letrans.android.translator.R;

public class SettingButton extends LinearLayout {
    private ImageView mIcon;
    private TextView mLabel;
    private View mDotIcon;

    public SettingButton(Context context) {
        this(context, null);
    }

    public SettingButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SettingButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LayoutInflater.from(context).inflate(R.layout.setting_button, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIcon = (ImageView) findViewById(R.id.icon);
        mLabel = (TextView) findViewById(R.id.label);
        ColorStateList colorStateList = getResources().getColorStateList(
                R.color.setting_button_color, null);
        mLabel.setTextColor(colorStateList);
        mDotIcon = findViewById(R.id.dot_icon);
    }

    public void setIcon(int resId) {
        mIcon.setImageResource(resId);
    }

    public void setIcon(Drawable drawable) {
        mIcon.setImageDrawable(drawable);
    }

    public void setLabel(int resId) {
        mLabel.setText(resId);
    }

    public void setLabel(CharSequence text) {
        mLabel.setText(text);
    }

    public void setDotVisible(boolean visible) {
        mDotIcon.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mIcon.setEnabled(enabled);
        mLabel.setEnabled(enabled);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        mIcon.setSelected(selected);
        mLabel.setSelected(selected);
    }
}
