package com.gymflow.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class Secrets {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";

    private Secrets() {
    }

    /** Mật khẩu tạm 10 ký tự, luôn có chữ và số (đúng quy tắc mật khẩu của hệ thống). */
    public static String temporaryPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        for (int i = 0; i < 3; i++) sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        return sb.toString();
    }

    /** Mã khôi phục 8 ký tự; CSDL chỉ lưu bản băm SHA-256. */
    public static String resetToken() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        return sb.toString();
    }

    public static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.trim().toUpperCase().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
