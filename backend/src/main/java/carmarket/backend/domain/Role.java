package carmarket.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleCode code;

    @Column(nullable = false, length = 50)
    private String name;

    public RoleCode getCode() { return code; }
    public void setCode(RoleCode code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
