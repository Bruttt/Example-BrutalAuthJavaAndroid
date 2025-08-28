package com.Bruttt.brutalauth;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * BrutalAuth Java client for ANDROID ONLY (JSON over HTTPS).
 *
 * Endpoints:
 *   POST https://{host}/register-user  { licenseKey, username, password, hwid, applicationId }
 *   POST https://{host}/login-user     { username, password, hwid, applicationId }
 *
 * NOTE: Do NOT call from the main thread (use a background thread/Coroutine/WorkManager).
 */
public final class BrutalAuth {

    private final String applicationId;
    private final String baseUrl;
    private final Context appContext;


    public BrutalAuth(Context context, String applicationId) {
        this(context, applicationId, "https://api.brutalauth.site");
    }

    public BrutalAuth(Context context, String applicationId, String hostOrBaseUrl) {
        this.appContext = context.getApplicationContext();
        this.applicationId = applicationId;

        // Normalize: allow "api.example.com" or full URL
        String url = hostOrBaseUrl == null ? "" : hostOrBaseUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        this.baseUrl = url;
    }

    public AuthResult registerUser(String licenseKey, String username, String password) {
        try {
            String hwid = HWID.getSalted(appContext, applicationId);
            JSONObject body = new JSONObject()
                    .put("licenseKey", licenseKey)
                    .put("username", username)
                    .put("password", password)
                    .put("hwid", hwid)
                    .put("applicationId", applicationId);

            JSONObject res = postJson("/register-user", body);
            return AuthResult.fromResponse(res);
        } catch (Exception e) {
            return AuthResult.error("HTTP error during register: " + e.getMessage(), -1, null);
        }
    }

    public AuthResult loginUser(String username, String password) {
        try {
            String hwid = HWID.getSalted(appContext, applicationId);
            JSONObject body = new JSONObject()
                    .put("username", username)
                    .put("password", password)
                    .put("hwid", hwid)
                    .put("applicationId", applicationId);

            JSONObject res = postJson("/login-user", body);
            return AuthResult.fromResponse(res);
        } catch (Exception e) {
            return AuthResult.error("HTTP error during login: " + e.getMessage(), -1, null);
        }
    }

    public boolean registerUserOk(String licenseKey, String username, String password) {
        return registerUser(licenseKey, username, password).success;
    }
    public boolean loginUserOk(String username, String password) {
        return loginUser(username, password).success;
    }



    private JSONObject postJson(String path, JSONObject jsonBody) throws IOException {
        byte[] payload = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
        String fullUrl = baseUrl + path;

        URL url = new URL(fullUrl);
        URLConnection uc = url.openConnection();
        if (!(uc instanceof HttpURLConnection)) {
            throw new IOException("Not an HTTP(S) URL: " + fullUrl);
        }

        HttpURLConnection c = (HttpURLConnection) uc;


        if (c instanceof HttpsURLConnection) {
        }

        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", "BrutalAuth/1.1-Android");
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        c.setRequestProperty("Accept", "application/json");
        c.setFixedLengthStreamingMode(payload.length);

        try (OutputStream os = c.getOutputStream()) {
            os.write(payload);
        }

        int code = c.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
        String resp = readAll(is);
        if (resp == null) resp = "";

        resp = resp.replace("\uFEFF", "").trim();

        try {
            if (resp.isEmpty()) {
                JSONObject obj = new JSONObject();
                obj.put("success", false);
                obj.put("error", "Empty response");
                obj.put("status", code);
                return obj;
            }

            if (resp.startsWith("[")) {
                JSONObject obj = new JSONObject();
                obj.put("success", code >= 200 && code < 300);
                obj.put("data", new JSONArray(resp));
                obj.put("status", code);
                return obj;
            }

            if (resp.startsWith("<")) {
                JSONObject obj = new JSONObject();
                obj.put("success", false);
                obj.put("error", "Non-JSON response (HTML)");
                obj.put("status", code);
                obj.put("bodyPreview", resp.substring(0, Math.min(400, resp.length())));
                return obj;
            }

            JSONObject obj = new JSONObject(resp);
            if (!obj.has("success")) obj.put("success", code >= 200 && code < 300);
            obj.put("status", code);
            return obj;

        } catch (Exception ex) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("success", false);
                obj.put("error", "JSON parse error: " + ex.getMessage());
                obj.put("status", code);
                obj.put("bodyPreview", resp.substring(0, Math.min(400, resp.length())));
            } catch (Exception ignored) {}
            return obj;
        } finally {
            c.disconnect();
        }
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    public static final class AuthResult {
        public final boolean success;
        public final String message;
        public final int httpStatus;
        public final JSONObject raw;

        private AuthResult(boolean success, String message, int httpStatus, JSONObject raw) {
            this.success = success;
            this.message = message;
            this.httpStatus = httpStatus;
            this.raw = raw;
        }

        public static AuthResult error(String msg, int status, JSONObject raw) {
            return new AuthResult(false, msg, status, raw);
        }

        public static AuthResult fromResponse(JSONObject res) {
            if (res == null) return error("Null response", -1, null);
            boolean ok = res.optBoolean("success", false);
            int status = res.optInt("status", -1);


            String msg = res.optString("message",
                    res.optString("error",
                            res.optString("msg", ok ? "OK" : "Request failed")));

            return new AuthResult(ok, msg, status, res);
        }
    }

    public static final class HWID {
        private static final char[] HEX = "0123456789ABCDEF".toCharArray();

        /** Returns hex( SHA-256( applicationId + "::" + ANDROID_ID + "::" + Build-sig ) ). */
        public static String getSalted(Context context, String applicationId) {
            byte[] raw = generateSalted(context, applicationId);
            return bytesToHex(raw);
        }

        public static byte[] generateSalted(Context context, String applicationId) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");

                String androidId = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );
                if (androidId == null) androidId = "unknown";

                String buildSig =
                        safe(Build.BOARD) + "|" +
                                safe(Build.BRAND) + "|" +
                                safe(Build.DEVICE) + "|" +
                                safe(Build.MANUFACTURER) + "|" +
                                safe(Build.MODEL) + "|" +
                                safe(Build.PRODUCT) + "|" +
                                safe(Build.HARDWARE) + "|" +
                                safe(Build.FINGERPRINT);

                String material = String.valueOf(applicationId) + "::" + androidId + "::" + buildSig;
                return md.digest(material.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new Error("SHA-256 algorithm not found", e);
            }
        }

        private static String safe(String s) { return (s == null) ? "" : s; }

        public static String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 2];
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2]     = HEX[v >>> 4];
                hexChars[j * 2 + 1] = HEX[v & 0x0F];
            }
            return new String(hexChars);
        }
    }
}
