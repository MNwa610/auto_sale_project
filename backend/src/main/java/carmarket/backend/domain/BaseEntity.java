package carmarket.backend.domain;

import jakarta.persistence.*;

/** Внутренний числовой ключ сохраняет связи с данными КР1. */
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }
}
