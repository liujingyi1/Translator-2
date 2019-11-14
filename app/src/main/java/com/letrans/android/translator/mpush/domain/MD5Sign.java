package com.letrans.android.translator.mpush.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MD5Sign {
    private static String secret;
    public static String md5key = "eyJzdWIiOiJhc2RmcXdlMTIzIiwiaWF0IjoxNTM1MzQ5NDQ2LCJleHAiOjE1NDA1MzM0NDZ9";

    public void setSecret(String secret) {
        MD5Sign.secret = secret;
    }

    /**
     * 方法描述:将字符串MD5加码 生成32位md5码
     *
     * @inStr
     */
    public static String md5(String inStr) {
        try {
            byte[] bytes = inStr.getBytes("utf-8");
            return parseByte2HexStr(bytes);
        } catch (Exception e) {
            throw new RuntimeException("MD5签名过程中出现错误");
        }
    }

    /**
     * 将二进制转换成16进制
     * @param buf
     * @return
     */
    private static String parseByte2HexStr(byte buf[]) {
        if (null == buf) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }



    /**
     * 方法描述:签名字符串
     *
     *
     * @param params
     *            需要签名的参数
     * @param appSecret
     *            签名密钥
     * @return
     */
    public static String sign(HashMap<String, String> params, String appSecret) {
        StringBuilder valueSb = new StringBuilder();
        params.put("appSecret", appSecret);
        // 将参数以参数名的字典升序排序
        Map<String, String> sortParams = new TreeMap<>(params);
        Set<Map.Entry<String, String>> entrys = sortParams.entrySet();
        // 遍历排序的字典,并拼接value1+value2......格式
        for (Map.Entry<String, String> entry : entrys) {
            valueSb.append(entry.getValue());
        }
        params.remove("appSecret");
        return md5(valueSb.toString());
    }
}
