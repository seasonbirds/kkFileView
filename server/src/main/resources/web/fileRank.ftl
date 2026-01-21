<!DOCTYPE html>

<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>最受欢迎文件排行榜</title>
    <link rel="icon" href="./favicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="css/loading.css"/>
    <link rel="stylesheet" href="bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="bootstrap-table/bootstrap-table.min.css"/>
    <link rel="stylesheet" href="css/theme.css"/>
    <script type="text/javascript" src="js/jquery-3.6.1.min.js"></script>
    <script type="text/javascript" src="js/jquery.form.min.js"></script>
    <script type="text/javascript" src="bootstrap/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="bootstrap-table/bootstrap-table.min.js"></script>
    <script type="text/javascript" src="js/base64.min.js"></script>
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
            <li class="active"><a href="./fileRank">最受欢迎文件</a></li>
        </ul>
    </div>
</nav>

<div class="container theme-showcase" role="main" style="margin-top: 80px;">
    <!--  文件排行榜标题  -->
    <div class="page-header">
        <h1>最受欢迎文件排行榜</h1>
        <p>以下是基于文件预览次数统计的最受欢迎文件列表</p>
    </div>

    <!--  选择TopN  -->
    <div class="panel panel-default">
        <div class="panel-body">
            <div class="row">
                <div class="col-md-6">
                    <div class="form-group">
                        <label for="topNSelect">选择展示数量：</label>
                        <select id="topNSelect" class="form-control" style="width: 200px; display: inline-block; margin-left: 10px;">
                            <option value="10" <#if currentTopN == 10>selected</#if>>Top 10</option>
                            <option value="50" <#if currentTopN == 50>selected</#if>>Top 50</option>
                            <option value="100" <#if currentTopN == 100>selected</#if>>Top 100</option>
                        </select>
                        <button id="refreshBtn" class="btn btn-primary" style="margin-left: 10px;">刷新</button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!--  排行榜表格  -->
    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">文件预览次数排行</h3>
        </div>
        <div class="panel-body">
            <table id="rankTable" class="table table-striped table-bordered">
                <thead>
                    <tr>
                        <th style="width: 80px; text-align: center;">排序</th>
                        <th style="text-align: center;">文件名称</th>
                        <th style="width: 150px; text-align: center;">预览次数</th>
                    </tr>
                </thead>
                <tbody>
                    <#if rankList?? && rankList?size > 0>
                        <#list rankList as item>
                            <tr>
                                <td style="text-align: center;">${item.rank}</td>
                                <td>${item.fileName}</td>
                                <td style="text-align: center;">${item.count}</td>
                            </tr>
                        </#list>
                    <#else>
                        <tr>
                            <td colspan="3" style="text-align: center;">暂无数据</td>
                        </tr>
                    </#if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
    $(function () {
        // 选择TopN后刷新页面
        $('#refreshBtn').click(function () {
            var topN = $('#topNSelect').val();
            window.location.href = './fileRank?topN=' + topN;
        });

        // 回车触发刷新
        $('#topNSelect').keypress(function (e) {
            if (e.which === 13) {
                $('#refreshBtn').click();
            }
        });
    });
</script>
</body>
</html>
