package org.fastlight.aop.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Charsets;
import org.apache.commons.lang3.StringUtils;

/**
 * @author ychost
 * @date 2021-04-13
 **/
public class BannerLogger {
    /**
     * @formatter:off
     * 当 BANNER 文件没找到的时候，用这个默认的
     * @see <a href="https://www.bootschool.net/ascii">Banner 生成器</a>
     */
    private static final String DEFAULT_BANNER =
          " ████████                     ██       ██                     \n"
        + "░██░░░░░                     ░██      ████             ██████ \n"
        + "░██        ██████    ██████ ██████   ██░░██    ██████ ░██░░░██\n"
        + "░███████  ░░░░░░██  ██░░░░ ░░░██░   ██  ░░██  ██░░░░██░██  ░██\n"
        + "░██░░░░    ███████ ░░█████   ░██   ██████████░██   ░██░██████ \n"
        + "░██       ██░░░░██  ░░░░░██  ░██  ░██░░░░░░██░██   ░██░██░░░  \n"
        + "░██      ░░████████ ██████   ░░██ ░██     ░██░░██████ ░██     \n"
        + "░░        ░░░░░░░░ ░░░░░░     ░░  ░░      ░░  ░░░░░░  ░░      ${version}";

    /**
     * @formatter:on
     * 默认的 BANNER 文件
     */
    private static final String BANNER_FILE = "META-INF/aspect/banner.txt";

    /**
     * 生成的 Banner  缓存
     */
    private static final String BANNER;

    // 初始化 Banner 数据
    static {
        URL url = BannerLogger.class.getClassLoader().getResource(BANNER_FILE);
        String banner = DEFAULT_BANNER;
        if (url != null) {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(url.openStream(), Charsets.UTF_8))) {
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                if (sb.length() > 0) {
                    banner = sb.toString();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        // 填充 version 变量
        String version = FastAspectProperties.getProperty("fast.version");
        BANNER = banner.replaceAll("\\$\\{version}","v"+ version);
    }

    /**
     * 打印标识
     */
    private static final AtomicBoolean IS_PRINTED_BANNER = new AtomicBoolean(false);

    /**
     * 输出 Banner 到控制台，仅会打印一次
     */
    public static void printBanner() {
        if (IS_PRINTED_BANNER.get()) {
            return;
        }
        System.out.println(BANNER);
        IS_PRINTED_BANNER.set(true);
    }

}
