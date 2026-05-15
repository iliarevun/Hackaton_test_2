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
    // imagen-3.0-generate-002 — via v1 (not v1beta)
    private static final String GEMINI_IMAGEN_URL =
            "https://generativelanguage.googleapis.com/v1/models/" +
                    "imagen-3.0-generate-002:predict?key=";

    // gemini-2.0-flash-preview-image-generation — confirmed working 2026
    private static final String GEMINI_FLASH_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.0-flash-preview-image-generation:generateContent?key=";

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
            String imageTheme,
            byte[] userCoverBytes) throws Exception {

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

        // 3. Use user-provided cover image OR generate one
        byte[] coverPng;
        if (userCoverBytes != null && userCoverBytes.length > 0) {
            // Convert to PNG if needed
            try {
                java.awt.image.BufferedImage uploaded = ImageIO.read(new ByteArrayInputStream(userCoverBytes));
                if (uploaded == null) throw new IllegalArgumentException("Invalid cover image");
                ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
                ImageIO.write(uploaded, "PNG", pngOut);
                coverPng = pngOut.toByteArray();
                log.info("Using user-provided cover image ({}x{})", uploaded.getWidth(), uploaded.getHeight());
            } catch (Exception ex) {
                log.warn("Could not read user cover image: {}, using generated", ex.getMessage());
                coverPng = generateCoverImage(imageTheme, isFile, fileName);
            }
        } else {
            coverPng = generateCoverImage(imageTheme, isFile, fileName);
        }

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


    private byte[] generateCoverImage(String rawStyle, boolean isFile, String fileName) {
        // Map frontend style name → Java2D art keyword via buildImagePrompt
        String artStyle = buildImagePrompt(rawStyle, isFile, fileName);
        log.info("Generating Java2D art: rawStyle='{}' -> artStyle='{}'", rawStyle, artStyle);
        return generateFallbackImage(artStyle);
    }

    /**
     * Maps style name from frontend chip to Java2D art style keyword.
     * Empty/null → random rotation through all styles.
     */
    private String buildImagePrompt(String style, boolean isFile, String fileName) {
        // Known artistic styles from the frontend chips
        String[] allStyles = {"cyberpunk","impressionism","watercolor","glitch","vaporwave",
                "oilpainting","ukiyoe","noir","surrealism","pixelart",
                "ocean","sunset","forest","sakura","plasma"};
        if (style == null || style.isBlank()) {
            // Truly random — different index every call via nanoTime
            return allStyles[(int)(Math.abs(System.nanoTime()) % allStyles.length)];
        }
        String s = style.toLowerCase().trim();
        // Direct style match
        return switch (s) {
            case "cyberpunk"     -> "cyberpunk";
            case "impressionism" -> "impressionism";
            case "watercolor"    -> "watercolor";
            case "glitch"        -> "glitch";
            case "vaporwave"     -> "vaporwave";
            case "oilpainting"   -> "oilpainting";
            case "ukiyoe"        -> "ukiyoe";
            case "noir"          -> "noir";
            case "surrealism"    -> "surrealism";
            case "pixelart"      -> "pixelart";
            // legacy / fallback
            case "ocean","beach","sea"           -> "ocean";
            case "forest","autumn","tree"        -> "forest";
            case "sunset","sunrise"              -> "sunset";
            case "sakura","cherry","blossom"     -> "sakura";
            case "mountain","snow"               -> "mountain";
            case "city","street","urban"         -> "city";
            case "neon","abstract","digital"     -> "neon";
            default -> {
                // custom text — hash it to a deterministic style + use nanoTime for variation
                int idx = (int)(Math.abs(s.hashCode() ^ System.nanoTime()) % allStyles.length);
                yield allStyles[idx];
            }
        };
    }

    private byte[] generateFallbackImage(String theme) {
        int w = 800, h = 600;
        BufferedImage img  = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY);

        // Use nanoTime as seed — guaranteed unique per call
        long nano = System.nanoTime();
        SecureRandom rnd = new SecureRandom(
                java.nio.ByteBuffer.allocate(8).putLong(nano).array());

        // artStyle is a resolved keyword from buildImagePrompt (never empty here)
        String t = (theme != null && !theme.isBlank()) ? theme.toLowerCase().trim() : "plasma";
        // unused legacy var — keep compiler happy
        int autoVariant = (int)(nano % 8);

        switch (t) {
            case "cyberpunk"     -> drawCyberpunk(img,g2,w,h,rnd);
            case "impressionism" -> drawImpressionism(img,g2,w,h,rnd);
            case "watercolor"    -> drawWatercolor(img,g2,w,h,rnd);
            case "glitch"        -> drawGlitch(img,g2,w,h,rnd);
            case "vaporwave"     -> drawVaporwave(img,g2,w,h,rnd);
            case "oilpainting"   -> drawOilPainting(img,g2,w,h,rnd);
            case "ukiyoe"        -> drawUkiyoe(img,g2,w,h,rnd);
            case "noir"          -> drawNoir(img,g2,w,h,rnd);
            case "surrealism"    -> drawSurrealism(img,g2,w,h,rnd);
            case "pixelart"      -> drawPixelArt(img,g2,w,h,rnd);
            case "ocean"         -> drawOcean(img,g2,w,h,rnd);
            case "sunset"        -> drawSunset(img,g2,w,h,rnd);
            case "forest"        -> drawForest(img,g2,w,h,rnd);
            case "mountain"      -> drawMountain(img,g2,w,h,rnd);
            case "city"          -> drawCity(img,g2,w,h,rnd);
            case "neon"          -> drawNeon(img,g2,w,h,rnd);
            case "sakura"        -> drawSakura(img,g2,w,h,rnd);
            default              -> drawPlasma(img,g2,w,h,rnd);
        }

        g2.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) { throw new RuntimeException(e); }
    }


    // ── Cyberpunk: dark city + neon signs + rain ─────────────────────
    private void drawCyberpunk(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        // Sky gradient
        int cpHue=rnd.nextInt(40);for(int y=0;y<h;y++){float t=(float)y/h;g2.setColor(new java.awt.Color(Math.clamp((int)(5+t*8+cpHue/4),0,255),(int)(0+t*5),(Math.clamp((int)(20+t*30+cpHue),0,255))));g2.drawLine(0,y,w,y);}
        // Buildings silhouette
        for(int i=0;i<28;i++){int bx=rnd.nextInt(w+60)-30,bw=14+rnd.nextInt(50),bh=80+rnd.nextInt(260),br=8+rnd.nextInt(20),bg=5+rnd.nextInt(15);
            g2.setColor(new java.awt.Color(br,bg,br+10));g2.fillRect(bx,h-bh,bw,bh);
            for(int wx=bx+2;wx<bx+bw-3;wx+=7) for(int wy=h-bh+4;wy<h-5;wy+=9){
                if(rnd.nextInt(4)!=0){boolean mg=rnd.nextInt(3)==0;
                    g2.setColor(mg?new java.awt.Color(255,0,200,90+rnd.nextInt(100)):new java.awt.Color(0,200,255,90+rnd.nextInt(100)));
                    g2.fillRect(wx,wy,4,6);}}}
        // Neon signs
        int[][] signs={{w/4,h/3,255,20,180},{w*2/3,h/4,20,230,255},{w/2,h*2/5,255,100,0}};
        for(int[] s:signs){for(int r=40;r>0;r-=5){float a=(1f-(float)r/40)*.35f;
            g2.setColor(new java.awt.Color(s[2],s[3],s[4],(int)(a*255)));g2.fillOval(s[0]-r,s[1]-r/2,r*2,r);}}
        // Rain
        g2.setStroke(new java.awt.BasicStroke(.8f));
        for(int i=0;i<200;i++){int rx=rnd.nextInt(w),ry=rnd.nextInt(h),rl=5+rnd.nextInt(12);
            g2.setColor(new java.awt.Color(80,160,255,15+rnd.nextInt(35)));g2.drawLine(rx,ry,rx+2,ry+rl);}
        // Horizontal light bleed on ground
        for(int y=(int)(h*.75);y<h;y++){float t=((float)y-h*.75f)/(h*.25f);
            g2.setColor(new java.awt.Color(255,0,200,(int)(18*(1-t))));g2.drawLine(0,y,w/2,y);
            g2.setColor(new java.awt.Color(0,200,255,(int)(18*(1-t))));g2.drawLine(w/2,y,w,y);}
    }

    // ── Impressionism: thick brushstrokes, pastel fields ─────────────
    private void drawImpressionism(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        // Sky wash
        for(int y=0;y<h;y++){float t=(float)y/h;g2.setColor(new java.awt.Color((int)(135+t*60),(int)(185+t*30),(int)(220-t*60)));g2.drawLine(0,y,w,y);}
        // Brushstrokes — thick ellipses at angles
        for(int i=0;i<380;i++){int bx=rnd.nextInt(w),by=rnd.nextInt(h),bw=12+rnd.nextInt(38),bh=4+rnd.nextInt(10);
            double angle=rnd.nextDouble()*Math.PI;
            float t=(float)by/h;
            int[] palette = t<.45f ? new int[]{80+rnd.nextInt(120),140+rnd.nextInt(80),180+rnd.nextInt(70)}
                    : new int[]{60+rnd.nextInt(100),110+rnd.nextInt(90),30+rnd.nextInt(80)};
            g2.setColor(new java.awt.Color(Math.min(255,palette[0]),Math.min(255,palette[1]),Math.min(255,palette[2]),120+rnd.nextInt(100)));
            java.awt.geom.AffineTransform at=g2.getTransform();g2.rotate(angle,bx,by);g2.fillOval(bx-bw/2,by-bh/2,bw,bh);g2.setTransform(at);}
        // Sun blob
        for(int r=55;r>0;r-=4){g2.setColor(new java.awt.Color(255,230,100,(int)((1f-(float)r/55)*180)));g2.fillOval(w/4-r,h/5-r,r*2,r*2);}
    }

    // ── Watercolor: soft bleeds + paper texture ───────────────────────
    private void drawWatercolor(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        g2.setColor(new java.awt.Color(248,244,238));g2.fillRect(0,0,w,h);
        int[][] palette={{180,80,100},{80,140,200},{120,180,80},{200,130,60},{100,80,160}};
        for(int i=0;i<12;i++){int[]c=palette[rnd.nextInt(palette.length)];int cx=rnd.nextInt(w),cy=rnd.nextInt(h),cr=80+rnd.nextInt(160);
            for(int r=cr;r>0;r-=6){float a=(float)r/cr*.28f;
                g2.setColor(new java.awt.Color(c[0],c[1],c[2],(int)(a*255)));
                g2.fillOval(cx-r+rnd.nextInt(20)-10,cy-r+rnd.nextInt(20)-10,r*2,r*2);}}
        // Paper grain
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) if(rnd.nextInt(8)==0){
            int px=img.getRGB(x,y);int v=rnd.nextInt(18)-9;
            int r2=Math.clamp(((px>>16)&0xFF)+v,0,255),g2c=Math.clamp(((px>>8)&0xFF)+v,0,255),b2=Math.clamp((px&0xFF)+v,0,255);
            img.setRGB(x,y,(0xFF<<24)|(r2<<16)|(g2c<<8)|b2);}
    }

    // ── Glitch Art: corrupted scanlines + color shifts ────────────────
    private void drawGlitch(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        // Base gradient
        int glHue=rnd.nextInt(60);for(int y=0;y<h;y++){float t=(float)y/h;g2.setColor(new java.awt.Color(Math.clamp((int)(20+t*40+glHue/3),0,255),0,Math.clamp((int)(60+t*100+glHue),0,255)));g2.drawLine(0,y,w,y);}
        // Glitch blocks — horizontal slices shifted
        for(int i=0;i<60;i++){int gy=rnd.nextInt(h),gh=1+rnd.nextInt(8),gshift=rnd.nextInt(80)-40;
            for(int row=gy;row<Math.min(h,gy+gh);row++){
                for(int x=0;x<w;x++){int srcX=Math.clamp(x+gshift,0,w-1);img.setRGB(x,row,img.getRGB(srcX,row));}}}
        // RGB channel split blocks
        for(int i=0;i<20;i++){int bx=rnd.nextInt(w),by=rnd.nextInt(h),bw2=20+rnd.nextInt(120),bh2=2+rnd.nextInt(15);
            g2.setColor(new java.awt.Color(255,0,80,60+rnd.nextInt(80)));g2.fillRect(bx-4,by,bw2,bh2);
            g2.setColor(new java.awt.Color(0,255,200,60+rnd.nextInt(80)));g2.fillRect(bx+4,by,bw2,bh2);}
        // Scanlines
        for(int y=0;y<h;y+=3){g2.setColor(new java.awt.Color(0,0,0,40));g2.drawLine(0,y,w,y);}
        // White noise flashes
        for(int i=0;i<300;i++){int nx=rnd.nextInt(w),ny=rnd.nextInt(h);
            g2.setColor(new java.awt.Color(255,255,255,rnd.nextInt(100)));g2.fillRect(nx,ny,rnd.nextInt(4)+1,1);}
    }

    // ── Vaporwave: pink/purple grid + sun + chrome text vibes ─────────
    private void drawVaporwave(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        int vpHue=rnd.nextInt(50)-25;for(int y=0;y<h;y++){float t=(float)y/h;g2.setColor(new java.awt.Color(Math.clamp((int)(20+t*40+vpHue),0,255),Math.clamp((int)(0+t*10+vpHue/2),0,255),Math.clamp((int)(60+t*80),0,255)));g2.drawLine(0,y,w,y);}
        // Retro sun
        int sx=w/2,sy=(int)(h*.42);
        int[][] sunGrad={{255,50,150},{255,100,180},{255,150,200},{255,200,220}};
        for(int i=0;i<sunGrad.length;i++){int r=80-i*18;
            g2.setColor(new java.awt.Color(sunGrad[i][0],sunGrad[i][1],sunGrad[i][2]));
            g2.fillOval(sx-r,sy-r/2,r*2,r);}
        // Horizontal stripes on sun
        for(int i=1;i<6;i++){int sy2=sy-30+i*10;
            g2.setColor(new java.awt.Color(20,0,60,200));g2.fillRect(sx-82,sy2,164,4);}
        // Grid floor
        int gy=(int)(h*.45);
        g2.setColor(new java.awt.Color(200,50,255,80));
        for(int x=-w;x<w*2;x+=30)g2.drawLine(x,gy,(int)(w/2+(x-w/2)*.3f),h);
        for(int y=gy;y<h;y+=20){float t=(float)(y-gy)/(h-gy);g2.drawLine(0,y,w,y);}
        // Stars
        for(int i=0;i<120;i++){int sx2=rnd.nextInt(w),sy2=rnd.nextInt(gy);int br=180+rnd.nextInt(75);
            g2.setColor(new java.awt.Color(br,br,Math.min(255,br+30),rnd.nextInt(200)+55));g2.fillOval(sx2,sy2,2,2);}
    }

    // ── Oil Painting: thick impasto, rich colors ──────────────────────
    private void drawOilPainting(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        // Rich dark background
        for(int y=0;y<h;y++){float t=(float)y/h;g2.setColor(new java.awt.Color((int)(30+t*50),(int)(20+t*40),(int)(10+t*30)));g2.drawLine(0,y,w,y);}
        // Thick impasto strokes
        int[][] oils={{180,60,20},{220,140,30},{80,120,40},{30,70,140},{160,40,100},{200,160,80}};
        for(int i=0;i<500;i++){int[]c=oils[rnd.nextInt(oils.length)];int bx=rnd.nextInt(w),by=rnd.nextInt(h);
            int sw=8+rnd.nextInt(35),sh=3+rnd.nextInt(8);double angle=rnd.nextDouble()*Math.PI;
            g2.setColor(new java.awt.Color(Math.min(255,c[0]+rnd.nextInt(40)-20),Math.min(255,c[1]+rnd.nextInt(40)-20),Math.min(255,c[2]+rnd.nextInt(40)-20),140+rnd.nextInt(90)));
            java.awt.geom.AffineTransform at=g2.getTransform();g2.rotate(angle,bx,by);
            g2.fillRoundRect(bx-sw/2,by-sh/2,sw,sh,sh/2,sh/2);g2.setTransform(at);}
        // Varnish gloss highlights
        for(int i=0;i<30;i++){int hx=rnd.nextInt(w),hy=rnd.nextInt(h),hr=3+rnd.nextInt(12);
            g2.setColor(new java.awt.Color(255,255,240,20+rnd.nextInt(60)));g2.fillOval(hx,hy,hr,hr/2);}
    }

    // ── Ukiyo-e: flat color blocks + wave patterns ────────────────────
    private void drawUkiyoe(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        g2.setColor(new java.awt.Color(240,230,200));g2.fillRect(0,0,w,h);
        // Sky bands
        int[][] sky={{180,210,230},{150,190,215},{120,170,205}};
        for(int i=0;i<sky.length;i++){g2.setColor(new java.awt.Color(sky[i][0],sky[i][1],sky[i][2]));g2.fillRect(0,i*(h/4),w,h/4+2);}
        // Mountain
        g2.setColor(new java.awt.Color(80,50,60));
        g2.fillPolygon(new int[]{w/4,w/2,3*w/4},new int[]{h/2,h/5,h/2},3);
        g2.setColor(new java.awt.Color(240,240,255));
        g2.fillPolygon(new int[]{(int)(w*.38),(int)(w*.5),(int)(w*.62)},new int[]{(int)(h*.3),(int)(h*.2),(int)(h*.3)},3);
        // Wave pattern at bottom
        g2.setColor(new java.awt.Color(30,80,160));g2.fillRect(0,(int)(h*.65),w,(int)(h*.35));
        g2.setStroke(new java.awt.BasicStroke(2));
        for(int wy=(int)(h*.65);wy<h;wy+=18){g2.setColor(new java.awt.Color(255,255,255,100));
            for(int x=0;x<w;x+=40){g2.drawArc(x,wy,40,14,0,180);}}
        // Border
        g2.setColor(new java.awt.Color(80,50,40));g2.setStroke(new java.awt.BasicStroke(6));g2.drawRect(8,8,w-16,h-16);
    }

    // ── Film Noir: high contrast B&W + shadows ────────────────────────
    private void drawNoir(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        g2.setColor(new java.awt.Color(10,10,10));g2.fillRect(0,0,w,h);
        // Street light cone
        int lx=rnd.nextInt(w/2)+w/4;
        for(int r=280;r>0;r-=8){float a=(1f-(float)r/280)*.18f;g2.setColor(new java.awt.Color(220,200,160,(int)(a*255)));g2.fillOval(lx-r,0-r/2,r*2,r*2);}
        // Rain streaks
        for(int i=0;i<300;i++){int rx=rnd.nextInt(w),ry=rnd.nextInt(h),rl=8+rnd.nextInt(20);
            g2.setColor(new java.awt.Color(120,120,140,15+rnd.nextInt(30)));g2.setStroke(new java.awt.BasicStroke(.8f));g2.drawLine(rx,ry,rx+3,ry+rl);}
        // Building silhouettes
        for(int i=0;i<15;i++){int bx=rnd.nextInt(w+40)-20,bw2=20+rnd.nextInt(60),bh2=100+rnd.nextInt(200);
            g2.setColor(new java.awt.Color(8+rnd.nextInt(12),8+rnd.nextInt(12),8+rnd.nextInt(12)));g2.fillRect(bx,h-bh2,bw2,bh2);
            for(int wx=bx+4;wx<bx+bw2-4;wx+=9) for(int wy=h-bh2+6;wy<h-6;wy+=12){
                if(rnd.nextInt(5)==0){g2.setColor(new java.awt.Color(200,180,100,80+rnd.nextInt(80)));g2.fillRect(wx,wy,4,5);}}}
        // Puddle reflection
        for(int y=(int)(h*.8);y<h;y++){float t=((float)y-h*.8f)/(h*.2f);g2.setColor(new java.awt.Color(220,200,160,(int)(12*(1-t))));g2.drawLine(0,y,w,y);}
        // Vignette
        for(int r=Math.max(w,h);r>50;r-=15){float a=(float)r/Math.max(w,h);g2.setColor(new java.awt.Color(0,0,0,(int)((1-a)*80)));g2.drawOval(w/2-r,h/2-r,r*2,r*2);}
    }

    // ── Surrealism: floating objects + impossible geometry ────────────
    private void drawSurrealism(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        // Split sky/ground
        for(int y=0;y<h/2;y++){float t=(float)y/(h/2);g2.setColor(new java.awt.Color((int)(255-t*100),(int)(220-t*120),(int)(180-t*60)));g2.drawLine(0,y,w,y);}
        for(int y=h/2;y<h;y++){float t=(float)(y-h/2)/(h/2);g2.setColor(new java.awt.Color((int)(40+t*20),(int)(90+t*40),(int)(50+t*20)));g2.drawLine(0,y,w,y);}
        // Horizon line
        g2.setColor(new java.awt.Color(80,50,30));g2.setStroke(new java.awt.BasicStroke(2));g2.drawLine(0,h/2,w,h/2);
        // Floating spheres
        for(int i=0;i<6;i++){int sx=50+rnd.nextInt(w-100),sy=50+rnd.nextInt(h/2-80),sr=18+rnd.nextInt(55);
            int[] clr={rnd.nextInt(200)+55,rnd.nextInt(200)+55,rnd.nextInt(200)+55};
            g2.setColor(new java.awt.Color(clr[0],clr[1],clr[2]));g2.fillOval(sx-sr,sy-sr,sr*2,sr*2);
            g2.setColor(new java.awt.Color(255,255,255,80));g2.fillOval(sx-sr/3,sy-sr/2,sr/3,sr/3);
            // Shadow under sphere
            g2.setColor(new java.awt.Color(0,0,0,40));g2.fillOval(sx-sr,h/2,sr*2,10);}
        // Melting clock (simplified — wavy rectangle)
        g2.setColor(new java.awt.Color(220,190,140));
        int cx=w/3,cy=h/2;java.awt.geom.GeneralPath gp=new java.awt.geom.GeneralPath();
        gp.moveTo(cx,cy);gp.curveTo(cx+40,cy-10,cx+80,cy+30,cx+90,cy+70);
        gp.lineTo(cx+70,cy+80);gp.curveTo(cx+60,cy+40,cx+20,cy+10,cx,cy+20);gp.closePath();
        g2.fill(gp);g2.setColor(new java.awt.Color(80,60,40));g2.setStroke(new java.awt.BasicStroke(1.5f));g2.draw(gp);
    }

    // ── Pixel Art: chunky 8×8 blocks ─────────────────────────────────
    private void drawPixelArt(BufferedImage img, java.awt.Graphics2D g2, int w, int h, SecureRandom rnd) {
        int ps=8; // pixel size
        int[][] palette={{20,12,28},{68,36,52},{48,52,109},{78,74,78},{133,76,48},{52,101,36},
                {208,70,72},{218,212,94},{109,170,44},{210,125,44},{218,212,94},{20,12,28}};
        // Sky
        for(int y=0;y<h/2;y+=ps) for(int x=0;x<w;x+=ps){g2.setColor(new java.awt.Color(48,52,109));g2.fillRect(x,y,ps,ps);}
        // Ground
        for(int y=h/2;y<h;y+=ps) for(int x=0;x<w;x+=ps){g2.setColor(new java.awt.Color(52,101,36));g2.fillRect(x,y,ps,ps);}
        // Stars
        for(int i=0;i<40;i++){int sx=rnd.nextInt(w/ps)*ps,sy=rnd.nextInt(h/2/ps)*ps;g2.setColor(new java.awt.Color(218,212,94));g2.fillRect(sx,sy,ps,ps);}
        // Mountains
        for(int i=0;i<4;i++){int mx=rnd.nextInt(w),mh=4+rnd.nextInt(10);
            for(int row=0;row<mh;row++) for(int col=-row;col<=row;col++){
                int px=(mx+col*ps)/ps*ps,py=(h/2-row*ps)/ps*ps;if(px>=0&&px<w&&py>=0&&py<h){g2.setColor(new java.awt.Color(78,74,78));g2.fillRect(px,py,ps,ps);}}}
        // Trees
        for(int i=0;i<8;i++){int tx=rnd.nextInt(w/ps)*ps,ty=h/2-4*ps;
            g2.setColor(new java.awt.Color(20,12,28));g2.fillRect(tx,ty+2*ps,ps,2*ps);
            g2.setColor(new java.awt.Color(52,101,36));for(int r=-2;r<=2;r++) for(int c=-2;c<=2;c++) if(Math.abs(r)+Math.abs(c)<=3) g2.fillRect(tx+c*ps,ty+r*ps,ps,ps);}
        // Grid lines for pixel effect
        g2.setColor(new java.awt.Color(0,0,0,20));
        for(int x=0;x<w;x+=ps)g2.drawLine(x,0,x,h);
        for(int y=0;y<h;y+=ps)g2.drawLine(0,y,w,y);
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
