package com.gui.kline.server.security;

import com.gui.kline.server.service.DeviceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * API Key Authentication Filter for validating device API keys in incoming requests.
 * This is used for device-to-server communication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private final DeviceService deviceService;
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String DEVICE_ID_HEADER = "X-DEVICE-ID";
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        final String apiKey = request.getHeader(API_KEY_HEADER);
        final String deviceId = request.getHeader(DEVICE_ID_HEADER);
        
        // For sync endpoints, also check query parameters
        String apiKeyParam = request.getParameter("apiKey");
        String deviceIdParam = request.getParameter("deviceId");
        
        String effectiveApiKey = apiKey != null ? apiKey : apiKeyParam;
        String effectiveDeviceId = deviceId != null ? deviceId : deviceIdParam;
        
        if (effectiveApiKey == null || effectiveDeviceId == null) {
            // No API key provided, continue with other authentication methods
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Validate the device and API key
            boolean isValid = deviceService.validateDeviceApiKey(effectiveApiKey);
            
            if (isValid) {
                // Create authentication token for the device
                UserDetails userDetails = createDeviceUserDetails(effectiveDeviceId, effectiveApiKey);
                
                UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                
                // Update device activity
                deviceService.updateActivity(effectiveDeviceId);
            }
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("API Key Authentication Error for device {}: {}", effectiveDeviceId, e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key or Device ID");
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Don't filter if already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return true;
        }
        
        // Don't filter public endpoints that don't require device authentication
        String path = request.getRequestURI();
        return path.contains("/swagger") || 
               path.contains("/v3/api-docs") ||
               path.contains("/webjars");
    }
    
    /**
     * Create UserDetails for a device
     */
    private UserDetails createDeviceUserDetails(String deviceId, String apiKey) {
        // Devices have DEVICE role with limited permissions
        return User.builder()
                .username("device:" + deviceId)
                .password("") // Not used for API key authentication
                .roles("DEVICE")
                .authorities(Collections.emptyList())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}