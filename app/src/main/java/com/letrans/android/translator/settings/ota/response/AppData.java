package com.letrans.android.translator.settings.ota.response;

import java.util.ArrayList;

public class AppData {

    private String _id;
    private String packageName;
    private String versionCode;
    private String downloadUrl;
    private String versionName;
    private String installType;
    private String describe;
    private String mdf;

    private ArrayList<DiffChild> diffChild;
    private DiffChild child;

    private boolean diffUpdate;
    private String diffDownloadUrl;
    private String diffOldVersionName;
    private String diffChildSize;

    public AppData() {
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public String getMdf() {
        return mdf;
    }

    public void setMdf(String mdf) {
        this.mdf = mdf;
    }

    public void setInstallType(String installType) {
        this.installType = installType;
    }

    public String getInstallType() {
        return installType;
    }

    public ArrayList<DiffChild> getDiffChildList() {
        return diffChild;
    }

    public void setDiffChild(DiffChild child) {
        this.child = child;
    }

    public DiffChild getChild() {
        return child;
    }

    public String getDiffSize() {
        return diffChildSize;
    }

    public void setDiffSize(String diffChildSize) {
        this.diffChildSize = diffChildSize;
    }

    public String getDiffDownloadUrl() {
        return diffDownloadUrl;
    }

    public void setDiffDownloadUrl(String diffDownloadUrl) {
        this.diffDownloadUrl = diffDownloadUrl;
    }

    public String getDiffOldVersionName() {
        return diffOldVersionName;
    }

    public void setDiffOldVersionName(String diffOldVersionName) {
        this.diffOldVersionName = diffOldVersionName;
    }

    public boolean isDiffUpdate() {
        return diffUpdate;
    }

    public void setDiffUpdate(boolean diffUpdate) {
        this.diffUpdate = diffUpdate;
    }

    public class DiffChild {
        private String oldVersionName;
        private String size;
        private String downloadUrl;

        public String getDownloadUrl() {
            return this.downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public String getSize() {
            return this.size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getOldVersionName() {
            return this.oldVersionName;
        }

        public void setOldVersionName(String oldVersionName) {
            this.oldVersionName = oldVersionName;
        }
    }
}
