
package com.manan.cloudfilemanager.dto;

public record PresignedUrlResponse(
        String downloadUrl,
        long expiresInSeconds
) {
}
