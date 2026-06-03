package com.orderly.api.product.infrastructure.storage;

import com.orderly.api.shared.domain.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores product images on local disk and returns public relative URLs.
 */
@Service
public class ProductImageStorageService {

    // [SECURITY FIX VUL-08] Límite de 5 MB para imágenes. Previene DoS por archivos
    // gigantes.
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L;

    // [SECURITY FIX VUL-08] Magic bytes de formatos de imagen permitidos.
    // El Content-Type lo declara el cliente y es spoofeable; los magic bytes están
    // en el archivo.
    private static final byte[] MAGIC_JPEG = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
    private static final byte[] MAGIC_PNG = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 };
    private static final byte[] MAGIC_GIF = new byte[] { 0x47, 0x49, 0x46, 0x38 };
    private static final byte[] MAGIC_WEBP_RIFF = new byte[] { 0x52, 0x49, 0x46, 0x46 };

    private final Path uploadRoot;

    public ProductImageStorageService(@Value("${orderly.media.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String store(UUID businessId, UUID productId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainException("La imagen del producto es obligatoria.");
        }

        // [SECURITY FIX VUL-08] Verificar tamaño antes de leer el contenido completo
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new DomainException("La imagen no puede superar 5 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new DomainException("El archivo debe ser una imagen válida.");
        }

        // [SECURITY FIX VUL-08] Verificar magic bytes reales del archivo, no confiar en
        // Content-Type.
        // Un atacante puede subir un .jsp malicioso con Content-Type: image/png.
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(12);
            if (!isValidImageMagic(header)) {
                throw new DomainException("El contenido del archivo no corresponde a una imagen válida.");
            }
        } catch (IOException e) {
            throw new DomainException("No se pudo verificar el contenido del archivo.");
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        Path businessDir = uploadRoot.resolve("products").resolve(businessId.toString());
        // [SECURITY FIX VUL-08] Nombre generado por el sistema (UUID + timestamp), sin
        // input del usuario
        String filename = productId + "-" + System.currentTimeMillis() + extension;
        Path targetFile = businessDir.resolve(filename).normalize();

        // [SECURITY FIX] Verificar que el path resuelto siga dentro del directorio
        // esperado (anti path traversal)
        if (!targetFile.startsWith(uploadRoot)) {
            throw new DomainException("Ruta de archivo inválida.");
        }

        try {
            Files.createDirectories(businessDir);
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new DomainException("No se pudo guardar la imagen del producto.");
        }

        return "/uploads/products/" + businessId + "/" + filename;
    }

    /**
     * [SECURITY FIX VUL-08] Verifica que los primeros bytes coincidan con formatos
     * de imagen reales.
     */
    private boolean isValidImageMagic(byte[] header) {
        if (header.length < 4)
            return false;
        if (startsWith(header, MAGIC_JPEG))
            return true;
        if (startsWith(header, MAGIC_PNG))
            return true;
        if (startsWith(header, MAGIC_GIF))
            return true;
        // WebP: RIFF????WEBP — bytes 0-3 = RIFF, bytes 8-11 = WEBP
        if (startsWith(header, MAGIC_WEBP_RIFF) && header.length >= 12) {
            return header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50;
        }
        return false;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length)
            return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i])
                return false;
        }
        return true;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot > -1 && dot < originalFilename.length() - 1) {
                String ext = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
                if (ext.matches("\\.[a-z0-9]{2,5}")) {
                    return ext;
                }
            }
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
