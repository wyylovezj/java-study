package com.wyy.filter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;

/**
 * EncodingFilter
 * 字符编码过滤器
 * 用于设置请求和响应的字符编码，防止中文乱码
 * @author wyy
 * @date 2023/07/07
 */

public class EncodingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(EncodingFilter.class);
    private String encoding;

    /**
     * 初始化方法
     * @param filterConfig
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 从 web.xml 的 init-param 读取编码配置
        this.encoding = filterConfig.getInitParameter("encoding");
        if (this.encoding == null || this.encoding.isEmpty()) {
            // 设置默认值为 UTF-8
            this.encoding = "UTF-8";
        }
        logger.info("[EncodingFilter] init");
    }

    /**
     * 过滤方法
     * @param request
     * @param response
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 设置请求的字符编码
        request.setCharacterEncoding(this.encoding);
        // 设置响应的字符编码
        response.setContentType("application/json;charset=" + this.encoding);
        logger.info("[EncodingFilter] before chain - encoding set to {}", this.encoding);
        chain.doFilter(request, response);
        logger.info("[EncodingFilter] after chain");
    }

    /**
     * 销毁方法
     */
    @Override
    public void destroy() {
        logger.info("[EncodingFilter] destroyed");
    }
}
