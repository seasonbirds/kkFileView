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
 * 支持的协议：HTTP/HTTPS、本地文件协议(file:)
 * 支持的办公文件类型：doc/docx、xls/xlsx、ppt/pptx等
 *
 * @author keking
 */
@Controller
public class DownloadSourceFileController {

    private static final Logger logger = LoggerFactory.getLogger(DownloadSourceFileController.class);

    /**
     * 支持的办公文件后缀集合
     *
     * 包含以下类型：
     * - Word文档：doc、docx、docm、dot、dotx、dotm
     * - Excel文档：xls、xlsx、xlsm、csv、xlt、xltx、xltm、xlam
     * - PPT文档：ppt、pptx
     * - Visio文档：vsd、vsdx
     * - 其他：rtf、wps、et、dps、ett、odt、ods、odp
     */
    private static final Set<String> OFFICE_SUFFIXES = new HashSet<>(Arrays.asList(
            "doc", "docx", "docm", "dot", "dotx", "dotm",
            "xls", "xlsx", "xlsm", "csv", "xlt", "xltx", "xltm", "xlam",
            "ppt", "pptx",
            "vsd", "vsdx",
            "rtf",
            "wps", "et", "dps", "ett",
            "odt", "ods", "odp"
    ));

    /**
     * 文件处理服务，用于获取文件属性
     */
    private final FileHandlerService fileHandlerService;

    /**
     * 构造函数注入依赖
     *
     * @param fileHandlerService 文件处理服务
     */
    public DownloadSourceFileController(FileHandlerService fileHandlerService) {
        this.fileHandlerService = fileHandlerService;
    }

    /**
     * 下载源文件接口
     *
     * 主要流程：
     * 1. 解码Base64编码的URL参数
     * 2. 验证URL有效性
     * 3. 获取文件属性并判断是否为办公文件
     * 4. 根据文件协议类型获取本地文件路径：
     *    - file协议：直接解析本地路径
     *    - HTTP/HTTPS协议：下载远程文件到本地临时目录
     * 5. 验证文件是否存在
     * 6. 设置下载响应头并将文件内容写入响应流
     * 7. 清理临时文件（非本地文件协议时）
     *
     * @param url      Base64编码的文件URL
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    @GetMapping("/downloadSourceFile")
    public void downloadSourceFile(String url, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 步骤1：解码URL参数，解码失败直接返回错误响应
        String fileUrl = decodeUrl(url, response);
        if (fileUrl == null) {
            return;
        }

        logger.info("Download source file request, url: {}", fileUrl);

        // 步骤2：获取文件属性，判断是否为办公文件
        FileAttribute fileAttribute = fileHandlerService.getFileAttribute(fileUrl, request);
        String fileName = fileAttribute.getName();
        String suffix = fileAttribute.getSuffix();

        // 步骤3：仅允许下载办公文件
        if (!isOfficeFile(suffix)) {
            logger.warn("Not an office file, suffix: {}", suffix);
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "仅允许下载办公文件");
            return;
        }

        String localFilePath = null;
        boolean isLocalFile = false;

        try {
            // 步骤4：根据协议类型获取本地文件路径
            if (fileUrl.toLowerCase().startsWith("file:")) {
                // 本地文件协议，直接解析路径
                localFilePath = extractLocalFilePath(fileUrl);
                isLocalFile = true;
            } else {
                // HTTP/HTTPS协议，下载远程文件到本地临时目录
                ReturnResponse<String> downloadResponse = DownloadUtils.downLoad(fileAttribute, fileName);
                if (downloadResponse.isFailure()) {
                    logger.error("Download source file failed: {}", downloadResponse.getMsg());
                    writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "下载文件失败：" + downloadResponse.getMsg());
                    return;
                }
                localFilePath = downloadResponse.getContent();
            }

            // 步骤5：验证文件是否存在
            File file = new File(localFilePath);
            if (!file.exists()) {
                logger.error("Source file not exists: {}", localFilePath);
                writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "文件不存在");
                return;
            }

            // 步骤6：执行文件下载
            try {
                setDownloadResponseHeaders(response, fileName, file.length());
                copyFileToResponse(file, response);
                logger.info("Download source file success: {}", fileName);
            } finally {
                // 步骤7：清理临时文件（非本地文件协议时需要删除下载的临时文件）
                if (!isLocalFile && localFilePath != null) {
                    try {
                        Files.deleteIfExists(new File(localFilePath).toPath());
                    } catch (Exception e) {
                        logger.warn("Failed to delete temp file: {}", localFilePath, e);
                    }
                }
            }

        } catch (Exception e) {
            // 全局异常处理，确保响应未提交时返回错误信息
            logger.error("Download source file error", e);
            if (!response.isCommitted()) {
                writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
            }
        }
    }

    /**
     * 解码Base64编码的URL参数
     *
     * @param url      Base64编码的URL
     * @param response HTTP响应对象（用于解码失败时返回错误）
     * @return 解码后的URL字符串，解码失败返回null
     * @throws IOException IO异常
     */
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

    /**
     * 从file协议URL中提取本地文件路径
     *
     * 处理Windows和Unix/Linux平台的路径差异：
     * - Windows：file:/C:/path/to/file -> C:/path/to/file
     * - Unix/Linux：file:/path/to/file -> /path/to/file
     *
     * @param fileUrl file协议的URL
     * @return 本地文件绝对路径
     * @throws IOException URL解析异常
     */
    private String extractLocalFilePath(String fileUrl) throws IOException {
        java.net.URL urlObj = new java.net.URL(fileUrl);
        String path = urlObj.getPath();
        // Windows平台特殊处理：移除路径前导的斜杠
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
                path = path.substring(1);
            }
        }
        return path;
    }

    /**
     * 设置文件下载的HTTP响应头
     *
     * 设置以下头信息：
     * - Content-Type：application/octet-stream（二进制流）
     * - Content-Disposition：attachment，指定下载文件名（支持中文文件名）
     * - Content-Length：文件大小
     *
     * @param response HTTP响应对象
     * @param fileName 文件名
     * @param fileSize 文件大小（字节）
     * @throws IOException IO异常
     */
    private void setDownloadResponseHeaders(HttpServletResponse response, String fileName, long fileSize) throws IOException {
        // URL编码文件名，处理中文文件名，将+替换为%20（空格的标准编码）
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/octet-stream");
        // 使用RFC 5987规范的filename*格式，支持多种语言
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFileName);
        response.setHeader("Content-Length", String.valueOf(fileSize));
    }

    /**
     * 将文件内容复制到HTTP响应输出流
     *
     * 使用缓冲流提高传输效率，使用try-with-resources自动关闭流
     *
     * @param file     源文件
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    private void copyFileToResponse(File file, HttpServletResponse response) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            // 4KB缓冲区，平衡内存使用和IO效率
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            // 刷新输出流，确保所有数据都写入响应
            os.flush();
        }
    }

    /**
     * 输出JSON格式的错误响应
     *
     * @param response HTTP响应对象
     * @param status   HTTP状态码
     * @param message  错误信息
     * @throws IOException IO异常
     */
    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        // 简单的JSON字符串拼接，避免额外依赖
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
    }

    /**
     * 判断文件后缀是否为办公文件类型
     *
     * @param suffix 文件后缀（不含点号）
     * @return 是否为办公文件
     */
    private boolean isOfficeFile(String suffix) {
        return suffix != null && OFFICE_SUFFIXES.contains(suffix.toLowerCase());
    }
}
