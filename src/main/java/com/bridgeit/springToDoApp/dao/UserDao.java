package com.bridgeit.springToDoApp.dao;

import com.bridgeit.springToDoApp.model.Token;
import com.bridgeit.springToDoApp.model.User;

public interface UserDao {

	int saveUser(User user);

	User loginUser(User user);

	User emailValidation(String email);

	User getUserById(int id);

	boolean updateUser(User user);

	
	//public void addToken(Token token);
	
	//public Token getToken(String accessToken);

}
