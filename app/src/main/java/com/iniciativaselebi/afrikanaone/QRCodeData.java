package com.iniciativaselebi.afrikanaone;
public class QRCodeData {
    private String qrCodeKey;
    private String userId;
    private String base64QRCode;
    private String name;
    private String email;
    private int lastGameScore;
    private int lastGamePuntuacion;
    private String timestamp;

    public QRCodeData(String qrCodeKey, String userId, String base64QRCode, String name, String email, int lastGameScore, int lastGamePuntuacion, String timestamp) {
        this.qrCodeKey = qrCodeKey;
        this.userId = userId;
        this.base64QRCode = base64QRCode;
        this.name = name;
        this.email = email;
        this.lastGameScore = lastGameScore;
        this.lastGamePuntuacion = lastGamePuntuacion;
        this.timestamp = timestamp;
    }

    public String getQrCodeKey() {
        return qrCodeKey;
    }

    public void setQrCodeKey(String qrCodeKey) {
        this.qrCodeKey = qrCodeKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBase64QRCode() {
        return base64QRCode;
    }

    public void setBase64QRCode(String base64QRCode) {
        this.base64QRCode = base64QRCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getLastGameScore() {
        return lastGameScore;
    }

    public void setLastGameScore(int lastGameScore) {
        this.lastGameScore = lastGameScore;
    }

    public int getLastGamePuntuacion() {
        return lastGamePuntuacion;
    }

    public void setLastGamePuntuacion(int lastGamePuntuacion) {
        this.lastGamePuntuacion = lastGamePuntuacion;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // Add getters and setters for each field
}

