<!DOCTYPE html>

<html lang="en">
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
    <script type="text/javascript" src="js/base64.min.js"></script>
    <style>
        .ranking-container {
            margin-top: 80px;
            padding: 20px;
        }
        .ranking-header {
            margin-bottom: 30px;
        }
        .ranking-header h1 {
            color: #333;
            font-size: 28px;
            margin-bottom: 10px;
        }
        .limit-selector {
            margin-bottom: 20px;
        }
        .limit-selector label {
            font-weight: bold;
            margin-right: 10px;
        }
        .limit-selector select {
            padding: 5px 10px;
            border-radius: 4px;
            border: 1px solid #ccc;
        }
        .table thead th {
            background-color: #337ab7;
            color: white;
        }
        .rank-badge {
            display: inline-block;
            min-width: 30px;
            height: 30px;
            line-height: 30px;
            text-align: center;
            border-radius: 50%;
            color: white;
            font-weight: bold;
        }
        .rank-1 {
            background-color: #ffd700;
            color: #333;
        }
        .rank-2 {
            background-color: #c0c0c0;
            color: #333;
        }
        .rank-3 {
            background-color: #cd7f32;
            color: #fff;
        }
        .rank-other {
            background-color: #337ab7;
        }
        .preview-btn {
            margin-left: 10px;
        }
        .empty-message {
            text-align: center;
            color: #666;
            padding: 40px;
            font-size: 16px;
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
            <li class="active"><a href="./ranking">文件排行榜</a></li>
            <li><a href="./integrated">接入说明</a></li>
            <li><a href="./record">版本发布记录</a></li>
            <li><a href="./sponsor">赞助开源</a></li>
        </ul>
    </div>
</nav>

<div class="container ranking-container">
    <div class="ranking-header">
        <h1>🏆 最受欢迎文件排行榜</h1>
        <p class="text-muted">根据文件预览次数实时统计排名</p>
    </div>

    <div class="limit-selector">
        <label for="limitSelect">显示数量：</label>
        <select id="limitSelect" class="form-control inline" style="width: auto; display: inline-block;">
            <option value="10" selected>Top 10</option>
            <option value="50">Top 50</option>
            <option value="100">Top 100</option>
        </select>
        <button id="refreshBtn" class="btn btn-primary" style="margin-left: 10px;">
            <span class="glyphicon glyphicon-refresh"></span> 刷新
        </button>
    </div>

    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">排行榜列表</h3>
        </div>
        <div class="panel-body">
            <table id="rankingTable" 
                   data-toggle="table"
                   data-url="${baseUrl}api/ranking/topFiles"
                   data-method="get"
                   data-pagination="true"
                   data-page-list="[10, 25, 50, 100]"
                   data-page-size="10"
                   data-side-pagination="client"
                   data-search="true"
                   data-show-columns="true"
                   data-show-toggle="true"
                   data-show-refresh="false"
                   data-striped="true"
                   data-sort-name="previewCount"
                   data-sort-order="desc">
                <thead>
                    <tr>
                        <th data-field="rank" data-title="排名" data-sortable="true" data-width="80">排名</th>
                        <th data-field="fileName" data-title="文件名称" data-sortable="true">文件名称</th>
                        <th data-field="previewCount" data-title="预览次数" data-sortable="true" data-width="120">预览次数</th>
                        
                    </tr>
                </thead>
            </table>
            <div id="emptyDiv" class="empty-message" style="display: none;">
                <span class="glyphicon glyphicon-info-sign"></span> 暂无数据
            </div>
        </div>
    </div>
</div>

<script>
    var currentLimit = 10;

    function getRankBadge(rank) {
        var badgeClass = 'rank-other';
        if (rank === 1) {
            badgeClass = 'rank-1';
        } else if (rank === 2) {
            badgeClass = 'rank-2';
        } else if (rank === 3) {
            badgeClass = 'rank-3';
        }
        return '<span class="rank-badge ' + badgeClass + '">' + rank + '</span>';
    }

    function loadRankingData() {
        $('#rankingTable').bootstrapTable('refresh', {
            url: '${baseUrl}api/ranking/topFiles?limit=' + currentLimit
        });
    }

    $(function () {
        $('#rankingTable').bootstrapTable({
            onLoadSuccess: function (data) {
                if (data.code === 0 && data.data && data.data.length > 0) {
                    $('#emptyDiv').hide();
                    $('#rankingTable').show();
                    
                    $('#rankingTable').bootstrapTable('load', data.data);
                } else {
                    $('#rankingTable').hide();
                    $('#emptyDiv').show();
                }
            },
            onLoadError: function (status) {
                $('#rankingTable').hide();
                $('#emptyDiv').html('<span class="glyphicon glyphicon-exclamation-sign"></span> 加载数据失败: ' + status).show();
            },
            columns: [
                {
                    field: 'rank',
                    title: '排名',
                    sortable: true,
                    width: 80,
                    formatter: function (value, row) {
                        return getRankBadge(row.rank);
                    }
                },
                {
                    field: 'fileName',
                    title: '文件名称',
                    sortable: true,
                    formatter: function (value, row) {
                        var maxLength = 50;
                        if (value.length > maxLength) {
                            return '<span title="' + value + '">' + value.substring(0, maxLength) + '...</span>';
                        }
                        return value;
                    }
                },
                {
                    field: 'previewCount',
                    title: '预览次数',
                    sortable: true,
                    width: 120,
                    align: 'center',
                    formatter: function (value, row) {
                        return '<strong>' + value + '</strong>';
                    }
                }
            ]
        });

        $('#limitSelect').change(function () {
            currentLimit = $(this).val();
            loadRankingData();
        });

        $('#refreshBtn').click(function () {
            loadRankingData();
        });

        loadRankingData();
    });
</script>
</body>
</html>