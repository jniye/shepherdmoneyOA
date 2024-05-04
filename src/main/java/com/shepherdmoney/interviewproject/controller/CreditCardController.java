package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CreditCardRepository creditCardRepository;


    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length

        // Retrieve the user from the repository
        User user = userRepository.findById(payload.getUserId()).orElse(null);

        // Check if the user exists
        if (user == null) {
            // If not, return 404 Not Found
            return ResponseEntity.notFound().build();
        }

        // Check if a creditCard with the same CardNumber already exists in the database
        CreditCard existingCreditCard = creditCardRepository.findByNumber(payload.getCardNumber());
        if(existingCreditCard != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(existingCreditCard.getId());
        }

        // If valid, Create a new CreditCard entity
        CreditCard creditCard = new CreditCard();
        creditCard.setIssuanceBank(payload.getCardIssuanceBank());
        creditCard.setNumber(payload.getCardNumber());
        // Associate the credit card with the user
        creditCard.setOwner(user);

        // Save the credit card to the database
        creditCard = creditCardRepository.save(creditCard);

        // Return the ID of the newly created credit card
        return ResponseEntity.ok(creditCard.getId());
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null
        // Retrieve the user from the repository
        User user = userRepository.findById(userId).orElse(null);

        // Check if the user exists
        if (user == null) {
            // If not, return 404 Not Found
            return ResponseEntity.notFound().build();
        }

        // Retrieve all credit cards associated with the user
        List<CreditCard> creditCards = creditCardRepository.findByOwner(user);

        // Convert CreditCard entities to CreditCardView DTOs
        List<CreditCardView> creditCardViews = creditCards.stream()
                .map(this::convertToCreditCardView)
                .collect(Collectors.toList());

        // Return the list of credit card views
        return ResponseEntity.ok(creditCardViews);
    }

    // Helper method to convert CreditCard entity to CreditCardView DTO
    private CreditCardView convertToCreditCardView(CreditCard creditCard) {
        return CreditCardView.builder()
                .issuanceBank(creditCard.getIssuanceBank())
                .number(creditCard.getNumber())
                .build();
    }


    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request

        // Retrieve the credit card associated with the provided number
        CreditCard creditCard = creditCardRepository.findByNumber(creditCardNumber);

        // Check if a credit card with the provided number exists
        if (creditCard == null) {
            // If not found, return 400 Bad Request
            return ResponseEntity.badRequest().build();
        }

        // Retrieve the user associated with the credit card
        User user = creditCard.getOwner();

        // Check if a user is associated with the credit card
        if (user == null) {
            // If no user is associated, return 400 Bad Request
            return ResponseEntity.badRequest().build();
        }

        // Return the user ID associated with the credit card in a 200 OK response
        return ResponseEntity.ok(user.getId());
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Map<String, Double>> updateBalanceForSome(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      1. For the balance history in the credit card
        //      2. If there are gaps between two balance dates, fill the empty date with the balance of the previous date
        //      3. Given the payload `payload`, calculate the balance different between the payload and the actual balance stored in the database
        //      4. If the different is not 0, update all the following budget with the difference
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/11, balance: 100}, {date: 4/10, balance: 100}],
        //      Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 100}]
        //      This is because
        //      1. You would first populate 4/11 with previous day's balance (4/10), so {date: 4/11, amount: 100}
        //      2. And then you observe there is a +10 difference
        //      3. You propagate that +10 difference until today
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.

        // Check if the payload is valid
        if (payload == null || payload.length == 0) {
            // Payload is invalid, return bad request
            return ResponseEntity.badRequest().body(null);
        }

        Map<String, Double> creditCardBalances = new HashMap<>();

        for (UpdateBalancePayload updateBalancePayload : payload) {
            String creditCardNumber = updateBalancePayload.getCreditCardNumber();
            LocalDate balanceDate = updateBalancePayload.getBalanceDate();
            double balanceAmount = updateBalancePayload.getBalanceAmount();

            // Retrieve the balance history for the current credit card number
            CreditCard creditCard = creditCardRepository.findByNumber(creditCardNumber);

            // if creditCard not found
            if (creditCard == null) {
                return ResponseEntity.badRequest().body(null);
            }

            TreeMap<LocalDate, BalanceHistory> balanceHistory = creditCard.getBalanceHistory();
            if (!balanceHistory.isEmpty()) {
                if (balanceHistory.lastEntry().getKey().isAfter(balanceDate)) {
                    // update from balanceDate to last entry
                    updateBalanceHistoryFromDate(balanceHistory, balanceDate, balanceAmount);
                } else {
                    // Fill gaps in the balance history with the last known balance
                    fillBalanceHistoryGaps(balanceHistory, balanceDate);
                }
            }
            // Update the balance for the current date
            BalanceHistory newBalanceHistory = new BalanceHistory();
            newBalanceHistory.setDate(balanceDate);
            newBalanceHistory.setBalance(balanceAmount);

            // Convert LocalDate to date and add to balanceHistory
            balanceHistory.put(balanceDate, newBalanceHistory);

            // Update the balance history for the current credit card number
            creditCard.setBalanceHistory(balanceHistory);

            // Add credit card number and current balance to the map
            creditCardBalances.put(creditCardNumber, balanceHistory.lastEntry().getValue().getBalance());
        }

        return ResponseEntity.ok(creditCardBalances);

    }

    public void fillBalanceHistoryGaps(TreeMap<LocalDate, BalanceHistory> balanceHistory, LocalDate balanceDate) {
        // Get the last recorded date in balanceHistory
        Map.Entry<LocalDate, BalanceHistory> lastEntry = balanceHistory.lastEntry();
        // Fill in the gap between last date and current date
        LocalDate currentDate = balanceHistory.lastEntry().getKey();

        while (currentDate.isBefore(balanceDate)) { // Iterate until today
            currentDate = currentDate.plusDays(1);
            BalanceHistory newBalanceHistory = new BalanceHistory();
            newBalanceHistory.setDate(currentDate);
            newBalanceHistory.setBalance(lastEntry.getValue().getBalance());
            balanceHistory.put(currentDate, newBalanceHistory);
        }
    }

    public void updateBalanceHistoryFromDate(TreeMap<LocalDate, BalanceHistory> balanceHistory, LocalDate balanceDate, double balanceAmount) {
        double difference = balanceAmount - balanceHistory.get(balanceDate).getBalance();
        // get the start of updateDate
        LocalDate updateDate = balanceDate;
        // get the end of updateDate
        LocalDate currentDate = balanceHistory.lastEntry().getKey().plusDays(1);
        // update each entry in a while loop
        while (updateDate.isBefore(currentDate)) {
            // update the entry with updateDate plus the balanceAmount with difference
            BalanceHistory updatedHistory = balanceHistory.getOrDefault(updateDate, new BalanceHistory());
            double updatedBalance = updatedHistory.getBalance() + difference;
            // check if updatedBalance will be less than 0
            if(updatedBalance <= 0) updatedHistory.setBalance(0);
            // if greater than 0, update with its value
            else updatedHistory.setBalance(updatedBalance);
            // update balanceHistory
            balanceHistory.put(updateDate, updatedHistory);
            // set updateDate to next Date
            updateDate = updateDate.plusDays(1);
        }
    }
}
