<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>文件预览排行榜</title>
    <link rel="icon" href="./favicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="bootstrap-table/bootstrap-table.min.css"/>
    <link rel="stylesheet" href="css/theme.css"/>
    <script type="text/javascript" src="js/jquery-3.6.1.min.js"></script>
    <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="bootstrap-table/bootstrap-table.min.js"></script>
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
        <p>统计基于Redis实现，实时展示文件预览热度</p>
    </div>

    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">
                文件预览次数排行
                <div class="pull-right">
                    <select id="topNSelect" class="form-control" style="display: inline-block; width: auto; height: 28px; padding: 2px 12px; margin-top: -5px;">
                        <option value="10">Top 10</option>
                        <option value="50">Top 50</option>
                        <option value="100">Top 100</option>
                    </select>
                    <button id="refreshBtn" class="btn btn-default btn-sm" style="margin-left: 10px; margin-top: -5px;">
                        <span class="glyphicon glyphicon-refresh"></span> 刷新
                    </button>
                </div>
            </h3>
        </div>
        <div class="panel-body">
            <table id="rankTable" class="table table-striped table-bordered" style="width:100%;"></table>
        </div>
    </div>

    <div class="panel panel-info">
        <div class="panel-heading">
            <h3 class="panel-title">统计说明</h3>
        </div>
        <div class="panel-body">
            <ul>
                <li>每次成功调用 <code>/onlinePreview</code> 接口计为一次预览</li>
                <li>使用Redis Sorted Set实现，支持高并发下的原子计数</li>
                <li>统计数据持久化存储在Redis中</li>
                <li>预览请求成功响应后才进行计数，确保统计准确</li>
            </ul>
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
        function initTable(topN) {
            $('#rankTable').bootstrapTable('destroy').bootstrapTable({
                url: '${baseUrl}api/fileRank?topN=' + topN,
                method: 'GET',
                pagination: true,
                pageSize: topN,
                pageList: [10, 20, 50, 100],
                search: true,
                showColumns: true,
                showRefresh: true,
                minimumCountColumns: 2,
                dataField: 'content', // 从ReturnResponse的content字段获取数据
                responseHandler: function(res) {
                    if (res.code === 0) {
                        return res.content;
                    }
                    return [];
                },
                columns: [{
                    field: 'rank',
                    title: '排名',
                    align: 'center',
                    width: '80px',
                    formatter: function(value, row, index) {
                        if (value === 1) {
                            return '<span class="label label-danger" style="font-size: 14px;">🥇 ' + value + '</span>';
                        } else if (value === 2) {
                            return '<span class="label label-warning" style="font-size: 14px;">🥈 ' + value + '</span>';
                        } else if (value === 3) {
                            return '<span class="label label-info" style="font-size: 14px;">🥉 ' + value + '</span>';
                        }
                        return '<span class="label label-default">' + value + '</span>';
                    }
                }, {
                    field: 'fileName',
                    title: '文件名称',
                    sortable: true,
                    formatter: function(value) {
                        return '<span class="text-primary" title="' + value + '">' + value + '</span>';
                    }
                }, {
                    field: 'previewCount',
                    title: '预览次数',
                    align: 'center',
                    width: '120px',
                    sortable: true,
                    formatter: function(value) {
                        return '<span class="badge" style="font-size: 14px;">' + value + '</span>';
                    }
                }],
                onLoadSuccess: function(data) {
                    if (data.length === 0) {
                        $('#rankTable').bootstrapTable('showLoading');
                    }
                },
                onLoadError: function(status) {
                    if (status === 404 || status === 500) {
                        alert('获取排行榜数据失败，请检查Redis连接配置');
                    }
                }
            });
        }

        // 初始加载Top10
        initTable(10);

        // 切换TopN
        $('#topNSelect').change(function() {
            var topN = $(this).val();
            initTable(topN);
        });

        // 刷新按钮
        $('#refreshBtn').click(function() {
            var topN = $('#topNSelect').val();
            $('#rankTable').bootstrapTable('refresh');
        });
    });
</script>
</body>
</html>
