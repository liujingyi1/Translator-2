package com.letrans.android.translator.database;

import com.letrans.android.translator.database.beans.UserBean;

public interface UserDao {
    UserBean getUser();
    UserBean getUserById(int id);
    int update(UserBean userBean);
}
