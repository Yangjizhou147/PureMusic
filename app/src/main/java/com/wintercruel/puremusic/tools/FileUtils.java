package com.wintercruel.puremusic.tools;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import com.wintercruel.puremusic.MusicPlayNext;

import java.io.File;

public class FileUtils {

    // 获取文件路径
    public static String getPathFromUri(Context context, Uri uri) {
        // 检查uri是否为null
        if (uri == null) {
            Log.e("FileUtils", "Uri is null");
//            Toast.makeText(context.getApplicationContext(), "Uri is null", Toast.LENGTH_LONG).show();
            return null; // 或者返回一个默认路径
        }

        String filePath = null;
        String[] projection = {MediaStore.Audio.Media.DATA};

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(columnIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 如果从MediaStore未能获取路径，尝试从文件Uri中解析
        if (filePath == null) {
            if (uri.getPath() == null) {
                Log.d("歌词文件：", "未找到");
                Toast.makeText(context.getApplicationContext(), "歌词文件未找到", Toast.LENGTH_LONG).show();
            }
            filePath = uri.getPath();
        }

        return filePath;
    }

}