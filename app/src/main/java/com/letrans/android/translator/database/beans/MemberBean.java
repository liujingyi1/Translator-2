package com.letrans.android.translator.database.beans;

import com.letrans.android.translator.database.TranslatorProvider;

public class MemberBean {
    private long id;
    private String deviceId;
    private String name;
    private String nickName;
    private int sex;
    private int photoId;
    private String language;
    private String description;
    private int favorite = -1;

    public MemberBean(long id) {
        this.id = id;
    }

    private MemberBean(String deviceId, String name, String nickName,
                       int sex, int photoId, String language, String description,
                       int favorite) {
        this.deviceId = deviceId;
        this.name = name;
        this.nickName = nickName;
        this.sex = sex;
        this.photoId = photoId;
        this.language = language;
        this.description = description;
        this.favorite = favorite;
        this.id = TranslatorProvider.getInstance().insert(this);
    }

    public static MemberBean create(String deviceId, String name, String nickName,
                                    int sex, int photoId, String language, String description,
                                    int favorite) {
        return new MemberBean(deviceId, name, nickName, sex, photoId, language, description, favorite);
    }

    public boolean update() {
        int count = TranslatorProvider.getInstance().update(this);
        return count > 0;
    }

    public boolean delete() {
        int count = TranslatorProvider.getInstance().delete(this);
        return count > 0;
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

    public int getFavorite() {
        return favorite;
    }

    public void setFavorite(int favorite) {
        this.favorite = favorite;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public long getId() {
        return id;
    }

    private void setId(long id) {
        this.id = id;
    }

    public int convertToResId(int photoId) {
        return photoId;
    }

    public int convertToId(int photoResId) {
        return photoResId;
    }

    public static class Builder {
        private String deviceId;
        private String name;
        private String nickName;
        private int sex;
        private int photoId;
        private String language;
        private String description;
        private int favorite = -1;

        public Builder(String deviceId) {
            this.deviceId = deviceId;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setNickName(String nickName) {
            this.nickName = nickName;
            return this;
        }

        /**
         *
         * @param sex 1: man, 2: women
         * @return
         */
        public Builder setSex(int sex) {
            this.sex = sex;
            return this;
        }

        public Builder setPhotoId(int photoId) {
            this.photoId = photoId;
            return this;
        }

        public Builder setLanguage(String language) {
            this.language = language;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         *
         * @param favorite :1 -> had favorite, 0 -> not favorite
         * @return
         */
        public Builder setFavorite(int favorite) {
            this.favorite = favorite;
            return this;
        }

        public MemberBean build() {
            return new MemberBean(deviceId, name, nickName, sex, photoId,
                    language, description, favorite);
        }
    }
}
