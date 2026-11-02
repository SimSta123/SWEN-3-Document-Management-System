package at.technikum.swen3.paperless.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("HTTP method={} path={} remoteAddress={} status={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                response.getStatus());
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("HTTP completed: method={} path={} remoteAddress={} status={} message={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    response.getStatus(),
                    request.getAttribute("logMessage"));
        }
    }
}