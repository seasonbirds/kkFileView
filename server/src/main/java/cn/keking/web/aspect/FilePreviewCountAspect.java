package cn.keking.web.aspect;

import cn.keking.service.cache.CacheService;
import cn.keking.utils.WebUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件预览次数统计切面
 */
@Aspect
@Component
public class FilePreviewCountAspect {

    private final Logger logger = LoggerFactory.getLogger(FilePreviewCountAspect.class);

    private final CacheService fileRankCacheService;

    public FilePreviewCountAspect(@Qualifier("fileRankCacheService") CacheService fileRankCacheService) {
        this.fileRankCacheService = fileRankCacheService;
    }

    @Pointcut("execution(* cn.keking.web.controller.OnlinePreviewController.onlinePreview(..))")
    public void filePreviewPointcut() {}

    @AfterReturning(pointcut = "filePreviewPointcut()", returning = "result")
    public void afterFilePreview(JoinPoint joinPoint, Object result) {
        try {
            // 获取方法参数
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                String url = (String) args[0];
                // 解码URL
                String fileUrl = WebUtils.decodeUrl(url);
                // 从URL中提取文件名
                String fileName = extractFileNameFromUrl(fileUrl);
                if (fileName != null && !fileName.isEmpty()) {
                    // 增加文件预览次数
                    fileRankCacheService.incrementFilePreviewCount(fileName);
                    logger.info("文件预览次数统计：{}，当前次数：{}", fileName, fileRankCacheService.getFilePreviewCount(fileName));
                }
            }
        } catch (Exception e) {
            logger.error("文件预览次数统计失败", e);
        }
    }

    /**
     * 从URL中提取文件名
     * @param url URL
     * @return 文件名
     */
    private String extractFileNameFromUrl(String url) {
        try {
            // 解码URL
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
            // 提取文件名
            Pattern pattern = Pattern.compile("[^/\\]+\\.[^/\\]+$");
            Matcher matcher = pattern.matcher(decodedUrl);
            if (matcher.find()) {
                return matcher.group();
            }
        } catch (Exception e) {
            logger.error("从URL中提取文件名失败", e);
        }
        return null;
    }
}
