<!DOCTYPE html>

<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>kkFileView最受欢迎文件排行榜</title>
    <link rel="icon" href="./favicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="bootstrap-table/bootstrap-table.min.css"/>
    <link rel="stylesheet" href="css/theme.css"/>
    <script type="text/javascript" src="js/jquery-3.6.1.min.js"></script>
    <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="bootstrap-table/bootstrap-table.min.js"></script>
    <style>
        .ranking-container {
            margin-top: 80px;
        }
        .ranking-header {
            margin-bottom: 20px;
        }
        .ranking-select {
            margin-bottom: 20px;
        }
        .rank-1 { color: #FFD700; font-weight: bold; }
        .rank-2 { color: #C0C0C0; font-weight: bold; }
        .rank-3 { color: #CD7F32; font-weight: bold; }
        .rank-other { color: #333; }
        .file-name {
            max-width: 400px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            display: inline-block;
        }
    </style>
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
            <li class="active"><a href="./ranking">排行榜</a></li>
        </ul>
    </div>
</nav>

<div class="container theme-showcase ranking-container" role="main">
    <div class="page-header ranking-header">
        <h1>最受欢迎文件排行榜</h1>
        <p class="text-muted">基于文件预览次数统计，实时展示最受欢迎的文件</p>
    </div>

    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">排行榜设置</h3>
        </div>
        <div class="panel-body">
            <div class="row ranking-select">
                <div class="col-md-3">
                    <label for="topNSelect">显示数量：</label>
                    <select id="topNSelect" class="form-control" style="display: inline-block; width: auto;">
                        <option value="10" selected>Top 10</option>
                        <option value="50">Top 50</option>
                        <option value="100">Top 100</option>
                    </select>
                </div>
                <div class="col-md-3">
                    <button id="refreshBtn" class="btn btn-primary">刷新数据</button>
                </div>
            </div>
        </div>
    </div>

    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">排行榜列表</h3>
        </div>
        <div class="panel-body">
            <table id="rankingTable" class="table table-striped table-hover"></table>
        </div>
    </div>
</div>

<script>
    $(function() {
        // 初始化表格
        initTable();

        // 下拉框改变事件
        $('#topNSelect').change(function() {
            refreshTable();
        });

        // 刷新按钮点击事件
        $('#refreshBtn').click(function() {
            refreshTable();
        });
    });

    function initTable() {
        $('#rankingTable').bootstrapTable({
            url: './api/ranking',
            method: 'get',
            queryParams: function(params) {
                return {
                    topN: $('#topNSelect').val()
                };
            },
            responseHandler: function(res) {
                if (res.code === 0) {
                    return res.data;
                } else {
                    return [];
                }
            },
            columns: [{
                field: 'rank',
                title: '排名',
                align: 'center',
                width: '80',
                formatter: function(value, row, index) {
                    var rankClass = 'rank-other';
                    if (value === 1) rankClass = 'rank-1';
                    else if (value === 2) rankClass = 'rank-2';
                    else if (value === 3) rankClass = 'rank-3';
                    return '<span class="' + rankClass + '">' + value + '</span>';
                }
            }, {
                field: 'fileName',
                title: '文件名称',
                formatter: function(value, row, index) {
                    return '<span class="file-name" title="' + value + '">' + value + '</span>';
                }
            }, {
                field: 'previewCount',
                title: '预览次数',
                align: 'center',
                width: '150',
                formatter: function(value, row, index) {
                    return '<span class="badge">' + value + '</span>';
                }
            }],
            pagination: false,
            sidePagination: 'client',
            striped: true,
            hover: true
        });
    }

    function refreshTable() {
        $('#rankingTable').bootstrapTable('refresh', {
            query: {
                topN: $('#topNSelect').val()
            }
        });
    }
</script>

</body>
</html>
