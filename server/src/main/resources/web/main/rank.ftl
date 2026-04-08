<!DOCTYPE html>

<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>kkFileView文件预览排行榜</title>
    <link rel="icon" href="./favicon.ico" type="image/x-icon">
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
        .rank-1 { background-color: #FFD700; }
        .rank-2 { background-color: #C0C0C0; }
        .rank-3 { background-color: #CD7F32; }
        .rank-other { background-color: #6c757d; }
        .top-select {
            margin-bottom: 20px;
        }
        .top-select label {
            margin-right: 10px;
            font-weight: normal;
        }
        .top-select .btn-group {
            margin-right: 5px;
        }
        .table>tbody>tr>td {
            vertical-align: middle;
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
            <li class="active"><a href="./rank">文件排行榜</a></li>
            <li><a href="./sponsor">赞助开源</a></li>
        </ul>
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <div class="page-header">
        <h1>最受欢迎文件排行榜</h1>
        统计各文件的预览次数，预览次数越多排名越靠前。
    </div>
    
    <#if rankEnabled>
    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">
                <div class="top-select">
                    <label>显示数量：</label>
                    <div class="btn-group" role="group">
                        <button type="button" class="btn btn-default top-btn" data-top="10">Top 10</button>
                        <button type="button" class="btn btn-default top-btn" data-top="50">Top 50</button>
                        <button type="button" class="btn btn-default top-btn" data-top="100">Top 100</button>
                    </div>
                </div>
            </h3>
        </div>
        <div class="panel-body">
            <table id="rankTable" class="table table-striped table-hover"></table>
        </div>
    </div>
    <#else>
    <div class="panel panel-warning">
        <div class="panel-heading">
            <h3 class="panel-title">功能未启用</h3>
        </div>
        <div class="panel-body">
            <div class="alert alert-warning" role="alert">
                <strong>提示：</strong>文件预览排行榜功能未启用。
                <br><br>
                如需启用，请在配置文件中添加以下配置：
                <br><br>
                <code>rank.redis.enabled=true</code>
                <br>
                <code>rank.redis.address=127.0.0.1:6379</code>
                <br>
                <code>rank.redis.database=1</code>
                <br><br>
                配置说明：
                <ul>
                    <li><code>rank.redis.enabled</code>: 是否启用排行榜功能，true为启用，false为禁用</li>
                    <li><code>rank.redis.address</code>: Redis服务器地址，格式为 host:port</li>
                    <li><code>rank.redis.password</code>: Redis密码（可选）</li>
                    <li><code>rank.redis.database</code>: Redis数据库索引，默认使用独立的数据库避免与系统原有Redis冲突</li>
                </ul>
            </div>
        </div>
    </div>
    </#if>
</div>

<#if beian?? && beian != "default">
    <div style="display: grid; place-items: center;">
        <div>
            <a target="_blank"  href="https://beian.miit.gov.cn/">${beian}</a>
        </div>
    </div>
</#if>

<script>
    var currentTopN = ${topN};
    
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
    
    function escapeHtml(text) {
        if (text === null || text === undefined) {
            return '';
        }
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    function loadRankData(topN) {
        currentTopN = topN;
        
        $('.top-btn').removeClass('btn-success').addClass('btn-default');
        $('.top-btn[data-top="' + topN + '"]').removeClass('btn-default').addClass('btn-success');
        
        $.getJSON('${baseUrl}api/rank?topN=' + topN, function(data) {
            if (data.enabled && data.data) {
                $('#rankTable').bootstrapTable('load', data.data);
            }
        });
    }
    
    $(function() {
        $('.top-btn[data-top="' + currentTopN + '"]').removeClass('btn-default').addClass('btn-success');
        
        $('#rankTable').bootstrapTable({
            url: '${baseUrl}api/rank?topN=' + currentTopN,
            method: 'get',
            pagination: true,
            pageSize: currentTopN > 50 ? 50 : currentTopN,
            pageList: [10, 20, 50, 100],
            search: true,
            columns: [{
                field: 'rank',
                title: '排序',
                align: 'center',
                width: 80,
                formatter: function(value, row, index) {
                    return getRankBadge(value);
                }
            }, {
                field: 'fileName',
                title: '文件名称',
                formatter: function(value, row, index) {
                    return '<span title="' + escapeHtml(value) + '">' + escapeHtml(value) + '</span>';
                }
            }, {
                field: 'previewCount',
                title: '预览次数',
                align: 'center',
                width: 120,
                formatter: function(value, row, index) {
                    return '<span class="badge badge-info">' + value + '</span>';
                }
            }]
        });
        
        $('.top-btn').click(function() {
            var topN = $(this).data('top');
            loadRankData(topN);
        });
    });
</script>
</body>
</html>
