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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 下载源文件Controller
 * <p>
 * 提供办公文件（doc/docx、xls/xlsx、ppt/pptx等）的源文件下载功能。
 *
 * @author keking
 */
@Controller
public class DownloadSourceFileController {

    private static final Logger logger = LoggerFactory.getLogger(DownloadSourceFileController.class);

    private final FileHandlerService fileHandlerService;

    public DownloadSourceFileController(FileHandlerService fileHandlerService) {
        this.fileHandlerService = fileHandlerService;
    }

    /**
     * 下载源文件接口
     * <p>
     * 接收Base64编码的文件URL，下载转换前的源文件。
     * 仅允许下载办公类型文件。
     *
     * @param url      Base64编码的文件URL
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    @GetMapping("/downloadSourceFile")
    public void downloadSourceFile(String url, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileUrl;
        try {
            fileUrl = WebUtils.decodeUrl(url);
        } catch (Exception ex) {
            String errorMsg = String.format(OnlinePreviewController.BASE64_DECODE_ERROR_MSG, "url");
            logger.error(errorMsg, ex);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":400,\"message\":\"" + errorMsg + "\"}");
            return;
        }

        if (!StringUtils.hasText(fileUrl)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":400,\"message\":\"url参数为空\"}");
            return;
        }

        logger.info("Download source file request, url: {}", fileUrl);

        FileAttribute fileAttribute = fileHandlerService.getFileAttribute(fileUrl, request);
        String fileName = fileAttribute.getName();
        String suffix = fileAttribute.getSuffix();

        if (!isOfficeFile(suffix)) {
            logger.warn("Not an office file, suffix: {}", suffix);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"仅允许下载办公文件\"}");
            return;
        }

        String localFilePath = null;
        boolean isLocalFile = false;

        try {
            if (fileUrl.toLowerCase().startsWith("file:")) {
                java.net.URL urlObj = new java.net.URL(fileUrl);
                String path = urlObj.getPath();
                if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                    if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
                        path = path.substring(1);
                    }
                }
                localFilePath = path;
                isLocalFile = true;
            } else {
                ReturnResponse<String> downloadResponse = DownloadUtils.downLoad(fileAttribute, fileName);
                if (downloadResponse.isFailure()) {
                    logger.error("Download source file failed: {}", downloadResponse.getMsg());
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":500,\"message\":\"下载文件失败：" + downloadResponse.getMsg() + "\"}");
                    return;
                }
                localFilePath = downloadResponse.getContent();
            }

            File file = new File(localFilePath);
            if (!file.exists()) {
                logger.error("Source file not exists: {}", localFilePath);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":404,\"message\":\"文件不存在\"}");
                return;
            }

            try {
                String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFileName);
                response.setHeader("Content-Length", String.valueOf(file.length()));

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        response.getOutputStream().write(buffer, 0, bytesRead);
                    }
                    response.getOutputStream().flush();
                }

                logger.info("Download source file success: {}", fileName);
            } catch (IOException e) {
                logger.error("Stream download failed", e);
                throw e;
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
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"服务器内部错误\"}");
            }
        }
    }

    /**
     * 判断是否为办公文件
     * <p>
     * 支持的办公文件类型包括：
     * - Word文档：doc、docx、docm、dot、dotx、dotm、rtf
     * - Excel文档：xls、xlsx、xlsm、csv、xlt、xltx、xltm、xlam
     * - PPT文档：ppt、pptx
     * - Visio文档：vsd、vsdx
     * - WPS文档：wps、et、dps、ett
     * - OpenOffice文档：odt、ods、odp
     *
     * @param suffix 文件后缀（不含点号）
     * @return 是否为办公文件
     */
    private boolean isOfficeFile(String suffix) {
        if (suffix == null) {
            return false;
        }
        String lowerSuffix = suffix.toLowerCase();
        return lowerSuffix.equals("doc") || lowerSuffix.equals("docx") || lowerSuffix.equals("docm") ||
               lowerSuffix.equals("xls") || lowerSuffix.equals("xlsx") || lowerSuffix.equals("xlsm") ||
               lowerSuffix.equals("csv") || lowerSuffix.equals("ppt") || lowerSuffix.equals("pptx") ||
               lowerSuffix.equals("vsd") || lowerSuffix.equals("vsdx") || lowerSuffix.equals("rtf") ||
               lowerSuffix.equals("wps") || lowerSuffix.equals("et") || lowerSuffix.equals("dps") ||
               lowerSuffix.equals("odt") || lowerSuffix.equals("ods") || lowerSuffix.equals("odp") ||
               lowerSuffix.equals("dot") || lowerSuffix.equals("dotx") || lowerSuffix.equals("dotm") ||
               lowerSuffix.equals("xlt") || lowerSuffix.equals("xltx") || lowerSuffix.equals("xltm") ||
               lowerSuffix.equals("ett") || lowerSuffix.equals("xlam"));
    }
}
