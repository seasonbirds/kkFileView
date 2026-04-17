<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>kkFileView - 用户登录</title>
    <link rel="icon" href="${baseUrl}favicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="${baseUrl}bootstrap/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="${baseUrl}css/theme.css"/>
    <script type="text/javascript" src="${baseUrl}js/jquery-3.6.1.min.js"></script>
    <script type="text/javascript" src="${baseUrl}bootstrap/js/bootstrap.min.js"></script>
    <style>
        body {
            background-color: #f5f5f5;
            padding-top: 40px;
            padding-bottom: 40px;
        }
        .login-container {
            max-width: 400px;
            margin: 0 auto;
            padding: 30px;
            background-color: #fff;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .login-header {
            text-align: center;
            margin-bottom: 30px;
        }
        .login-header h1 {
            font-size: 28px;
            color: #333;
            margin: 0;
        }
        .login-header p {
            color: #666;
            margin-top: 10px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-control {
            height: 45px;
            font-size: 16px;
        }
        .btn-login {
            width: 100%;
            height: 45px;
            font-size: 18px;
            background-color: #5cb85c;
            border-color: #4cae4c;
        }
        .btn-login:hover {
            background-color: #4cae4c;
        }
        .alert {
            margin-top: 15px;
            display: none;
        }
        .navbar {
            margin-bottom: 0;
        }
    </style>
</head>
<body>

<nav class="navbar navbar-inverse navbar-fixed-top">
    <div class="container">
        <div class="navbar-header">
            <a class="navbar-brand" href="https://kkview.cn" target='_blank'>kkFileView</a>
        </div>
    </div>
</nav>

<div class="container">
    <div class="login-container">
        <div class="login-header">
            <h1>用户登录</h1>
            <p>请登录以访问文件预览服务</p>
        </div>

        <form id="loginForm">
            <div class="form-group">
                <label for="phone">手机号</label>
                <input type="text" class="form-control" id="phone" name="phone" placeholder="请输入手机号">
            </div>

            <div class="form-group">
                <label for="password">密码</label>
                <input type="password" class="form-control" id="password" name="password" placeholder="请输入密码">
            </div>

            <button type="button" id="loginBtn" class="btn btn-success btn-login">登 录</button>

            <div id="errorAlert" class="alert alert-danger alert-dismissable" role="alert">
                <button type="button" class="close" data-dismiss="alert" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
                <strong id="errorMsg">登录失败</strong>
            </div>
        </form>
    </div>
</div>

<script>
    $(function () {
        $('#password').keypress(function (e) {
            if (e.which === 13) {
                $('#loginBtn').click();
            }
        });

        $('#loginBtn').click(function () {
            var phone = $('#phone').val().trim();
            var password = $('#password').val().trim();

            if (phone === '') {
                showError('请输入手机号');
                return;
            }

            if (password === '') {
                showError('请输入密码');
                return;
            }

            $('#loginBtn').attr('disabled', true).text('登录中...');

            $.ajax({
                url: '${baseUrl}doLogin',
                type: 'POST',
                data: {
                    phone: phone,
                    password: password
                },
                dataType: 'json',
                success: function (data) {
                    if (data.code === 0) {
                        window.location.href = '${baseUrl}index';
                    } else {
                        showError(data.msg);
                        $('#loginBtn').attr('disabled', false).text('登 录');
                    }
                },
                error: function () {
                    showError('登录请求失败，请稍后重试');
                    $('#loginBtn').attr('disabled', false).text('登 录');
                }
            });
        });
    });

    function showError(msg) {
        $('#errorMsg').text(msg);
        $('#errorAlert').show();
        setTimeout(function () {
            $('#errorAlert').hide();
        }, 3000);
    }
</script>
</body>
</html>
