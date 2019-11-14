package com.letrans.android.translator.composemessage;

import android.app.Activity;

import com.letrans.android.translator.database.beans.MemberBean;
import com.letrans.android.translator.database.beans.MessageBean;
import com.letrans.android.translator.database.beans.ThreadsBean;
import com.letrans.android.translator.languagemodel.ILanguagePresenter;
import com.letrans.android.translator.languagemodel.LanguageItem;

import java.util.HashMap;
import java.util.List;

public class ComposeMessageContact {
    public interface IComposeMessageView {
        // 语音转文字完成更新界面
        void callbackSTT(MessageBean messageBean);

        // 根据声音分贝更新界面
        void updateVoiceLevel(int level);

        // 收到消息,更新数据开始播放动画
        void receiveMessage(MessageBean messageBean);

        // 文字转语音完成停止动画（如果有报错给用户提示）
        void callbackTTS(int status, MessageBean message);

        // 聊天记录音频播放结束停止动画
        void onPlayCompletion();

        // 对方进入了聊天界面，更新国旗
        void updateOtherIcon(String language);

        // 被对方干掉了，你得拜拜了
        void finishByHungup();

        //发生某些情况时关闭聊天界面，如匹配被解除
        void closeView();
    }

    public interface IComposeMessagePresenter extends ILanguagePresenter {
        // 1.	获取配对者MemberBean对象
        HashMap<String, MemberBean> getMembers(String threadId);

        // 2.	获取配对者LanguageItem对象
        // ILanguagePresenter中已经定义了LanguageItem getLanguageItem(String code);

        // 3.	开始说话
        void recordStart();

        // 4.	结束说话
        void recordFinish();

        // 4.	初始化STT，并设置mSTTFinishedListener发送消息
        void initSTT(Activity activity);

        // 5.	初始化MPush，接收到消息
        void initMPush();

        // 6.	配对者状态变化
        // 看initMPush中的onEnter和onHangUp

        // 7.	被配对者挂断
        // 看initMPush中的onHangUp中的HANGUP_OTHER

        // 8.	创建Thread
        ThreadsBean createThread(int threadType);

        // 9.	初始化TTS,转完直接播报或synthesizeToUri
        void initTTS(Activity activity);

        // 9.	播放语音
        void playSound(MessageBean messageBean);

        void playSoundByText(MessageBean messageBean);

        // 10.	挂断（发送已经退出的消息）
        void hangUpSelf();

        // 11.	远程挂断（通知对方挂断）
        void hangUpOther();

        // 12.	调整字体大小
        void setTextSize(int size);

        // 13.	获取字体大小
        int getTextSize();

        // 14.	设置是否自动播放
        void setAuto();

        // 15.	获取是否自动播放
        boolean getAuto();

        // 16.	初始化翻译，并设置mTranslateFinishedListener开始文字转语音
        void initTranslate();

        // 17.	获取信息列表
        List<MessageBean> getMessageBeanList(String serverThreadId);

        // 18.	查询对方语言
        void searchOtherLanguage();

        // 19.	确认下自己是不是访客
        boolean isGuest();

        void stopPlaySound();
        void resumePlaySound();

        void onResume();

        void onPause();

        LanguageItem getLanguageItem();
        LanguageItem getPairedLanguageItem();

        void setSecondLanguage(String language);
    }
}
