package com.letrans.android.translator.composemessage;

import android.Manifest;
import android.animation.Animator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Slide;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.MainActivity;
import com.letrans.android.translator.R;
import com.letrans.android.translator.TranslatorApp;
import com.letrans.android.translator.database.DbConstants.MessageType;
import com.letrans.android.translator.database.beans.MessageBean;
import com.letrans.android.translator.database.beans.ThreadsBean;
import com.letrans.android.translator.database.beans.UserBean;
import com.letrans.android.translator.home.HomeActivity;
import com.letrans.android.translator.languagemodel.LanguageItem;
import com.letrans.android.translator.mvpbase.BaseMvpActivity;
import com.letrans.android.translator.mvpbase.BasePresenter;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.tts.TTSConstants;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.NetBroadcastReceiver;
import com.letrans.android.translator.utils.NetUtil;
import com.letrans.android.translator.view.AudioRecorderButton;
import com.letrans.android.translator.view.RecorderDialogManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ComposeMessageActivity extends BaseMvpActivity implements View.OnClickListener, ComposeMessageContact.IComposeMessageView, NetBroadcastReceiver.NetEvent {
    private static final String TAG = "RTranslator/ComposeMessageActivity";

    private static final int MSG_RECEIVED = 1001;
    private static final int MSG_PLAY_ANIMI = 1002;
    private static final int MSG_SPEAK_SUCCESS = 1003;
    private static final int MSG_OTHER_LANGUAGE_CHANGED_RECEIVED = 1004;
    private static final int MSG_STT_CALLBACK = 1005;
    private static final int MSG_SPEAK_ERROR = 1006;


    private String[] PERMISSIONS_NEED = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    private List<String> permissions = new ArrayList<>();
    @BindView(R.id.id_compose_list)
    RecyclerView mComposeList;

    private ComposeListAdapter mComposeLiseAdapter;
    private List<MessageBean> mDatas = null;

    @BindView(R.id.id_recorder_btn)
    AudioRecorderButton mAudioRecorderButton;
    private TextViewAnim mTextViewAnim;
    private List<List<Integer>> mTextViewAnimList = new ArrayList<>();
    @BindView(R.id.id_compose_end)
    View mEndConversationBtn;
    @BindView(R.id.id_compose_end_other)
    View mEndOtherConversationBtn;
    @BindView(R.id.id_owner_icon)
    TextView mOwnerIcon;
    @BindView(R.id.id_paired_icon)
    TextView mPairedIcon;
    @BindView(R.id.id_auto_img)
    TextView mAutoImage;
    @BindView(R.id.id_compose_text_size_large)
    ImageButton mTextSizeLarge;
    @BindView(R.id.id_compose_text_size_normal)
    ImageButton mTextSizeNormal;
    @BindView(R.id.id_compose_text_size_small)
    ImageButton mTextSizeSmall;
    @BindView(R.id.id_recorder_dialog)
    View mRecorderDialog;
    @BindView(R.id.id_recorder_dialog_voice)
    ImageView mRecorderVioce;

    private LinearLayoutManager mLayoutManager;

    private ComposeMessageContact.IComposeMessagePresenter mComposeMessagePresenter;

    private float[] mTextSizeScales = new float[]{0.8f, 1.0f, 1.3f};
    private int mTextSizeIndex;
    private int mDefaultTextSize;
    private int mTransitionAnimDuration;
    private boolean mAutoReport;
    private int mTextSizeBgRes;
    private int mTextSizeSelectedBgRes;
    private int mAutoImgOffRes;
    private boolean isRecording = false;
    private boolean isEnterAnimDone = false;
    private AlertDialog NoNetDialog;
    private String secondLanguage;
    private boolean isDestoryed = false;

    private long clickTime = 0;

    private RecorderDialogManager mRecorderDialogManager;

    @Override
    public int getLayoutResId() {
        return R.layout.activity_compose_message;
    }

    @Override
    protected int getStatusBarColor() {
        if (AppContext.STYLE_TYPE_LIGHT == TStorageManager.getInstance().getStyleType()) {
            return Color.parseColor("#D9DCE4");
        } else {
            return Color.parseColor("#FF000000");
        }
    }

    @Override
    protected void setTheme() {
        if (AppContext.STYLE_TYPE_LIGHT == TStorageManager.getInstance().getStyleType()) {
            setTheme(R.style.ComposeThemeLight);
        } else {
            setTheme(R.style.ComposeThemeDack);
        }
    }

    @Override
    public void initView() {
        ButterKnife.bind(this);
        mAudioRecorderButton.setRecorderDialogManager(mRecorderDialogManager);
        mAudioRecorderButton.setAudioRecorderStateListener(new AudioRecorderButton.onAudioRecorderStateListener() {
            @Override
            public void onFinish(boolean isToShort, int stats) {
                Logger.i(TAG, "AudioRecorderStateListener - onFinish");
                mComposeMessagePresenter.recordFinish();
            }

            @Override
            public void onStart() {
                Logger.i(TAG, "AudioRecorderStateListener - onStart");
                mComposeMessagePresenter.recordStart();
            }
        });

        mLayoutManager = new LinearLayoutManager(this);
        mComposeList.setLayoutManager(mLayoutManager);
        mComposeLiseAdapter = new ComposeListAdapter(this, mDatas, mComposeMessagePresenter);
        mComposeLiseAdapter.updateTextSize(mDefaultTextSize * mTextSizeScales[mTextSizeIndex]);
        mComposeList.setAdapter(mComposeLiseAdapter);
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        mComposeList.setItemAnimator(itemAnimator);
        mComposeLiseAdapter.setOnItemClickListener(new ComposeListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Logger.v(TAG, "onItemClick:" + position);
                long currentTime = System.currentTimeMillis();
                if (currentTime - clickTime < 100) {
                    Logger.w(TAG, "click to Fast !");
                    return;
                }
                clickTime = currentTime;
                if ((mDatas.get(position).getType() == MessageType.TYPE_SOUND
                        || mDatas.get(position).getType() == MessageType.TYPE_SOUND_TEXT)
                        && !mDatas.get(position).isSend()) {
                    //播放动画
                    mTextViewAnim.startPlayAnim((TextView) view,
                            mDatas.get(position).getText(),
                            mTextViewAnimList.get(mTextSizeIndex).size() - 1);

                    String code = TStorageManager.getInstance().getUser().getLanguage();
                    //播放音频
                    if (AppContext.ROOBO.equals(TStorageManager.getInstance().getLanguageItem(code).getTts())) {
                        mComposeMessagePresenter.playSoundByText(mDatas.get(position));
                    } else {
                        mComposeMessagePresenter.playSound(mDatas.get(position));
                    }
                }
            }
        });
        mComposeLiseAdapter.setReceivedPlayDrawableRes(mTextViewAnimList.get(mTextSizeIndex).get(mTextViewAnimList.get(mTextSizeIndex).size() - 1));

        LanguageItem item = mComposeMessagePresenter.getLanguageItem();
        mOwnerIcon.setCompoundDrawablesWithIntrinsicBounds(null,
                getResources().getDrawable(item.getProfilPictureRes(), null), null, null);
        mOwnerIcon.setText(item.getLanguageName());
        mOwnerIcon.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!isEnterAnimDone) {
                    isEnterAnimDone = true;
                    startOwnerAnim();
                }
            }
        });

        mEndConversationBtn.setOnClickListener(this);
        mEndOtherConversationBtn.setVisibility(mComposeMessagePresenter.isGuest() ? View.GONE : View.VISIBLE);
        mEndOtherConversationBtn.setOnClickListener(this);
        mAutoReport = mComposeMessagePresenter.getAuto();
        mAutoImage.setCompoundDrawablesWithIntrinsicBounds(null,
                getDrawable(mAutoReport ? R.drawable.compose_auto_on : mAutoImgOffRes),
                null, null);
        mAutoImage.setOnClickListener(this);

        if (mComposeMessagePresenter.getTextSize() == 0) {
            mTextSizeSmall.setBackgroundResource(mTextSizeSelectedBgRes);
        } else if (mComposeMessagePresenter.getTextSize() == 1) {
            mTextSizeNormal.setBackgroundResource(mTextSizeSelectedBgRes);
        } else {
            mTextSizeLarge.setBackgroundResource(mTextSizeSelectedBgRes);
        }

        mTextSizeLarge.setOnClickListener(this);
        mTextSizeNormal.setOnClickListener(this);
        mTextSizeSmall.setOnClickListener(this);
    }

    @Override
    public void onPlayCompletion() {
        H.sendEmptyMessage(MSG_SPEAK_SUCCESS);
    }

    @Override
    public void updateOtherIcon(String language) {
        Message message = new Message();
        message.what = MSG_OTHER_LANGUAGE_CHANGED_RECEIVED;
        message.obj = language;
        H.sendMessage(message);
    }

    @Override
    public void finishByHungup() {
        if (TStorageManager.getInstance().getUser().getRole() == UserBean.ROLE_TYPE_OWNER
                && TStorageManager.getInstance().getAdminDefaultPage() == 2) {
            startActivity(new Intent(ComposeMessageActivity.this, HomeActivity.class));
            finish();
        } else {
            startActivity(new Intent(ComposeMessageActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void closeView() {
        if (TStorageManager.getInstance().getUser().getRole() == UserBean.ROLE_TYPE_OWNER
                && TStorageManager.getInstance().getAdminDefaultPage() == 2) {
            startActivity(new Intent(ComposeMessageActivity.this, HomeActivity.class));
            finish();
        } else {
            startActivity(new Intent(ComposeMessageActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void initData() {
        mRecorderDialogManager = new RecorderDialogManager(this);

        mTextViewAnim = TextViewAnim.getInstance(getApplicationContext());
        mDatas = mComposeMessagePresenter.getMessageBeanList(AppContext.LOCAL_SERVER_THREAD_ID);

        mTextSizeIndex = mComposeMessagePresenter.getTextSize();
        mDefaultTextSize = getResources().getDimensionPixelSize(R.dimen.msg_text_size);

        initTextAnimList();
        mTextViewAnim.setAnimSrcList(mTextViewAnimList);
        mTextViewAnim.setResIndex(mTextSizeIndex);
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(R.attr.Compose_UI_slide_bar_textSize_background, tv, true);
        mTextSizeBgRes = tv.resourceId;
        getTheme().resolveAttribute(R.attr.Compose_UI_slide_bar_textSize_background_select, tv, true);
        mTextSizeSelectedBgRes = tv.resourceId;
        getTheme().resolveAttribute(R.attr.Compose_UI_slide_bar_auto_img, tv, true);
        mAutoImgOffRes = tv.resourceId;

        NetBroadcastReceiver.registEvent(this, this);
        NoNetDialog = new AlertDialog.Builder(this).setTitle(R.string.compose_no_net_title)
                .setMessage(R.string.compose_no_net_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(AppContext.ACTION_NETWORK_SETTING));
                    }
                }).create();
        NoNetDialog.setCancelable(false);
    }

    private void initTextAnimList() {
        mTextViewAnimList.clear();
        List<Integer> s = new ArrayList<>();
        s.add(R.drawable.compose_play_recordor_wave_receive_s_v1);
        s.add(R.drawable.compose_play_recordor_wave_receive_s_v2);
        s.add(R.drawable.compose_play_recordor_wave_receive_s_v3);

        List<Integer> normal = new ArrayList<>();
        normal.add(R.drawable.compose_play_recordor_wave_receive_v1);
        normal.add(R.drawable.compose_play_recordor_wave_receive_v2);
        normal.add(R.drawable.compose_play_recordor_wave_receive_v3);

        List<Integer> l = new ArrayList<>();
        l.add(R.drawable.compose_play_recordor_wave_receive_l_v1);
        l.add(R.drawable.compose_play_recordor_wave_receive_l_v2);
        l.add(R.drawable.compose_play_recordor_wave_receive_l_v3);

        mTextViewAnimList.add(s);
        mTextViewAnimList.add(normal);
        mTextViewAnimList.add(l);
    }

    @Override
    protected BasePresenter bindPresenter() {
        return mComposeMessagePresenter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mComposeMessagePresenter = new ComposeMessagePresenterImpl(this, this, ThreadsBean
                .ThreadType.TYPE_ONE_BY_ONE);
        super.onCreate(savedInstanceState);
        Logger.v(TAG, "onCreate");
        setTransitionAnim();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Logger.v(TAG, "onNewIntent");
        if (mTextViewAnim == null) {
            Logger.w(TAG, "WTF !");
        }
    }

    private void setTransitionAnim() {
        mTransitionAnimDuration = getResources().getInteger(R.integer.transition_anim_duration);

        if (AppContext.USE_TRANSITION_ANIM) {
            Slide enter = new Slide(Gravity.TOP);
            enter.setDuration(mTransitionAnimDuration);
            enter.setInterpolator(new LinearInterpolator());
            getWindow().setEnterTransition(enter);

            ChangeBounds elementEnter = new ChangeBounds();
            elementEnter.setDuration(mTransitionAnimDuration);
            elementEnter.setInterpolator(new OvershootInterpolator());
            getWindow().setSharedElementEnterTransition(elementEnter);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.id_auto_img: {
                Logger.v(TAG, "Auto");
                mAutoReport = !mAutoReport;
                mAutoImage.setCompoundDrawablesWithIntrinsicBounds(null,
                        getDrawable(mAutoReport ? R.drawable.compose_auto_on : mAutoImgOffRes),
                        null, null);
                mComposeMessagePresenter.setAuto();

                /*
                if (AppContext.STYLE_TYPE_LIGHT == TStorageManager.getInstance().getStyleType()) {
                    TStorageManager.getInstance().setStyleType(AppContext.STYLE_TYPE_DRAK);
                } else {
                    TStorageManager.getInstance().setStyleType(AppContext.STYLE_TYPE_LIGHT);
                }
                */
                break;
            }

            case R.id.id_compose_end: {
                mComposeMessagePresenter.hangUpSelf();
                if (TStorageManager.getInstance().getUser().getRole() == UserBean.ROLE_TYPE_OWNER
                        && TStorageManager.getInstance().getAdminDefaultPage() == 2) {
                    startActivity(new Intent(ComposeMessageActivity.this, HomeActivity.class));
                    finish();
                } else {
                    startActivity(new Intent(ComposeMessageActivity.this, MainActivity.class));
                    finish();
                }
                break;
            }

            case R.id.id_compose_end_other: {
                mComposeMessagePresenter.hangUpOther();
                mEndOtherConversationBtn.setVisibility(View.GONE);
                break;
            }

            case R.id.id_compose_text_size_large: {
                if (mComposeMessagePresenter.getTextSize() != 2) {
                    mTextSizeIndex = 2;
                    mComposeMessagePresenter.setTextSize(mTextSizeIndex);
                    mTextViewAnim.setResIndex(mTextSizeIndex);
                    mComposeLiseAdapter.updateTextSize(mDefaultTextSize * mTextSizeScales[mTextSizeIndex]);
                    mComposeLiseAdapter.setReceivedPlayDrawableRes(mTextViewAnimList.get(mTextSizeIndex).get(mTextViewAnimList.get(mTextSizeIndex).size() - 1));
                    mTextSizeLarge.setBackgroundResource(mTextSizeSelectedBgRes);
                    mTextSizeNormal.setBackgroundResource(mTextSizeBgRes);
                    mTextSizeSmall.setBackgroundResource(mTextSizeBgRes);
                    mComposeLiseAdapter.notifyDataSetChanged();

                    int currentPos = mDatas.size() - 1;
                    Logger.w(TAG, "currentPos = " + currentPos);
                    if (currentPos > 0) {
                        mComposeList.smoothScrollToPosition(currentPos);
                    }
                }
                break;
            }

            case R.id.id_compose_text_size_normal: {
                if (mComposeMessagePresenter.getTextSize() != 1) {
                    mTextSizeIndex = 1;
                    mComposeMessagePresenter.setTextSize(mTextSizeIndex);
                    mTextViewAnim.setResIndex(mTextSizeIndex);
                    mComposeLiseAdapter.updateTextSize(mDefaultTextSize * mTextSizeScales[mTextSizeIndex]);
                    mComposeLiseAdapter.setReceivedPlayDrawableRes(mTextViewAnimList.get(mTextSizeIndex).get(mTextViewAnimList.get(mTextSizeIndex).size() - 1));
                    mTextSizeLarge.setBackgroundResource(mTextSizeBgRes);
                    mTextSizeNormal.setBackgroundResource(mTextSizeSelectedBgRes);
                    mTextSizeSmall.setBackgroundResource(mTextSizeBgRes);
                    mComposeLiseAdapter.notifyDataSetChanged();

                    int currentPos = mDatas.size() - 1;
                    Logger.w(TAG, "currentPos = " + currentPos);
                    if (currentPos > 0) {
                        mComposeList.smoothScrollToPosition(currentPos);
                    }
                }
                break;
            }

            case R.id.id_compose_text_size_small: {
                if (mComposeMessagePresenter.getTextSize() != 0) {
                    mTextSizeIndex = 0;
                    mComposeMessagePresenter.setTextSize(mTextSizeIndex);
                    mTextViewAnim.setResIndex(mTextSizeIndex);
                    mComposeLiseAdapter.updateTextSize(mDefaultTextSize * mTextSizeScales[mTextSizeIndex]);
                    mComposeLiseAdapter.setReceivedPlayDrawableRes(mTextViewAnimList.get(mTextSizeIndex).get(mTextViewAnimList.get(mTextSizeIndex).size() - 1));
                    mTextSizeLarge.setBackgroundResource(mTextSizeBgRes);
                    mTextSizeNormal.setBackgroundResource(mTextSizeBgRes);
                    mTextSizeSmall.setBackgroundResource(mTextSizeSelectedBgRes);
                    mComposeLiseAdapter.notifyDataSetChanged();
                }
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Logger.v(TAG, "orientation = " + newConfig.orientation);
    }

    @Override
    protected void onResume() {
        mComposeMessagePresenter.onResume();
        mComposeMessagePresenter.searchOtherLanguage();
        super.onResume();
//        startOwnerAnim();
        checkPermission();
    }

    @Override
    protected void onPause() {
        mComposeMessagePresenter.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        isDestoryed = true;
        super.onDestroy();
        Logger.v(TAG, "onDestroy");
        NetBroadcastReceiver.unRegistEvent(this);

        H.removeMessages(0);
        mTextViewAnim.stopPlayAnim();
        mTextViewAnim = null;
        mDatas = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_MUTE == keyCode && !isRecording) {
            if (!NetUtil.isNetworkConnected(TranslatorApp.getAppContext())) {
                Logger.w(TAG, "No network");
            } else {
                isRecording = true;
                mComposeMessagePresenter.stopPlaySound();
                mRecorderDialog.setVisibility(View.VISIBLE);
                mComposeMessagePresenter.recordStart();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Logger.v(TAG, "onKeyUp: " + keyCode);
        if (KeyEvent.KEYCODE_MUTE == keyCode) {
            mComposeMessagePresenter.recordFinish();
            mRecorderDialog.setVisibility(View.GONE);
            mComposeMessagePresenter.resumePlaySound();
            isRecording = false;
        }
        return super.onKeyUp(keyCode, event);
    }

    protected void checkPermission() {
        permissions.clear();

        for (String permission : PERMISSIONS_NEED) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission);
            }
        }

        if (!permissions.isEmpty()) {
            String[] ps = new String[permissions.size()];
            permissions.toArray(ps);
            ActivityCompat.requestPermissions(this, ps, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void callbackSTT(MessageBean messageBean) {
        Message message = new Message();
        message.what = MSG_STT_CALLBACK;
        message.obj = messageBean;
        H.sendMessage(message);
    }

    @Override
    public void updateVoiceLevel(int level) {
        mAudioRecorderButton.updateVoiceLevel(level);
        if (mRecorderDialog.getVisibility() == View.VISIBLE) {
            if (level < 1) {
                level = 1;
            } else if (level > 7) {
                level = 7;
            }

            int resId = mContext.getResources().getIdentifier("ic_recorder_v" + level, "drawable", mContext.getPackageName());
            mRecorderVioce.setImageResource(resId);
        }
    }

    @Override
    public void receiveMessage(MessageBean messageBean) {
        Message message = new Message();
        message.what = MSG_RECEIVED;
        message.obj = messageBean;
        H.sendMessage(message);
    }

    @Override
    public void callbackTTS(int status, MessageBean messageBean) {
        Logger.v(TAG, "STATE_SPEAK_START-" + messageBean.getPositionInList());
        switch (status) {
            case TTSConstants.STATE_SPEAK_START: {
                if (mComposeMessagePresenter.getAuto()) {
                    if (mComposeMessagePresenter.getAuto()) {
                        Logger.d(TAG, "- smoothScrollToPosition: " + messageBean.getPositionInList());
                        mComposeList.smoothScrollToPosition(messageBean.getPositionInList());
                    }
                    Message message = new Message();
                    message.what = MSG_PLAY_ANIMI;
                    message.arg1 = messageBean.getPositionInList();
                    H.sendMessageDelayed(message, 30);// delay 200 > 0

                }
                break;
            }
            case TTSConstants.STATE_SPEAK_SUCCESS: {
                H.sendEmptyMessage(MSG_SPEAK_SUCCESS);
                break;
            }

            case TTSConstants.STATE_SPEAK_ERROR: {
                H.sendEmptyMessage(MSG_SPEAK_ERROR);
                break;
            }

            case TTSConstants.STATE_TIMEOUT: {
                break;
            }

            case TTSConstants.STATE_SYN_SUCCESS: {
                break;
            }

            case TTSConstants.STATE_SYN_ERROR: {
                break;
            }
        }
    }

    private Handler H = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isDestoryed) {
                Logger.w(TAG, "Activity has destroy, RETURN! Message:" + msg.what);
                return;
            }
            switch (msg.what) {
                case MSG_RECEIVED: {
                    MessageBean messageBean = (MessageBean) msg.obj;
                    mComposeLiseAdapter.addItem(messageBean);
                    int currentPos = mDatas.size() - 1;
                    messageBean.setPositionInList(currentPos);
                    Logger.w(TAG, "MSG_RECEIVED - currentPos = " + currentPos);
                    if (!mComposeMessagePresenter.getAuto()) {
                        Logger.d(TAG, "MSG_RECEIVED - smoothScrollToPosition: " + currentPos);
                        mComposeList.smoothScrollToPosition(currentPos);
                    }
                    break;
                }

                case MSG_STT_CALLBACK: {
                    MessageBean messageBean = (MessageBean) msg.obj;
                    if (messageBean != null) {
                        mComposeLiseAdapter.addItem(messageBean);
                        Logger.d(TAG, "MSG_STT_CALLBACK - smoothScrollToPosition: " + (mDatas.size() - 1));
                        mComposeList.smoothScrollToPosition(mDatas.size() - 1);
                        mAudioRecorderButton.dismissRecordingDialog();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.final_response_error, Toast.LENGTH_LONG).show();
                        mAudioRecorderButton.dismissRecordingDialog();
                    }
                    break;
                }

                case MSG_PLAY_ANIMI: {
                    Logger.v(TAG, "MSG_PLAY_ANIMI, " + msg.arg1);
                    View item = mLayoutManager.findViewByPosition(msg.arg1);
                    //播放动画
                    if (item == null) {
                        Logger.w(TAG, "itemView is NULL");
                    } else {
                        mTextViewAnim.startPlayAnim((TextView) item.findViewById(R.id.id_msg_txt),
                                mDatas.get(msg.arg1).getText(),
                                mTextViewAnimList.get(mTextSizeIndex).size() - 1);
                    }
                    break;
                }
                case MSG_SPEAK_SUCCESS: {
                    mTextViewAnim.stopPlayAnim();
                    break;
                }

                case MSG_SPEAK_ERROR: {
                    mTextViewAnim.stopPlayAnim();
                    break;
                }

                case MSG_OTHER_LANGUAGE_CHANGED_RECEIVED: {
                    String language = (String) msg.obj;
                    mComposeMessagePresenter.setSecondLanguage(language);
                    if (TextUtils.isEmpty(language)) {
                        mPairedIcon.setCompoundDrawablesWithIntrinsicBounds(null,
                                getResources().getDrawable(R.drawable.ic_profile_waiting, null), null, null);
                        mPairedIcon.setText(R.string.compose_waiting);
                        mEndOtherConversationBtn.setVisibility(View.GONE);
                    } else {
                        LanguageItem item = mComposeMessagePresenter.getLanguageItem(language);
                        mPairedIcon.setCompoundDrawablesWithIntrinsicBounds(null,
                                getResources().getDrawable(item.getProfilPictureRes(), null), null, null);
                        mPairedIcon.setText(item.getLanguageName());
                        if (!mComposeMessagePresenter.isGuest()) {
                            mEndOtherConversationBtn.setVisibility(View.VISIBLE);
                        }
                    }
                    startPaireAnim();
                    break;
                }
            }
        }
    };

    private void startOwnerAnim() {
        Animator mPaireAnimator = ViewAnimationUtils.createCircularReveal(mOwnerIcon,
                mOwnerIcon.getWidth() / 2, mOwnerIcon.getHeight() / 2,
                0,
                mOwnerIcon.getHeight());
        mPaireAnimator.setDuration(500);
        mPaireAnimator.setInterpolator(new AccelerateInterpolator());
        mPaireAnimator.start();
    }

    private void startPaireAnim() {
        Animator mPaireAnimator = ViewAnimationUtils.createCircularReveal(mPairedIcon,
                mPairedIcon.getWidth() / 2, mPairedIcon.getHeight() / 2,
                0,
                mPairedIcon.getHeight());
        mPaireAnimator.setDuration(500);
        mPaireAnimator.setInterpolator(new AccelerateInterpolator());
        mPaireAnimator.start();
    }

    @Override
    public void onNetChange(boolean hasNetwork) {
        Logger.v(TAG, "onNetChange = " + hasNetwork);
        if (hasNetwork) {
            if (NoNetDialog != null && NoNetDialog.isShowing()) {
                NoNetDialog.dismiss();
            }
        } else {
            if (NoNetDialog != null) {
                NoNetDialog.show();
            }
        }
    }
}