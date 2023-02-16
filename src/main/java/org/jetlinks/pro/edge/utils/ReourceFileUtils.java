package org.jetlinks.pro.edge.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 资源文件工具类.
 * 用于初始化时，复制资源文件到服务器
 *
 * @author zhangji 2023/1/10
 */
@Slf4j
public class ReourceFileUtils {

    // 脚本文件缓存目录
    public static final String SHELL_TEMP_PATH        = System.getProperty("jetlinks.edge.sh.path", "./data/edge/sh");
    // 程序文件缓存目录
    public static final String ABSOLUTE_APP_TEMP_PATH = System.getProperty("jetlinks.edge.app.path", "/usr/local/app/edge");

    public static void copyShellFile(String path,
                                     String filename) {
        copyFile(path, filename, SHELL_TEMP_PATH, false);
    }

    public static void copyAppFile(String path,
                                   String filename) {
        copyFile(path, filename, ABSOLUTE_APP_TEMP_PATH, true);
    }

    /**
     * 将文件复制到缓存目录
     *
     * @param path       配置文件目录
     * @param filename   文件名
     * @param targetPath 缓存目录
     * @param deleteOld  是否覆盖旧文件
     * @return 缓存目录中的配置文件地址
     */
    private static void copyFile(String path,
                                 String filename,
                                 String targetPath,
                                 boolean deleteOld) {
        if (StringUtils.hasText(filename)) {
            ClassPathResource resource = new ClassPathResource(path + File.separator + filename);
            try (InputStream inputStream = resource.getInputStream()) {
                File tempFile = new File(targetPath + File.separator + filename);
                tempFile.getParentFile().mkdirs();
                if (tempFile.exists()) {
                    // 覆盖文件
                    if (deleteOld) {
                        tempFile.delete();
                    } else {
                        return;
                    }
                }
                log.info("复制文件{}到目录{}", filename, tempFile.getParentFile().getAbsolutePath());
                Files.copy(inputStream, tempFile.toPath());
            } catch (Exception e) {
                log.error("copy file failed! ", e);
            }
        }
    }
}
