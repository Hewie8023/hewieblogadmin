package com.hewie.blog;

import com.hewie.blog.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TestCreateToken {
    public static void main(String[] args) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", "868509148860383232");
        claims.put("user_name", "test");
        claims.put("role", "role_normal");
        claims.put("avatar", "https://cdn.sunofbeaches.com/images/default_avatar.png");

        String token = JwtUtil.createToken(claims);
        log.info(token);
    }
}
