package com.letrans.android.translator.database;

import com.letrans.android.translator.database.beans.MemberBean;

import java.util.List;

public interface MemberDao {
    List<MemberBean> getAllMembers();
    MemberBean getMemberById(int id);
    MemberBean getMemberByDeviceId(String deviceId);
    long insert(MemberBean memberBean);
    int update(MemberBean memberBean);
    int delete(MemberBean memberBean);
    void clean();
}
