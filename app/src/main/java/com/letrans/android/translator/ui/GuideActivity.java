package com.letrans.android.translator.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.VideoView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.letrans.android.translator.R;
import com.letrans.android.translator.home.HomeActivity;
import com.letrans.android.translator.lyy.LyyConstants;
import com.letrans.android.translator.lyy.TranslatePool;
import com.letrans.android.translator.lyy.TranslateWork;
import com.letrans.android.translator.lyy.WorkCallBack;
import com.letrans.android.translator.mpush.sdk.MPush;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.NetUtil;
import com.letrans.android.translator.view.Banner;
import com.letrans.android.translator.view.WeatherWidget;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GuideActivity extends AppCompatActivity {
    private static final String TAG = "RTranslator/GuideActivity";

    @BindView(R.id.weather_view)
    WeatherWidget weatherWidget;
    @BindView(R.id.banner)
    Banner banner;
    @BindView(R.id.notice_text_webview)
    WebView webView;
    @BindView(R.id.video_view)
    VideoView videoView;
    @BindView(R.id.corver_view)
    View corverView;
    @BindView(R.id.view_group)
    ViewGroup viewGroup;

    Context mContext;

    String noticeTextString = "";
    List<String> noticePics = new ArrayList<>();
    AlphaAnimation showAnimation;

    private static String NOTICE_UPDATE_ACTION_TEXT = "com.translator.UPDATE_NOTICE_TEXT";
    private static String NOTICE_UPDATE_ACTION_PICS = "com.translator.UPDATE_NOTICE_PICS";
    private IntentFilter intentFilter;
    private MyBroadcastReceiver myBroadcastReceiver;
    private static final int UPDATE_NOTICE_TEXT = 1;
    private static final int UPDATE_NOTICE_PIC = 2;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_NOTICE_TEXT: {
                    updateSource();
                    webView.loadData(noticeTextString, "text/html; charset=UTF-8", null);
                    break;
                }
                case UPDATE_NOTICE_PIC: {
                    updateSource();
                    banner.update(noticePics);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setStatusBarColor(getStatusBarColor());

        if (isLightColor(getStatusBarColor())) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        mContext = getApplicationContext();
        setContentView(R.layout.activity_guide);

        ButterKnife.bind(this);

        updateSource();

        initView();
        showView();

        intentFilter = new IntentFilter();
        intentFilter.addAction(NOTICE_UPDATE_ACTION_TEXT);
        intentFilter.addAction(NOTICE_UPDATE_ACTION_PICS);
        myBroadcastReceiver = new MyBroadcastReceiver();
        registerReceiver(myBroadcastReceiver, intentFilter);
    }

    private void showView() {
        Logger.i(TAG, "showView");
        banner.setBannerStyle(Banner.BannerConfig.CIRCLE_INDICATOR);
        banner.setDelayTime(3000);
        banner.setIndicatorGravity(Banner.BannerConfig.CENTER);
        if (noticePics == null || noticePics.size() == 0) {
            //getLyyPics();
        } else {
            banner.setImages(noticePics).start();
        }

        if (TextUtils.isEmpty(noticeTextString)) {
            //getLyyText();
        } else {
            webView.loadData(noticeTextString, "text/html; charset=UTF-8", null);
        }

        String uri = "android.resource://" + getPackageName() + "/" + R.raw.video;
        videoView.setVideoURI(Uri.parse(uri));
    }

    private void updateSource() {
        noticeTextString = TStorageManager.getInstance().getNoticeText();
        Logger.i(TAG, "noticeTextString=" + noticeTextString);

        noticePics.clear();
        String pics = TStorageManager.getInstance().getNoticePic();
        Logger.i(TAG, "pics=" + pics);
        if (!TextUtils.isEmpty(pics)) {
            String[] picarr = pics.split(",");
            for (String pic : picarr) {
                Logger.i(TAG, "pic=" + pic);
                noticePics.add(pic);
            }
        }
    }

    private void initView() {
        webView.setBackgroundColor(Color.TRANSPARENT);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                Logger.i(TAG, "onPrepared...");
                mediaPlayer.start();
                mediaPlayer.setLooping(true);
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Logger.i(TAG, "onCompletion...");
                stopPlaybackVideo();
            }
        });
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                Logger.i(TAG, "onError... what="+what+" extra="+extra);
                stopPlaybackVideo();
                return true;
            }
        });
  
        videoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoHomepage();
            }
        });

        corverView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GuideActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        showAnimation = new AlphaAnimation(0.0f, 1.0f);
        showAnimation.setDuration(1000);
        showAnimation.setFillAfter(true);
        showAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                viewGroup.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void getLyyText() {
        WorkCallBack<TranslateWork> workCallBack = new WorkCallBack<TranslateWork>() {
            @Override
            public void callback(TranslateWork translator) {
                String result = translator.getNotice(LyyConstants.OPFLAG_NOTICE_TEXT);
                if (!TextUtils.isEmpty(result)) {
                    JSONObject jsonObject = JSON.parseObject(result);
                    String pic01 = (String) jsonObject.get("txt_01");
                    TStorageManager.getInstance().setNoticeText(pic01);
                }
            }
        };
        new Thread(TranslatePool.execute(workCallBack)).start();
    }

    private void getLyyPics() {
        WorkCallBack<TranslateWork> workCallBack = new WorkCallBack<TranslateWork>() {
            @Override
            public void callback(TranslateWork translator) {
                String result = translator.getNotice(LyyConstants.OPFLAG_NOTICE_PIC);
                if (!TextUtils.isEmpty(result)) {
                    List<String> picStrings = new ArrayList<>();
                    JSONObject jsonObject = JSON.parseObject(result);
                    String pic01 = (String) jsonObject.get("pic_01");
                    if (!TextUtils.isEmpty(pic01)) picStrings.add(pic01);
                    String pic02 = (String) jsonObject.get("pic_02");
                    if (!TextUtils.isEmpty(pic02)) picStrings.add(pic02);
                    String pic03 = (String) jsonObject.get("pic_03");
                    if (!TextUtils.isEmpty(pic03)) picStrings.add(pic03);

                    StringBuilder sb = new StringBuilder();
                    try {
                        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/lyypic/";
                        File filedir = new File(dir);
                        if (!filedir.exists()) {
                            filedir.mkdir();
                        }
                        for (int i = 0; i < picStrings.size(); i++) {
                            byte[] picData = Base64.decode(picStrings.get(i), Base64.DEFAULT);
                            Bitmap bitmap01 = BitmapFactory.decodeByteArray(picData, 0, picData.length, null);
                            File file = new File(dir + "pic_0" + i + ".jpg");
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                            FileOutputStream out = new FileOutputStream(file);
                            bitmap01.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            out.flush();
                            out.close();
                            Logger.i(TAG, "file_" + i + ":" + file.getAbsoluteFile());
                            sb.append(file.getAbsoluteFile());
                            if (i < picStrings.size() - 1) sb.append(",");
                        }
                        if (!TextUtils.isEmpty(sb)) {
                            TStorageManager.getInstance().setNoticePic(sb.toString());
                        }
                    } catch (Exception e) {
                        Logger.i(TAG, "getlyypics exception="+e.getMessage());
                        e.printStackTrace();
                    }

                    handler.sendEmptyMessage(UPDATE_NOTICE_PIC);
                }
            }
        };
        new Thread(TranslatePool.execute(workCallBack)).start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_MUTE == keyCode) {
            gotoHomepage();
        }

        return super.onKeyDown(keyCode, event);
    }

    private void gotoHomepage() {
        Intent intent = new Intent(GuideActivity.this, HomeActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        banner.startAutoPlay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.i(TAG, "onResume");
        if (!videoView.isPlaying()) {
            videoView.resume();
        }
        viewGroup.startAnimation(showAnimation);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.i(TAG, "onPause");
        if (videoView.canPause()){
            videoView.pause();
        }
        viewGroup.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        banner.stopAutoPlay();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(myBroadcastReceiver);
        stopPlaybackVideo();
        super.onDestroy();
    }

    private boolean isLightColor(int color) {
        return ColorUtils.calculateLuminance(color) >= 0.5;
    }

    protected int getStatusBarColor() {
        return Color.parseColor("#D9DCE4");
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NOTICE_UPDATE_ACTION_TEXT.equals(intent.getAction())) {
                Logger.i(TAG, "receive update notice message text");
                handler.sendEmptyMessage(UPDATE_NOTICE_TEXT);
            } else if (NOTICE_UPDATE_ACTION_PICS.equals(intent.getAction())) {
                Logger.i(TAG, "receive update notice message pics");
                handler.sendEmptyMessage(UPDATE_NOTICE_PIC);
            }
        }
    }
    private void stopPlaybackVideo() {
        try {
            videoView.stopPlayback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
