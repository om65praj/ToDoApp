package com.bridgeit.springToDoApp.service;

import com.bridgeit.springToDoApp.model.Token;
import com.bridgeit.springToDoApp.model.User;

public interface UserService {

	int saveUser(User user);

	User loginUser(User user);

	User emailValidate(String email);

	User getUserById(int id);

	boolean updateUser(User user);

	//public void addToken(Token token);
	
	//public Token getToken(String AccessToken);
}
