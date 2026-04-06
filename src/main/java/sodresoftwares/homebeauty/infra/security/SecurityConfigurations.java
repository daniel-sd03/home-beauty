 package sodresoftwares.homebeauty.infra.security;

 import jakarta.servlet.http.HttpServletResponse;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.http.HttpMethod;
 import org.springframework.security.authentication.AuthenticationManager;
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
public class SecurityConfigurations {
	
	private final SecurityFilter securityFilter;
	
	public SecurityConfigurations(SecurityFilter securityFilter) {
		this.securityFilter = securityFilter;
	}
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		return httpSecurity
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authrize -> authrize
						.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/register/professional").permitAll()
						.requestMatchers("/error").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.anyRequest().authenticated()
				)
		        .exceptionHandling(exception -> 
		            exception.authenticationEntryPoint((request, response, authException) -> {
		                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		                response.setContentType("application/json");
		                response.getWriter().write("""
		                {
		                  "error": "Unauthorized"
		                }
		                """);
		            })
	            )
				.addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
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
