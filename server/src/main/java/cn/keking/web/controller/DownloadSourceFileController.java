package cn.keking.web.controller;

import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FileHandlerService;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 下载源文件Controller
 *
 * 提供办公文件的源文件下载功能。
 *
 * @author keking
 */
@Controller
public class DownloadSourceFileController {

    private static final Logger logger = LoggerFactory.getLogger(DownloadSourceFileController.class);

    private static final Set<String> OFFICE_SUFFIXES = new HashSet<>(Arrays.asList(
            "doc", "docx", "docm", "dot", "dotx", "dotm",
            "xls", "xlsx", "xlsm", "csv", "xlt", "xltx", "xltm", "xlam",
            "ppt", "pptx",
            "vsd", "vsdx",
            "rtf",
            "wps", "et", "dps", "ett",
            "odt", "ods", "odp"
    ));

    private final FileHandlerService fileHandlerService;

    public DownloadSourceFileController(FileHandlerService fileHandlerService) {
        this.fileHandlerService = fileHandlerService;
    }

    /**
     * 下载源文件接口
     *
     * @param url      Base64编码的文件URL
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    @GetMapping("/downloadSourceFile")
    public void downloadSourceFile(String url, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileUrl = decodeUrl(url, response);
        if (fileUrl == null) {
            return;
        }

        logger.info("Download source file request, url: {}", fileUrl);

        FileAttribute fileAttribute = fileHandlerService.getFileAttribute(fileUrl, request);
        String fileName = fileAttribute.getName();
        String suffix = fileAttribute.getSuffix();

        if (!isOfficeFile(suffix)) {
            logger.warn("Not an office file, suffix: {}", suffix);
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "仅允许下载办公文件");
            return;
        }

        String localFilePath = null;
        boolean isLocalFile = false;

        try {
            if (fileUrl.toLowerCase().startsWith("file:")) {
                localFilePath = extractLocalFilePath(fileUrl);
                isLocalFile = true;
            } else {
                ReturnResponse<String> downloadResponse = DownloadUtils.downLoad(fileAttribute, fileName);
                if (downloadResponse.isFailure()) {
                    logger.error("Download source file failed: {}", downloadResponse.getMsg());
                    writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "下载文件失败：" + downloadResponse.getMsg());
                    return;
                }
                localFilePath = downloadResponse.getContent();
            }

            File file = new File(localFilePath);
            if (!file.exists()) {
                logger.error("Source file not exists: {}", localFilePath);
                writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "文件不存在");
                return;
            }

            try {
                setDownloadResponseHeaders(response, fileName, file.length());
                copyFileToResponse(file, response);
                logger.info("Download source file success: {}", fileName);
            } finally {
                if (!isLocalFile && localFilePath != null) {
                    try {
                        Files.deleteIfExists(new File(localFilePath).toPath());
                    } catch (Exception e) {
                        logger.warn("Failed to delete temp file: {}", localFilePath, e);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Download source file error", e);
            if (!response.isCommitted()) {
                writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
            }
        }
    }

    private String decodeUrl(String url, HttpServletResponse response) throws IOException {
        try {
            String fileUrl = WebUtils.decodeUrl(url);
            if (!StringUtils.hasText(fileUrl)) {
                writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "url参数为空");
                return null;
            }
            return fileUrl;
        } catch (Exception ex) {
            String errorMsg = String.format(OnlinePreviewController.BASE64_DECODE_ERROR_MSG, "url");
            logger.error(errorMsg, ex);
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, errorMsg);
            return null;
        }
    }

    private String extractLocalFilePath(String fileUrl) throws IOException {
        java.net.URL urlObj = new java.net.URL(fileUrl);
        String path = urlObj.getPath();
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
                path = path.substring(1);
            }
        }
        return path;
    }

    private void setDownloadResponseHeaders(HttpServletResponse response, String fileName, long fileSize) throws IOException {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFileName);
        response.setHeader("Content-Length", String.valueOf(fileSize));
    }

    private void copyFileToResponse(File file, HttpServletResponse response) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
    }

    private boolean isOfficeFile(String suffix) {
        return suffix != null && OFFICE_SUFFIXES.contains(suffix.toLowerCase());
    }
}
