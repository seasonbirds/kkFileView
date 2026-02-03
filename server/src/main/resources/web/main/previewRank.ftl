<!DOCTYPE html>

<html lang="zh-CN">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>文件预览排行榜 - kkFileView</title>
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
            font-size: 14px;
        }
        .rank-1 {
            background-color: #FFD700;
            color: #fff;
        }
        .rank-2 {
            background-color: #C0C0C0;
            color: #fff;
        }
        .rank-3 {
            background-color: #CD7F32;
            color: #fff;
        }
        .rank-other {
            background-color: #f0f0f0;
            color: #666;
        }
        .preview-count {
            font-weight: bold;
            color: #5cb85c;
        }
        .top-selector {
            margin-bottom: 20px;
        }
        .file-name {
            max-width: 500px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
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
        </ul>
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <div class="page-header">
        <h1>文件预览排行榜</h1>
        <p class="text-muted">统计各文件的预览次数，展示最受欢迎的文件Top榜单</p>
    </div>

    <div class="panel panel-default">
        <div class="panel-heading">
            <h3 class="panel-title">排行榜设置</h3>
        </div>
        <div class="panel-body">
            <div class="row top-selector">
                <div class="col-md-6">
                    <div class="form-group">
                        <label for="topSelect">显示数量：</label>
                        <select id="topSelect" class="form-control" style="width: 150px; display: inline-block;">
                            <option value="10" selected>Top 10</option>
                            <option value="50">Top 50</option>
                            <option value="100">Top 100</option>
                        </select>
                        <button id="refreshBtn" class="btn btn-primary" style="margin-left: 10px;">
                            <span class="glyphicon glyphicon-refresh"></span> 刷新
                        </button>
                    </div>
                </div>
                <div class="col-md-6 text-right">
                    <p class="text-muted" style="margin-top: 7px;">
                        <span class="glyphicon glyphicon-info-sign"></span>
                        数据实时更新，基于Redis Sorted Set实现，高并发安全
                    </p>
                </div>
            </div>
        </div>
    </div>

    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">最受欢迎文件榜单</h3>
        </div>
        <div class="panel-body">
            <table id="rankTable" class="table table-striped table-hover"></table>
        </div>
    </div>
</div>

<div class="loading_container" style="position: fixed; display: none;">
    <div class="spinner">
        <div class="spinner-container container1">
            <div class="circle1"></div>
            <div class="circle2"></div>
            <div class="circle3"></div>
            <div class="circle4"></div>
        </div>
        <div class="spinner-container container2">
            <div class="circle1"></div>
            <div class="circle2"></div>
            <div class="circle3"></div>
            <div class="circle4"></div>
        </div>
        <div class="spinner-container container3">
            <div class="circle1"></div>
            <div class="circle2"></div>
            <div class="circle3"></div>
            <div class="circle4"></div>
        </div>
    </div>
</div>

<script>
    $(function() {
        // 初始化表格
        initTable();

        // 下拉选择事件
        $('#topSelect').change(function() {
            loadRankData($(this).val());
        });

        // 刷新按钮事件
        $('#refreshBtn').click(function() {
            loadRankData($('#topSelect').val());
        });

        // 初始加载数据
        loadRankData(10);
    });

    function initTable() {
        $('#rankTable').bootstrapTable({
            columns: [
                {
                    field: 'rank',
                    title: '排名',
                    align: 'center',
                    valign: 'middle',
                    width: '100px',
                    formatter: function(value, row, index) {
                        var rankClass = 'rank-other';
                        if (value === 1) rankClass = 'rank-1';
                        else if (value === 2) rankClass = 'rank-2';
                        else if (value === 3) rankClass = 'rank-3';
                        return '<span class="rank-badge ' + rankClass + '">' + value + '</span>';
                    }
                },
                {
                    field: 'fileName',
                    title: '文件名称',
                    align: 'left',
                    valign: 'middle',
                    formatter: function(value, row, index) {
                        return '<span class="file-name" title="' + escapeHtml(value) + '">' + escapeHtml(value) + '</span>';
                    }
                },
                {
                    field: 'previewCount',
                    title: '预览次数',
                    align: 'center',
                    valign: 'middle',
                    width: '150px',
                    formatter: function(value, row, index) {
                        return '<span class="preview-count">' + formatNumber(value) + '</span>';
                    }
                }
            ],
            data: [],
            pagination: false,
            striped: true,
            bordered: true
        });
    }

    function loadRankData(topN) {
        showLoading();
        $.ajax({
            url: './api/previewRank/topN',
            type: 'GET',
            data: { topN: topN },
            dataType: 'json',
            success: function(response) {
                hideLoading();
                if (response.code === 0) {
                    $('#rankTable').bootstrapTable('load', response.data);
                } else {
                    showError('加载数据失败：' + response.msg);
                }
            },
            error: function(xhr, status, error) {
                hideLoading();
                showError('加载数据失败，请稍后重试');
                console.error('加载排行榜数据失败：', error);
            }
        });
    }

    function showLoading() {
        $('.loading_container').show();
    }

    function hideLoading() {
        $('.loading_container').hide();
    }

    function showError(msg) {
        alert(msg);
    }

    function escapeHtml(text) {
        if (!text) return '';
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(text));
        return div.innerHTML;
    }

    function formatNumber(num) {
        if (num === undefined || num === null) return '0';
        return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    }
</script>

</body>
</html>
