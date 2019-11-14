package com.letrans.android.translator.settings.role;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.letrans.android.translator.R;
import com.letrans.android.translator.database.beans.UserBean;
import com.letrans.android.translator.mpush.domain.ServerApi;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.ToastUtils;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class RoleSettingFragment extends Fragment {
    private String TAG = "RTranslator/PairSettingFragment";

    @BindView(R.id.start_page_1)
    ImageView startPage1;
    @BindView(R.id.start_page_2)
    ImageView startPage2;
    @BindView(R.id.role_admin)
    RadioButton mRbAdmin;
    @BindView(R.id.role_custom)
    RadioButton mRbCustom;
    @BindView(R.id.language_list)
    Spinner mLanguageSpinner;
//    @BindView(R.id.weather_view)
//    WeatherWidget weatherWidget;
    @BindView(R.id.role_custom_group)
    View customGroup;
    @BindView(R.id.role_admin_group)
    View adminGroup;
    @BindView(R.id.start_page_text1)
    TextView startPageTitle1;
    @BindView(R.id.start_page_text2)
    TextView startPageTitle2;

    View mRootView;
    private Unbinder unbinder;
    UserBean userBean;
    String[] mLanguageNames;
    String[] mLanguageCodes;
    int rawRole;
    String rawLanguage;
    int tempRole;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userBean = UserBean.getUser();
        rawRole = userBean.getRole();
        rawLanguage = userBean.getLanguage();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_role, container, false);
        unbinder = ButterKnife.bind(this, mRootView);

        initView();

        return mRootView;
    }

    public void initView() {
        mRbCustom.setChecked(userBean.getRole() == UserBean.ROLE_TYPE_GUEST);
        mRbAdmin.setChecked(userBean.getRole() == UserBean.ROLE_TYPE_OWNER);
        mLanguageSpinner.setEnabled(userBean.getRole() == UserBean.ROLE_TYPE_OWNER);

        mLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Observable.just(i)
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) throws Exception {
                                userBean.setLanguage(mLanguageCodes[integer]);
                                userBean.update();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                            }
                        });
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        mLanguageNames = getResources().getStringArray(R.array.language_name_array);
        mLanguageCodes = getResources().getStringArray(R.array.language_code_array);
        String language = userBean.getLanguage();
        for (int i = 0; i < mLanguageCodes.length; i++) {
            if (language.equals(mLanguageCodes[i])) {
                mLanguageSpinner.setSelection(i, true);
                break;
            }
        }

        updatePageSelectView();

        RxView.clicks(customGroup)
                .throttleFirst(1, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .doOnNext(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {
                        userBean.setRole(UserBean.ROLE_TYPE_GUEST);
                        userBean.update();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object serverResponse) throws Exception {
                        mRbCustom.setChecked(true);
                        mRbAdmin.setChecked(false);
                        mLanguageSpinner.setEnabled(false);

                        if (tempRole == UserBean.ROLE_TYPE_OWNER) {
                            updatePageSelectView();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.i(TAG, "throwable="+throwable.getMessage());
                    }
                });

        RxView.clicks(adminGroup)
                .throttleFirst(1, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .doOnNext(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {
                        userBean.setRole(UserBean.ROLE_TYPE_OWNER);
                        userBean.update();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object serverResponse) throws Exception {
                        mRbCustom.setChecked(false);
                        mRbAdmin.setChecked(true);
                        mLanguageSpinner.setEnabled(true);

                        if (tempRole == UserBean.ROLE_TYPE_GUEST) {
                            updatePageSelectView();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.i(TAG, "throwable="+throwable.getMessage());
                    }
                });
    }

    private void updatePageSelectView() {
        tempRole = userBean.getRole();
        if(tempRole == UserBean.ROLE_TYPE_GUEST) {
            startPage1.setImageDrawable(getActivity().getDrawable(R.drawable.start_page_1));
            startPageTitle1.setText(R.string.start_page_language);
            startPage2.setImageDrawable(getActivity().getDrawable(R.drawable.start_page_2));
            startPageTitle2.setText(R.string.start_page_notice);

            startPage1.setSelected(TStorageManager.getInstance().getCustomDefaultPage() == 1);
            startPage2.setSelected(TStorageManager.getInstance().getCustomDefaultPage() == 2);
        } else if (tempRole == UserBean.ROLE_TYPE_OWNER) {
            startPage1.setImageDrawable(getActivity().getDrawable(R.drawable.start_page_1));
            startPageTitle1.setText(R.string.start_page_language);
            startPage2.setImageDrawable(getActivity().getDrawable(R.drawable.start_page_3));
            startPageTitle2.setText(R.string.start_page_chat);

            startPage1.setSelected(TStorageManager.getInstance().getAdminDefaultPage() == 1);
            startPage2.setSelected(TStorageManager.getInstance().getAdminDefaultPage() == 2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (rawRole != userBean.getRole()
                || !rawLanguage.equals(userBean.getLanguage())) {
            ServerApi.getInstance()
                    .changeRole(TStorageManager.getInstance().getDeviceId()
                            , userBean.getRole(), userBean.getLanguage(), null);
        }
    }


    @Override
    public void onDestroy() {
        if (unbinder != null) {
            unbinder.unbind();
        }
        super.onDestroy();
    }

    @OnClick({R.id.start_page_1, R.id.start_page_2})
    public void onClick(ImageView view) {
        switch (view.getId()) {
            case R.id.start_page_1: {
                startPage1.setSelected(true);
                startPage2.setSelected(false);
                if ((userBean.getRole() == UserBean.ROLE_TYPE_OWNER)) {
                    TStorageManager.getInstance().setAdminDefaultPage(1);
                } else if (userBean.getRole() == UserBean.ROLE_TYPE_GUEST) {
                    TStorageManager.getInstance().setCustomDefaultPage(1);
                }
                break;
            }
            case R.id.start_page_2: {
                startPage1.setSelected(false);
                startPage2.setSelected(true);
                if ((userBean.getRole() == UserBean.ROLE_TYPE_OWNER)) {
                    TStorageManager.getInstance().setAdminDefaultPage(2);
                } else if (userBean.getRole() == UserBean.ROLE_TYPE_GUEST) {
                    TStorageManager.getInstance().setCustomDefaultPage(2);
                }
                break;
            }
        }
    }
}
