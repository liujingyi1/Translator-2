package com.letrans.android.translator.microsoft.translator;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.letrans.android.translator.R;
import com.letrans.android.translator.TranslatorApp;
import com.letrans.android.translator.lyy.LyyConstants;
import com.letrans.android.translator.translate.ITranslate;
import com.letrans.android.translator.translate.ITranslateFinishedListener;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.Utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MSTranslate implements ITranslate {
    private static final String TAG = "RTranslator/MSTranslate";

    static String host = "https://api.cognitive.microsofttranslator.com";
    static String path = "/translate?api-version=3.0";

    private ITranslateFinishedListener mTranslateFinishedListener;
    boolean isRelease = false;

    @Override
    public void setTranslateFinishedListener(ITranslateFinishedListener listener) {
        this.mTranslateFinishedListener = listener;
    }

    @Override
    public void doTranslate(String content, String fromLanguage, String targetLanguage, long token) {
        Logger.i(TAG, "MSTranslate doTranslate");
        if (!isRelease) {
            try {

                fromLanguage = LyyConstants.getCountryCode(fromLanguage, "");
                targetLanguage = LyyConstants.getCountryCode(targetLanguage, "");

                String response = Translate(content, fromLanguage, targetLanguage);
                Logger.i(TAG, "response="+response);
                String result = "";

                if (response != null){
                    JsonParser parser = new JsonParser();
                    JsonElement json = parser.parse(response);
                    JsonObject jsonObject = json.getAsJsonArray().get(0).getAsJsonObject();
                    JsonObject resultJsonObject = jsonObject.getAsJsonArray("translations").get(0).getAsJsonObject();
                    result = resultJsonObject.get("text").getAsString();
                    Logger.i(TAG, "jsonObject="+jsonObject);
                }
                if (mTranslateFinishedListener != null) {
                    mTranslateFinishedListener.onTranslateFinish(result, token);
                }
            } catch (Exception e) {
                if (mTranslateFinishedListener != null) {
                    mTranslateFinishedListener.onTranslateFinish("", token);
                }
                Logger.e(TAG, "exception="+e.getMessage());
            }
        }
    }

    @Override
    public void release() {
        isRelease = true;
    }

    public String Post (URL url, String content) throws Exception {
        Log.i(TAG, "Post url="+url+" content="+content);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", content.getBytes("UTF-8").length + "");
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", Utils.getEncryptString(TranslatorApp.getAppContext(), R.string.microsoft_translat_key));
        String uuid = java.util.UUID.randomUUID().toString();
        connection.setRequestProperty("X-ClientTraceId", uuid);
        connection.setDoOutput(true);


        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        byte[] encoded_content = content.getBytes("UTF-8");
        wr.write(encoded_content, 0, encoded_content.length);
        wr.flush();
        wr.close();

        StringBuilder response = new StringBuilder ();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        return response.toString();
    }

    public String Translate (String contentStr, String from, String to) throws Exception {
        URL url = new URL (host + path + "&from="+from+"&to="+to);

        List<RequestBody> objList = new ArrayList<RequestBody>();
        objList.add(new RequestBody(contentStr));
        String content = new Gson().toJson(objList);

        return Post(url, content);
    }

    public String prettify(String json_text) {
        JsonParser parser = new JsonParser();
        JsonElement json = parser.parse(json_text);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);
    }

    public class RequestBody {
        String Text;

        public RequestBody(String text) {
            this.Text = text;
        }
    }
}
