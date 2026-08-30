<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- 登录成功后的主页；不在 AuthFilter 白名单内，未登录访问会被重定向回 login.jsp --%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>首页 - java-jdbc</title>
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
        .welcome-box {
            text-align: center;
            background: #fff;
            border-radius: 8px;
            padding: 48px 64px;
            box-shadow: 0 2px 12px rgba(0, 0, 0, .08);
        }
        .welcome-box h1 {
            color: #333;
            font-size: 22px;
            margin-bottom: 12px;
        }
        .welcome-box p {
            color: #888;
            font-size: 14px;
        }
        .welcome-box .stats {
            margin-top: 20px;
            padding-top: 16px;
            border-top: 1px solid #f0f0f0;
            color: #666;
            font-size: 13px;
        }
        .stats b {
            color: #1890ff;
            font-weight: 600;
        }
    </style>
</head>
<body>
<div class="welcome-box">
    <h1>登录成功，欢迎你：${sessionScope.user.username}</h1>
    <p>这是一个受 AuthFilter 保护的页面，未登录访问会被重定向到登录页</p>

    <%-- SessionListener 已将统计发布到 ServletContext（application 作用域）。
         EL 会按 page→request→session→application 顺序搜索，直接写 ${onlineCount} 也能读到，
         这里显式写 applicationScope 与上方 sessionScope 风格一致、来源更一目了然；
         empty 判断作兜底：监听器尚未发布过属性时显示 0，避免页面输出空字符串 --%>
    <p class="stats">
        当前在线：<b>${empty applicationScope.onlineCount ? 0 : applicationScope.onlineCount}</b> 人
        &nbsp;|&nbsp;
        已登录：<b>${empty applicationScope.loginCount ? 0 : applicationScope.loginCount}</b> 人
    </p>
</div>
</body>
</html>
