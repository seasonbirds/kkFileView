<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8"/>
    <style type="text/css">
        body {
            margin: 0 auto;
            width: 900px;
            background-color: #CCB;
        }

        .container {
            width: 700px;
            height: 700px;
            margin: 0 auto;
        }

        img {
            width: auto;
            height: auto;
            max-width: 100%;
            max-height: 100%;
            padding-bottom: 36px;
        }

        span {
            display: block;
            font-size: 20px;
            color: blue;
        }
    </style>
</head>

<body>
<div class="container">
    <img src="images/sorry.jpg"/>
    <span>
        访问受限，具体原因如下：
        <p style="color: red;">${msg}</p>
    </span>
    <p>有任何疑问，请联系系统管理员。</p>
</div>
</body>
</html>
