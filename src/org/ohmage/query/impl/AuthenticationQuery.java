/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.query.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import jbcrypt.BCrypt;

import org.ohmage.annotator.Annotator.ErrorCode;
import org.ohmage.domain.User;
import org.ohmage.exception.DataAccessException;
import org.ohmage.query.IAuthenticationQuery;
import org.ohmage.request.UserRequest;
import org.springframework.jdbc.core.RowMapper;

/**
 * Gathers the login information about the user in the request.
 * 
 * @author John Jenkins
 */
public final class AuthenticationQuery extends Query implements IAuthenticationQuery{
	// Gets the user's hashed password from the database.
	private static final String SQL_GET_PASSWORD = 
		"SELECT password " +
		"FROM user " +
		"WHERE username = ?";
	
	// Gets the user's login information.
	private static final String SQL_GET_USER = 
		"SELECT enabled, new_account " + 
        "FROM user " +
        "WHERE username = ? " +
        "AND password = ?";
	
	/**
	 * Container for the results of this Query. This includes information 
	 * specific to the request's user pertaining to their login capabilities.
	 * 
	 * This inner class is static so that it can be used by the static call,
	 * public so that the results of this Query can be understood by its caller,
	 * and final so that no one else tries to subclass it. It specific to this
	 * Query.
	 * 
	 * @author John Jenkins
	 */
	public static final class UserInformation {
		private final boolean enabled;
		private final boolean newAccount;
		
		/**
		 * The only constructor as this class doesn't have default values.
		 * 
		 * @param enabled Whether or not the user's account is enabled.
		 * 
		 * @param newAccount Whether or not the user's account is new.
		 */
		public UserInformation(boolean enabled, boolean newAccount) {
			this.enabled = enabled;
			this.newAccount = newAccount;
		}
		
		/**
		 * Returns whether or not this user's account is enabled.
		 * 
		 * @return Whether or not this user's account is enabled.
		 */
		public boolean getEnabled() {
			return enabled;
		}
		
		/**
		 * Returns whether or not this user's account is new.
		 * 
		 * @return Whether or not this user's account is new.
		 */
		public boolean getNewAccount() {
			return newAccount;
		}
	}
	
	private static AuthenticationQuery instance;
	
	/**
	 * Creates this object.
	 * 
	 * @param dataSource The DataSource to use when querying the database.
	 */
	private AuthenticationQuery(DataSource dataSource) {
		super(dataSource);
		
		instance = this;
	}
	
	/* (non-Javadoc)
	 * @see org.ohmage.query.impl.IAuthenticationQuery#execute(org.ohmage.request.UserRequest)
	 */
	@Override
	public UserInformation execute(UserRequest userRequest) throws DataAccessException {
		User user = userRequest.getUser();
		String hashedPassword;
		
		// Hash the password if necessary.
		if(user.hashPassword()) {
			try {
				String actualPassword = 
					(String) instance.getJdbcTemplate().queryForObject(
							SQL_GET_PASSWORD, 
							new Object[] { user.getUsername() },
							String.class);
				hashedPassword = BCrypt.hashpw(user.getPassword(), actualPassword);
				userRequest.getUser().setHashedPassword(hashedPassword);
			}
			catch(org.springframework.dao.IncorrectResultSizeDataAccessException e) {
				// If there were multiple users with the same username,
				if(e.getActualSize() > 1) {
					throw new DataAccessException("Data integrity issue on user table. More than one user with the same username.", e);
				}
				
				// If there weren't any users, return and let the service
				// handle the lack of results.
				userRequest.setFailed(ErrorCode.AUTHENTICATION_FAILED, "Unknown user or incorrect password.");
				return null;
			}
			catch(org.springframework.dao.DataAccessException e) {
				throw new DataAccessException("Error while executing SQL '" + SQL_GET_PASSWORD + "' with parameter: " + user.getPassword(), e);
			}
		}
		// Otherwise, use the current password.
		else {
			hashedPassword = user.getPassword();
		}
		
		// Get the user's information from the database.
		try {
			UserInformation userInformation = instance.getJdbcTemplate().queryForObject(
					SQL_GET_USER, 
					new Object[] { user.getUsername(), hashedPassword }, 
					new RowMapper<UserInformation>() {
						@Override
						public UserInformation mapRow(ResultSet rs, int rowNum) throws SQLException {
							return new UserInformation(
									rs.getBoolean("enabled"),
									rs.getBoolean("new_account"));
						}
					});
			
			return userInformation;
		}
		catch(org.springframework.dao.IncorrectResultSizeDataAccessException e) {
			// If there were multiple users with the same username,
			if(e.getActualSize() > 1) {
				throw new DataAccessException("Multiple users have the same username.", e);
			}
			
			// If the username wasn't found or the username and password 
			// combination were incorrect.
			userRequest.setFailed(ErrorCode.AUTHENTICATION_FAILED, "Unknown user or incorrect password.");
			return null;
		} 
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException("Error executing SQL '" + SQL_GET_USER + "' with the following parameters: " + 
				user.getUsername() + " (password omitted)", e);
		}
	}
}
