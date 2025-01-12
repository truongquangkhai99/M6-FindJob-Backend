package com.m6findjobbackend.controller;


import com.m6findjobbackend.dto.request.ChangePassword;
import com.m6findjobbackend.dto.request.SignInForm;
import com.m6findjobbackend.dto.request.SignUpForm;
import com.m6findjobbackend.dto.response.JwtResponse;
import com.m6findjobbackend.dto.response.ResponeAccount;
import com.m6findjobbackend.dto.response.ResponseMessage;
import com.m6findjobbackend.model.*;
import com.m6findjobbackend.model.email.MailObject;
import com.m6findjobbackend.security.jwt.JwtProvider;
import com.m6findjobbackend.security.userprincipal.UserDetailServices;
import com.m6findjobbackend.security.userprincipal.UserPrinciple;
import com.m6findjobbackend.service.account.AccountService;
import com.m6findjobbackend.service.company.CompanyService;
import com.m6findjobbackend.service.company.ICompanyService;
import com.m6findjobbackend.service.email.EmailServiceImpl;
import com.m6findjobbackend.service.role.RoleService;
import com.m6findjobbackend.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@RestController
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    AccountService accountService;
    @Autowired
    RoleService roleService;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    JwtProvider jwtProvider;
    @Autowired
    UserDetailServices userDetailService;
    @Autowired
    CompanyService companyService;

    @Autowired
    EmailServiceImpl emailService;

    @Autowired
    UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> register(@Valid @RequestBody SignUpForm signUpForm) {
        if (accountService.existsByUsername(signUpForm.getUsername())) {
            return new ResponseEntity<>(new ResponeAccount("no_user", -1L), HttpStatus.OK);
        }
        String passwordOld = signUpForm.getPassword();
        Account account = new Account(signUpForm.getUsername(), passwordEncoder.encode(passwordOld));
        Set<String> strRoles = signUpForm.getRoles();
        Set<Role> roles = new HashSet<>();
        strRoles.forEach(role -> {
            switch (role) {
                case "admin":
                    Role adminRole = roleService.findByName(RoleName.ADMIN).orElseThrow(
                            () -> new RuntimeException("Role not found")
                    );
                    roles.add(adminRole);
                    break;
                case "company":
                    Role pmRole = roleService.findByName(RoleName.COMPANY).orElseThrow(() -> new RuntimeException("Role not found"));
                    roles.add(pmRole);
                    int min = 10000000;
                    int max = 99999999;
                    String passwordNew = String.valueOf((int) Math.floor(Math.round((Math.random() * (max - min + 1) + min))));
                    account.setPassword(passwordEncoder.encode(passwordNew));
                    MailObject mailObject = new MailObject("findJob@job.com",account.getUsername(), "Account Paso Verified", "Tài Khoản Của Bạn Là"+" \nusername:" +account.getUsername() + "\npassword: " + passwordNew );
                    emailService.sendSimpleMessage(mailObject);
                    break;
                default:
                    Role userRole = roleService.findByName(RoleName.USER).orElseThrow(() -> new RuntimeException("Role not found"));
                    MailObject mailObject1 = new MailObject("findJob@job.com", account.getUsername(), "Account Paso Verified", "Tài khoản của bạn là: username: " + account.getUsername() + "\npassword: " + passwordOld);
                    emailService.sendSimpleMessage(mailObject1);
                    roles.add(userRole);
            }
        });

        account.setRoles(roles);
        account.setStatus(Status.NON_ACTIVE);
        accountService.save(account);
        return new ResponseEntity<>(new ResponeAccount("Yes", account.getId()), HttpStatus.OK);
    }




    @PostMapping("/signin")
    public ResponseEntity<?> login(@RequestBody SignInForm signInForm) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signInForm.getUsername(), signInForm.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtProvider.createToken(authentication);
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        Long id = ((UserPrinciple) authentication.getPrincipal()).getId();
        String a = authentication.getAuthorities().toString();
        Long idCustom = -1L;
//        System.out.println("dinh " + userPrinciple.getStatus().);

        if (userPrinciple.getStatus().equalsIgnoreCase(String.valueOf(Status.NON_ACTIVE))){
            return new ResponseEntity<>(new ResponseMessage("LOCK"),HttpStatus.OK);
        }
        if (a.equals("[COMPANY]")) {
            Optional<Company> company = companyService.findAllByAccount_Id(id);
            idCustom = company.get().getId();
        }
        if (a.equals("[USER]")) {
            Optional<User> user = userService.findByAccount_Id(id);
            idCustom = user.get().getId();
        }
        if (a.equals("[ADMIN]")) {
            idCustom = -10L;
        }


        return ResponseEntity.ok(new JwtResponse(id, idCustom, token, userPrinciple.getUsername(), userPrinciple.getAuthorities()));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePassword changePassword) {
        Account account = userDetailService.getCurrentUser();
        if (account.getUsername().equals("Anonymous")) {
            return new ResponseEntity<>(new ResponseMessage("Please login"), HttpStatus.OK);
        }
        account.setPassword(passwordEncoder.encode(changePassword.getPassword()));
        accountService.save(account);
        return new ResponseEntity<>(new ResponseMessage("yes"), HttpStatus.OK);
    }
//    @PutMapping("/change-avatar")
//    public ResponseEntity<?> updateAvatar(@RequestBody ChangeAvatar avatar){

    //
//    }
    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        Optional<Account> account = accountService.findById(id);
        System.out.println(account.get().getStatus());
        if (!account.isPresent()) {
            return new ResponseEntity<>(new ResponseMessage("Không có user này"), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(account.get().getStatus(), HttpStatus.OK);
    }


    @GetMapping("/verify/{id}")
    public ResponseEntity<Account> verifyAccount(@PathVariable Long id) {
        Optional<Account> account = accountService.findById(id);
        if (!account.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        account.get().setStatus(Status.ACTIVE);
        accountService.save(account.get());
        return new ResponseEntity<>(account.get(), HttpStatus.OK);
    }


    @GetMapping("/showAllAccount")
    public ResponseEntity<?> showAllAccount(){
        List<Account> accounts = (List<Account>) accountService.findAll();
        for (int i = 0; i <accounts.size(); i++) {
            if (accounts.get(i).getStatus().equals(Status.NON_ACTIVE)){
                accounts.get(i).setStatus2(false);
            } else {
                accounts.get(i).setStatus2(true);
            }
        }
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @PutMapping("/editStatusAccount/{id}")
    public ResponseEntity<?> editStatus(@PathVariable Long id) {
        Optional<Account> accountOptional = accountService.findById(id);
        if (accountOptional.get().getStatus().equals(Status.NON_ACTIVE)) {
            accountOptional.get().setStatus2(true);
            accountOptional.get().setStatus(Status.ACTIVE);
        } else {
            accountOptional.get().setStatus2(false);
            accountOptional.get().setStatus(Status.NON_ACTIVE);

        }
        accountService.save(accountOptional.get());
        return new ResponseEntity<>(new ResponseMessage("yes"), HttpStatus.OK);
    }
}
