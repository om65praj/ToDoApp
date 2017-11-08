package com.bridgeit.springToDoApp.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeit.springToDoApp.Utility.Encryption;
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

	@Autowired
	Encryption encryption;

	
	private Logger logger = (Logger) LogManager.getLogger(UserController.class);
	

	@RequestMapping(value = "/registerUser", method = RequestMethod.POST)
	public ResponseEntity<String> saveUser(@RequestBody User user, HttpServletRequest request) {

		String isValidator = validator.validateSaveUser(user);
		if (isValidator.equals("Success")) {
			user.setActive(false);
			int id = userService.saveUser(user);
			logger.info("Registration successful");
			if (id != 0) {
				String activeToken = GenerateJWT.generate(id);
				String url = request.getRequestURL().toString();
				url = url.substring(0, url.lastIndexOf("/")) + "/" + "verifyMail/" + activeToken;
				try {
					mailService.sendEmail("om4java@gmail.com", user.getEmail(), "Welcome to bridgelabz", url);
					logger.info("Please login email");
					return new ResponseEntity<String>(isValidator, HttpStatus.OK);
				} catch (MailException e) {
					logger.error("Mail don't send");
					e.printStackTrace();
				}
			}
		}
		return new ResponseEntity<String>(isValidator, HttpStatus.CONFLICT);
	}

	@RequestMapping(value = "/verifyMail/{activeToken:.+}", method = RequestMethod.GET)
	public ResponseEntity<ErrorMessage> verifyMail(@PathVariable("activeToken") String activeToken,
			HttpServletResponse response) throws IOException {
		User user = null;
		int id = VerifiedJWT.verify(activeToken);
		try {
			user = userService.getUserById(id);
			logger.info("User details "+user);
		} catch (Exception e) {
			logger.error("user not found ");
			e.printStackTrace();
		}
		user.setActive(true);
		try {
			userService.updateUser(user);
			logger.info("Account is activated ");
		} catch (Exception e) {
			logger.error("Account is not activated");
			e.printStackTrace();
		}
		message.setStatus(200);
		logger.info("user Email id verified successfully now plzz login....");
		message.setMessage("user Email id verified successfully now plzz login....");
		response.sendRedirect("http://localhost:8080/ToDoApp/#!/login");
		return new ResponseEntity<ErrorMessage>(message, HttpStatus.OK);
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ErrorMessage> loginUser(@RequestBody User user, HttpSession session) {
		user = userService.loginUser(user);
		logger.info("Login successful "+user);
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
		logger.info("Logout seccessful ");
		return new ResponseEntity<ErrorMessage>(message, HttpStatus.OK);
	}

	@RequestMapping(value = "/forgotpassword", method = RequestMethod.POST)
	public ErrorMessage forgotPassword(@RequestBody User user, HttpServletRequest request, HttpSession session) {

		String url = request.getRequestURL().toString();
		int lastIndex = url.lastIndexOf("/");
		String urlofForgotPassword = url.substring(0, lastIndex) + "#!/resetpassword";
		user = userService.emailValidate(user.getEmail());
		if (user == null) {
			message.setMessage("Please enter valid emailID");
			message.setStatus(500);
			logger.debug("Please enter valid emailID");
			return message;
		}
		try {
			String generateOTP = GenerateJWT.generate(user.getId());
			session.setAttribute("Token", generateOTP);
			mailService.sendEmail("om4java@gmail.com", user.getEmail(), "", urlofForgotPassword + "");
		} catch (Exception e) {
			logger.error("email don't match");
			e.printStackTrace();
			message.setStatus(400);
			return message;
		}
		logger.info("Forgot password seccessful");
		message.setMessage("Forget Success");
		message.setStatus(200);
		return message;
	}

	@RequestMapping(value = "/resetpassword", method = RequestMethod.PUT)
	public ErrorMessage resetPassword(@RequestBody User user, HttpSession session) {

		String email = user.getEmail();
		String password = encryption.encryptPassword(user.getPassword());
		int userId = VerifiedJWT.verify((String) session.getAttribute("Token"));
		logger.info("UserId is "+userId);
		if (userId == 0) {
			logger.error("UserId is null "+userId);
			message.setMessage("Invalid OTP : ");
			message.setStatus(500);
			return message;
		}

		user = userService.emailValidate(email);
		if (user == null) {
			logger.error("User email is null "+user);
			message.setMessage("User not found :");
			message.setStatus(500);
			return message;
		}

		user.setPassword(password);
		if (userService.updateUser(user)) {
			logger.info("Password update is successful ");
			message.setMessage("Reset password is success :");
			message.setStatus(200);
			return message;
		} else {
			logger.error("Reset password unseccessful");
			message.setMessage("Password could not be changed");
			message.setStatus(-200);
			return message;
		}
	}

}
