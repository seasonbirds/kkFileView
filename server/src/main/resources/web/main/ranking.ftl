<!DOCTYPE html>

<html lang="zh-CN">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>文件预览排行榜 - kkFileView</title>
    <link rel="icon" href="./favicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="bootstrap-table/bootstrap-table.min.css"/>
    <link rel="stylesheet" href="css/theme.css"/>
    <script type="text/javascript" src="js/jquery-3.6.1.min.js"></script>
    <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="bootstrap-table/bootstrap-table.min.js"></script>
</head>
<body>

<nav class="navbar navbar-inverse navbar-fixed-top">
    <div class="container">
        <div class="navbar-header">
            <a class="navbar-brand" href="https://kkview.cn" target='_blank'>kkFileView</a>
        </div>
        <ul class="nav navbar-nav">
            <li><a href="./index">首页</a></li>
            <li><a href="./integrated">接入说明</a></li>
            <li><a href="./record">版本发布记录</a></li>
            <li><a href="./sponsor">赞助开源</a></li>
            <li class="active"><a href="./ranking">文件预览排行榜</a></li>
        </ul>
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <div class="page-header">
        <h1>最受欢迎文件排行榜</h1>
        统计各文件的预览次数，预览次数越多越受欢迎。
    </div>

    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">Top ${topN} 排行榜</h3>
            <div class="btn-group" style="float: right; margin-top: -28px; margin-right: 10px;">
                <button type="button" class="btn btn-sm ${(topN == 10)?then('btn-primary', 'btn-default')}" onclick="changeTopN(10)">Top 10</button>
                <button type="button" class="btn btn-sm ${(topN == 50)?then('btn-primary', 'btn-default')}" onclick="changeTopN(50)">Top 50</button>
                <button type="button" class="btn btn-sm ${(topN == 100)?then('btn-primary', 'btn-default')}" onclick="changeTopN(100)">Top 100</button>
            </div>
        </div>
        <div class="panel-body">
            <table class="table table-striped table-bordered">
                <thead>
                    <tr>
                        <th>排名</th>
                        <th>文件名</th>
                        <th>预览次数</th>
                    </tr>
                </thead>
                <tbody>
                    <#list rankingList as item>
                    <tr>
                        <td>
                            <#if item_rank == 1>
                                <span class="label label-danger" style="font-size: 14px;">1</span>
                            <#elseif item_rank == 2>
                                <span class="label label-warning" style="font-size: 14px;">2</span>
                            <#elseif item_rank == 3>
                                <span class="label label-info" style="font-size: 14px;">3</span>
                            <#else>
                                <span class="label label-default" style="font-size: 14px;">${item_rank}</span>
                            </#if>
                        </td>
                        <td>${item.fileName}</td>
                        <td><span class="badge" style="background-color: #0d6efd;">${item.previewCount}</span></td>
                    </tr>
                    <#else>
                    <tr>
                        <td colspan="3" class="text-center">暂无数据</td>
                    </tr>
                    </#list>
                </tbody>
            </table>
        </div>
    </div>
</div>

<#if beian?? && beian != "default">
    <div style="display: grid; place-items: center;">
        <div>
            <a target="_blank" href="https://beian.miit.gov.cn/">${beian}</a>
        </div>
    </div>
</#if>

<script>
    function changeTopN(topN) {
        window.location.href = '${baseUrl}ranking?topN=' + topN;
    }
</script>

</body>
</html>
