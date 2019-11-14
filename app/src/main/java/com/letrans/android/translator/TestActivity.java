package com.letrans.android.translator;

import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.letrans.android.translator.media.MediaManager;
import com.letrans.android.translator.mpush.TMPushService;
import com.letrans.android.translator.recorder.AudioRecordForTest;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class TestActivity extends AppCompatActivity {
    private static final String TAG = "RTranslator/AudioRecordForTest";

    private static final String BP = Environment.getExternalStorageDirectory() + "/" + "666";
    private static final String WAV_P = Environment.getExternalStorageDirectory() + "/" + "TEST.wav";


    TextView textView;
    CheckBox checkBox;

    AudioRecordForTest audioRecordForTest;
    List<String> pl;
    List<String> ccccccpl = new ArrayList<>();

    int index = 0;

    boolean isRecording = false;
    private static final int maxCount = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        audioRecordForTest  = AudioRecordForTest.getInstance();
        checkBox = (CheckBox) findViewById(R.id.id_cb);
        textView = (TextView) findViewById(R.id.id_test_text);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.v(TAG, "onClick");
                String filename = Utils.getCurrentTime();
                pl = audioRecordForTest.getPcmPathList();

                ccccccpl.clear();
                Logger.v(TAG, "size:"+ pl.size());
                String ccT = "";
                for (int i = 0; i < (pl.size() / maxCount) + 1; i++) {
                    Logger.v(TAG, "i" + i + ", maxCount:" + (i+1) * maxCount);
                    ccT = BP + "/"
                            + filename + "_" + i+".wav";

                    audioRecordForTest.copyWaveFiles(pl, ccT, i*maxCount, (i+1) * maxCount);
                    ccccccpl.add(ccT);

                }

                index = 0;
                Logger.v(TAG, "index:"+index+", " + ccccccpl.get(index));
                if (checkBox.isChecked()) {
                    MediaManager.playSound(ccccccpl.get(index), onCompletionListener, null);
                } else {
                   audioRecordForTest.copyWaveFiles(pl, WAV_P, 0, pl.size());
                    MediaManager.playSound(WAV_P, null, null);
                }
            }
        });
    }


    MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            Logger.v(TAG, "onCompletion:" + index);
            index++;
            if (index < ccccccpl.size()) {
                Logger.v(TAG, "index:"+index+", " + ccccccpl.get(index));
                MediaManager.playSound(ccccccpl.get(index), onCompletionListener, null);
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MUTE && !isRecording) {
            isRecording = true;
            textView.setText("开始");
            audioRecordForTest.prepare(BP, Utils.getCurrentTime());
            audioRecordForTest.startRecord();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MUTE) {
            audioRecordForTest.stopRecord();
            textView.setText("结束");
            isRecording = false;
        }
        return super.onKeyUp(keyCode, event);
    }
}
