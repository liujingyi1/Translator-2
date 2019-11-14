package com.letrans.android.translator.mvpbase;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.letrans.android.translator.utils.Logger;

public abstract class BaseMvpActivity extends AppCompatActivity {

    private static final String TAG = "RTranslator/BaseMvpActivity";

    private BasePresenter presenter = null;
    public Context mContext;

    private int mStatusBarColor = Color.TRANSPARENT;
    private int mNavigationBarColor = Color.TRANSPARENT;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getStatusBarColor());
        window.setNavigationBarColor(getNavigationBarColor());

        if (isLightColor(getStatusBarColor())) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        setContentView(getLayoutResId());
        mContext = this;
        presenter = bindPresenter();
        initData();
        initView();
    }

    private boolean isLightColor(int color) {
        return ColorUtils.calculateLuminance(color) >= 0.5;
    }


    /**
     * 返回资源的布局
     *
     * @return
     */
    public abstract int getLayoutResId();

    protected int getStatusBarColor(){
        return mStatusBarColor;
    };

    protected int getNavigationBarColor(){
        return mNavigationBarColor;
    };

    protected void setTheme(){};

    /**
     * 组件初始化操作
     */
    public abstract void initView();

    /**
     * 页面初始化页面数据，在initView之后调用
     */
    public abstract void initData();

    /**
     * 绑定presenter，主要用于销毁工作
     *
     * @return
     */
    protected abstract BasePresenter bindPresenter();

    /**
     * 如果重写了此方法，一定要调用super.onDestroy();
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            Logger.i(TAG, "onDestroy presenter:" + presenter);
            presenter.onDestroy();
            presenter = null;
            System.gc();
        }
    }
}
