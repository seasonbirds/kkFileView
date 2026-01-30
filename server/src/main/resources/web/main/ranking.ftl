<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>文件预览排行榜</title>
    <link rel="stylesheet" href="${base_url}css/bootstrap.min.css">
    <link rel="stylesheet" href="${base_url}css/main.css">
    <script src="${base_url}js/jquery-3.6.0.min.js"></script>
    <script src="${base_url}js/bootstrap.min.js"></script>
    <style>
        .ranking-container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
        }
        .ranking-header {
            text-align: center;
            margin-bottom: 30px;
        }
        .ranking-controls {
            margin-bottom: 20px;
            text-align: center;
        }
        .ranking-table {
            margin-top: 20px;
        }
        .rank-number {
            font-weight: bold;
            font-size: 1.2em;
        }
        .rank-1 {
            color: #FFD700;
        }
        .rank-2 {
            color: #C0C0C0;
        }
        .rank-3 {
            color: #CD7F32;
        }
        .file-name {
            max-width: 300px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .preview-count {
            font-weight: bold;
        }
        .loading {
            text-align: center;
            padding: 50px;
        }
        .error-message {
            color: #d9534f;
            text-align: center;
            padding: 20px;
        }
    </style>
</head>
<body>
<div class="container-fluid">
    <div class="row">
        <div class="col-md-12">
            <div class="ranking-container">
                <div class="ranking-header">
                    <h1>文件预览排行榜</h1>
                    <p class="lead">最受欢迎的文件预览排行</p>
                </div>
                
                <div class="ranking-controls">
                    <div class="btn-group" role="group">
                        <button type="button" class="btn btn-default top-btn ${topN == 10 ? 'active' : ''}" data-top="10">Top 10</button>
                        <button type="button" class="btn btn-default top-btn ${topN == 50 ? 'active' : ''}" data-top="50">Top 50</button>
                        <button type="button" class="btn btn-default top-btn ${topN == 100 ? 'active' : ''}" data-top="100">Top 100</button>
                    </div>
                    <button type="button" class="btn btn-primary" id="refresh-btn">
                        <span class="glyphicon glyphicon-refresh"></span> 刷新
                    </button>
                </div>
                
                <div id="loading" class="loading" style="display: none;">
                    <span class="glyphicon glyphicon-refresh glyphicon-refresh-animate"></span> 加载中...
                </div>
                
                <div id="error-message" class="error-message" style="display: none;"></div>
                
                <div id="ranking-content" class="ranking-table">
                    <!-- 排行榜内容将通过JavaScript动态加载 -->
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    $(document).ready(function() {
        let currentTopN = 10;
        
        // 加载排行榜数据
        function loadRankingData(topN) {
            $('#loading').show();
            $('#error-message').hide();
            $('#ranking-content').hide();
            
            $.ajax({
                url: '${base_url}ranking/data',
                type: 'GET',
                data: { topN: topN },
                success: function(response) {
                    $('#loading').hide();
                    
                    if (response.success) {
                        renderRankingTable(response.data);
                        $('#ranking-content').show();
                    } else {
                        $('#error-message').text(response.message || '获取排行榜数据失败');
                        $('#error-message').show();
                    }
                },
                error: function() {
                    $('#loading').hide();
                    $('#error-message').text('网络错误，请稍后重试');
                    $('#error-message').show();
                }
            });
        }
        
        // 渲染排行榜表格
        function renderRankingTable(data) {
            let html = '<table class="table table-striped table-bordered">' +
                '<thead>' +
                '<tr>' +
                '<th class="text-center">排名</th>' +
                '<th>文件名称</th>' +
                '<th class="text-center">预览次数</th>' +
                '</tr>' +
                '</thead>' +
                '<tbody>';
            
            if (data.length === 0) {
                html += '<tr><td colspan="3" class="text-center">暂无数据</td></tr>';
            } else {
                for (let i = 0; i < data.length; i++) {
                    let item = data[i];
                    let rankClass = '';
                    if (item.rank === 1) rankClass = 'rank-1';
                    else if (item.rank === 2) rankClass = 'rank-2';
                    else if (item.rank === 3) rankClass = 'rank-3';
                    
                    html += '<tr>' +
                        '<td class="text-center"><span class="rank-number ' + rankClass + '">' + item.rank + '</span></td>' +
                        '<td><span class="file-name" title="' + item.fileName + '">' + item.fileName + '</span></td>' +
                        '<td class="text-center preview-count">' + item.previewCount + '</td>' +
                        '</tr>';
                }
            }
            
            html += '</tbody></table>';
            $('#ranking-content').html(html);
        }
        
        // Top按钮点击事件
        $('.top-btn').click(function() {
            $('.top-btn').removeClass('active');
            $(this).addClass('active');
            currentTopN = parseInt($(this).data('top'));
            loadRankingData(currentTopN);
        });
        
        // 刷新按钮点击事件
        $('#refresh-btn').click(function() {
            loadRankingData(currentTopN);
        });
        
        // 初始加载
        loadRankingData(currentTopN);
    });
</script>

<style>
    .glyphicon-refresh-animate {
        -animation: spin .7s infinite linear;
        -webkit-animation: spin2 .7s infinite linear;
    }
    
    @keyframes spin2 {
        from { -webkit-transform: rotate(0deg);}
        to { -webkit-transform: rotate(360deg);}
    }
    
    @-webkit-keyframes spin2 {
        from { -webkit-transform: rotate(0deg);}
        to { -webkit-transform: rotate(360deg);}
    }
</style>
</body>
</html>