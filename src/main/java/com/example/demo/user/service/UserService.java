package com.example.demo.user.service;

import com.example.demo.user.model.UserModel;
import com.example.demo.user.model.UserRepository;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Iterable<UserModel> getAll(){
        return userRepository.findAll();
    }

    public UserModel save(UserModel a) {
        return userRepository.save(a);
    }

    public Optional<UserModel> findById(Integer id) {
        return userRepository.findById(id);
    }

    public void deleteById(Integer id) {
        userRepository.deleteById(id);
    }
}
