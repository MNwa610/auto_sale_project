package carmarket.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@MappedSuperclass
public abstract class VersionedEntity extends BaseEntity {
    @Version
    private long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void updateTimestamp() { updatedAt = Instant.now(); }

    /** Изменения характеристик/тегов также должны обновлять версию их владельца. */
    public void touch() { updateTimestamp(); }

    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
