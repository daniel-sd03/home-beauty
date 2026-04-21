package sodresoftwares.homebeauty.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import sodresoftwares.homebeauty.repositories.UserRepository;

@Service
public class AuthorizationService implements UserDetailsService{

	private final UserRepository userRepository;
	
	public AuthorizationService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		 UserDetails user = userRepository.findByLogin(username);
	        if (user == null) {
	            throw new UsernameNotFoundException("User not found");
	        }
	        return user;
	}
}
