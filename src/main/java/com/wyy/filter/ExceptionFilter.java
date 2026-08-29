package com.wyy.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * 全局异常过滤器
 * 位于过滤器链最外层，兜底捕获后续 Filter 与 Servlet 抛出的所有未处理异常：
 * 记录完整堆栈日志（附带请求路径与方法），并向客户端返回统一的 JSON 错误响应，
 * 避免 Tomcat 默认错误页直接暴露堆栈、SQL 等内部信息
 * <p>
 * 通过 HttpServletResponseWrapper 包装响应：下游 Filter/Servlet 写入的内容先缓存到内存，
 * sendRedirect/sendError/flush 等提交动作全部延迟生效，响应不会在链深处提前提交，
 * 因此捕获异常后总能 reset 并整体重写为统一错误响应
 */
public class ExceptionFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("[ExceptionFilter] init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        // 包装响应传给下游：内容写入、重定向、错误页全部延迟，避免响应提前提交
        BufferedResponseWrapper wrappedResp = new BufferedResponseWrapper(resp);
        try {
            chain.doFilter(request, wrappedResp);
            // 正常流程：将缓冲的状态码、响应头、内容一次性写回真实响应
            wrappedResp.flushTo(resp);
        } catch (Exception e) {
            // 记录完整堆栈，附带请求路径与方法便于排查定位
            logger.error("[ExceptionFilter] 请求处理异常, uri: {}, method: {}", req.getRequestURI(), req.getMethod(), e);
            // 包装后真实响应必然未提交，可放心清空并统一返回 JSON 错误响应
            resp.reset();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":500,\"message\":\"系统繁忙，请稍后重试\"}");
        }
    }

    @Override
    public void destroy() {
        logger.info("[ExceptionFilter] destroy");
    }

    /**
     * 响应包装器
     * 拦截 getWriter/getOutputStream，将下游写入的内容缓存到内存缓冲；
     * 重写 flushBuffer/sendRedirect/sendError 为延迟生效，确保响应不会提前提交
     */
    private static class BufferedResponseWrapper extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private ServletOutputStream outputStream;
        private PrintWriter writer;

        public BufferedResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            // 与真实响应行为一致：getWriter 与 getOutputStream 只能二选一
            if (writer != null) {
                throw new IllegalStateException("getWriter() 已被调用，不能同时获取 OutputStream");
            }
            if (outputStream == null) {
                this.outputStream = new ServletOutputStream() {
                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener writeListener) {
                    }

                    @Override
                    public void write(int b) {
                        buffer.write(b);
                    }
                };
            }
            return outputStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            // 与真实响应行为一致：getWriter 与 getOutputStream 只能二选一
            if (outputStream != null) {
                throw new IllegalStateException("getOutputStream() 已被调用，不能同时获取 Writer");
            }
            if (writer == null) {
                // 使用与真实响应一致的字符编码；未设置时默认 UTF-8，避免中文乱码
                String charset = getCharacterEncoding();
                if (charset == null) {
                    charset = "UTF-8";
                }
                this.writer = new PrintWriter(new OutputStreamWriter(buffer, charset));
            }
            return writer;
        }

        @Override
        public void flushBuffer() throws IOException {
            // 重写为空操作：内容只进内存缓冲，不向真实响应刷新，防止响应提前提交
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            // 延迟生效：只记录 302 状态与 Location 头，由 flushTo 统一发送
            // 注意：location 需为完整路径，调用方需自行拼接上下文路径（与容器默认行为一致）
            setStatus(HttpServletResponse.SC_FOUND);
            setHeader("Location", location);
        }

        @Override
        public void sendError(int sc) throws IOException {
            // 延迟生效：只记录错误状态码，不立即提交错误页
            setStatus(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            setStatus(sc);
        }

        @Override
        public void reset() {
            // 同时清空内容缓冲与真实响应的状态码、响应头
            buffer.reset();
            super.reset();
        }

        @Override
        public void resetBuffer() {
            buffer.reset();
            super.resetBuffer();
        }

        /**
         * 将缓冲的状态码、响应头与内容一次性写回真实响应
         * 只有此方法会真正提交响应，保证异常场景下响应必然未提交
         */
        public void flushTo(HttpServletResponse response) throws IOException {
            // 状态码为 4xx/5xx 且无任何内容时，转交容器错误页机制（web.xml 的 error-page 配置）
            if (getStatus() >= HttpServletResponse.SC_BAD_REQUEST && buffer.size() == 0) {
                response.sendError(getStatus());
                return;
            }
            response.setStatus(getStatus());
            // 拷贝下游设置的响应头（Content-Type、Set-Cookie 等）
            for (String name : getHeaderNames()) {
                for (String value : getHeaders(name)) {
                    response.addHeader(name, value);
                }
            }
            // 确保 Writer 中的字符全部刷入缓冲
            if (writer != null) {
                writer.flush();
            }
            response.setContentLength(buffer.size());
            response.getOutputStream().write(buffer.toByteArray());
            response.getOutputStream().flush();
        }
    }
}
