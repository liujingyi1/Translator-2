package com.letrans.android.translator.settings.common;


import android.app.AlarmManager;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.SystemProxy;
import com.letrans.android.translator.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class FragmentDateTime extends Fragment {
    private static final String TAG = "FragmentDateTime";
    TimeZoneChangedReceiver mTimeZoneChangedReceiver;
    public static final String EXTRA_TIME_PREF_24_HOUR_FORMAT =
            "android.intent.extra.TIME_PREF_24_HOUR_FORMAT";
    private Spinner mTimeZone;
    private Switch mSwitch_12_24;
    private Switch mAutoTimeZone;

    private SystemProxy mSystemProxy;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mSystemProxy = SystemProxy.getInstance();
        View view = inflater.inflate(R.layout.setting_date_time, container, false);
        mTimeZone = (Spinner) view.findViewById(R.id.sp_time_zone);
        mSwitch_12_24 = (Switch) view.findViewById(R.id.sw_24_hour_format);
        mAutoTimeZone = (Switch) view.findViewById(R.id.sw_auto_time_zone);
        configDateAndTime();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mTimeZoneChangedReceiver == null) {
            mTimeZoneChangedReceiver = new TimeZoneChangedReceiver();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(mTimeZoneChangedReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTimeZoneChangedReceiver != null) {
            getContext().unregisterReceiver(mTimeZoneChangedReceiver);
        }
    }

    private static class TimeZoneRow implements Comparable<TimeZoneRow> {

        private static final boolean SHOW_DAYLIGHT_SAVINGS_INDICATOR = false;

        public final String mId;
        public final String mDisplayName;
        public final int mOffset;

        public TimeZoneRow(String id, String name, long currentTimeMillis) {
            final TimeZone tz = TimeZone.getTimeZone(id);
            final boolean useDaylightTime = tz.useDaylightTime();
            mId = id;
            mOffset = tz.getOffset(currentTimeMillis);
            mDisplayName = buildGmtDisplayName(name, useDaylightTime);
        }

        @Override
        public int compareTo(TimeZoneRow another) {
            return mOffset - another.mOffset;
        }

        public String buildGmtDisplayName(String displayName, boolean useDaylightTime) {
            final int p = Math.abs(mOffset);
            final StringBuilder name = new StringBuilder("(GMT");
            name.append(mOffset < 0 ? '-' : '+');

            name.append(p / DateUtils.HOUR_IN_MILLIS);
            name.append(':');

            int min = p / 60000;
            min %= 60;

            if (min < 10) {
                name.append('0');
            }
            name.append(min);
            name.append(") ");
            name.append(displayName);
            if (useDaylightTime && SHOW_DAYLIGHT_SAVINGS_INDICATOR) {
                name.append(" \u2600"); // Sun symbol
            }
            return name.toString();
        }
    }


    private class TimeZoneChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == Intent.ACTION_TIMEZONE_CHANGED) {
                final String[][] timezones = getAllTimezones();
                TimeZone locale = TimeZone.getDefault();
                String localeID = locale.getID();
                String[] localeIDs = timezones[0];
                int index = 64;
                for (int i = 0; i < localeIDs.length; i++) {
                    if (localeID.endsWith(localeIDs[i])) {
                        index = i;
                    }
                }
                mTimeZone.setSelection(index);
            }
        }
    }

    public String[][] getAllTimezones() {
        final Resources res = getActivity().getResources();
        final String[] ids = res.getStringArray(R.array.timezone_values);
        final String[] labels = res.getStringArray(R.array.timezone_labels);

        int minLength = ids.length;
        if (ids.length != labels.length) {
            minLength = Math.min(minLength, labels.length);
            Logger.d("Tag", "Timezone ids and labels have different length!");
        }

        final long currentTimeMillis = System.currentTimeMillis();
        final List<TimeZoneRow> timezones = new ArrayList<>(minLength);
        for (int i = 0; i < minLength; i++) {
            timezones.add(new TimeZoneRow(ids[i], labels[i], currentTimeMillis));
        }
        Collections.sort(timezones);

        final String[][] timeZones = new String[2][timezones.size()];
        int i = 0;
        for (TimeZoneRow row : timezones) {
            timeZones[0][i] = row.mId;
            timeZones[1][i++] = row.mDisplayName;
        }
        return timeZones;
    }

    private void configDateAndTime() {
        int auto_zone = mSystemProxy.getInt(SystemProxy.TABLE_GLOBAL,
                Settings.Global.AUTO_TIME, 1);
        mTimeZone.setEnabled(auto_zone > 0 ? false : true);
        mAutoTimeZone.setChecked(auto_zone > 0 ? true : false);
        Logger.d(TAG, "configDateAndTime: auto_zone = " + auto_zone);
        mAutoTimeZone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = ((Switch) v).isChecked();
                mSystemProxy.putInt(SystemProxy.TABLE_GLOBAL,
                        Settings.Global.AUTO_TIME_ZONE, isChecked ? 1 : 0);
                mSystemProxy.putInt(SystemProxy.TABLE_GLOBAL,
                        Settings.Global.AUTO_TIME, isChecked ? 1 : 0);
                mTimeZone.setEnabled(!isChecked);
            }
        });

        mSwitch_12_24.setChecked(DateFormat.is24HourFormat(getContext()));
        mSwitch_12_24.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = ((Switch) v).isChecked();
                mSystemProxy.putString(SystemProxy.TABLE_SYSTEM,
                        Settings.System.TIME_12_24, isChecked ? "24" : "12");
                mSystemProxy.broadcastHourFormat(isChecked);
            }
        });
        final String[][] timezones = getAllTimezones();
        TimeZone locale = TimeZone.getDefault();
        String localeID = locale.getID();
        String[] localeIDs = timezones[0];
        int index = 64;
        for (int i = 0; i < localeIDs.length; i++) {
            if (localeID.endsWith(localeIDs[i])) {
                index = i;
            }
        }
        mTimeZone.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, timezones[1]));
        mTimeZone.setSelection(index);
        Logger.d(TAG, "configDateAndTime:  locale = " + locale.getDisplayName() + ", localeID" + localeID);

        mTimeZone.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Logger.d(TAG, "onItemSelected: position = " + position);
                setTimeZone(timezones[0][position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });
    }

    public void setTimeZone(String localeID) {
        final AlarmManager alarm = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        alarm.setTimeZone(localeID);
    }
}
