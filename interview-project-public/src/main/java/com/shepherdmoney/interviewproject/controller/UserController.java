package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    // TODO: wire in the user repository (~ 1 line)
    @Autowired
    public UserRepository userRepository;
    @Autowired
    private CreditCardRepository creditCardRepository;

    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        // TODO: Create an user entity with information given in the payload, store it in the database
        //       and return the id of the user in 200 OK response

        // Check if a user with the same email already exists in the database
        User existingUser = userRepository.findByEmail(payload.getEmail());
        if (existingUser != null) {
            // Handle the case where a user with the same email and name already exists
            return ResponseEntity.status(HttpStatus.CONFLICT).body(existingUser.getId());
        }

        // If valid, Create a new user
        User user = new User();
        // Set name and email from payload
        user.setName(payload.getName());
        user.setEmail(payload.getEmail());

        // Save user to the database
        userRepository.save(user);

        // Return the ID of the newly created user
        return ResponseEntity.ok(user.getId());
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        // TODO: Return 200 OK if a user with the given ID exists, and the deletion is successful
        //       Return 400 Bad Request if a user with the ID does not exist
        //       The response body could be anything you consider appropriate

        // Check if a user with the given ID exists
        if (!userRepository.existsById(userId)) {
            // If the user does not exist, return a 400 Bad Request response
            return ResponseEntity.badRequest().body("User with ID " + userId + " does not exist.");
        }

        // Delete all credit cards owned by the user
        creditCardRepository.deleteByOwnerId(userId);

        // Delete the user from the database
        userRepository.deleteById(userId);

        // Return a 200 OK response
        return ResponseEntity.ok("User with ID " + userId + " has been successfully deleted, along with all associated credit cards.");
    }
}
