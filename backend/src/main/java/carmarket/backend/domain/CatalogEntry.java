package carmarket.backend.domain;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class CatalogEntry extends VersionedEntity {
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false)
    private boolean enabled = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
