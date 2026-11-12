package com.bakouan.app.service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.security.MessageDigest;
import java.util.HexFormat;

final class SignatureTestImages {
    static byte[] png(int variant) throws Exception {
        BufferedImage image = new BufferedImage(400, 130, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(22, 52, 98)); g.setStroke(new BasicStroke(3f));
        g.drawArc(30 + variant, 20, 140, 75, 10, 310); g.drawLine(60, 105, 300 - variant, 30); g.drawArc(140, 35, 120, 60, 190, 260); g.drawLine(50, 105, 340, 105);
        g.dispose(); ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(image, "PNG", out); return out.toByteArray();
    }
    static void seed(Connection connection, String user) throws Exception {
        byte[] png = png(0);
        try (var p = connection.prepareStatement("INSERT INTO ba_user_signature(user_id,contenu,empreinte,updated_date) VALUES (?,?,?,now()) ON CONFLICT(user_id) DO UPDATE SET contenu=EXCLUDED.contenu,empreinte=EXCLUDED.empreinte,updated_date=EXCLUDED.updated_date")) {
            p.setString(1, user); p.setBytes(2, png); p.setString(3, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png))); p.executeUpdate();
        }
    }
}
