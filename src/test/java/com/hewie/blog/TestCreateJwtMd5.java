package com.hewie.blog;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public class TestCreateJwtMd5 {
    public static void main(String[] args) {
        String jwtKeyMd5 = DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8));
        System.out.println(jwtKeyMd5);
    }
}
