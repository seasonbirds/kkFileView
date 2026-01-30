<!DOCTYPE html>

<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>文件排行榜 - kkFileView</title>
    <link rel="icon" href="./favicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="bootstrap-table/bootstrap-table.min.css"/>
    <link rel="stylesheet" href="css/theme.css"/>
    <script type="text/javascript" src="js/jquery-3.6.1.min.js"></script>
    <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="bootstrap-table/bootstrap-table.min.js"></script>
    <style>
        .rank-btn-group .btn {
            margin-right: 5px;
        }
        .rank-btn-group .btn.active {
            background-color: #5cb85c;
            border-color: #4cae4c;
        }
        .table>thead>tr>th {
            text-align: center;
        }
        .table>tbody>tr>td {
            text-align: center;
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
        </ul>
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <div class="page-header">
        <h1>最受欢迎文件排行榜</h1>
        <p>基于文件预览次数统计，展示最受欢迎的文件</p>
    </div>

    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">
                排行榜
                <div class="btn-group pull-right rank-btn-group" role="group">
                    <button type="button" class="btn btn-default btn-sm active" data-top="10">Top 10</button>
                    <button type="button" class="btn btn-default btn-sm" data-top="50">Top 50</button>
                    <button type="button" class="btn btn-default btn-sm" data-top="100">Top 100</button>
                </div>
            </h3>
        </div>
        <div class="panel-body">
            <table id="rankTable" data-pagination="true"></table>
        </div>
    </div>
</div>

<#if beian?? && beian != "default">
    <div style="display: grid; place-items: center;">
        <div>
            <a target="_blank"  href="https://beian.miit.gov.cn/">${beian}</a>
        </div>
    </div>
</#if>

<script>
    $(function () {
        var currentTop = 10;
        var baseUrl = '${baseUrl}';
        if (baseUrl === 'default') {
            baseUrl = './';
        } else if (!baseUrl.endsWith('/')) {
            baseUrl += '/';
        }

        function loadRankData(top) {
            $.get(baseUrl + 'api/fileRank?top=' + top, function(data) {
                if (data.code === 0 && data.content) {
                    $('#rankTable').bootstrapTable('load', data.content);
                }
            });
        }

        $('#rankTable').bootstrapTable({
            url: baseUrl + 'api/fileRank?top=10',
            pagination: true,
            pageSize: 10,
            pageList: [10, 20, 50, 100],
            search: true,
            columns: [{
                field: 'rank',
                title: '排名',
                width: 80,
                formatter: function(value, row, index) {
                    if (value === 1) {
                        return '<span class="label label-danger" style="font-size: 14px;">' + value + '</span>';
                    } else if (value === 2) {
                        return '<span class="label label-warning" style="font-size: 14px;">' + value + '</span>';
                    } else if (value === 3) {
                        return '<span class="label label-primary" style="font-size: 14px;">' + value + '</span>';
                    }
                    return '<span class="label label-default" style="font-size: 14px;">' + value + '</span>';
                }
            }, {
                field: 'fileName',
                title: '文件名称',
                formatter: function(value, row) {
                    if (value) {
                        var decodedValue = value;
                        try {
                            decodedValue = decodeURIComponent(value);
                        } catch (e) {
                        }
                        var displayValue = decodedValue;
                        if (decodedValue.length > 80) {
                            displayValue = decodedValue.substring(0, 80) + '...';
                        }
                        var url = row.url || value;
                        return '<a href="./onlinePreview?url=' + encodeURIComponent(url) + '" target="_blank" title="' + decodedValue + '">' + displayValue + '</a>';
                    }
                    return value;
                }
            }, {
                field: 'previewCount',
                title: '预览次数',
                width: 100,
                sortable: true,
                formatter: function(value) {
                    return '<span class="badge" style="background-color: #5cb85c; font-size: 14px;">' + value + '</span>';
                }
            }]
        });

        $('.rank-btn-group .btn').on('click', function() {
            var top = $(this).data('top');
            if (top !== currentTop) {
                currentTop = top;
                $('.rank-btn-group .btn').removeClass('active');
                $(this).addClass('active');
                loadRankData(top);
            }
        });
    });
</script>
</body>
</html>
