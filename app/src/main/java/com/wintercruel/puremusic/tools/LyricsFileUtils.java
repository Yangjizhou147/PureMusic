package com.wintercruel.puremusic.tools;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsFileUtils {
    // 正则表达式：匹配 [mm:ss.SSS] 格式的时间戳
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})(\\.\\d{3})?\\]");

    // 保存歌词到 .lrc 文件的方法
    public static File saveLyricsToLrcFile(Context context, String lyrics, String fileName) {
        File lrcFile = null;

        try {
            // 确保文件名以 .lrc 结尾
            if (!fileName.endsWith(".lrc")) {
                fileName += ".lrc";
            }

            // 获取应用的私有存储目录
            File directory = context.getFilesDir();  // 应用的私有目录
            lrcFile = new File(directory, fileName);

            // 处理歌词，将时间戳格式化为 [mm:ss.SS]
            String formattedLyrics = formatLyrics(lyrics);

            // 将歌词内容写入 .lrc 文件
            FileOutputStream fos = new FileOutputStream(lrcFile);
            fos.write(formattedLyrics.getBytes());
            fos.close();

            System.out.println("歌词文件保存成功: " + lrcFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("保存歌词文件时出错");
        }

        return lrcFile;
    }

    // 格式化歌词，调整时间戳格式
    private static String formatLyrics(String lyrics) {
        StringBuilder formattedLyrics = new StringBuilder();
        Matcher matcher = TIMESTAMP_PATTERN.matcher(lyrics);

        int lastAppendPosition = 0; // 上一次添加的位置

        while (matcher.find()) {
            // 将未匹配的部分添加到结果中
            formattedLyrics.append(lyrics, lastAppendPosition, matcher.start());

            String minutes = matcher.group(1); // 获取分钟
            String seconds = matcher.group(2); // 获取秒
            String milliseconds = matcher.group(3); // 获取毫秒

            // 处理毫秒部分
            String newMilliseconds;
            if (milliseconds != null) {
                newMilliseconds = milliseconds.substring(1, 3); // 获取前两位
            } else {
                newMilliseconds = "00"; // 如果没有毫秒，设为 00
            }

            // 构造新的时间戳格式
            String newTimestamp = String.format("[%s:%s.%s]", minutes, seconds, newMilliseconds);
            formattedLyrics.append(newTimestamp); // 添加新的时间戳

            lastAppendPosition = matcher.end(); // 更新最后添加的位置
        }

        // 添加未匹配的部分
        formattedLyrics.append(lyrics, lastAppendPosition, lyrics.length());

        return formattedLyrics.toString();
    }
}