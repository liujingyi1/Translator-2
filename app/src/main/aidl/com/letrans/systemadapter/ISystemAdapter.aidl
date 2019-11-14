package com.letrans.systemadapter;

interface ISystemAdapter {

int getIntProperties(String key, int def);
long getLongProperties(String key, long def);
boolean getBooleanProperties(String key, boolean def);
void setStringProperties(String key, String value);
String getStringProperties(String key, String def);

void refreshStorage();
long getTotalRamMemory();
long getFreeRamMemory();
long getExternalTotalStorageMemory();
long getExternalFreeStorageMemory();
long getInternalTotalStorageMemory();
long getInternalFreeStorageMemory();
long[] getExternalStorageMemory();
long[] getInternalStorageMemory();

void eraseDevice();

String getWifiMacAddress();

//To call SettingsProvider
int getInt(String tableName, String name, int def);
boolean putInt(String tableName, String name, int value);
float getFloat(String tableName, String name, float def);
boolean putFloat(String tableName, String name, float value);
long getLong(String tableName, String name, long def);
boolean putLong(String tableName, String name, long value);
String getString(String tableName, String name);
boolean putString(String tableName, String name, String value);

//To change the system language
void updateLocales(String language, String country);
void updateFontScale(float values);
int getFontSizeIndex(in float[] indices);
void setBrightness(int brightness);
void broadcastHourFormat(boolean is24HourFormat);

String getCSN();

//Time setting
void setDate(int year, int month, int day);
void setTime(int hourOfDay, int minute);

void setScreenTimeout(long millis, int type);
long getScreenTimeout(int type);

void reboot();
}