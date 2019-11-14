package com.letrans.android.translator.view;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.bigkoo.pickerview.bean.CityBean;
import com.bigkoo.pickerview.listener.OnOptionsSelectListener;
import com.bigkoo.pickerview.view.CityPickerView;
import com.bigkoo.pickerview.view.WheelOptions;
import com.letrans.android.translator.R;
import com.letrans.android.translator.database.DatabaseHelper;
import com.letrans.android.translator.database.DbConstants;
import com.letrans.android.translator.database.TranslatorProvider;
import com.letrans.android.translator.mpush.domain.InputStreamConverterFactory;
import com.letrans.android.translator.mpush.domain.StringConverterFactory;
import com.letrans.android.translator.utils.NetBroadcastReceiver;
import com.letrans.android.translator.utils.NetUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class WeatherWidget extends RelativeLayout implements NetBroadcastReceiver.NetEvent{
    private static String TAG = "WeatherWidget";

    public static final int GET_WEATHER_DATA = 1;
    public static final int NETWORK_ERROR = 2;
    public static final int SERVER_ERROR = 3;
    public static final int LOCATION_ERROR = 4;
    private static final String SP_FILE_NAME = "WeatherWidget";
    private static final String SP_KEY_INIT = "alreadyInit";
    private static final String SP_KEY_CITYID = "city_id";
    private final String[] titleKey = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W",
            "X", "Y", "Z"};

    private final int REQUEST_RETRY_MAX_COUNT = 10;
    int retryCount = 0;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case GET_WEATHER_DATA: {
                    if (mLocationClient == null) {
                        return;
                    }
                    Log.i(TAG, "GET_WEATHER_DATA");
                    weatherGroup.setVisibility(View.INVISIBLE);
                    stateGroup.setVisibility(View.VISIBLE);
                    stateInfo.setText(getContext().getString(R.string.weather_update));

                    cityId = getCityIdFromPreference();
                    if (!TextUtils.isEmpty(cityId)) {
                        locationError = false;
                        getLocationSuccess = true;
                        getWeatherData();
                    } else {
                        getWeatherData();
                    }
                    break;
                }
                case NETWORK_ERROR: {
                    weatherGroup.setVisibility(View.INVISIBLE);
                    stateGroup.setVisibility(View.VISIBLE);
                    stateInfo.setText(getContext().getString(R.string.weather_net_error));
                    if (retryCount < REQUEST_RETRY_MAX_COUNT) {
                        try {
                            Thread.sleep(retryCount * 1000);
                            getWeatherData();
                        } catch (Exception e) {
                        }
                    }
                    break;
                }
                case SERVER_ERROR: {
                    weatherGroup.setVisibility(View.INVISIBLE);
                    stateGroup.setVisibility(View.VISIBLE);
                    stateInfo.setText(getContext().getString(R.string.weather_request_error));
                    break;
                }
                case LOCATION_ERROR: {
                    String id = getCityIdFromPreference();
                    if (isInit && !TextUtils.isEmpty(id)) {
                        locationError = false;
                        getLocationSuccess = true;
                        getWeatherData();
                    } else {
                        locationError = true;
                        weatherGroup.setVisibility(View.INVISIBLE);
                        stateGroup.setVisibility(View.VISIBLE);
                        stateInfo.setText(getContext().getString(R.string.weather_location_error));
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    View mRootView;
    TextView cityName;
    TextView currentDate;
    TextView weather;
    ImageView weatherIcon;
    TextView temp;
    Button settings;
    ViewGroup selectCity;
    ViewGroup weatherGroup;
    ViewGroup stateGroup;
    TextView stateInfo;
    ViewGroup progressBar;
    Button locationBtn;
    CityPickerView cityPickerView;
    
    ArrayList<String> options1Items = new ArrayList<>();
    List<List<CityBean>> options2Items = new ArrayList<>();
    List<List<List<CityBean>>> options3Items = new ArrayList<>();

    boolean getLocationSuccess = false;
    boolean locationError = false;
    String cityId = "";
    boolean isInit;

    //百度定位api
    public LocationClient mLocationClient = null;
    private MyLocationListener myListener = new MyLocationListener();
    //@
    DatabaseHelper databaseHelper;

    TimeChangeReceiver timeChangeReceiver;

    @Override
    public void onNetChange(boolean hasNetwork) {
        Log.i(TAG, "netMobile="+hasNetwork);

        if (hasNetwork) {
            if (!isInit) {
                readCityData();
            } else {
                handler.sendEmptyMessage(GET_WEATHER_DATA);
            }
        } else {
            handler.sendEmptyMessage(NETWORK_ERROR);
        }
    }

    public WeatherWidget(Context context) {
        this(context, null);
    }

    public WeatherWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.i(TAG, "onAttachedToWindow");

        //百度定位api
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedAddress(true);
        //option.setIsNeedLocationDescribe(true);

        mLocationClient = new LocationClient(getContext().getApplicationContext());
        mLocationClient.setLocOption(option);
        mLocationClient.registerLocationListener(myListener);
        //@

        NetBroadcastReceiver.registEvent(this, this);
        if (isInit) {
            timer.schedule(timerTask, 1000, 1000*60*60*2);
        } else {
            timer.schedule(timerTask, 1000 * 60 * 10, 1000*60*60*2);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);

        timeChangeReceiver = new TimeChangeReceiver();
        getContext().registerReceiver(timeChangeReceiver, intentFilter);

    }

    public WeatherWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        databaseHelper = TranslatorProvider.getInstance().getDatabaseHelper();

        SharedPreferences sp = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        isInit = sp.getBoolean(SP_KEY_INIT, false);
        if (!isInit) {
            readCityData();
        }
        mRootView = LayoutInflater.from(context).inflate(R.layout.weather_widget_layout, this);
        cityName = (TextView) mRootView.findViewById(R.id.city_name);
        currentDate = (TextView)mRootView.findViewById(R.id.current_date);
        weather = (TextView)mRootView.findViewById(R.id.weather_name);
        weatherIcon = (ImageView) mRootView.findViewById(R.id.weather_icon);
        temp = (TextView)mRootView.findViewById(R.id.temp);
        selectCity = (ViewGroup)mRootView.findViewById(R.id.select_city);
        weatherGroup = (ViewGroup)mRootView.findViewById(R.id.weather_group);
        stateGroup = (ViewGroup)mRootView.findViewById(R.id.state_info_group);
        stateInfo = (TextView)mRootView.findViewById(R.id.state_info);
        progressBar = (ViewGroup) mRootView.findViewById(R.id.progressbar);
        locationBtn = (Button) mRootView.findViewById(R.id.location);
        cityPickerView = (CityPickerView)mRootView.findViewById(R.id.pick_view);
        WheelOptions wheelOptions = cityPickerView.getWheelOptions();
        wheelOptions.setTextContentSize(24);
        wheelOptions.setOnOptionsSelectListener(new OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int i, int i1, int i2, View view) {
                CityBean cityBean = options3Items.get(i).get(i1).get(i2);
                cityId = cityBean.getAreaId()+".js";
                saveCityIdToPreference(cityId);
                locationError = false;
                getLocationSuccess = true;

                weatherGroup.setVisibility(VISIBLE);
                selectCity.setVisibility(View.GONE);
                handler.sendEmptyMessage(GET_WEATHER_DATA);
            }

            @Override
            public void onCancel() {
                weatherGroup.setVisibility(VISIBLE);
                selectCity.setVisibility(View.GONE);
            }
        });

        settings = (Button)findViewById(R.id.settings);
        settings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                initSelectCity();
            }
        });

        progressBar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        locationBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mLocationClient.start();
            }
        });
    }

    private void initSelectCity() {
        if (options1Items.size() == 0 || options2Items.size() == 0 || options3Items.size() == 0) {
            final SQLiteDatabase sqLiteDatabase = databaseHelper.getReadableDatabase();
            final String sql = "SELECT * FROM city WHERE "
                    + DbConstants.CityColumns.AREA_ID + "=" + DbConstants.CityColumns.BELONG_CITY
                    + " ORDER BY key ASC";
            final Cursor cursor = sqLiteDatabase.rawQuery(sql, null);
            if (cursor != null) {
                List<String> keyList = new ArrayList<>();
                for (String key : titleKey) {
                    keyList.add(key);
                }
                Observable.fromArray(titleKey)
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                progressBar.setVisibility(View.VISIBLE);
                                weatherGroup.setVisibility(View.GONE);
                            }
                        })
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(Schedulers.io())
                        .doOnNext(new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                boolean hasItem = false;
                                List<CityBean> option2_sub = new ArrayList<>();
                                List<List<CityBean>> option3_sub = new ArrayList<>();
                                while (cursor.moveToNext()) {
                                    String name = cursor.getString(cursor.getColumnIndex(DbConstants.CityColumns.CITY_NAME));
                                    String key = cursor.getString(cursor.getColumnIndex(DbConstants.CityColumns.KEY));
                                    int id = cursor.getInt(cursor.getColumnIndex(DbConstants.CityColumns.AREA_ID));
                                    if (s.equals(key)) {
                                        hasItem = true;
                                        CityBean cityBean = new CityBean();
                                        cityBean.setName(name);
                                        cityBean.setAreaId(String.valueOf(id));
                                        option2_sub.add(cityBean);

                                        String sql = "SELECT * FROM city WHERE " + DbConstants.CityColumns.BELONG_CITY + "=" + id;
                                        Cursor subCursor = sqLiteDatabase.rawQuery(sql, null);
                                        try {
                                            ArrayList<CityBean> options3_sub_sub = new ArrayList<>();
                                            if (subCursor != null) {
                                                while (subCursor.moveToNext()) {
                                                    CityBean subCityBean = new CityBean();
                                                    String subName = subCursor.getString(subCursor.getColumnIndex(DbConstants.CityColumns.CITY_NAME));
                                                    String subId = String.valueOf(subCursor.getInt(subCursor.getColumnIndex(DbConstants.CityColumns.AREA_ID)));
                                                    subCityBean.setName(subName);
                                                    subCityBean.setAreaId(subId);
                                                    options3_sub_sub.add(subCityBean);
                                                }
                                                option3_sub.add(options3_sub_sub);
                                            }
                                        } finally {
                                            subCursor.close();
                                        }
                                    } else {
                                        cursor.moveToPrevious();
                                        break;
                                    }
                                }
                                if (hasItem) {
                                    options1Items.add(s);
                                    options2Items.add(option2_sub);
                                    options3Items.add(option3_sub);
                                }
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {

                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                cursor.close();
                                Log.i("jingyi", "throwable=" + throwable.getMessage());
                            }
                        }, new Action() {
                            @Override
                            public void run() throws Exception {
                                Log.i("jingyi", "onComplete");
                                cursor.close();
                                selectCity.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                                if (options1Items.size() != 0) {
                                    cityPickerView.setPicker(options1Items, options2Items, options3Items);
                                }
                            }
                        });
            }
        } else {
            weatherGroup.setVisibility(View.GONE);
            selectCity.setVisibility(View.VISIBLE);
            cityPickerView.reSetCurrentItems();
        }
    }

    Retrofit mApiRetrofit = new Retrofit.Builder()
            .baseUrl("http://tianqi.2345.com/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(StringConverterFactory.create())
            .build();

    private interface WeatherApi {
        @GET("t/shikuang/"+"{city}")
        Observable<String> getWeather(
                @Path("city") String city
        );
    }

    private int CITY_NAME_COLUMN = 1;
    private int AREA_ID_COLUMN = 2;
    private int BELONG_ID_COLUMN = 3;

    //baidu
    public class MyLocationListener extends BDAbstractLocationListener{
        boolean read = false;
        @Override
        public void onReceiveLocation(BDLocation location){
            String addr = location.getAddrStr();    //获取详细地址信息
            String country = location.getCountry();    //获取国家
            String province = location.getProvince();    //获取省份
            String city = location.getCity();    //获取城市
            String district = location.getDistrict();    //获取区县
            String street = location.getStreet();    //获取街道信息

            Log.i(TAG, "province="+province+" city="+city);

            if (!TextUtils.isEmpty(province)) {
                getCityId(city, district);
                if (!getLocationSuccess) {
                    getCityId(city, city);
                }
            }
            if (getLocationSuccess) {
                handler.sendEmptyMessage(GET_WEATHER_DATA);
            }
        }
    }
    //@

    private void getCityId(String city, String district) {
        SQLiteDatabase sqLiteDatabase = databaseHelper.getReadableDatabase();
        String sql = "SELECT * FROM city";
        Cursor cursor = sqLiteDatabase.rawQuery(sql, null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.close();
            String shortDistrict = district.substring(0, 2);
            sql = "SELECT * FROM city WHERE " + DbConstants.CityColumns.CITY_NAME + " like ?";
            Cursor cityCursor = sqLiteDatabase.rawQuery(sql, new String[]{"%" + shortDistrict + "%"});
            try {
                if (cityCursor != null && cityCursor.getCount() > 0) {
                    while (cityCursor.moveToNext()) {
                        long areaId = cityCursor.getLong(AREA_ID_COLUMN);
                        long belongId = cityCursor.getLong(BELONG_ID_COLUMN);
                        Log.i(TAG, "areaId=" + areaId + " belongId=" + belongId);

                        sql = "SELECT * FROM city WHERE " + DbConstants.CityColumns.AREA_ID + " = ?";
                        Cursor belongCityCursor = sqLiteDatabase.rawQuery(sql, new String[]{String.valueOf(belongId)});
                        try {
                            if (belongCityCursor != null && belongCityCursor.getCount() > 0) {
                                belongCityCursor.moveToNext();
                                String belongCityName = belongCityCursor.getString(CITY_NAME_COLUMN);
                                Log.i(TAG, "belongCityName=" + belongCityName + " city=" + city);

                                if (city.contains(belongCityName)) {
                                    Log.i(TAG, ".............");
                                    getLocationSuccess = true;
                                    cityId = areaId + ".js";
                                    saveCityIdToPreference(cityId);
                                }
                            }
                        } finally {
                            belongCityCursor.close();
                        }
                    }
                }
            } finally {
                cityCursor.close();
            }
            if (!getLocationSuccess) {
                handler.sendEmptyMessage(LOCATION_ERROR);
            }
        }
    }

    public void getWeatherData() {
        if (NetUtil.isNetworkConnected(getContext())) {
            if (locationError) {
                return;
            }
            if (!getLocationSuccess) {
                mLocationClient.start();
                return;
            }
            //"71146.js"
            mApiRetrofit.create(WeatherApi.class)
                    .getWeather(cityId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) throws Exception {
                            String[] subStrings = s.split("weatherinfovar = ");
                            Log.i(TAG, "weather s=" + s);

                            int first = s.indexOf("{");
                            int last = s.lastIndexOf("}");
                            String json = s.substring(first, last+1);

                            WeatherData weatherData = JSON.parseObject(json, WeatherData.class);

                            updateView(weatherData);
                            retryCount = 0;
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            handler.sendEmptyMessage(SERVER_ERROR);
                            Log.i(TAG, "weather throwable=" + throwable.getMessage());
                        }
                    });

        } else {
            handler.sendEmptyMessage(NETWORK_ERROR);
        }
    }

    private void updateView(WeatherData weatherData) {
        weatherGroup.setVisibility(View.VISIBLE);
        stateGroup.setVisibility(View.INVISIBLE);

        cityName.setText(weatherData.weatherinfo.city);
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        String date = format.format(new Date(System.currentTimeMillis()));
        currentDate.setText(date);
        weather.setText(weatherData.weatherinfo.weather);
        int drawableId = getDrawableId("c_"+weatherData.weatherinfo.weatherIcon);
        if (drawableId <= 0) {
            weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.c_null));
        } else {
            weatherIcon.setImageDrawable(getResources().getDrawable(drawableId));
        }
        temp.setText(weatherData.weatherinfo.temp);
    }

    @SuppressWarnings("deprecation")
    private int getDrawableId(String code) {
            int id = getResources().getIdentifier(code, "drawable", "com.letrans.android.translator");
            return id;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mHeightMeasureSpec = heightMeasureSpec;

        int heightMode = MeasureSpec.getMode(mHeightMeasureSpec);
        if (heightMode == MeasureSpec.AT_MOST) {
            mHeightMeasureSpec = MeasureSpec.makeMeasureSpec((int) dp2px(150, getContext()), MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public float dp2px(float dpValue, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpValue, context.getResources().getDisplayMetrics());
    }

    public float sp2px(float spValue, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                spValue, context.getResources().getDisplayMetrics());
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.i(TAG, "onDetachedFromWindow");
        timer.cancel();
        timer = null;

        mLocationClient.unRegisterLocationListener(myListener);
        mLocationClient.stop();

        getContext().unregisterReceiver(timeChangeReceiver);

        super.onDetachedFromWindow();
    }

    Retrofit mCityApiRetrofit = new Retrofit.Builder()
            .baseUrl("http://tianqi.2345.com/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(InputStreamConverterFactory.create())
            .build();

    private interface GetCity {
        @GET("js/citySelectData.js")
        Observable<InputStream> getCityId();
    }

    public void readCityData(){
//        DatabaseHelper databaseHelper = new DatabaseHelper(getContext());
//        SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
//        ContentValues values = new ContentValues();

//        try {
//            InputStream inputStream1 = getContext().getAssets().open("city_id_detail.txt");
//            InputStreamReader inputStreamReader = new InputStreamReader(inputStream1, Constants.UTF_8);
//
//            BufferedReader reader = new BufferedReader(inputStreamReader);
//            StringBuffer sb = new StringBuffer("");
//            String line;
//            while ((line = reader.readLine()) != null) {
//                JSONArray list = JSON.parseArray(line.split("=")[1]);
//                for (int i = 0; i < list.size(); i++) {
//                    Log.i("jingyi", "i="+i+" content="+list.get(i));
//                    String[] item = list.get(i).toString().split("|");
//
//                    values.put(DbConstants.CityColumns.CITY_NAME, );
//                    sqLiteDatabase.insert(DbConstants.Tables.TABLE_CITY,
//                    DbConstants.CityColumns.CITY_NAME, values);
//
//                }
//            }
//        }catch (IOException e) {
//            Log.i("jingyi", "e="+e.getMessage());
//        }

        mCityApiRetrofit.create(GetCity.class)
                .getCityId()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        weatherGroup.setVisibility(View.INVISIBLE);
                        stateGroup.setVisibility(View.VISIBLE);
                        stateInfo.setText(getContext().getString(R.string.weather_init));
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<InputStream>() {
                    @Override
                    public void accept(InputStream inputStream) throws Exception {
                        SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();
                        Log.i(TAG, "start readingcity");

                        String sql = "SELECT * FROM city";
                        Cursor cursor = sqLiteDatabase.rawQuery(sql, null);
                        try {
                            if (cursor != null && cursor.getCount() > 0) {
                                int count = cursor.getCount();

                                Thread.sleep(1000);
                                cursor.close();
                                cursor = sqLiteDatabase.rawQuery(sql, null);
                                int count2 = cursor.getCount();

                                if (count != count2) {
                                    while (count != count2) {
                                        count = count2;
                                        Thread.sleep(1000);
                                        cursor.close();
                                        cursor = sqLiteDatabase.rawQuery(sql, null);
                                        count2 = cursor.getCount();
                                    }
                                    handler.sendEmptyMessage(GET_WEATHER_DATA);
                                    return;
                                } else {
                                    Log.i(TAG, "delete items");
                                    sqLiteDatabase.delete(DbConstants.Tables.TABLE_CITY,
                                            null, null);
                                }
                            }
                        }finally {
                            cursor.close();
                        }

                        ContentValues values = new ContentValues();
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "gbk");
                        BufferedReader reader = new BufferedReader(inputStreamReader);
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("provqx[")) {
                                JSONArray list = JSON.parseArray(line.split("=")[1]);
                                for (int i = 0; i < list.size(); i++) {
                                    String[] items = list.get(i).toString().split("\\|");

                                    for (String item : items) {
                                        String[] databaseValues = item.split("-");
                                        values.put(DbConstants.CityColumns.CITY_NAME, databaseValues[1].split(" ")[1]);
                                        values.put(DbConstants.CityColumns.AREA_ID, databaseValues[0]);
                                        values.put(DbConstants.CityColumns.BELONG_CITY, databaseValues[2]);
                                        values.put(DbConstants.CityColumns.KEY, databaseValues[1].split(" ")[0]);

                                        sqLiteDatabase.insert(DbConstants.Tables.TABLE_CITY,
                                                DbConstants.CityColumns.CITY_NAME, values);
                                    }
                                }
                            }
                        }
                        Log.i(TAG, "read finished");
                        SharedPreferences sp = getContext().getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
                        sp.edit().putBoolean(SP_KEY_INIT, true).commit();
                        handler.sendEmptyMessage(GET_WEATHER_DATA);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        handler.sendEmptyMessage(LOCATION_ERROR);
                        Log.i(TAG, "throwable="+throwable);
                    }
                });
    }

    private void saveCityIdToPreference(String id) {
        SharedPreferences sp = getContext().getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(SP_KEY_CITYID, id).commit();
    }

    private String getCityIdFromPreference() {
        SharedPreferences sp = getContext().getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
        return sp.getString(SP_KEY_CITYID, "");
    }

    static class WeatherData {
        public WeatherInfo weatherinfo;
        static class WeatherInfo {
            public String city;
            public String cityid;
            public String temp;
            public String SD;
            public String WD;
            public String WS;
            public String QY;
            public String JS;
            public String pm25;
            public String idx;
            public String lv_hint;
            public String aqiLevel;
            public String weather;
            public int weatherIcon;
        }

        @Override
        public String toString() {
            return "WeatherData{" +
                    "weatherinfo=" + weatherinfo +
                    '}';
        }
    }

    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            handler.sendEmptyMessage(GET_WEATHER_DATA);
        }
    };

    class TimeChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_TIME_TICK:
                case Intent.ACTION_TIME_CHANGED:
                case Intent.ACTION_TIMEZONE_CHANGED: {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                    String date = format.format(new Date(System.currentTimeMillis()));
                    currentDate.setText(date);
                    break;
                }
            }
        }
    }

}
