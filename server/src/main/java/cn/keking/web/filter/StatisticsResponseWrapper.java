package cn.keking.web.filter;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * 统计响应包装器
 * 用于包装HttpServletResponse，以便获取响应状态码
 * 
 * @author kkfileview
 */
public class StatisticsResponseWrapper extends HttpServletResponseWrapper {

    private int status = 200;

    public StatisticsResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void setStatus(int sc) {
        super.setStatus(sc);
        this.status = sc;
    }

    @Override
    public void sendError(int sc) throws IOException {
        super.sendError(sc);
        this.status = sc;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        super.sendError(sc, msg);
        this.status = sc;
    }

    public int getStatus() {
        return status;
    }
}