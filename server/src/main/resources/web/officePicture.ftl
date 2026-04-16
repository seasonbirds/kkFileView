<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8" />
    <title>PDF图片预览</title>
    <#include "*/commonHeader.ftl">
    <script src="js/lazyload.js"></script>
    <style>
        body {
            background-color: #404040;
        }
        .container {
            width: 100%;
            height: 100%;
        }
        .img-area {
            text-align: center;
        }
        .my-photo {
            max-width: 98%;
            margin:0 auto;
            border-radius:3px;
            box-shadow:rgba(0,0,0,0.15) 0 0 8px;
            background:#FBFBFB;
            border:1px solid #ddd;
            margin:1px auto;
            padding:5px;
        }

    </style>
</head>
<body>
<div class="container">
    <#list imgUrls as img>
        <div class="img-area">
            <img class="my-photo" alt="loading"  data-src="${img}" src="images/loading.gif">
        </div>
    </#list>
</div>
<#if "false" == switchDisabled>
    <img src="images/pdf.svg" width="48" height="48" style="position: fixed; cursor: pointer; top: 40%; right: 48px; z-index: 999;" alt="使用PDF预览" title="使用PDF预览" onclick="changePreviewType('pdf')"/>
</#if>
<#if isOfficeFile?? && isOfficeFile>
    <div style="position: fixed; top: 20px; right: 48px; z-index: 999;">
        <button onclick="downloadSourceFile()" style="background-color: #1890ff; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; font-size: 14px;">
            下载源文件
        </button>
    </div>
</#if>
<script>
    window.onload = function () {
        /*初始化水印*/
        initWaterMark();
        checkImgs();
    };
    window.onscroll = throttle(checkImgs);
    function changePreviewType(previewType) {
        var url = window.location.href;
        if (url.indexOf("officePreviewType=image") !== -1) {
            url = url.replace("officePreviewType=image", "officePreviewType="+previewType);
        } else {
            url = url + "&officePreviewType="+previewType;
        }
        if ('allImages' === previewType) {
            window.open(url)
        } else {
            window.location.href = url;
        }
    }
    function downloadSourceFile() {
        var url = '${originalFileUrl}';
        var baseUrl = '${baseUrl}';
        if (!baseUrl.endsWith('/')) {
            baseUrl = baseUrl + '/';
        }
        window.location.href = baseUrl + 'downloadSourceFile?url=' + encodeURIComponent(url);
    }
</script>
</body>
</html>
