package com.gloyoo.userbackend.user.service;

import com.gloyoo.userbackend.user.dto.UserRequest;
import com.gloyoo.userbackend.user.entity.Role;
import com.gloyoo.userbackend.user.entity.User;
import com.gloyoo.userbackend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository ;

    public UserService(UserRepository userRepository , PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    //add a user
    public User registerUser(UserRequest user){

        if (userRepository.existsByEmailIgnoreCase(user.getEmail())){
            throw new IllegalArgumentException("User already exists");
        }

        User userEntity = new User();
        userEntity.setName(user.getName());
        userEntity.setEmail(user.getEmail());
        userEntity.setPassword(passwordEncoder.encode(user.getPassword()));
        userEntity.setRole(Role.USER);

        userRepository.save(userEntity);

        return userEntity;

    }

    //update user
    public User updateUser(String email,UserRequest user){
        User userEntity = findByEmailOrThrow(email);
        userEntity.setName(user.getName());
        userEntity.setEmail(user.getEmail());
        userEntity.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(userEntity);
        return userEntity;
    }

    // delete user
    public void deleteUser(String email){
        User userEntity = findByEmailOrThrow(email);
        userRepository.delete(userEntity);
    }

    // find user or throw exception
    public User findByEmailOrThrow(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public User findByIdOrThrow(String id) {
        return userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
