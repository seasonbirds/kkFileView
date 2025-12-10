<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, user-scalable=yes, initial-scale=1.0">
    <title>PDF预览</title>
    <#include "*/commonHeader.ftl">
    <script src="js/base64.min.js" type="text/javascript"></script>
</head>

<body>
<#if pdfUrl?contains("http://") || pdfUrl?contains("https://")>
    <#assign finalUrl="${pdfUrl}">
<#else>
    <#assign finalUrl="${baseUrl}${pdfUrl}">
</#if>
<iframe src="" width="100%" frameborder="0"></iframe>
<#if "false" == switchDisabled>
    <img src="images/jpg.svg" width="48" height="48" style="position: fixed; cursor: pointer; top: 40%; right: 48px; z-index: 999;" alt="使用图片预览" title="使用图片预览" onclick="goForImage()"/>
</#if>
<!-- 下载源文件按钮，默认隐藏 -->
<img id="downloadSourceBtn" src="images/download.svg" width="48" height="48" style="position: fixed; cursor: pointer; top: 50%; right: 48px; z-index: 999; display: none;" alt="下载源文件" title="下载源文件" onclick="downloadSourceFile()"/>
</body>

<script type="text/javascript">
    var url = '${finalUrl}';
    var baseUrl = '${baseUrl}'.endsWith('/') ? '${baseUrl}' : '${baseUrl}' + '/';
    if (!url.startsWith(baseUrl)) {
        url = baseUrl + 'getCorsFile?urlPath=' + encodeURIComponent(Base64.encode(url));
    }
    document.getElementsByTagName('iframe')[0].src = "${baseUrl}pdfjs/web/viewer.html?file=" + encodeURIComponent(url) + "&disablepresentationmode=${pdfPresentationModeDisable}&disableopenfile=${pdfOpenFileDisable}&disableprint=${pdfPrintDisable}&disabledownload=${pdfDownloadDisable}&disablebookmark=${pdfBookmarkDisable}&disableediting=${pdfDisableEditing}";
    document.getElementsByTagName('iframe')[0].height = document.documentElement.clientHeight - 10;
    /**
     * 页面变化调整高度
     */
    window.onresize = function () {
        var fm = document.getElementsByTagName("iframe")[0];
        fm.height = window.document.documentElement.clientHeight - 10;
    }

    function goForImage() {
        var url = window.location.href
        
        if (url.indexOf("tifPreviewType=pdf") != -1) {
            url = url.replace("tifPreviewType=pdf", "tifPreviewType=jpg");
        } else {
            url = url + "&tifPreviewType=jpg";
        }

        if (url.indexOf("officePreviewType=pdf") != -1) {
            url = url.replace("officePreviewType=pdf", "officePreviewType=image");
        } else {
            url = url + "&officePreviewType=image";
        }
        window.location.href = url;
    }

    /*初始化水印*/
    window.onload = function () {
        initWaterMark();
        // 检查是否为doc或docx文件，如果是则显示下载按钮
        checkAndShowDownloadButton();
    }
    
    /**
     * 检查文件类型并显示下载按钮
     */
    function checkAndShowDownloadButton() {
        var url = window.location.href;
        // 从URL中提取原始文件信息
        var fileInfo = ${file};
        if (fileInfo && fileInfo.suffix) {
            var suffix = fileInfo.suffix.toLowerCase();
            if (suffix === 'doc' || suffix === 'docx') {
                document.getElementById('downloadSourceBtn').style.display = 'block';
            }
        }
    }
    
    /**
     * 下载源文件
     */
    function downloadSourceFile() {
        // 获取当前预览的文件URL
        var urlParams = new URLSearchParams(window.location.search);
        var url = urlParams.get('url');
        if (url) {
            // 跳转到下载接口
            window.location.href = '${baseUrl}downloadSourceFile?url=' + url;
        }
    }
</script>
</html>
