<!DOCTYPE html>

<html lang="en">
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
    <script type="text/javascript" src="js/base64.min.js"></script>
    <style>
        .top1 {
            color: #FFD700;
            font-weight: bold;
            font-size: 18px;
        }
        .top2 {
            color: #C0C0C0;
            font-weight: bold;
            font-size: 16px;
        }
        .top3 {
            color: #CD7F32;
            font-weight: bold;
            font-size: 16px;
        }
        .rank-badge {
            display: inline-block;
            width: 28px;
            height: 28px;
            line-height: 28px;
            text-align: center;
            border-radius: 50%;
            background-color: #777;
            color: #fff;
            font-weight: bold;
        }
        .top1-badge {
            background-color: #FFD700;
        }
        .top2-badge {
            background-color: #C0C0C0;
        }
        .top3-badge {
            background-color: #CD7F32;
        }
        .rank-selector {
            margin-bottom: 10px;
        }
        .rank-selector .btn {
            margin-right: 5px;
        }
        .rank-selector .btn.active {
            background-color: #337ab7;
            color: #fff;
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
            <li class="active"><a href="./rank">文件排行榜</a></li>
        </ul>
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <div class="page-header">
        <h1>最受欢迎文件排行榜</h1>
        <p>基于文件预览次数统计，数据实时更新</p>
    </div>

    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">
                文件预览排行榜
                <div class="pull-right rank-selector">
                    <button class="btn btn-default btn-sm active" data-limit="10">Top 10</button>
                    <button class="btn btn-default btn-sm" data-limit="50">Top 50</button>
                    <button class="btn btn-default btn-sm" data-limit="100">Top 100</button>
                </div>
            </h3>
        </div>
        <div class="panel-body">
            <table id="rankTable" class="table table-striped"></table>
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
    function formatRank(value, row, index) {
        var badgeClass = 'rank-badge';
        if (value === 1) {
            badgeClass += ' top1-badge';
        } else if (value === 2) {
            badgeClass += ' top2-badge';
        } else if (value === 3) {
            badgeClass += ' top3-badge';
        }
        return '<span class="' + badgeClass + '">' + value + '</span>';
    }

    function formatFileName(value, row, index) {
        var baseUrl = "${baseUrl}";
        return '<a href="' + baseUrl + 'onlinePreview?url=' + encodeURIComponent(Base64.encode(row.fileUrl)) + '" target="_blank" title="' + value + '">' + value + '</a>';
    }

    function formatPreviewCount(value, row, index) {
        var colorClass = '';
        if (row.rank === 1) {
            colorClass = 'top1';
        } else if (row.rank === 2) {
            colorClass = 'top2';
        } else if (row.rank === 3) {
            colorClass = 'top3';
        }
        return '<span class="' + colorClass + '">' + value + '</span>';
    }

    function loadRankData(limit) {
        $.get('${baseUrl}api/fileRank?limit=' + limit, function(data) {
            if (data.code === 0 && data.data) {
                $('#rankTable').bootstrapTable('load', data.data);
            } else {
                $('#rankTable').bootstrapTable('load', []);
            }
        });
    }

    $(function () {
        $('#rankTable').bootstrapTable({
            columns: [{
                field: 'rank',
                title: '排名',
                width: 80,
                align: 'center',
                formatter: formatRank
            }, {
                field: 'fileName',
                title: '文件名称'
            }, {
                field: 'previewCount',
                title: '预览次数',
                width: 100,
                align: 'center',
                formatter: formatPreviewCount,
                sortable: true
            }],
            data: []
        });

        loadRankData(10);

        $('.rank-selector .btn').on('click', function() {
            var limit = $(this).data('limit');
            $('.rank-selector .btn').removeClass('active');
            $(this).addClass('active');
            loadRankData(limit);
        });
    });
</script>
</body>
</html>