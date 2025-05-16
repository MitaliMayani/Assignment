import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.RsponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@SpringBootApplication
@RestController
@RequestMapping("/api/shorten")

public class Main{
    private final String pocketbaseUrl = "Your_PostBase_Url";
    private final String collectionName = "Short_Url";
    private final RestTemplate restTemplate = new RestTemplate;
    private final Random random = new Random();
    private final String allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final int shortCodeLength = 7;

    public static void main(String[] args) {
        Main.run(Main.class, args);
    }

    @postMapping
    public ResponseEntuty<Map<String , String>> shortenUrl(@RequestBody Map<String, String> payload) {
        String longUrl = payload.get("longUrl");
        if (longUrl == null || longUrl.isEmpty()){
            return ResponseEntity.badRequest().body(Map.of("error", "Long URL is required"));
        }

        String shortCode = generateShortCode();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("long_url", longUrl);
        requestBody.put("short_code", shortCode);
        requestBody.put("created_art", LocalDateTime.now().toString());

        try {
            ResponseEntity<Map> pocketbaseResponse = restTemplate.postForEntity(
                    pocketbaseUrl + "/api/collections/" + collectionName + "/records", requestBody, Map.class);

            if (pocketbaseResponse.getStatusCode().is2xxSuccessful()) {
                StringshortUrl = "http://yourdomain.com/" + shortCode;
                return ResponseEntity.ok(Map.of("shortUrl", shortUrl));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).
                        body(Map.of("error", "failed to save to PocketBase"));
            }
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).
                    body(Map.of("error", "Error communicating with PocketBase: " + e.getMessage()));


        }
    }

    private String generateShortCode(){
        StringBuilder sb = new StringBuilder(shortCodeLength);
        for (int i = 0; i<shortCodeLength; i++){
            sb.append(allowedChars.charAt(random.nextInt(allowedChars.length())));
        }
        return sb.toString();
    }

    @GetMapping("/{shortCode}")
    public  ResponseEntity<void> redirect(@PathVariable String shortCode){
        try{
            ResponseEntity<Map> pocketBaseResponse = restTemplate.getForEntity(
                    pocketbaseUrl + "/api/collections/" + collectionName + "/recors?filter=(sort_code='{shortCode}')",
                    Map.class, shortCode
            );

            if (pocketBaseResponse.getStatusCode(.is2xxSuccessful() &&
            pocketBaseResponse.getBody() != null &&
            pocketBaseResponse.getBody().containsKey("items") &&
            !((java.util.List) pocketBaseResponse.getBody().get("items")).isEmpty()) {
                Map<String, Object> record  = (Map<String, Object>) ((java.util.List) pocketBaseResponse.getBody().get("items")).get(0);
                String longUrl = (String) record.get("long_url");
                return  ResponseEntity.status(HttpStatus.FOUND).header("Location", longUrl).build();
            }
            else {
                return ResponseEntity.notFound().build();
            }
        }
        catch (Exception e){
            return ResponseEntity.status(HttpsStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



}