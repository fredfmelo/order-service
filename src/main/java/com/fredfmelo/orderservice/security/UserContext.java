package com.fredfmelo.orderservice.security;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class UserContext {

    private ServletRequestAttributes getAtrributes() {
        return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    }

    public UUID getUserId() {
        return UUID.fromString(getAtrributes().getRequest().getHeader("X-User-Id"));
    }

    public String getEmail() {
        return getAtrributes().getRequest().getHeader("X-User-Email");
    }

    public String getRole() {
        return getAtrributes().getRequest().getHeader("X-User-Role");
    }

}