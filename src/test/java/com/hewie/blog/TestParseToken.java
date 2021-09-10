package com.hewie.blog;

import com.hewie.blog.utils.JwtUtil;
import io.jsonwebtoken.Claims;

public class TestParseToken {
    public static void main(String[] args) {
        Claims claims = JwtUtil.parseJWT("eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoicm9sZV9ub3JtYWwiLCJ1c2VyX25hbWUiOiJ0ZXN0IiwiaWQiOiI4Njg1MDkxNDg4NjAzODMyMzIiLCJhdmF0YXIiOiJodHRwczovL2Nkbi5zdW5vZmJlYWNoZXMuY29tL2ltYWdlcy9kZWZhdWx0X2F2YXRhci5wbmciLCJleHAiOjE2MjcxNDc1NDd9.lYOTnMqLzuLPMqtD_-zg6AKh3Y2NdA8DAYAgZAnaKb8");
        String userId = (String) claims.get("id");
        System.out.println(userId);
    }
}
