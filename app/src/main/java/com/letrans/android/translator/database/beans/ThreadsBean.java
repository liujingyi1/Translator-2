package com.letrans.android.translator.database.beans;

import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.database.TranslatorProvider;
import com.letrans.android.translator.utils.FileUtils;
import com.letrans.android.translator.utils.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ThreadsBean {
    private static final String TAG = "RTranslator/ThreadsBean";

    private long id;
    private String serverThreadId;
    private String date;
    private int messageCount;
    private String title;
    private int unreadCount;
    private String storageFolderName;
    private int threadType;
    private int deleted;
    private String lastMsgDate;

    private static final String ROOT_PATH = AppContext.SOUND_ABSOLUTE_FILE_DIR;

    private List<MessageBean> messageBeans;
    //Key: deviceId
    private HashMap<String, MemberBean> members;

    public interface ThreadType {
        int TYPE_SELF = 1;
        int TYPE_ONE_BY_ONE = 2;
        int TYPE_GROUP = 3;
    }

    public ThreadsBean(long id) {
        this.id = id;
    }

    private ThreadsBean(String serverThreadId, String date, int messageCount,
                        String title, int unreadCount, String storageFolderName,
                        int threadType, String lastMsgDate) {
        this.serverThreadId = serverThreadId;
        this.date = date;
        this.messageCount = messageCount;
        this.title = title;
        this.unreadCount = unreadCount;
        this.storageFolderName = storageFolderName;
        this.threadType = threadType;
        this.lastMsgDate = lastMsgDate;
        members = new HashMap<>();
        messageBeans = new ArrayList<>();
        this.id = TranslatorProvider.getInstance().insert(this);
    }

    public static ThreadsBean create(String serverThreadId, String date, int messageCount,
                                     String title, int unreadCount, String storageFolderName,
                                     int threadType) {
        return new ThreadsBean(serverThreadId, date, messageCount, title,
                unreadCount, storageFolderName, threadType, null);
    }

    public boolean update() {
        TranslatorProvider.getInstance().update(this, false);
        return true;
    }

    public boolean delete() {
        int count = TranslatorProvider.getInstance().delete(this);
        return count > 0 && FileUtils.deleteFiles(new File(ROOT_PATH, storageFolderName));
    }

    public boolean isDeleted() {
        return deleted == 1;
    }

    public boolean isGroup() {
        return threadType == ThreadType.TYPE_GROUP;
    }

    public long getId() {
        return id;
    }

    private void setId(long id) {
        this.id = id;
    }

    public String getServerThreadId() {
        return serverThreadId;
    }

    public void setServerThreadId(String serverThreadId) {
        this.serverThreadId = serverThreadId;
    }

    public void addMember(MemberBean memberBean) {
        if (members.containsKey(memberBean.getDeviceId())) {
            Logger.w(TAG, "Member exist - " + memberBean.getDeviceId());
        } else {
            members.put(memberBean.getDeviceId(), memberBean);
            TranslatorProvider.getInstance().update(this, true);
        }
    }

    public void removeMember(MemberBean memberBean) {
        removeMember(memberBean.getDeviceId());
    }

    public void removeMember(String deviceId) {
        members.remove(deviceId);
        TranslatorProvider.getInstance().update(this, true);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isRead() {
        return unreadCount == 0;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public List<MessageBean> getMessageBeans() {
        return messageBeans;
    }

    public void setMessageBeans(List<MessageBean> messageBeans) {
        this.messageBeans = messageBeans;
    }

    public HashMap<String, MemberBean> getMembers() {
        return members;
    }

    public String getStorageFolderName() {
        return storageFolderName;
    }

    public void setStorageFolderName(String storageFolderName) {
        this.storageFolderName = storageFolderName;
    }

    /**
     * @return threadType see {@link ThreadType}
     */
    public int getThreadType() {
        return threadType;
    }

    /**
     * @param threadType see {@link ThreadType}
     */
    public void setThreadType(int threadType) {
        this.threadType = threadType;
    }

    /**
     * @return 0:not deleted, 1:deleted
     */
    public int getDeleted() {
        return deleted;
    }

    /**
     * @param deleted 0:not deleted; 1:deleted
     */
    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    public String getLastMsgDate() {
        return lastMsgDate;
    }

    public void setLastMsgDate(String lastMsgDate) {
        this.lastMsgDate = lastMsgDate;
    }

    public static class Builder {
        private String serverThreadId;
        private String date;
        private int messageCount;
        private String title;
        private int unreadCount;
        private String storageFolderName;
        private int threadType;
        private String lastMsgDate;

        public Builder() {
        }

        public Builder(String serverThreadId) {
            this.serverThreadId = serverThreadId;
        }

        public Builder setServerThreadId(String serverThreadId) {
            this.serverThreadId = serverThreadId;
            return this;
        }

        public Builder setDate(String date) {
            this.date = date;
            return this;
        }

        public Builder setMessageCount(int messageCount) {
            this.messageCount = messageCount;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setUnreadCount(int unreadCount) {
            this.unreadCount = unreadCount;
            return this;
        }

        public Builder setStorageFolderName(String storageFolderName) {
            this.storageFolderName = storageFolderName;
            return this;
        }

        public Builder setThreadType(int threadType) {
            this.threadType = threadType;
            return this;
        }

        public Builder setLastMsgDate(String lastMsgDate) {
            this.lastMsgDate = lastMsgDate;
            return this;
        }

        public ThreadsBean build() {
            return new ThreadsBean(serverThreadId, date, messageCount,
                    title, unreadCount, storageFolderName, threadType, lastMsgDate);
        }
    }
}
