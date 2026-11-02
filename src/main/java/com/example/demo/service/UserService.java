package com.example.demo.service;

import com.example.demo.model.UserModel;
import com.example.demo.model.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public Iterable<UserModel> getAll(){
        return userRepository.findAll();
    }

    public UserModel saveUser(UserModel userModel) {

        String hashedPassword = passwordEncoder.encode(userModel.getPassword());
        userModel.setPassword((hashedPassword));

        return userRepository.save(userModel);
    }

    public Optional<UserModel> findById(Integer id) {
        return userRepository.findById(id);
    }

    public void deleteById(Integer id) {
        userRepository.deleteById(id);
    }
}
