<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>kkFileView - 文件预览排名</title>
    <link rel="icon" href="./favicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="bootstrap-table/bootstrap-table.min.css"/>
    <link rel="stylesheet" href="css/theme.css"/>
    <script type="text/javascript" src="js/jquery-3.6.1.min.js"></script>
    <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="bootstrap-table/bootstrap-table.min.js"></script>
    <script type="text/javascript" src="js/base64.min.js"></script>
    <style>
        .ranking-btn-group {
            margin-bottom: 20px;
        }
        .ranking-btn-group .btn {
            margin-right: 10px;
        }
        .ranking-btn-group .btn.active {
            background-color: #5cb85c;
            border-color: #4cae4c;
        }
        .rank-number {
            font-weight: bold;
            font-size: 16px;
            color: #337ab7;
        }
        .top-3 .rank-number {
            color: #d9534f;
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
            <li class="active"><a href="./ranking">文件排名</a></li>
        </ul>
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <div class="page-header">
        <h1>最受欢迎文件排名</h1>
        <p>统计所有成功预览的文件，按预览次数从高到低排序</p>
    </div>
    
    <div class="ranking-btn-group">
        <button type="button" class="btn btn-default active" data-top="10">Top 10</button>
        <button type="button" class="btn btn-default" data-top="50">Top 50</button>
        <button type="button" class="btn btn-default" data-top="100">Top 100</button>
    </div>
    
    <div class="panel panel-default">
        <div class="panel-heading">
            <h3 class="panel-title">文件预览排行榜</h3>
        </div>
        <div class="panel-body">
            <table id="rankingTable" 
                   class="table table-striped table-bordered table-hover"
                   data-pagination="true"
                   data-page-size="10"
                   data-page-list="[10, 25, 50, 100]"
                   data-search="true"
                   data-show-columns="true"
                   data-show-refresh="true">
            </table>
        </div>
    </div>
</div>

<#if beian?? && beian != "default">
    <div style="display: grid; place-items: center; margin-top: 20px;">
        <div>
            <a target="_blank" href="https://beian.miit.gov.cn/">${beian}</a>
        </div>
    </div>
</#if>

<script>
    $(function () {
        var currentTop = 10;
        
        // 初始化表格
        loadRankingData(currentTop);
        
        // 切换Top N按钮
        $('.ranking-btn-group .btn').click(function() {
            $('.ranking-btn-group .btn').removeClass('active');
            $(this).addClass('active');
            currentTop = $(this).data('top');
            loadRankingData(currentTop);
        });
        
        function loadRankingData(topN) {
            $.ajax({
                url: '${baseUrl}api/ranking?topN=' + topN,
                type: 'GET',
                dataType: 'json',
                success: function(response) {
                    if (response.code === 0 && response.data) {
                        renderTable(response.data);
                    } else {
                        alert('加载排名数据失败: ' + (response.msg || '未知错误'));
                    }
                },
                error: function() {
                    alert('加载排名数据失败，请稍后重试');
                }
            });
        }
        
        function renderTable(data) {
            $('#rankingTable').bootstrapTable('destroy').bootstrapTable({
                data: data,
                columns: [{
                    field: 'rank',
                    title: '排名',
                    align: 'center',
                    width: '80px',
                    formatter: function(value, row, index) {
                        var rank = index + 1;
                        var badgeClass = '';
                        if (rank === 1) {
                            badgeClass = 'danger';
                        } else if (rank === 2) {
                            badgeClass = 'warning';
                        } else if (rank === 3) {
                            badgeClass = 'info';
                        } else {
                            badgeClass = 'default';
                        }
                        return '<span class="label label-' + badgeClass + ' rank-number">' + rank + '</span>';
                    },
                    cellStyle: function(value, row, index) {
                        if (index < 3) {
                            return { classes: 'top-3' };
                        }
                        return {};
                    }
                }, {
                    field: 'fileUrl',
                    title: '文件URL',
                    formatter: function(value) {
                        return '<a href="#" onclick="previewFile(\'' + encodeURIComponent(value) + '\'); return false;" title="点击预览">' + 
                               (value.length > 80 ? value.substring(0, 80) + '...' : value) + '</a>';
                    }
                }, {
                    field: 'count',
                    title: '预览次数',
                    align: 'center',
                    width: '120px',
                    sortable: true
                }],
                onClickRow: function(row, $element) {
                    previewFile(encodeURIComponent(row.fileUrl));
                }
            });
        }
        
        function previewFile(encodedUrl) {
            var b64Encoded = Base64.encode(encodedUrl);
            window.open('${baseUrl}onlinePreview?url=' + encodeURIComponent(b64Encoded));
        }
    });
</script>
</body>
</html>
