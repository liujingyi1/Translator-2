package com.letrans.android.translator.database.beans;

import com.letrans.android.translator.database.TranslatorProvider;

public class UserBean {
    private long id;
    private String deviceId;
    private String name;
    private String nickName;
    private int sex;
    private int photoId;
    private String language;
    private String description;
    private int role;

    public static final int ROLE_TYPE_OWNER = 1;
    public static final int ROLE_TYPE_GUEST = 2;

    public static UserBean getUser() {
        return TranslatorProvider.getInstance().getUser();
    }

    public boolean update(String name, String nickName, int sex,
                          int photoId, String language, String description,
                          int role) {
        this.name = name;
        this.nickName = nickName;
        this.sex = sex;
        this.photoId = photoId;
        this.language = language;
        this.description = description;
        this.role = role;
        int count = TranslatorProvider.getInstance().update(this);
        return count > 0;
    }

    public boolean update() {
        int count = TranslatorProvider.getInstance().update(this);
        return count > 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public int getPhotoId() {
        return photoId;
    }

    public void setPhotoId(int photoId) {
        this.photoId = photoId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return return the value with {@link #ROLE_TYPE_OWNER} or {@link #ROLE_TYPE_GUEST}.
     */
    public int getRole() {
        return role;
    }

    /**
     *
     * @param role Set value with {@link #ROLE_TYPE_OWNER} or {@link #ROLE_TYPE_GUEST}.
     */
    public void setRole(int role) {
        this.role = role;
    }

    public boolean isGuest() {
        return role == ROLE_TYPE_GUEST;
    }

    public int convertToResId(int photoId) {
        return photoId;
    }

    public int convertToId(int photoResId) {
        return photoResId;
    }
}
