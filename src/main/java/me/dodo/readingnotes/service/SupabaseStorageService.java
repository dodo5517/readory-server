package me.dodo.readingnotes.service;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

//@Service
public class SupabaseStorageService {

    @Value("${supabase.storage.url}")
    private String supabaseUrl;

    @Value("${supabase.storage.key}")
    private String supabaseKey;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    private final RestTemplate restTemplate;

    public SupabaseStorageService() {
        this.restTemplate = new RestTemplate();
    }

    // 파일 업로드
    public String uploadFile(byte[] image, String fileName, String contentType) {
        String url = supabaseUrl + "/" + bucket + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.setContentType(MediaType.parseMediaType(contentType));

        HttpEntity<byte[]> entity = new HttpEntity<>(image, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

        return getPublicUrl(fileName);
    }

    // 파일 다운로드
    public byte[] downloadFile(String fileName) {
        String url = supabaseUrl + "/" + bucket + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, byte[].class);

        return response.getBody();
    }

    // 파일 삭제
    public void deleteFile(String fileName) {
        String url = supabaseUrl + "/" + bucket + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
    }

    // Public URL 생성
    public String getPublicUrl(String fileName) {
        return (supabaseUrl + "/" + bucket + "/" + fileName);
    }
}