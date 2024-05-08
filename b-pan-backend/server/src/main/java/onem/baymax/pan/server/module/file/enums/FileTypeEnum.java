package onem.baymax.pan.server.module.file.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import onem.baymax.pan.core.exception.BPanBusinessException;
import org.apache.commons.lang3.StringUtils;

/**
 * 文件类型枚举
 *
 * @author hujiabin wrote in 2024/5/8 09:16
 */
@AllArgsConstructor
@Getter
public enum FileTypeEnum {

    /**
     * 文件类型（1 普通文件 2 压缩文件 3 excel 4 word 5 pdf 6 txt 7 图片 8 音频 9 视频 10 ppt 11 源码文件 12 csv）
     */
    NORMAL_FILE(1, "NORMAL_FILE", 1, fileSuffix -> true),
    ARCHIVE_FILE(2, "ARCHIVE_FILE", 2, fileSuffix -> {
        List<String> matchFileSuffixes = Arrays.asList(".rar", ".zip", ".cab", ".iso", ".jar", ".ace", ".7z", ".tar", ".gz", ".arj", ".lah",
                ".uue", ".bz2", ".z", ".war");
        return StringUtils.isNotBlank(fileSuffix) && matchFileSuffixes.contains(fileSuffix);
    }),
    EXCEL_FILE(3, "EXCEL", 3, fileSuffix -> {
        List<String> matchFileSuffixes = Arrays.asList(".xlsx", ".xls");
        return StringUtils.isNotBlank(fileSuffix) && matchFileSuffixes.contains(fileSuffix);
    }),
    WORD_FILE(4, "WORD_FILE", 4, fileSuffix -> {
        List<String> matchFileSuffixes = Arrays.asList(".docx", ".doc");
        return StringUtils.isNotBlank(fileSuffix) && matchFileSuffixes.contains(fileSuffix);
    }),
    PDF_FILE(5, "PDF_FILE", 5, fileSuffix -> {
        List<String> matchFileSuffixes = Collections.singletonList(".pdf");
        return StringUtils.isNotBlank(fileSuffix) && matchFileSuffixes.contains(fileSuffix);
    }),
    TXT_FILE(6, "TXT_FILE", 6, fileSuffix -> {
        List<String> matchFileSuffixes = Collections.singletonList(".txt");
        return StringUtils.isNotBlank(fileSuffix) && matchFileSuffixes.contains(fileSuffix);
    }),
    IMAGE_FILE(7, "IMAGE_FILE", 7, fileSuffix -> {
        List<String> matchFileSuffixes = Arrays.asList(".bmp", ".gif", ".png", ".ico", ".eps", ".psd", ".tga", ".tiff", ".jpg", ".jpeg");
        return StringUtils.isNotBlank(fileSuffix) && matchFileSuffixes.contains(fileSuffix);
    }),
    AUDIO_FILE(8, "AUDIO_FILE", 8, fileSuffix -> {
        List<String> matchFileSuffixes = Arrays.asList(".mp3", ".mkv", ".mpg", ".rm", ".wma");
        return StringUtils.isNotBlank(fileSuffix) && matchFileSuffixes.contains(fileSuffix);
    }),
    VIDEO_FILE(9, "VIDEO_FILE", 9, fileSuffix -> {
        List<String> matchFileSuffixes = Arrays.asList(".avi", ".3gp", ".mp4", ".flv", ".rmvb", ".mov");
        return StringUtils.isNotBlank(fileSuffix) && matchFileSuffixes.contains(fileSuffix);
    }),
    POWER_POINT_FILE(10, "POWER_POINT_FILE", 10, fileSuffix -> {
        List<String> matchFileSuffixes = Arrays.asList(".ppt", ".pptx");
        return StringUtils.isNotBlank(fileSuffix) && matchFileSuffixes.contains(fileSuffix);
    }),
    SOURCE_CODE_FILE(11, "SOURCE_CODE_FILE", 11, fileSuffix -> {
        List<String> matchFileSuffixes = Arrays.asList(".java", ".obj", ".h", ".c", ".html", ".net", ".php", ".css", ".js", ".ftl", ".jsp",
                ".asp");
        return StringUtils.isNotBlank(fileSuffix) && matchFileSuffixes.contains(fileSuffix);
    }),
    CSV_FILE(12, "CSV_FILE", 12, fileSuffix -> {
        List<String> matchFileSuffixes = Collections.singletonList(".csv");
        return StringUtils.isNotBlank(fileSuffix) && matchFileSuffixes.contains(fileSuffix);
    });

    /**
     * 文件类型的code
     */
    private final Integer code;

    /**
     * 文件类型描述
     */
    private final String desc;

    /**
     * 排序字段
     * 按照降序顺序排序
     */
    private final Integer order;

    /**
     * 文件类型匹配器
     */
    private final Predicate<String> tester;

    /**
     * 根据文件名称的后缀获取对应的文件类型映射code
     *
     * @param fileSuffix 文件后缀
     * @return 类型
     */
    public static Integer getFileTypeCode(String fileSuffix) {
        Optional<FileTypeEnum> result = Arrays.stream(values())
                .sorted(Comparator.comparingInt(FileTypeEnum::getOrder).reversed())
                .filter(value -> value.getTester().test(fileSuffix))
                .findFirst();
        if (result.isPresent()) {
            return result.get().getCode();
        }
        throw new BPanBusinessException("获取文件类型失败");
    }

}

