<!DOCTYPE html>

<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>kkFileView - 最受欢迎文件排行榜</title>
    <link rel="icon" href="./favicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="bootstrap-table/bootstrap-table.min.css"/>
    <link rel="stylesheet" href="css/theme.css"/>
    <script type="text/javascript" src="js/jquery-3.6.1.min.js"></script>
    <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="bootstrap-table/bootstrap-table.min.js"></script>
    <style>
        .ranking-badge {
            display: inline-block;
            width: 30px;
            height: 30px;
            line-height: 30px;
            text-align: center;
            border-radius: 50%;
            font-weight: bold;
            color: white;
        }
        .ranking-1 { background-color: #FFD700; }
        .ranking-2 { background-color: #C0C0C0; }
        .ranking-3 { background-color: #CD7F32; }
        .ranking-other { background-color: #6c757d; }
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

<div class="container theme-showcase" role="main">
    <div class="page-header">
        <h1>最受欢迎文件排行榜</h1>
    </div>

    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">文件预览次数排行</h3>
        </div>
        <div class="panel-body">
            <div class="row" style="margin-bottom: 15px;">
                <div class="col-md-6">
                    <label>显示数量：</label>
                    <select id="limitSelect" class="form-control" style="display: inline-block; width: auto;">
                        <option value="10">Top 10</option>
                        <option value="50">Top 50</option>
                        <option value="100">Top 100</option>
                    </select>
                </div>
                <div class="col-md-6" style="text-align: right;">
                    <button id="refreshBtn" class="btn btn-success">刷新</button>
                </div>
            </div>

            <table id="rankingTable" data-pagination="false"></table>
        </div>
    </div>
</div>

<script>
    $(function () {
        function loadRankingData(limit) {
            $('#rankingTable').bootstrapTable('destroy');
            $('#rankingTable').bootstrapTable({
                url: 'ranking/list?limit=' + limit,
                columns: [{
                    field: 'rank',
                    title: '排名',
                    width: 80,
                    align: 'center',
                    formatter: function(value, row, index) {
                        var badgeClass = 'ranking-other';
                        if (value === 1) {
                            badgeClass = 'ranking-1';
                        } else if (value === 2) {
                            badgeClass = 'ranking-2';
                        } else if (value === 3) {
                            badgeClass = 'ranking-3';
                        }
                        return '<span class="ranking-badge ' + badgeClass + '">' + value + '</span>';
                    }
                }, {
                    field: 'fileName',
                    title: '文件名称',
                    formatter: function(value, row, index) {
                        if (value.length > 100) {
                            return '<span title="' + value + '">' + value.substring(0, 100) + '...</span>';
                        }
                        return '<span title="' + value + '">' + value + '</span>';
                    }
                }, {
                    field: 'previewCount',
                    title: '预览次数',
                    width: 120,
                    align: 'center'
                }]
            }).on('load-error.bs.table', function (e, status) {
                console.error('加载排行榜数据失败:', status);
            });
        }

        $('#limitSelect').on('change', function() {
            var limit = $(this).val();
            loadRankingData(limit);
        });

        $('#refreshBtn').on('click', function() {
            var limit = $('#limitSelect').val();
            loadRankingData(limit);
        });

        loadRankingData(10);
    });
</script>
</body>
</html>
