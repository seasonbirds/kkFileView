<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>最受欢迎文件排行榜 - kkFileView</title>
    <link rel="icon" href="./favicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="css/loading.css"/>
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="bootstrap-table/bootstrap-table.min.css"/>
    <link rel="stylesheet" href="css/theme.css"/>
    <script type="text/javascript" src="js/jquery-3.6.1.min.js"></script>
    <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="bootstrap-table/bootstrap-table.min.js"></script>
    <style>
        .rank-badge {
            display: inline-block;
            width: 28px;
            height: 28px;
            line-height: 28px;
            text-align: center;
            border-radius: 50%;
            font-weight: bold;
            color: #fff;
        }
        .rank-1 { background-color: #ffd700; }
        .rank-2 { background-color: #c0c0c0; }
        .rank-3 { background-color: #cd7f32; }
        .rank-other { background-color: #6c757d; }
        .panel-heading .btn-group {
            margin-left: 15px;
        }
    </style>
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
            <li class="active"><a href="./rank">文件排行榜</a></li>
        </ul>
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <!--  排行榜标题  -->
    <div class="page-header">
        <h1>最受欢迎文件排行榜</h1>
        <p>统计各文件的预览次数，预览次数越多排名越靠前</p>
    </div>

    <!--  排行榜表格  -->
    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title" style="display: inline-block;">文件预览排行榜</h3>
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-default btn-sm" onclick="loadRankData(10)">Top 10</button>
                <button type="button" class="btn btn-default btn-sm" onclick="loadRankData(50)">Top 50</button>
                <button type="button" class="btn btn-default btn-sm" onclick="loadRankData(100)">Top 100</button>
            </div>
            <span id="currentTop" style="margin-left: 10px; color: #666;">当前显示: Top 10</span>
        </div>
        <div class="panel-body">
            <table id="rankTable" data-pagination="true" class="table table-striped"></table>
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
    $(function () {
        // 初始化表格
        initRankTable();
        // 默认加载Top10
        loadRankData(10);
    });

    function initRankTable() {
        $('#rankTable').bootstrapTable({
            pageNumber: 1,
            pageSize: 10,
            pagination: true,
            pageList: [10, 20, 50, 100],
            search: true,
            columns: [{
                field: 'rank',
                title: '排序',
                align: 'center',
                width: 80,
                formatter: function(value, row, index) {
                    var badgeClass = 'rank-other';
                    if (value === 1) badgeClass = 'rank-1';
                    else if (value === 2) badgeClass = 'rank-2';
                    else if (value === 3) badgeClass = 'rank-3';
                    return '<span class="rank-badge ' + badgeClass + '">' + value + '</span>';
                }
            }, {
                field: 'fileName',
                title: '文件名称'
            }, {
                field: 'previewCount',
                title: '预览次数',
                align: 'center',
                width: 120,
                sortable: true
            }]
        });
    }

    function loadRankData(topN) {
        $('#currentTop').text('当前显示: Top ' + topN);
        $.ajax({
            url: '${baseUrl}api/rank/files?topN=' + topN,
            type: 'GET',
            dataType: 'json',
            success: function(data) {
                if (data.code === 0) {
                    $('#rankTable').bootstrapTable('load', data.content);
                } else {
                    alert('加载排行榜数据失败: ' + data.msg);
                }
            },
            error: function() {
                alert('加载排行榜数据失败，请稍后重试');
            }
        });
    }
</script>
</body>
</html>
