package com.letrans.android.translator.database;

import com.letrans.android.translator.database.beans.MessageBean;

import java.util.List;

public interface MessageDao {
    MessageBean getMessageById(long id);
    List<MessageBean> getMessageByThreadId(int threadId);
    List<MessageBean> getMessageByMemberId(int memberId);
    List<MessageBean> getMessageByThreadIdAndMemberId(int threadId, int memberId);
    long insert(MessageBean messageBean);
    int update(MessageBean messageBean);
    int delete(MessageBean messageBean);
    int deleteMessageByThreadId(int threadId);
    void clean();
}
