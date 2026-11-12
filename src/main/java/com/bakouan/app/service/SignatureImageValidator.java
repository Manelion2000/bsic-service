package com.bakouan.app.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;
import java.util.Set;

@Component
public class SignatureImageValidator {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final int MAX_SOURCE_DIMENSION = 4096;
    public static final long MAX_SOURCE_PIXELS = 12_000_000;
    public static final int MAX_NORMALIZED_WIDTH = 900;
    public static final int MAX_NORMALIZED_HEIGHT = 300;
    private static final Set<String> FORMATS = Set.of("PNG", "JPEG", "JPG", "WEBP", "BMP", "GIF", "TIFF", "TIF");
    private static final String FORMAT_HELP = "PNG, JPEG, WebP, BMP, GIF fixe ou TIFF à une seule page";

    /** Décode le contenu réel, borne la mémoire et réencode sans métadonnées. */
    public byte[] normalize(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES)
            throw invalid("Choisissez une signature " + FORMAT_HELP + " de 2 Mo maximum.");
        byte[] content;
        try { content = file.getBytes(); }
        catch (IOException e) { throw invalid("Lecture de la signature impossible."); }
        if (content.length > MAX_BYTES) throw invalid("La signature ne doit pas dépasser 2 Mo.");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid("Le fichier n'est pas une image valide. Formats acceptés : " + FORMAT_HELP + ".");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toUpperCase(Locale.ROOT);
                if (!FORMATS.contains(format)) throw invalid("Formats acceptés : " + FORMAT_HELP + ".");
                rejectAnimation(content, format);
                reader.setInput(input, false, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 20 || height < 10 || width > MAX_SOURCE_DIMENSION || height > MAX_SOURCE_DIMENSION || (long) width * height > MAX_SOURCE_PIXELS)
                    throw invalid("Dimensions invalides : au moins 20 × 10 pixels, au plus 4096 pixels par côté et 12 mégapixels.");
                if (reader.getNumImages(true) != 1) throw invalid("La signature doit contenir une seule image fixe. Les animations et les fichiers multipages ne sont pas acceptés.");
                BufferedImage source = reader.read(0);
                int left = width, top = height, right = -1, bottom = -1, ink = 0;
                BufferedImage transparent = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                    int pixel = source.getRGB(x, y), alpha = pixel >>> 24;
                    boolean white = ((pixel >> 16) & 255) > 245 && ((pixel >> 8) & 255) > 245 && (pixel & 255) > 245;
                    if (alpha > 20 && !white) {
                        transparent.setRGB(x, y, pixel); ink++;
                        left = Math.min(left, x); right = Math.max(right, x); top = Math.min(top, y); bottom = Math.max(bottom, y);
                    }
                }
                if (ink < 20) throw invalid("L'image est vide. Importez une signature lisible sur un fond clair ou transparent.");
                left = Math.max(0, left - 8); top = Math.max(0, top - 8);
                right = Math.min(width - 1, right + 8); bottom = Math.min(height - 1, bottom + 8);
                int w = right - left + 1, h = bottom - top + 1;
                double scale = Math.min(1d, Math.min((double) MAX_NORMALIZED_WIDTH / w, (double) MAX_NORMALIZED_HEIGHT / h));
                BufferedImage result = new BufferedImage(Math.max(1, (int) (w * scale)), Math.max(1, (int) (h * scale)), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = result.createGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.drawImage(transparent, 0, 0, result.getWidth(), result.getHeight(), left, top, right + 1, bottom + 1, null);
                } finally { g.dispose(); }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (!ImageIO.write(result, "PNG", out) || out.size() > MAX_BYTES) throw invalid("La signature ne peut pas être enregistrée.");
                return out.toByteArray();
            } finally { reader.dispose(); }
        } catch (ResponseStatusException e) { throw e; }
        catch (IOException | RuntimeException e) { throw invalid("Image illisible ou endommagée. Importez une autre signature."); }
    }

    // Les lecteurs PNG classiques voient seulement l'image par défaut d'un APNG.
    private void rejectAnimation(byte[] content, String format) {
        if ("PNG".equals(format)) {
            for (int offset = 8; offset + 12 <= content.length;) {
                long length = unsignedInt(content, offset, false);
                if (length > content.length - offset - 12) break;
                if (chunk(content, offset + 4, "acTL") && length >= 8 && unsignedInt(content, offset + 8, false) > 1) throw animation();
                offset += (int) length + 12;
            }
        } else if ("WEBP".equals(format)) {
            for (int offset = 12; offset + 8 <= content.length;) {
                long length = unsignedInt(content, offset + 4, true);
                if (length > content.length - offset - 8) break;
                if (chunk(content, offset, "VP8X") && length > 0 && (content[offset + 8] & 2) != 0) throw animation();
                offset += (int) length + 8 + (int) (length & 1);
            }
        }
    }
    private long unsignedInt(byte[] bytes, int offset, boolean littleEndian) {
        long value = 0;
        for (int i = 0; i < 4; i++) value = (value << 8) | (bytes[offset + (littleEndian ? 3 - i : i)] & 255);
        return value;
    }
    private boolean chunk(byte[] bytes, int offset, String name) {
        for (int i = 0; i < 4; i++) if (bytes[offset + i] != name.charAt(i)) return false;
        return true;
    }
    private ResponseStatusException animation() { return invalid("La signature doit être une image fixe. Les animations ne sont pas acceptées."); }

    private ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
