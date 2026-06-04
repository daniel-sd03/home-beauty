package sodresoftwares.homebeauty.model.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class User implements UserDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(name = "email", unique = true, nullable = false)
	private String login;

	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	@Column(nullable = false)
	private String firstName;

	@Column(nullable = false)
	private String lastName;

	private String phone;

	@Column(unique = true)
	private String cpf;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	private String gender;

	@Column(name = "profile_picture_url")
	private String profilePictureUrl;

	@Column(name = "is_active")
	private boolean isActive = false;

	@Column(name = "verification_code")
	private String verificationCode;

	@Column(name = "verification_code_expiry")
	private LocalDateTime verificationCodeExpiry;

	@Column(name = "deletion_requested_at")
	private LocalDateTime deletionRequestedAt;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@CreatedBy
	@Column(name = "created_by", updatable = false)
	private String createdBy;

	@LastModifiedBy
	@Column(name = "updated_by")
	private String updatedBy;

	@Override
	public boolean isEnabled() {
		return this.isActive;
	}

	public boolean hasCompleteUserProfile() {
		return phone != null && !phone.isBlank()
				&& cpf != null && !cpf.isBlank()
				&& birthDate != null
				&& gender != null && !gender.isBlank();
	}

	public String getFullName() {
		if (this.lastName == null || this.lastName.isBlank()) {
			return this.firstName;
		}
		return this.firstName + " " + this.lastName;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return login;
	}
}