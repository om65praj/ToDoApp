package com.bridgeit.springToDoApp.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeit.springToDoApp.model.ErrorMessage;
import com.bridgeit.springToDoApp.model.User;
import com.bridgeit.springToDoApp.service.MailService;
import com.bridgeit.springToDoApp.service.UserService;
import com.bridgeit.springToDoApp.token.GenerateJWT;
import com.bridgeit.springToDoApp.token.VerifiedJWT;
import com.bridgeit.springToDoApp.validation.Validator;

@RestController
public class UserController {

	@Autowired
	UserService userService;

	@Autowired
	Validator validator;

	@Autowired
	ErrorMessage message;

	@Autowired
	MailService mailService;

	@RequestMapping(value = "/registerUser", method = RequestMethod.POST)
	public ResponseEntity<String> saveUser(@RequestBody User user) {
		String isValidator = validator.validateSaveUser(user);
		if (isValidator.equals("Success")) {
			userService.saveUser(user);
			mailService.sendEmail("om4java@gmail.com", user.getEmail(), "Welcome to Bridgelabz",
					"Registration successful");
			return new ResponseEntity<String>(isValidator, HttpStatus.OK);
		}
		return new ResponseEntity<String>(isValidator, HttpStatus.CONFLICT);
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ErrorMessage> loginUser(@RequestBody User user, HttpSession session) {
		user = userService.loginUser(user);
		String generatetoken = GenerateJWT.generate(user.getId());
		session.setAttribute("user", user);
		message.setMessage(generatetoken);
		return new ResponseEntity<ErrorMessage>(message, HttpStatus.OK);
	}

	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	public ResponseEntity<ErrorMessage> logout(HttpSession session) {
		session.removeAttribute("user");
		session.invalidate();
		message.setMessage("Logout seccessful");
		return new ResponseEntity<ErrorMessage>(message, HttpStatus.OK);
	}

	@RequestMapping(value = "/forgotpassword", method = RequestMethod.POST)
	public ErrorMessage forgotPassword(@RequestBody User user, HttpServletRequest request) {

		user = userService.emailValidate(user.getEmail());
		System.out.println("valid user with emailID : " + user);

		if (user == null) {
			message.setMessage("Please enter valid emailID");
			message.setStatus(500);
			return message;
		}

		try {
			String generateOTP = GenerateJWT.generate(user.getId());
			mailService.sendEmail("om4java@gmail.com", user.getEmail(), "OTP is :", generateOTP);
			
		} catch (Exception e) {
			e.printStackTrace();
			message.setStatus(400);
			return message;
		}

		message.setMessage("Forget Success");
		message.setStatus(200);
		return message;
	}
	
	
	@RequestMapping(value = "/resetpassword", method = RequestMethod.PUT)
	public ErrorMessage resetPassword(@RequestBody User user) {
		
		System.out.println("Reset user : "+user);
		String email = user.getEmail();
		String password = user.getPassword();
		String otp = user.getFirstName();
		

		System.out.println("OTP: " + otp);
		System.out.println("Inside reset");

		int userId = VerifiedJWT.verify(otp);

		if (userId == 0) {
			message.setMessage("Invalid OTP");
			message.setStatus(500);
			return message;
		}

		user = null;
		
		user = userService.emailValidate(email);
		if (user == null) {
			message.setMessage("No user found on this email id");
			message.setStatus(500);
			return message;
		}

		if (userId != user.getId()) {
			message.setMessage("Invalid OTP");
			message.setStatus(500);
			return message;
		}
		
		user.setPassword(password);
		
		if (userService.updateUser(user)) {
			message.setMessage("Success");
			message.setStatus(200);
			return message;
		} else {
			message.setMessage("Password could not be changed");
			message.setStatus(100);
			return message;
		}
	}

}
