package me.dodo.readingnotes.util;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Iterator;

public class ImageValidator {

    public static void validateMagicBytes(MultipartFile image) {
        byte[] header;
        try (var is = image.getInputStream()) {
            header = is.readNBytes(12);
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.");
        }

        if (isJpeg(header) || isPng(header) || isWebP(header)) {
            return;
        }
        throw new IllegalArgumentException("지원하지 않거나 위조된 이미지 형식입니다.");
    }

    public static void validateDimensions(MultipartFile image) {
        ImageInputStream iis = null;
        ImageReader reader = null;
        try {
            iis = ImageIO.createImageInputStream(image.getInputStream());
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return; // JDK가 못 읽는 포맷(예: 환경에 따라 webp) — 차원 검증 건너뜀
            }
            reader = readers.next();
            reader.setInput(iis);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            if ((long) width * height > 64_000_000L) {
                throw new IllegalArgumentException("이미지 해상도가 너무 큽니다.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");
        } finally {
            if (reader != null) reader.dispose();
            if (iis != null) {
                try { iis.close(); } catch (IOException ignored) {}
            }
        }
    }

    private static boolean isJpeg(byte[] h) {
        return h.length >= 3
                && (h[0] & 0xFF) == 0xFF
                && (h[1] & 0xFF) == 0xD8
                && (h[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] h) {
        return h.length >= 8
                && (h[0] & 0xFF) == 0x89
                && (h[1] & 0xFF) == 0x50
                && (h[2] & 0xFF) == 0x4E
                && (h[3] & 0xFF) == 0x47
                && (h[4] & 0xFF) == 0x0D
                && (h[5] & 0xFF) == 0x0A
                && (h[6] & 0xFF) == 0x1A
                && (h[7] & 0xFF) == 0x0A;
    }

    private static boolean isWebP(byte[] h) {
        return h.length >= 12
                && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
    }
}
