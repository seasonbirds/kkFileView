<!DOCTYPE html>

<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>kkFileView文件预览排行榜</title>
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
            width: 30px;
            height: 30px;
            line-height: 30px;
            text-align: center;
            border-radius: 50%;
            font-weight: bold;
            color: #fff;
        }
        .rank-1 { background-color: #FFD700; }
        .rank-2 { background-color: #C0C0C0; }
        .rank-3 { background-color: #CD7F32; }
        .rank-other { background-color: #6c757d; }
        .top-selector {
            margin-bottom: 20px;
        }
        .top-selector .btn {
            margin-right: 10px;
        }
        .top-selector .btn.active {
            background-color: #5cb85c;
            border-color: #4cae4c;
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
            <li class="active"><a href="./rank">预览排行榜</a></li>
        </ul>
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <div class="page-header">
        <h1>最受欢迎文件排行榜</h1>
        展示预览次数最多的文件列表。
    </div>

    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">选择展示数量</h3>
        </div>
        <div class="panel-body">
            <div class="top-selector">
                <button type="button" class="btn btn-default active" data-top="10">Top 10</button>
                <button type="button" class="btn btn-default" data-top="50">Top 50</button>
                <button type="button" class="btn btn-default" data-top="100">Top 100</button>
            </div>
        </div>
    </div>

    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">排行榜列表</h3>
        </div>
        <div class="panel-body">
            <table id="rankTable" data-pagination="false" data-page-size="100">
                <thead>
                    <tr>
                        <th data-field="rank" data-formatter="rankFormatter">排名</th>
                        <th data-field="fileName">文件名称</th>
                        <th data-field="previewCount">预览次数</th>
                    </tr>
                </thead>
            </table>
        </div>
    </div>
</div>

<div class="loading_container" style="position: fixed;">
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
    var currentTop = ${top!'10'};

    function showLoadingDiv() {
        var height = window.document.documentElement.clientHeight - 1;
        $(".loading_container").css("height", height).show();
    }

    function hideLoadingDiv() {
        $(".loading_container").hide();
    }

    function rankFormatter(value, row, index) {
        var rank = row.rank;
        var rankClass = 'rank-other';
        if (rank === 1) {
            rankClass = 'rank-1';
        } else if (rank === 2) {
            rankClass = 'rank-2';
        } else if (rank === 3) {
            rankClass = 'rank-3';
        }
        return '<span class="rank-badge ' + rankClass + '">' + rank + '</span>';
    }

    function loadRankList(top) {
        showLoadingDiv();
        $.ajax({
            url: '${baseUrl}rank/list',
            type: 'GET',
            data: { top: top },
            success: function(data) {
                $('#rankTable').bootstrapTable('load', data);
                hideLoadingDiv();
            },
            error: function() {
                hideLoadingDiv();
                alert('加载排行榜数据失败，请稍后重试');
            }
        });
    }

    $(function () {
        $('#rankTable').bootstrapTable({
            data: []
        });

        loadRankList(currentTop);

        $('.top-selector button').click(function() {
            var top = $(this).data('top');
            currentTop = top;
            $('.top-selector button').removeClass('active');
            $(this).addClass('active');
            loadRankList(top);
        });

        if (currentTop === 10) {
            $('.top-selector button[data-top="10"]').addClass('active').siblings().removeClass('active');
        } else if (currentTop === 50) {
            $('.top-selector button[data-top="50"]').addClass('active').siblings().removeClass('active');
        } else if (currentTop === 100) {
            $('.top-selector button[data-top="100"]').addClass('active').siblings().removeClass('active');
        }
    });
</script>
</body>
</html>
