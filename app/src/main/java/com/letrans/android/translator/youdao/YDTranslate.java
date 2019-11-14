package com.letrans.android.translator.youdao;

import com.letrans.android.translator.translate.ITranslate;
import com.letrans.android.translator.translate.ITranslateFinishedListener;
import com.letrans.android.translator.utils.Logger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class YDTranslate implements ITranslate {

    private static final String TAG = "RTranslator/youdao/Translate";

    private ITranslateFinishedListener mTranslateFinishedListener;

    public YDTranslate() {
        Logger.i(TAG, "Use Translate!");
    }

    @Override
    public void doTranslate(String message, String fromLanguage, final String targetLanguage,
                            final long token) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://fanyi.youdao.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        IPostRequest request = retrofit.create(IPostRequest.class);

        Call<PostTranslation> call = request.getCall(message, getXTX(fromLanguage,
                targetLanguage));

        call.enqueue(new Callback<PostTranslation>() {
            @Override
            public void onResponse(Call<PostTranslation> call, Response<PostTranslation> response) {
                Logger.i(TAG, "Result is :" + response.body().getTranslateResult().get(0).get(0)
                        .getTgt());
                mTranslateFinishedListener.onTranslateFinish(response.body().getTranslateResult()
                        .get(0).get(0).getTgt(), token);
            }

            @Override
            public void onFailure(Call<PostTranslation> call, Throwable throwable) {
                Logger.i(TAG, "Translate failed!");
            }
        });
    }

    private String getXTX(String sourceLanguage, String targetLanguage) {
        StringBuffer xtx = new StringBuffer();

        xtx.append("ZH".equals(sourceLanguage.substring(0, 2).toUpperCase()) ? "ZH_CN" :
                sourceLanguage
                        .substring(0, 2).toUpperCase());

        xtx.append("2");

        xtx.append("ZH".equals(targetLanguage.substring(0, 2).toUpperCase()) ? "ZH_CN" :
                targetLanguage
                        .substring(0, 2).toUpperCase());

        Logger.i(TAG, "xtx:" + xtx);

        return xtx.toString();
    }

    @Override
    public void setTranslateFinishedListener(ITranslateFinishedListener listener) {
        mTranslateFinishedListener = listener;
    }

    @Override
    public void release() {

    }
}
