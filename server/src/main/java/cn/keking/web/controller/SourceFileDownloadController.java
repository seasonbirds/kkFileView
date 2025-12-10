package cn.keking.web.controller;

import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FileHandlerService;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 源文件下载控制器
 * 
 * @author kl
 */
@Controller
public class SourceFileDownloadController {

    private static final Logger logger = LoggerFactory.getLogger(SourceFileDownloadController.class);

    private final FileHandlerService fileHandlerService;

    public SourceFileDownloadController(FileHandlerService fileHandlerService) {
        this.fileHandlerService = fileHandlerService;
    }

    /**
     * 下载源文件
     * 仅支持doc、docx格式文件的源文件下载
     * IP白名单验证已通过过滤器实现
     * 
     * @param url 文件URL
     * @param request HTTP请求
     * @param response HTTP响应
     */
    @GetMapping("/downloadSourceFile")
    public void downloadSourceFile(@RequestParam("url") String url,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        
        try {
            // 解码URL
            String decodedUrl = WebUtils.decodeUrl(url);
            
            // 获取文件属性
            FileAttribute fileAttribute = fileHandlerService.getFileAttribute(decodedUrl, request);
            
            // 检查文件类型是否为doc或docx
            String suffix = fileAttribute.getSuffix();
            if (!"doc".equalsIgnoreCase(suffix) && !"docx".equalsIgnoreCase(suffix)) {
                logger.warn("不支持的文件类型下载尝试: {}", suffix);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Only doc and docx files can be downloaded.");
                return;
            }

            // 下载文件到本地
            ReturnResponse<String> downloadResponse = DownloadUtils.downLoad(fileAttribute, null);
            if (downloadResponse.getCode() != 0) {
                logger.warn("文件下载失败: {}", downloadResponse.getMsg());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Failed to download file: " + downloadResponse.getMsg());
                return;
            }

            // 获取文件路径
            String fileName = fileAttribute.getName();
            String filePath = downloadResponse.getContent();
            
            // 如果源文件路径不存在，尝试从缓存或转换路径获取
            if (filePath == null || filePath.isEmpty()) {
                logger.warn("无法获取源文件路径: {}", decodedUrl);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("Source file not found.");
                return;
            }

            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                logger.warn("源文件不存在: {}", filePath);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("Source file not found.");
                return;
            }

            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setContentLength((int) file.length());
            
            // 处理文件名编码
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename*=UTF-8''" + encodedFileName);

            // 写入文件内容
            try (InputStream inputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    response.getOutputStream().write(buffer, 0, bytesRead);
                }
                response.getOutputStream().flush();
                logger.info("源文件下载成功: {} ({} bytes)", fileName, file.length());
            }

        } catch (Exception e) {
            logger.error("下载源文件失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Failed to download source file.");
            } catch (IOException ioException) {
                logger.error("写入错误响应失败", ioException);
            }
        }
    }


}