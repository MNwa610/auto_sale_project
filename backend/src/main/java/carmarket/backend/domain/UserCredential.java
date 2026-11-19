package carmarket.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_credentials")
public class UserCredential {
    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false, length = 50)
    private String login;

    @Column(name = "password_hash", length = 60)
    private String passwordHash;

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
