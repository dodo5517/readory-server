package me.dodo.readingnotes.util;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageResizer {
    private static final int MAX_WIDTH = 325;
    private static final int MAX_HEIGHT = 272;

    // 비율 유지하면서 리사이징
    public byte[] resizeImageKeepRatio(MultipartFile image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(image.getInputStream())
                .size(MAX_WIDTH, MAX_HEIGHT)  // 비율 유지하며 이 크기 안에 맞춤
                .keepAspectRatio(true)
                .outputFormat(getFormat(image.getOriginalFilename()))
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }

    private String getFormat(String filename) {
        if (filename == null) return "jpg";
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "png" -> "png";
            case "gif" -> "gif";
            case "webp" -> "webp";
            default -> "jpg";
        };
    }
}
