package cc.riskswap.trader.admin.config;

import cn.hutool.core.util.StrUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class DatabaseScriptSupport {

    private DatabaseScriptSupport() {
    }

    public static List<String> splitStatements(String content) {
        if (StrUtil.isBlank(content)) {
            return Collections.emptyList();
        }
        String noBlockComments = content.replaceAll("/\\*([\\s\\S]*?)\\*/", " ");
        String noLineComments = noBlockComments.replaceAll("(?m)^\\s*--.*$", " ");
        return StrUtil.split(noLineComments, ';').stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    public static boolean isSpringBootTestEnvironment(String testFlag) {
        return "true".equalsIgnoreCase(String.valueOf(testFlag));
    }
}
