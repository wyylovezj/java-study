<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>出错了 - java-jdbc</title>
    <style>
        body {
            font-family: "Microsoft YaHei", Arial, sans-serif;
            background: #f0f2f5;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
        }
        .error-box {
            text-align: center;
            background: #fff;
            padding: 40px 60px;
            border-radius: 8px;
            box-shadow: 0 2px 12px rgba(0, 0, 0, .08);
        }
        h1 {
            color: #cf1322;
            font-size: 28px;
            margin-bottom: 16px;
        }
        p {
            color: #666;
            font-size: 14px;
            margin: 8px 0;
        }
        a {
            color: #1890ff;
            text-decoration: none;
        }
    </style>
</head>
<body>
<div class="error-box">
    <h1>系统繁忙</h1>
    <p>抱歉，页面出错了，请稍后重试</p>
    <p><a href="${pageContext.request.contextPath}/login.jsp">返回登录页</a></p>
</div>
</body>
</html>
