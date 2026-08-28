<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>登录 - java-jdbc</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: "Microsoft YaHei", Arial, sans-serif;
            background: #f0f2f5;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }
        .login-box {
            width: 360px;
            background: #fff;
            border-radius: 8px;
            padding: 32px 28px;
            box-shadow: 0 2px 12px rgba(0, 0, 0, .08);
        }
        .login-box h2 {
            text-align: center;
            margin-bottom: 24px;
            color: #333;
        }
        .login-box input {
            width: 100%;
            height: 40px;
            padding: 0 12px;
            margin-bottom: 16px;
            border: 1px solid #d9d9d9;
            border-radius: 4px;
            font-size: 14px;
        }
        .login-box input:focus {
            outline: none;
            border-color: #1890ff;
        }
        .login-box button {
            width: 100%;
            height: 40px;
            border: none;
            border-radius: 4px;
            background: #1890ff;
            color: #fff;
            font-size: 15px;
            cursor: pointer;
        }
        .login-box button:hover {
            background: #40a9ff;
        }
        .error {
            margin-bottom: 16px;
            padding: 8px 12px;
            background: #fff2f0;
            border: 1px solid #ffccc7;
            border-radius: 4px;
            color: #cf1322;
            font-size: 13px;
        }
    </style>
</head>
<body>
<div class="login-box">
    <h2>用户登录</h2>

    <%-- LoginServlet 校验失败会重定向回本页并携带 error 参数：
         empty=参数为空  pwd=密码错误  noUser=用户不存在 --%>
    <p class="error" ${empty param.error ? 'style="display:none"' : ''}>
        ${param.error == 'empty' ? '用户名和密码不能为空'
            : param.error == 'noUser' ? '用户不存在，请检查用户名'
            : param.error == 'pwd' ? '密码错误，请重新输入'
            : '登录失败，请重试'}
    </p>

    <%-- action 必须携带上下文路径；name 与 LoginServlet 中 req.getParameter() 的参数名一致 --%>
    <form method="post" action="${pageContext.request.contextPath}/login">
        <input type="text" name="username" placeholder="用户名" required>
        <input type="password" name="password" placeholder="密码" required>
        <button type="submit">登 录</button>
    </form>
</div>
</body>
</html>
