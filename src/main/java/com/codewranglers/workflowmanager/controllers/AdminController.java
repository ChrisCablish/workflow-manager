package com.codewranglers.workflowmanager.controllers;


import com.codewranglers.workflowmanager.models.User;
import com.codewranglers.workflowmanager.models.data.UserRepository;
import com.codewranglers.workflowmanager.models.dto.CreateUserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("")
    public String renderAdminPortal(Model model) {
        List<String> pages = new ArrayList<>();
        pages.add("User Management");
        pages.add("Role Management");
        pages.add("Workflow Management");

        List<String> user = new ArrayList<>();
        user.add("Manufacturing Operator");
        user.add("Product Manager");
        user.add("Administrator");

        List<String> urlStrings = new ArrayList<>();
        urlStrings.add("usermanagement");
        urlStrings.add("rolemanagement");
        urlStrings.add("workflowmanagement");

        model.addAttribute("pages", pages);
        model.addAttribute("user", user);
        model.addAttribute("url", urlStrings);
        return "/admin/index";
    }

    @GetMapping("/user_management")
    public String renderUserManagementPortal(Model model) {
        Iterable<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/user_management/index";
    }

    @GetMapping("/user_management/create_user")
    public String renderUserCreationPortal(Model model, HttpSession session) {
        model.addAttribute(new CreateUserDTO());
        return "admin/user_management/create_user";
    }


    @PostMapping("/user_management/create_user")
    public String renderUserCreated(@ModelAttribute @Valid CreateUserDTO createUserDTO, Errors errors, HttpServletRequest request) {

        if (errors.hasErrors()) {
            return "admin/user_management/create_user";
        }
        //check for existing user with username
        User existingUser = userRepository.findByUsername(createUserDTO.getUsername());
        if (existingUser != null) {
            errors.rejectValue("username", "username.alreadyExists", "That username is already in use." );
            return "admin/user_management/create_user";
        }
        //check password and confirmPassword match
        String password = createUserDTO.getPassword();
        String confirmPassword = createUserDTO.getConfirmPassword();
        if (!password.equals(confirmPassword)) {
            errors.rejectValue("password", "password.mismatch", "Passwords do not match." );
            return "admin/user_management/create_user";
        }

        User newUser = new User(createUserDTO.getFirstName(), createUserDTO.getLastName(), createUserDTO.getUsername(), createUserDTO.getEmail(), createUserDTO.getPassword(), createUserDTO.getRole());


        userRepository.save(newUser);
        return "redirect:/admin/user_management";
    }

    @GetMapping("/role_management")
    public String renderRoleManagementPortal() {
        return "/admin/role_management/index";
    }

    @GetMapping("/workflow_management")
    public String renderWorkflowManagementPortal() {
        return "/admin/workflow_management/index";
    }
}
