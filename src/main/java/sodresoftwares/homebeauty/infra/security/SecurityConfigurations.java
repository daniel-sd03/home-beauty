 package sodresoftwares.homebeauty.infra.security;

import lombok.RequiredArgsConstructor;

 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.http.HttpMethod;
 import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
 import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
 import org.springframework.security.authentication.AuthenticationManager;
 import org.springframework.security.config.Customizer;
 import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
 import org.springframework.security.config.annotation.web.builders.HttpSecurity;
 import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
 import org.springframework.security.config.http.SessionCreationPolicy;
 import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
 import org.springframework.security.crypto.password.PasswordEncoder;
 import org.springframework.security.web.SecurityFilterChain;
 import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfigurations {

	private final SecurityFilter securityFilter;
	private final CustomAuthenticationEntryPoint authenticationEntryPoint;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		return httpSecurity
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authrize -> authrize
						.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/verify").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/resend-code").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/register/professional").permitAll()
						.requestMatchers(HttpMethod.GET, "/categories").permitAll()
						.requestMatchers(HttpMethod.GET, "/users/search").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/auth/{id}/role/admin").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/auth/{id}/role/demote").hasRole("ADMIN")
						.requestMatchers("/error").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.anyRequest().authenticated()
				)
		        .exceptionHandling(exception ->
		            exception.authenticationEntryPoint(authenticationEntryPoint)
	            )
				.addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	 @Bean
	 public RoleHierarchy roleHierarchy() {
		 return RoleHierarchyImpl.withDefaultRolePrefix()
				 .role("ADMIN").implies("PROFESSIONAL")
				 .role("PROFESSIONAL").implies("USER")
				 .build();
	 }
	
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
