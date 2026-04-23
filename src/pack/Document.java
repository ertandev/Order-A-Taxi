package pack;

import java.time.LocalDateTime;
import java.util.UUID;

// --- DOCUMENT CLASS (Class Diagram) ---
public class Document {
    private UUID id = UUID.randomUUID();
    private String ownerId;      // Driver's UUID as String
    private DocumentType type;
    private String filePath;
    private LocalDateTime uploadedAt;
    private boolean isApproved;

    public Document(String ownerId, DocumentType type, String filePath) {
        this.ownerId = ownerId;
        this.type = type;
        this.filePath = filePath;
        this.uploadedAt = LocalDateTime.now();
        this.isApproved = false;
    }

    public UUID getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public DocumentType getType() { return type; }
    public String getFilePath() { return filePath; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { this.isApproved = approved; }

    @Override
    public String toString() {
        return "[" + type + "] " + (isApproved ? "Approved" : "Pending") + " | " + filePath;
    }
}
