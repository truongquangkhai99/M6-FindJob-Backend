package com.m6findjobbackend.controller;


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
    public ResponseEntity<?> register(@Valid @RequestBody SignUpForm signUpForm){
        if(accountService.existsByUsername(signUpForm.getUsername())){
            return new ResponseEntity<>(new ResponeAccount("no_user", -1L), HttpStatus.OK);
        }
        int min = 10000000;
        int max = 99999999;
        String newPassword = String.valueOf((int) Math.floor(Math.round((Math.random() * (max - min + 1) + min))));
        Account account = new Account(signUpForm.getUsername(),passwordEncoder.encode(newPassword));
        Set<String> strRoles = signUpForm.getRoles();
        Set<Role> roles = new HashSet<>();
        strRoles.forEach(role ->{
            switch (role){
                case "admin":
                    Role adminRole = roleService.findByName(RoleName.ADMIN).orElseThrow(
                            ()-> new RuntimeException("Role not found")
                    );
                    roles.add(adminRole);
                    break;
                case "company":
                    Role pmRole = roleService.findByName(RoleName.COMPANY).orElseThrow( ()-> new RuntimeException("Role not found"));
                    roles.add(pmRole);
                    break;
                default:
                    Role userRole = roleService.findByName(RoleName.USER).orElseThrow( ()-> new RuntimeException("Role not found"));
                    roles.add(userRole);
            }
        });
        MailObject mailObject = new MailObject("findJob@job.com",account.getUsername(), "Account Tinder Windy Verified", "Tài khoản của bạn là: username: " +account.getUsername() + "\npassword: " + newPassword );
        emailService.sendSimpleMessage(mailObject);
        account.setRoles(roles);
        account.setStatus(Status.NON_ACTIVE);
        accountService.save(account);
        return new ResponseEntity<>(new ResponeAccount("Yes", account.getId()),HttpStatus.OK);
    }
    @PostMapping("/signin")
    public ResponseEntity<?> login(@Valid @RequestBody SignInForm signInForm){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signInForm.getUsername(), signInForm.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtProvider.createToken(authentication);
        UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
        Long id = ((UserPrinciple) authentication.getPrincipal()).getId();
        String a = authentication.getAuthorities().toString();
        Long idCustom = -1L;
        if(a.equals("[COMPANY]")){
            Optional<Company> company = companyService.findAllByAccount_Id(id);
            idCustom = company.get().getId();
        }
        if(a.equals("[USER]")){
            Optional<User> user = userService.findByAccount_Id(id);
            idCustom = user.get().getId();
        }
        if(a.equals("[ADMIN]")){
            idCustom = -10L;
        }

        return ResponseEntity.ok(new JwtResponse(id,idCustom,token, userPrinciple.getUsername(),userPrinciple.getAuthorities()));
    }
//    @PutMapping("/change-avatar")
//    public ResponseEntity<?> updateAvatar(@RequestBody ChangeAvatar avatar){
//        User user = userDetailService.getCurrentUser();
//        if(user.getUsername().equals("Anonymous")){
//            return new ResponseEntity<>(new ResponMessage("Please login"),HttpStatus.OK);
//        }
//        user.setAvatar(avatar.getAvatar());
//        accountService.save(user);
//        return new ResponseEntity<>(new ResponMessage("yes"),HttpStatus.OK);
//
//    }
}
