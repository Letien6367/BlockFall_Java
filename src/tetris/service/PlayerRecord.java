package tetris.service;

import java.util.Objects;
import java.util.UUID;

public record PlayerRecord(UUID id, String name, long highScore) {
    public static final int MAX_NAME_LENGTH = 24;

    public PlayerRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (highScore < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }
    }

    static String normalizeName(String name) {
        Objects.requireNonNull(name, "name");
        String normalizedName = name.strip().replaceAll("\\s+", " ");
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên người chơi.");
        }
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Tên người chơi không được dài quá " + MAX_NAME_LENGTH + " ký tự.");
        }
        return normalizedName;
    }
}
