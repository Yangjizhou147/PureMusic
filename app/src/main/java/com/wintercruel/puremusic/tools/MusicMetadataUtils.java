package com.wintercruel.puremusic.tools;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;

public class MusicMetadataUtils {

    public static String getLyricsFromFile(String filePath) {
        try {
            // 读取音频文件
            AudioFile audioFile = AudioFileIO.read(new File(filePath));
            Tag tag = audioFile.getTag();

            // 从标签中获取歌词
            if (tag != null) {
                return tag.getFirst(FieldKey.LYRICS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}