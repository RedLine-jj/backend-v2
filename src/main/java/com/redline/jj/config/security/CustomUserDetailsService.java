package com.redline.jj.config.security;

import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.domain.user.User;
import com.redline.jj.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUserId())
            .password(user.getUserPw())
            .authorities("ROLE_USER")
            .build();
    }
}
