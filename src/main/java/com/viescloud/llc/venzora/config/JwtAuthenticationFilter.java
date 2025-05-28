package com.viescloud.llc.venzora.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.eco.viesspringutils.model.filter.MutableHttpServletRequestWrapper;
import com.viescloud.llc.venzora.model.authentication.DefaultEndpointEnum;
import com.viescloud.llc.venzora.service.authentication.JwtTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final ApplicationProperties applicationProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        //if request is options request, just let it pass
        if(request.getMethod().equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        var requestURI = request.getRequestURI();
        if(DefaultEndpointEnum.isDefaultEndpoint(requestURI) || 
                requestURI.contains("favicon") || 
                requestURI.contains("swagger") || 
                requestURI.contains("api-docs") ||
                requestURI.contains("actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (jwt != null && jwt.startsWith("Bearer ")) {
            try {
                var user = jwtTokenService.tryCheckIsValidToken(jwt).orElse(null);
                if(user == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Authorization header is missing or invalid");
                    return;
                }

                String userId = user.getId() + "";
                var modifiedRequest = new MutableHttpServletRequestWrapper(request);
                modifiedRequest.putHeader("user_id", userId);
                filterChain.doFilter(modifiedRequest, response);
                return;
            } 
            catch (ResponseStatusException ex) {
                throw ex;
            }
            catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token");
                return;
            }
        }
        else if(applicationProperties.getEnv().equals("local")) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Authorization header is missing or invalid");
        return;
    }
    
}
