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
            <li class="active"><a href="./popular-files">最受欢迎文件</a></li>
        </ul>
    </div>
</nav>

<div class="container theme-showcase" role="main">
    <!--  页面标题  -->
    <div class="page-header">
        <h1>最受欢迎文件排行榜</h1>
        <p>根据文件预览次数统计，展示最受欢迎的文件列表。</p>
    </div>

    <!--  排行榜设置  -->
    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">排行榜设置</h3>
        </div>
        <div class="panel-body">
            <div class="row">
                <div class="col-md-4">
                    <div class="form-group">
                        <label for="topNSelect">展示数量：</label>
                        <select id="topNSelect" class="form-control">
                            <option value="10">Top 10</option>
                            <option value="50">Top 50</option>
                            <option value="100">Top 100</option>
                        </select>
                    </div>
                </div>
                <div class="col-md-2">
                    <div class="form-group" style="margin-top: 24px;">
                        <button id="refreshBtn" type="button" class="btn btn-success">刷新排行榜</button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!--  排行榜列表  -->
    <div class="panel panel-success">
        <div class="panel-heading">
            <h3 class="panel-title">文件排行榜</h3>
        </div>
        <div class="panel-body">
            <table id="rankTable" class="table table-striped table-bordered">
                <thead>
                    <tr>
                        <th style="width: 10%; text-align: center;">排名</th>
                        <th style="width: 60%;">文件名称</th>
                        <th style="width: 30%; text-align: center;">预览次数</th>
                    </tr>
                </thead>
                <tbody id="rankTableBody">
                    <!--  数据将通过JavaScript动态填充  -->
                </tbody>
            </table>
            <div id="emptyMessage" class="text-center" style="padding: 40px; color: #999;">
                <p>暂无数据</p>
            </div>
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
    function showLoadingDiv() {
        var height = window.document.documentElement.clientHeight - 1;
        $(".loading_container").css("height", height).show();
    }

    function hideLoadingDiv() {
        $(".loading_container").hide();
    }

    function loadRankData(topN) {
        showLoadingDiv();
        $.ajax({
            url: '${baseUrl}api/popular-files?topN=' + topN,
            type: 'GET',
            dataType: 'json',
            success: function (data) {
                var tableBody = $("#rankTableBody");
                var emptyMessage = $("#emptyMessage");
                tableBody.empty();
                
                if (Object.keys(data).length === 0) {
                    emptyMessage.show();
                    $("#rankTable").hide();
                } else {
                    emptyMessage.hide();
                    $("#rankTable").show();
                    
                    var rank = 1;
                    $.each(data, function (fileName, count) {
                        var row = '<tr>' +
                            '<td style="text-align: center; vertical-align: middle;">' + rank + '</td>' +
                            '<td style="vertical-align: middle;">' + fileName + '</td>' +
                            '<td style="text-align: center; vertical-align: middle;">' + count + '</td>' +
                            '</tr>';
                        tableBody.append(row);
                        rank++;
                    });
                }
                hideLoadingDiv();
            },
            error: function () {
                alert('获取排行榜数据失败，请重试');
                hideLoadingDiv();
            }
        });
    }

    $(function () {
        // 初始加载Top 10数据
        loadRankData(10);
        
        // 刷新按钮点击事件
        $("#refreshBtn").click(function () {
            var topN = $("#topNSelect").val();
            loadRankData(topN);
        });
        
        // 下拉框选择事件
        $("#topNSelect").change(function () {
            var topN = $(this).val();
            loadRankData(topN);
        });
    });
</script>
</body>
</html>