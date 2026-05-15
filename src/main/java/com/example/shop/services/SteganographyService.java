package com.example.shop.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module 3 — Steganography + AES-256 Encryption
 *
 * Flow ENCRYPT:
 *   1. AES-256-CBC encrypt the payload (text or file bytes as base64)
 *   2. Call Gemini to generate a themed cover image
 *   3. LSB-embed the ciphertext into the image pixels
 *   4. Generate a one-time key token — stored in memory for 30 seconds only
 *   5. Return stego-PNG + key token (key shown once, then gone forever)
 *
 * Flow DECRYPT:
 *   1. LSB-extract the ciphertext from the uploaded stego-PNG
 *   2. Look up the key token (must be within 30 s window)
 *   3. AES-256-CBC decrypt → original payload
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SteganographyService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    // ── Key store: token → (aesPassword, expiresAt) ──────────────────────
    private static final Map<String, KeyEntry> keyStore = new ConcurrentHashMap<>();
    private record KeyEntry(String aesPassword, Instant expiresAt) {}

    private static final int    KEY_TTL_SECONDS = 30;
    private static final String AES_ALGO        = "AES/CBC/PKCS5Padding";
    private static final String LSB_END_MARKER  = "<<STEGO_END>>";
    // Imagen 3 — stable image generation
    private static final String GEMINI_IMAGEN_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "imagen-3.0-generate-002:predict?key=";

    // gemini-2.0-flash-exp — fallback multimodal image output
    private static final String GEMINI_FLASH_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.0-flash-exp:generateContent?key=";

    // ═════════════════════════════════════════════════════════════════════
    //  ENCRYPT  — returns { stegoImageBase64, key, expiresIn, mimeType }
    // ═════════════════════════════════════════════════════════════════════

    /**
     * @param payloadText  text to encrypt (for text mode)
     * @param fileBytes    raw file bytes (for file/image mode), may be null
     * @param fileName     original file name, used to restore on decrypt
     * @param imageTheme   prompt hint for Gemini cover image
     */
    public Map<String, Object> encryptAndEmbed(
            String payloadText,
            byte[] fileBytes,
            String fileName,
            String imageTheme) throws Exception {

        // 1. Build payload string: for files, base64-encode bytes with header
        String payload;
        boolean isFile = fileBytes != null && fileBytes.length > 0;
        if (isFile) {
            String b64 = Base64.getEncoder().encodeToString(fileBytes);
            payload = "FILE:" + fileName + ":" + b64;
        } else {
            payload = "TEXT:" + payloadText;
        }

        // 2. AES-256 encrypt
        SecureRandom rng = new SecureRandom();
        byte[] salt = new byte[16]; rng.nextBytes(salt);
        byte[] iv   = new byte[16]; rng.nextBytes(iv);
        String aesPassword = UUID.randomUUID().toString().replace("-", "");

        SecretKey secretKey = deriveKey(aesPassword, salt);
        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        // Pack salt(16)+iv(16)+ciphertext → Base64 string to embed
        byte[] packed = new byte[32 + encrypted.length];
        System.arraycopy(salt, 0, packed, 0, 16);
        System.arraycopy(iv,   0, packed, 16, 16);
        System.arraycopy(encrypted, 0, packed, 32, encrypted.length);
        String ciphertextB64 = Base64.getEncoder().encodeToString(packed);
        String toEmbed = ciphertextB64 + LSB_END_MARKER;

        // 3. Generate cover image via Gemini
        byte[] coverPng = generateCoverImage(imageTheme, isFile, fileName);

        // 4. LSB-embed the ciphertext into the image
        byte[] stegoPng = lsbEmbed(coverPng, toEmbed);

        // 5. One-time key token — 30 s TTL
        String keyToken = generateKeyToken();
        keyStore.put(keyToken, new KeyEntry(aesPassword, Instant.now().plusSeconds(KEY_TTL_SECONDS)));
        log.info("Stego encrypted: keyToken={}, payloadLen={}, isFile={}", keyToken, payload.length(), isFile);

        return Map.of(
                "stegoImageBase64", Base64.getEncoder().encodeToString(stegoPng),
                "mimeType",         "image/png",
                "key",              keyToken,
                "expiresIn",        KEY_TTL_SECONDS,
                "isFile",           isFile,
                "fileName",         fileName != null ? fileName : "secret.txt"
        );
    }

    // ═════════════════════════════════════════════════════════════════════
    //  DECRYPT  — returns { success, plaintext OR fileBase64+fileName }
    // ═════════════════════════════════════════════════════════════════════

    public Map<String, Object> decryptFromImage(byte[] stegoImageBytes, String keyToken) {
        purgeExpired();
        String key = normalizeKey(keyToken);
        KeyEntry entry = keyStore.get(key);

        if (entry == null) {
            return Map.of("success", false,
                    "error", "Ключ не знайдено або термін дії минув. На цій платформі розшифрування неможливе.");
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            keyStore.remove(key);
            return Map.of("success", false,
                    "error", "Термін дії ключа минув (30 секунд). Розшифрування на платформі неможливе.");
        }

        try {
            // 1. LSB-extract
            String extracted = lsbExtract(stegoImageBytes);
            int endIdx = extracted.indexOf(LSB_END_MARKER);
            if (endIdx < 0) return Map.of("success", false, "error", "У зображенні не знайдено прихованих даних.");
            String ciphertextB64 = extracted.substring(0, endIdx);

            // 2. Unpack salt+iv+ciphertext
            byte[] packed    = Base64.getDecoder().decode(ciphertextB64);
            byte[] salt      = Arrays.copyOfRange(packed, 0, 16);
            byte[] iv        = Arrays.copyOfRange(packed, 16, 32);
            byte[] encrypted = Arrays.copyOfRange(packed, 32, packed.length);

            // 3. AES decrypt
            SecretKey secretKey = deriveKey(entry.aesPassword(), salt);
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            String payload = new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);

            log.info("Stego decrypted: keyToken={}, payloadLen={}", key, payload.length());

            // 4. Parse payload
            if (payload.startsWith("FILE:")) {
                // FILE:<fileName>:<base64data>
                int firstColon = payload.indexOf(':', 5);
                String origName = payload.substring(5, firstColon);
                String fileB64  = payload.substring(firstColon + 1);
                return Map.of("success", true, "type", "file",
                        "fileName", origName, "fileBase64", fileB64);
            } else {
                // TEXT:<plaintext>
                String text = payload.startsWith("TEXT:") ? payload.substring(5) : payload;
                return Map.of("success", true, "type", "text", "plaintext", text);
            }

        } catch (Exception e) {
            log.error("Decrypt error: {}", e.getMessage());
            return Map.of("success", false, "error", "Помилка розшифрування. Перевірте ключ та зображення.");
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Key status check (for countdown timer on frontend)
    // ═════════════════════════════════════════════════════════════════════

    public Map<String, Object> keyStatus(String keyToken) {
        purgeExpired();
        KeyEntry entry = keyStore.get(normalizeKey(keyToken));
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return Map.of("alive", false, "remainingSeconds", 0);
        }
        long remaining = entry.expiresAt().getEpochSecond() - Instant.now().getEpochSecond();
        return Map.of("alive", true, "remainingSeconds", Math.max(0, remaining));
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Gemini image generation
    // ═════════════════════════════════════════════════════════════════════

    private byte[] generateCoverImage(String theme, boolean isFile, String fileName) {
        String prompt = buildImagePrompt(theme, isFile, fileName);

        // ── Attempt 1: Imagen 3 (predict API) ──────────────────────────────
        try {
            Map<String, Object> requestBody = Map.of(
                    "instances",  List.of(Map.of("prompt", prompt)),
                    "parameters", Map.of(
                            "sampleCount",       1,
                            "aspectRatio",       "4:3",
                            "safetyFilterLevel", "block_few",
                            "personGeneration",  "allow_adult"
                    )
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    GEMINI_IMAGEN_URL + apiKey,
                    new HttpEntity<>(requestBody, headers), String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode preds = root.path("predictions");
            if (preds.isArray() && preds.size() > 0) {
                String b64 = preds.get(0).path("bytesBase64Encoded").asText("");
                if (!b64.isBlank()) {
                    log.info("Imagen 3 generated cover image OK");
                    return Base64.getDecoder().decode(b64);
                }
            }
            log.warn("Imagen 3 returned no predictions: {}", root);
        } catch (Exception e) {
            log.warn("Imagen 3 failed: {}", e.getMessage());
        }

        // ── Attempt 2: gemini-2.0-flash-exp multimodal ────────────────────
        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text",
                                    "Create a beautiful, highly detailed, vivid photorealistic image of: " + prompt +
                                            ". High quality, artistic, colorful, sharp focus, 4K.")))),
                    "generationConfig", Map.of(
                            "responseModalities", List.of("IMAGE"),
                            "temperature", 1.0
                    )
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    GEMINI_FLASH_URL + apiKey,
                    new HttpEntity<>(requestBody, headers), String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");
            for (JsonNode part : parts) {
                if (part.has("inlineData")) {
                    String b64 = part.path("inlineData").path("data").asText("");
                    if (!b64.isBlank()) {
                        log.info("gemini-flash generated cover image OK");
                        return Base64.getDecoder().decode(b64);
                    }
                }
            }
            log.warn("gemini-flash returned no image; raw: {}", root.toString().substring(0, Math.min(300, root.toString().length())));
        } catch (Exception e) {
            log.warn("gemini-flash failed: {}", e.getMessage());
        }

        // ── Fallback: rich Java2D generative art ──────────────────────────
        log.info("Using Java2D art fallback for theme: {}", theme);
        return generateFallbackImage(theme);
    }

    private String buildImagePrompt(String theme, boolean isFile, String fileName) {
        if (theme != null && !theme.isBlank()) return theme;
        if (isFile && fileName != null) {
            String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')+1).toLowerCase() : "";
            return switch (ext) {
                case "jpg","jpeg","png","gif","webp" -> "A beautiful abstract art piece with vibrant colors, digital art style";
                case "pdf","doc","docx"              -> "An elegant library interior with warm lighting, books and wooden shelves";
                default -> "A serene mountain landscape at golden hour, photorealistic";
            };
        }
        return "A peaceful forest path in autumn, sunlight filtering through leaves, photorealistic";
    }

    /**
     * Rich theme-aware generative art — keyword matched from the full prompt string.
     * Never shows a plain blue gradient; always produces something visually interesting.
     */
    private byte[] generateFallbackImage(String theme) {
        int w = 800, h = 600;
        BufferedImage img  = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY);

        String t = theme != null ? theme.toLowerCase() : "";
        SecureRandom rnd = new SecureRandom();

        if      (t.contains("ocean") || t.contains("beach") || t.contains("sea"))  drawOcean(img,g2,w,h,rnd);
        else if (t.contains("sunset") || t.contains("sunrise") || t.contains("dawn")) drawSunset(img,g2,w,h,rnd);
        else if (t.contains("forest") || t.contains("tree") || t.contains("autumn")) drawForest(img,g2,w,h,rnd);
        else if (t.contains("mountain") || t.contains("snow") || t.contains("peak")) drawMountain(img,g2,w,h,rnd);
        else if (t.contains("city") || t.contains("street") || t.contains("urban")) drawCity(img,g2,w,h,rnd);
        else if (t.contains("abstract") || t.contains("neon") || t.contains("digital")) drawNeon(img,g2,w,h,rnd);
        else if (t.contains("cherry") || t.contains("blossom") || t.contains("sakura")) drawSakura(img,g2,w,h,rnd);
        else drawPlasma(img,g2,w,h,rnd);  // default: flowing plasma

        g2.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ── Ocean: aurora bands + bokeh + light rays ──────────────────────
    private void drawOcean(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        for (int y=0;y<h;y++){float t=(float)y/h;
            g2.setColor(new java.awt.Color((int)(2+t*8),(int)(15+t*60),(int)(90+t*130)));g2.drawLine(0,y,w,y);}
        float[][] bands={{.15f,.22f,0,160,200,120},{.32f,.18f,10,120,210,150},{.52f,.2f,20,80,195,170}};
        for(float[] b:bands){int cy=(int)(b[0]*h),bh=(int)(b[1]*h);
            for(int dy=-bh;dy<bh;dy++){int y=cy+dy;if(y<0||y>=h)continue;
                float a=1f-(float)Math.abs(dy)/bh;a=(float)Math.pow(a,2)*0.5f;
                g2.setColor(new java.awt.Color((int)b[2],(int)b[3],(int)b[4],(int)(a*255)));g2.drawLine(0,y,w,y);}}
        for(int i=0;i<70;i++){int bx=rnd.nextInt(w),by=rnd.nextInt(h),br=4+rnd.nextInt(28),al=18+rnd.nextInt(55);
            g2.setColor(new java.awt.Color(80,190,255,al));g2.fillOval(bx-br,by-br,br*2,br*2);
            g2.setColor(new java.awt.Color(200,240,255,al/3));g2.drawOval(bx-br,by-br,br*2,br*2);}
        g2.setColor(new java.awt.Color(180,230,255,15));
        for(int i=0;i<14;i++){int rx=rnd.nextInt(w);
            g2.fillPolygon(new int[]{rx-6,rx+6,rx+50+rnd.nextInt(50),rx-50-rnd.nextInt(50)},new int[]{0,0,h,h},4);}
    }

    // ── Sunset: layered sky + radial sun + water shimmer ─────────────
    private void drawSunset(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        java.awt.Color[] sky={new java.awt.Color(12,8,35),new java.awt.Color(70,18,75),
                new java.awt.Color(195,55,28),new java.awt.Color(238,125,18),new java.awt.Color(255,198,75)};
        for(int y=0;y<h;y++){float t=(float)y/h;int s=Math.min((int)(t*(sky.length-1)),sky.length-2);float lt=t*(sky.length-1)-s;
            java.awt.Color a=sky[s],b=sky[s+1];
            g2.setColor(new java.awt.Color((int)(a.getRed()+(b.getRed()-a.getRed())*lt),(int)(a.getGreen()+(b.getGreen()-a.getGreen())*lt),(int)(a.getBlue()+(b.getBlue()-a.getBlue())*lt)));g2.drawLine(0,y,w,y);}
        int sx=w/2,sy=(int)(h*.62);
        int[][] gl={{280,255,220,60,6},{200,255,180,48,18},{110,255,140,28,48},{55,255,100,18,75}};
        for(int[] gl2:gl){g2.setColor(new java.awt.Color(255,gl2[1],gl2[2],gl2[3]));g2.fillOval(sx-gl2[0],sy-gl2[0],gl2[0]*2,gl2[0]*2);}
        g2.setColor(new java.awt.Color(255,238,148));g2.fillOval(sx-36,sy-36,72,72);
        for(int y=(int)(h*.62);y<h;y++){float t=((float)y-h*.62f)/(h*.38f);int al=(int)(75*(1f-t));if(al>0){g2.setColor(new java.awt.Color(255,115,18,al));g2.drawLine(0,y,w,y);}}
        g2.setStroke(new java.awt.BasicStroke(1f));
        for(int i=0;i<40;i++){int lx=sx-120+rnd.nextInt(240),ly=(int)(h*.65)+rnd.nextInt((int)(h*.3)),lw=4+rnd.nextInt(65);
            g2.setColor(new java.awt.Color(255,215,95,25+rnd.nextInt(75)));g2.drawLine(lx,ly,lx+lw,ly);}
    }

    // ── Forest: night sky + stars + moon + pine silhouettes + fireflies ──
    private void drawForest(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        for(int y=0;y<h;y++){float t=(float)y/h;g2.setColor(new java.awt.Color((int)(4+t*4),(int)(6+t*18),(int)(14+t*28)));g2.drawLine(0,y,w,y);}
        for(int i=0;i<220;i++){int sx=rnd.nextInt(w),sy=rnd.nextInt((int)(h*.72));int br=155+rnd.nextInt(100);int sz=rnd.nextInt(3);g2.setColor(new java.awt.Color(br,br,Math.min(255,br+12)));g2.fillOval(sx,sy,sz+1,sz+1);}
        int mx=(int)(w*.75),my=(int)(h*.18);g2.setColor(new java.awt.Color(255,250,218,25));g2.fillOval(mx-58,my-58,116,116);
        g2.setColor(new java.awt.Color(255,246,195,110));g2.fillOval(mx-30,my-30,60,60);g2.setColor(new java.awt.Color(255,250,225));g2.fillOval(mx-20,my-20,40,40);
        int gy=(int)(h*.72);g2.setColor(new java.awt.Color(3,11,5));
        for(int i=0;i<20;i++){int tx=rnd.nextInt(w+100)-50,th=80+rnd.nextInt(200),tw=16+rnd.nextInt(38);
            g2.fillPolygon(new int[]{tx,tx+tw/2,tx+tw},new int[]{gy,gy-th,gy},3);
            g2.fillPolygon(new int[]{tx+tw/6,tx+tw/2,tx+5*tw/6},new int[]{gy-(int)(th*.44),gy-th-(int)(th*.24),gy-(int)(th*.44)},3);}
        g2.setColor(new java.awt.Color(5,17,7));g2.fillRect(0,gy,w,h-gy);
        for(int i=0;i<28;i++){int fx=rnd.nextInt(w),fy=(int)(h*.5)+rnd.nextInt((int)(h*.24));g2.setColor(new java.awt.Color(175,255,95,80+rnd.nextInt(125)));g2.fillOval(fx,fy,3,3);}
    }

    // ── Mountain: snowy peaks + alpine glow + stars ───────────────────
    private void drawMountain(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        for(int y=0;y<h;y++){float t=(float)y/h;g2.setColor(new java.awt.Color((int)(8+t*15),(int)(12+t*30),(int)(35+t*80)));g2.drawLine(0,y,w,y);}
        for(int i=0;i<180;i++){int sx=rnd.nextInt(w),sy=rnd.nextInt((int)(h*.5));int br=140+rnd.nextInt(115);g2.setColor(new java.awt.Color(br,br,Math.min(255,br+20)));g2.fillOval(sx,sy,2,2);}
        int[][] mts={{-80,380,580,h},{w/2-120,250,w/2+120,h},{w-200,320,w+80,h},{100,300,350,h},{w/2,200,w/2+180,h}};
        for(int[] m:mts){g2.setColor(new java.awt.Color(40,50,75));g2.fillPolygon(new int[]{m[0],m[2],m[0]+m[2]>>1},new int[]{h,h,m[1]},3);}
        for(int[] m:mts){int px=m[0]+m[2]>>1,py=m[1];g2.setColor(new java.awt.Color(235,240,255,200));
            g2.fillPolygon(new int[]{px-18,px,px+18},new int[]{py+55,py,py+55},3);}
        int al=70;for(int r=180;r>0;r-=12){g2.setColor(new java.awt.Color(200,220,255,Math.max(0,al-(180-r)/3)));g2.drawOval(w/2-r,(int)(h*.33)-r/2,r*2,r);}
    }

    // ── City: night skyline + window lights + reflections ────────────
    private void drawCity(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        for(int y=0;y<h;y++){float t=(float)y/h;g2.setColor(new java.awt.Color((int)(5+t*10),(int)(5+t*15),(int)(20+t*35)));g2.drawLine(0,y,w,y);}
        for(int i=0;i<100;i++){int sx=rnd.nextInt(w),sy=rnd.nextInt((int)(h*.45));int br=80+rnd.nextInt(175);g2.setColor(new java.awt.Color(br,br,Math.min(255,br+30),130));g2.fillOval(sx,sy,2,2);}
        int groundY=(int)(h*.6);
        for(int i=0;i<22;i++){int bx=rnd.nextInt(w+60)-30,bw=18+rnd.nextInt(55),bh=60+rnd.nextInt(220);
            g2.setColor(new java.awt.Color(15+rnd.nextInt(20),15+rnd.nextInt(20),25+rnd.nextInt(30)));
            g2.fillRect(bx,groundY-bh,bw,bh);
            for(int wx=bx+3;wx<bx+bw-4;wx+=8)for(int wy=groundY-bh+5;wy<groundY-5;wy+=10){
                if(rnd.nextInt(3)!=0){int[] rgb=rnd.nextInt(4)==0?new int[]{255,200,80}:new int[]{180,220,255};
                    g2.setColor(new java.awt.Color(rgb[0],rgb[1],rgb[2],100+rnd.nextInt(100)));g2.fillRect(wx,wy,5,7);}}}
        g2.setColor(new java.awt.Color(8,12,22));g2.fillRect(0,groundY,w,h-groundY);
        for(int y=groundY;y<h;y++){float t=(float)(y-groundY)/(h-groundY);
            g2.setColor(new java.awt.Color(0,20,60,(int)(60*(1-t))));g2.drawLine(0,y,w,y);}
        for(int i=0;i<40;i++){int lx=rnd.nextInt(w),ly=groundY+rnd.nextInt(h-groundY),lal=20+rnd.nextInt(70);
            int[] rgb=rnd.nextInt(3)==0?new int[]{255,180,60}:rnd.nextInt(2)==0?new int[]{60,160,255}:new int[]{255,80,180};
            g2.setColor(new java.awt.Color(rgb[0],rgb[1],rgb[2],lal));g2.drawLine(lx-10,ly,lx+10,ly);}
    }

    // ── Neon / Abstract: glowing orbs + grid + sparkles ──────────────
    private void drawNeon(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        g2.setColor(new java.awt.Color(2,0,12));g2.fillRect(0,0,w,h);
        g2.setColor(new java.awt.Color(0,180,255,12));
        for(int x=0;x<w;x+=40)g2.drawLine(x,0,x,h);
        for(int y=0;y<h;y+=40)g2.drawLine(0,y,w,y);
        int[][] orbs={{w/2,h/2,200,0,255,200},{(int)(w*.25),(int)(h*.35),140,200,0,255},{(int)(w*.75),(int)(h*.65),160,255,50,200}};
        for(int[] ob:orbs){for(int r=ob[2];r>0;r-=8){float a=(1f-(float)r/ob[2])*0.18f;
            g2.setColor(new java.awt.Color(ob[3],ob[4],ob[5],(int)(a*255)));g2.fillOval(ob[0]-r,ob[1]-r,r*2,r*2);}}
        g2.setStroke(new java.awt.BasicStroke(1.5f));
        for(int i=0;i<40;i++){int nx=rnd.nextInt(w),ny=rnd.nextInt(h),nr=4+rnd.nextInt(12);
            boolean mg=rnd.nextBoolean();int[] rgb=mg?new int[]{255,0,210}:new int[]{0,255,180};
            for(int r=nr+5;r>0;r--){float a=(float)r/(nr+5)*0.4f;
                g2.setColor(new java.awt.Color(rgb[0],rgb[1],rgb[2],(int)(a*255)));g2.fillOval(nx-r,ny-r,r*2,r*2);}}
        for(int y=0;y<h;y+=2){g2.setColor(new java.awt.Color(0,0,0,35));g2.drawLine(0,y,w,y);}
    }

    // ── Sakura: soft pink sky + falling petals + tree ────────────────
    private void drawSakura(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        for(int y=0;y<h;y++){float t=(float)y/h;g2.setColor(new java.awt.Color((int)(245+t*10),(int)(215+t*20),(int)(220+t*30)));g2.drawLine(0,y,w,y);}
        g2.setColor(new java.awt.Color(80,25,10));int tx=w/3;
        g2.setStroke(new java.awt.BasicStroke(14));g2.drawLine(tx,h,tx+20,(int)(h*.3));
        g2.setStroke(new java.awt.BasicStroke(7));g2.drawLine(tx+20,(int)(h*.3),tx-40,(int)(h*.1));
        g2.drawLine(tx+20,(int)(h*.3),tx+80,(int)(h*.15));
        g2.setStroke(new java.awt.BasicStroke(1f));
        for(int i=0;i<300;i++){int px=rnd.nextInt(w),py=rnd.nextInt(h);int r=3+rnd.nextInt(10);
            int al=60+rnd.nextInt(140);g2.setColor(new java.awt.Color(255,190+rnd.nextInt(40),200,al));
            g2.fillOval(px-r,py-r/2,r*2,(int)(r*1.3));}
        for(int i=0;i<60;i++){int px=rnd.nextInt(w),py=rnd.nextInt(h/2);int r=8+rnd.nextInt(18);
            g2.setColor(new java.awt.Color(255,170,185,40+rnd.nextInt(60)));g2.fillOval(px-r,py-r,r*2,r*2);}
    }

    // ── Plasma: smooth sine-wave colour field ─────────────────────────
    private void drawPlasma(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        double ox=rnd.nextDouble()*10,oy=rnd.nextDouble()*10;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            double v=Math.sin(x*.016+ox)+Math.sin(y*.012+oy)+Math.sin((x+y)*.01)+Math.sin(Math.sqrt(x*x+y*y)*.014);
            double t=(v+4)/8.0;
            int r=Math.clamp((int)(Math.sin(Math.PI*t)*210+45),0,255);
            int gr=Math.clamp((int)(Math.sin(Math.PI*t+2.09)*190+45),0,255);
            int b=Math.clamp((int)(Math.sin(Math.PI*t+4.19)*225+30),0,255);
            img.setRGB(x,y,(0xFF<<24)|(r<<16)|(gr<<8)|b);}
        for(int i=0;i<7;i++){int bx=60+rnd.nextInt(w-120),by=60+rnd.nextInt(h-120),br=35+rnd.nextInt(90);
            for(int r=br;r>0;r-=4){float a=(1f-(float)r/br)*.22f;g2.setColor(new java.awt.Color(255,255,255,(int)(a*255)));g2.fillOval(bx-r,by-r,r*2,r*2);}}
    }

    // ═════════════════════════════════════════════════════════════════════
    //  LSB Steganography
    // ═════════════════════════════════════════════════════════════════════

    private byte[] lsbEmbed(byte[] coverPng, String message) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(coverPng));
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        int totalBits = 32 + msgBytes.length * 8;
        if (totalBits > img.getWidth() * img.getHeight() * 3)
            throw new IllegalStateException("Зображення замале для цього повідомлення");

        boolean[] bits = new boolean[totalBits];
        int len = msgBytes.length;
        for (int i = 0; i < 32; i++) bits[i] = ((len >> (31-i)) & 1) == 1;
        for (int i = 0; i < msgBytes.length; i++)
            for (int b = 0; b < 8; b++)
                bits[32+i*8+b] = ((msgBytes[i] >> (7-b)) & 1) == 1;

        int idx = 0;
        outer: for (int y=0;y<img.getHeight();y++) for (int x=0;x<img.getWidth();x++) {
            int px=img.getRGB(x,y), a=(px>>24)&0xFF, r=(px>>16)&0xFF, g=(px>>8)&0xFF, bl=px&0xFF;
            if (idx<bits.length) r =setBit(r, bits[idx++]);
            if (idx<bits.length) g =setBit(g, bits[idx++]);
            if (idx<bits.length) bl=setBit(bl,bits[idx++]);
            img.setRGB(x,y,(a<<24)|(r<<16)|(g<<8)|bl);
            if (idx>=bits.length) break outer;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", out);
        return out.toByteArray();
    }

    private String lsbExtract(byte[] stegoPng) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(stegoPng));
        // Read 32 length bits
        boolean[] lenBits = new boolean[32];
        int bitIdx = 0;
        outer: for (int y=0;y<img.getHeight();y++) for (int x=0;x<img.getWidth();x++) {
            int px=img.getRGB(x,y);
            int[] ch={((px>>16)&0xFF),((px>>8)&0xFF),(px&0xFF)};
            for (int c:ch) { if(bitIdx<32) lenBits[bitIdx++]=((c&1)==1); else break outer; }
        }
        int msgLen=0; for (boolean b:lenBits) msgLen=(msgLen<<1)|(b?1:0);
        if (msgLen<=0||msgLen>2_000_000) return "";

        byte[] msgBytes = new byte[msgLen];
        int totalBits = 32 + msgLen*8;
        int pixBit = 0;
        outer2: for (int y=0;y<img.getHeight();y++) for (int x=0;x<img.getWidth();x++) {
            int px=img.getRGB(x,y);
            int[] ch={((px>>16)&0xFF),((px>>8)&0xFF),(px&0xFF)};
            for (int c:ch) {
                if (pixBit>=32) {
                    int mIdx=pixBit-32;
                    if (mIdx<msgLen*8) msgBytes[mIdx/8]=(byte)((msgBytes[mIdx/8]<<1)|(c&1));
                }
                pixBit++;
                if (pixBit>=totalBits) break outer2;
            }
        }
        return new String(msgBytes, StandardCharsets.UTF_8);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════════

    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private int setBit(int channel, boolean bit) { return bit ? (channel|1) : (channel&~1); }

    private String generateKeyToken() {
        return UUID.randomUUID().toString().replace("-","").substring(0,24).toUpperCase();
    }

    private String normalizeKey(String k) {
        return k == null ? "" : k.trim().toUpperCase().replace("-","").replace(" ","");
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        keyStore.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}
