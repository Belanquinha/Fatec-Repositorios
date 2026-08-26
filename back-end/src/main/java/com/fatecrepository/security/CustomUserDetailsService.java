package com.fatecrepository.security;

import com.fatecrepository.repository.GestorRepository;
import com.fatecrepository.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final GestorRepository gestorRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return gestorRepository.findByEmail(email)
            .map(CustomUserDetails::fromGestor)
            .orElseGet(() -> userRepository.findByEmail(email)
                .map(CustomUserDetails::fromUser)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado para o email informado")));
    }
}
