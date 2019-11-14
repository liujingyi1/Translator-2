package com.letrans.android.translator.home;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.Slide;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.storage.IDataCleanListener;
import com.letrans.android.translator.R;
import com.letrans.android.translator.composemessage.ComposeMessageActivity;
import com.letrans.android.translator.database.beans.UserBean;
import com.letrans.android.translator.languagemodel.LanguageItem;
import com.letrans.android.translator.mvpbase.BaseMvpActivity;
import com.letrans.android.translator.mvpbase.BasePresenter;
import com.letrans.android.translator.permission.RequestPermissionsActivity;
import com.letrans.android.translator.settings.SettingsActivity;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.view.CircleIndicatorView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HomeActivity extends BaseMvpActivity implements IHomeView, View.OnClickListener {
    private static final String TAG = "RTranslator/HomeActivity";
    private static final int PAGE_ROW_COUNT = 3;//行
    private static final int PAGE_COLUMNS_COUNT = 4;//列
    private static final int PAGE_ITEMS_COUNT = PAGE_ROW_COUNT * PAGE_COLUMNS_COUNT;

    private static final int TRANSITION_ANIM_TYPE_COMPOSE = 100;

    private List<RecyclerView> mPages = new ArrayList<>();
    private ViewPager mViewPager;
    private CircleIndicatorView mIndicatorView;
    private ImageButton mSettingsBtn;
    private ImageView mLeftIndicator;
    private ImageView mRightIndicator;
    private View mAnimItemView;

    private int pageNum;
    private int lastPageItemNum;
    private String mHomeTransitionName;
    private int mTransitionAnimDuration;

    private IHomePresenter mHomePresenter;

    private int mViewPagerWidth;

    private long mFirstClickTime = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mHomePresenter = new HomePresenterImpl(this);
        super.onCreate(savedInstanceState);

        if (RequestPermissionsActivity.startPermissionActivity(this)) {
            Logger.i(TAG, "[onCreate]startPermissionActivity,return.");
            return;
        }
        mHomeTransitionName = getString(R.string.home_transition_name);
        setTransitionAnim();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        resetTimer();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetTimer();
    }

    private void resetTimer() {
        if (timer != null) {
            if (finishTimerTask != null) {
                finishTimerTask.cancel();
            }
        }
        if (TStorageManager.getInstance().getUser().getRole() == UserBean.ROLE_TYPE_GUEST) {
            if ((TStorageManager.getInstance().getCustomDefaultPage() == 2)) {
                Logger.i(TAG, "resetTimer...");
                finishTimerTask = new FinishTimerTask();
                timer.schedule(finishTimerTask, 15000, 15000);
            }
        }
    }

    private void setTransitionAnim() {
        mTransitionAnimDuration = getResources().getInteger(R.integer.transition_anim_duration);
        if (AppContext.USE_TRANSITION_ANIM) {
            Slide exit = new Slide(Gravity.TOP);
            exit.setDuration(mTransitionAnimDuration);
            exit.setInterpolator(new LinearInterpolator());
            getWindow().setExitTransition(exit);
        }
    }

    @Override
    public int getLayoutResId() {
        return R.layout.activity_home;
    }

    @Override
    protected int getStatusBarColor() {
        return Color.parseColor("#D9DCE4");
    }

    @Override
    public void initView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        mViewPager = (ViewPager) findViewById(R.id.id_language_items_container);

        for (int i = 0; i < pageNum; i++) {
            RecyclerView view = (RecyclerView) inflater.inflate(R.layout.layout_home_language_page, null);
            GridLayoutManager layoutManager = new GridLayoutManager(this, PAGE_COLUMNS_COUNT);
            view.setLayoutManager(layoutManager);
            LanguageRecyclerAdapter adapter = new LanguageRecyclerAdapter(this, i,
                    mHomePresenter.getLanguageItemList());
            adapter.setOnItemClickListener(mPagedItemOnClickListener);
            view.setAdapter(adapter);
            view.addItemDecoration(mItemDecoration);
            view.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
            mPages.add(view);
        }
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
        mViewPager.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {

                    @Override
                    public boolean onPreDraw() {
                        mViewPager.getViewTreeObserver().removeOnPreDrawListener(this);
                        mViewPagerWidth = mViewPager.getWidth();
                        Logger.v(TAG, "mViewPagerWidth=" + mViewPagerWidth);
                        return true;
                    }
                });
        mViewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_UP: {
                        resetTimer();
                        break;
                    }
                }
                return false;
            }
        });
        mIndicatorView = (CircleIndicatorView) findViewById(R.id.id_home_page_indicator);
        mIndicatorView.setUpWithViewPager(mViewPager);
        mSettingsBtn = (ImageButton) findViewById(R.id.id_floating_action_button);
        mSettingsBtn.setOnClickListener(this);
        mSettingsBtn.setVisibility(View.GONE);

        mLeftIndicator = (ImageView) findViewById(R.id.id_home_left_indicator);
        mLeftIndicator.setOnClickListener(this);
        mRightIndicator = (ImageView) findViewById(R.id.id_home_right_indicator);
        mRightIndicator.setOnClickListener(this);
    }

    @Override
    public void initData() {
        pageNum = mHomePresenter.getLanguageItemList().size() / PAGE_ITEMS_COUNT + 1;
        lastPageItemNum = mHomePresenter.getLanguageItemList().size() - (pageNum - 1) * PAGE_ITEMS_COUNT;
        Logger.v(TAG, "pageNum=" + pageNum + ", lastPageItemNum=" + lastPageItemNum);

        TStorageManager.getInstance().registerDataCleanListener(mDataCleanListener);
    }

    @Override
    public void onDestroy() {
        TStorageManager.getInstance().unregisterDataCleanListener(mDataCleanListener);
        timer.cancel();
        timer = null;
        super.onDestroy();
    }

    IDataCleanListener mDataCleanListener = new IDataCleanListener() {
        @Override
        public void onDataCleaned() {
            Logger.v(TAG, "onDataCleaned");
        }
    };

    ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (position == pageNum - 2 && positionOffsetPixels > 0) {
                if (mSettingsBtn.getVisibility() != View.VISIBLE) {
                    mSettingsBtn.setVisibility(View.VISIBLE);
                }

                mSettingsBtn.setAlpha(positionOffsetPixels * 1.0f / mViewPagerWidth);
                mSettingsBtn.setRotation(positionOffsetPixels * -360f / mViewPagerWidth);
            } else if (position == pageNum - 1) {
                if (mSettingsBtn.getVisibility() != View.VISIBLE) {
                    mSettingsBtn.setVisibility(View.VISIBLE);
                }

                mSettingsBtn.setAlpha(1f);
                mSettingsBtn.setRotation(0);
            } else {
                mSettingsBtn.setVisibility(View.GONE);
            }
        }

        @Override
        public void onPageSelected(int position) {
//            mSettingsBtn.setVisibility(position + 1 == pageNum? View.VISIBLE: View.GONE);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    protected BasePresenter bindPresenter() {
        return mHomePresenter;
    }

    private OnItemClickListener mPagedItemOnClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(View view, final int index) {
            if (System.currentTimeMillis() - mFirstClickTime < 1200) {
                Logger.v(TAG, "click so fast!");
                return;
            }
            mFirstClickTime = System.currentTimeMillis();
            Logger.v(TAG, "onItemClick:" + index + " - " + mHomePresenter.getLanguageItemList().get(index).toString());

            if (!mHomePresenter.isNetworkConnected(HomeActivity.this)
                    || TStorageManager.getInstance().isPortal()) {
                Logger.i(TAG, "No network !");
                startActivityWithTransition(new Intent(AppContext.ACTION_NETWORK_SETTING), 0);
            } else if (TextUtils.isEmpty(mHomePresenter.getPairedId())) {
                Logger.i(TAG, "No paired !");
                startActivityWithTransition(new Intent(AppContext.ACTION_PAIR_SETTING), 0);
            } else {
                if (mAnimItemView != null) {
                    mAnimItemView.setTransitionName("");
                }
                mAnimItemView = view;
                mAnimItemView.setTransitionName(mHomeTransitionName);

                mHomePresenter.isBothWorker(index);
            }
        }
    };

    @Override
    public void isBothWorkerFeedback(int index) {
        mHomePresenter.gotoComposemessage(index);
    }

    @Override
    public void gotoComposemessage() {
        Intent composeActivityIntent = new Intent(HomeActivity.this, ComposeMessageActivity.class);
        startActivityWithTransition(composeActivityIntent, TRANSITION_ANIM_TYPE_COMPOSE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.id_floating_action_button: {
                Logger.v(TAG, "Settings click");
                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivityWithTransition(intent, 0);
                break;
            }

            case R.id.id_home_left_indicator: {
                int index = mViewPager.getCurrentItem();
                if (index > 0) {
                    index--;
                    mViewPager.setCurrentItem(index, true);
                }
                break;
            }

            case R.id.id_home_right_indicator: {
                int index = mViewPager.getCurrentItem();
                index++;
                if (index < mViewPager.getChildCount()) {
                    mViewPager.setCurrentItem(index, true);
                }
                break;
            }
        }

    }

    public interface OnItemClickListener {
        void onItemClick(View view, int index);
    }

    private void startActivityWithTransition(Intent i, int transitionType) {
        if (AppContext.USE_TRANSITION_ANIM) {
            switch (transitionType) {
                case TRANSITION_ANIM_TYPE_COMPOSE: {
                    startActivity(i, ActivityOptions.makeSceneTransitionAnimation(this, mAnimItemView, mHomeTransitionName).toBundle());
                    break;
                }

                default: {
                    startActivity(i, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
                    break;
                }
            }
        } else {
            startActivity(i);
        }


    }

    class LanguageRecyclerAdapter extends RecyclerView.Adapter<LanguageRecyclerHolder> {
        private int pageIndex;
        private List<LanguageItem> datas;
        private LayoutInflater inflater;

        private OnItemClickListener mOnItemClickListener;

        public void setOnItemClickListener(OnItemClickListener listener) {
            mOnItemClickListener = listener;
        }

        public LanguageRecyclerAdapter(Context context, int pageIndex, List<LanguageItem> datas) {
            this.pageIndex = pageIndex;
            this.datas = datas;
            inflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public LanguageRecyclerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            view = inflater.inflate(R.layout.layout_home_language_item, parent, false);
            LanguageRecyclerHolder holder = new LanguageRecyclerHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull LanguageRecyclerHolder holder, final int position) {
            //Logger.v(TAG, "onBindViewHolder-pageIndex="+pageIndex+", position="+position);
            final int realIndex = pageIndex * PAGE_ITEMS_COUNT + position;
            holder.icon.setImageResource(datas.get(realIndex).getIconRes());
            holder.name.setText(datas.get(realIndex).getLanguageName());
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(v, realIndex);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            if ((pageIndex + 1) < pageNum) {
                return PAGE_ITEMS_COUNT;
            } else {
                return lastPageItemNum;
            }
        }
    }

    class LanguageRecyclerHolder extends RecyclerView.ViewHolder {
        private View view;
        private ImageView icon;
        private TextView name;

        public LanguageRecyclerHolder(View itemView) {
            super(itemView);
            view = itemView;
            icon = (ImageView) itemView.findViewById(R.id.id_home_language_item_icon);
            name = (TextView) itemView.findViewById(R.id.id_home_language_item_name);
        }
    }

    PagerAdapter mPagerAdapter = new PagerAdapter() {

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            container.addView(mPages.get(position));
            return mPages.get(position);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView(mPages.get(position));
        }

        @Override
        public int getCount() {
            return mPages.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    };

    RecyclerView.ItemDecoration mItemDecoration = new RecyclerView.ItemDecoration() {
        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.onDraw(c, parent, state);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.top = 3;
            outRect.left = 3;
            outRect.right = 3;
            outRect.bottom = 3;
        }
    };

    private static final int MSG_FINISH_ACTIVITY = 1;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (MSG_FINISH_ACTIVITY == msg.what) {
                finish();
            }
        }
    };
    Timer timer = new Timer();
    FinishTimerTask finishTimerTask;

    class FinishTimerTask extends TimerTask {
        @Override
        public void run() {
            handler.sendEmptyMessage(MSG_FINISH_ACTIVITY);
        }
    }
}
