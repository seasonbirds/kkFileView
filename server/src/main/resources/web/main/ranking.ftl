<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>最受欢迎文件排行榜</title>
    <link rel="icon" href="./favicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="css/loading.css"/>
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="bootstrap-table/bootstrap-table.min.css"/>
    <link rel="stylesheet" href="css/theme.css"/>
    <script type="text/javascript" src="js/jquery-3.6.1.min.js"></script>
    <script type="text/javascript" src="js/jquery.form.min.js"></script>
    <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="bootstrap-table/bootstrap-table.min.js"></script>
</head>

<body>
<!-- Fixed navbar -->
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
            <li class="active"><a href="./ranking">排行榜</a></li>
        </ul>
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <div class="page-header">
        <h1>最受欢迎文件排行榜</h1>
        <div class="btn-group" role="group" aria-label="Top N选择">
            <a href="./ranking?topN=10" class="btn btn-default <#if topN == 10>active</#if>">Top 10</a>
            <a href="./ranking?topN=50" class="btn btn-default <#if topN == 50>active</#if>">Top 50</a>
            <a href="./ranking?topN=100" class="btn btn-default <#if topN == 100>active</#if>">Top 100</a>
        </div>
    </div>

    <table id="rankingTable" class="table table-striped table-bordered" style="width:100%">
        <thead>
        <tr>
            <th>排名</th>
            <th>文件名</th>
            <th>预览次数</th>
        </tr>
        </thead>
        <tbody>
        <#list topFiles as file>
        <tr>
            <td>${file_index + 1}</td>
            <td>${file.fileName}</td>
            <td>${file.previewCount}</td>
        </tr>
        </#list>
        <#if topFiles?size == 0>
        <tr>
            <td colspan="3" class="text-center">暂无数据</td>
        </tr>
        </#if>
        </tbody>
    </table>
</div>

<script type="text/javascript">
    $(function() {
        $('#rankingTable').bootstrapTable({
            pagination: true,
            pageSize: 10,
            pageList: [10, 25, 50, 100],
            search: true,
            showColumns: true,
            showToggle: true,
            showExport: true,
            exportTypes: ['csv', 'txt', 'xml', 'json', 'sql', 'excel'],
            minimumCountColumns: 2,
            clickToSelect: true,
            detailView: false,
            detailFormatter: function(index, row) {
                var html = [];
                $.each(row, function(key, value) {
                    html.push('<p><b>' + key + ':</b> ' + value + '</p>');
                });
                return html.join('');
            },
            columns: [
                {field: 'rank', title: '排名', sortable: true},
                {field: 'fileName', title: '文件名', sortable: true},
                {field: 'count', title: '预览次数', sortable: true}
            ]
        });
    });
</script>
</body>
</html>