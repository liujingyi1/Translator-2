package com.letrans.android.translator.lyy;

import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class LyyUtils {
    public static byte[] compressStringToGZip(String unGZipString) {
        byte[] result = null;
        if (TextUtils.isEmpty(unGZipString)) {
            result = null;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(baos);
            gzip.write(unGZipString.getBytes());
            gzip.close();
            byte[] encode = baos.toByteArray();
            baos.flush();
            baos.close();
            result = encode;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String decompressGZipToString(byte[] gzipBytes) {
        String result = "";
        if (gzipBytes.length == 0) {
            result = "";
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(gzipBytes);
            GZIPInputStream gzip = new GZIPInputStream(in);
            byte[] buffer = new byte[4096];//byte[] buffer = new byte[BUFFERSIZE];
            int n = 0;
            while ((n = gzip.read(buffer, 0, buffer.length)) > 0) {
                out.write(buffer, 0, n);
            }
            gzip.close();
            in.close();
            out.close();
            result = out.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String Base64Encode(byte[] content) {
        byte[] tmpt = Base64.encode(content, Base64.DEFAULT);
        return new String(tmpt);
    }

    public static byte[] Base64Decode(String content) {
        return Base64.decode(content, Base64.DEFAULT);
    }
}
