package com.hewie.blog.utils;

import com.hewie.blog.pojo.HewieUser;
import io.jsonwebtoken.Claims;

import java.util.HashMap;
import java.util.Map;

public class ClaimsUtils {

    public static final  String ID = "id";
    public static final  String USER_NAME = "user_name";
    public static final  String ROLES = "roles";
    public static final  String AVATAR = "avatar";
    public static final  String EMAIL = "email";
    public static final  String SIGN = "sign";
    public static final  String FROM = "from";

    public static Map<String, Object> user2Claims(HewieUser hewieUser, String from) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(ID, hewieUser.getId());
        claims.put(USER_NAME, hewieUser.getUserName());
        claims.put(ROLES, hewieUser.getRoles());
        claims.put(AVATAR, hewieUser.getAvatar());
        claims.put(EMAIL, hewieUser.getEmail());
        claims.put(SIGN, hewieUser.getSign());
        claims.put(FROM, from);

        return claims;
    }

    public static HewieUser claims2User(Claims claims) {
        HewieUser hewieUser = new HewieUser();
        String id = (String) claims.get(ID);
        hewieUser.setId(id);
        String userName = (String) claims.get(USER_NAME);
        hewieUser.setUserName(userName);
        String roles = (String) claims.get(ROLES);
        hewieUser.setRoles(roles);
        String avatar = (String) claims.get(AVATAR);
        hewieUser.setAvatar(avatar);
        String email = (String) claims.get(EMAIL);
        hewieUser.setEmail(email);
        String sign = (String) claims.get(SIGN);
        hewieUser.setSign(sign);

        return hewieUser;
    }

    public static String getFrom(Claims claims) {
        return (String) claims.get(FROM);
    }
}
