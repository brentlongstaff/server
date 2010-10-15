package edu.ucla.cens.awserver.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

import edu.ucla.cens.awserver.domain.SimpleUser;
import edu.ucla.cens.awserver.request.AwRequest;

/**
 * DAO for finding all of the users in a paricular campaign.
 * 
 * TODO - only return enabled users? All users are currently enabled by default until we put a more detailed registration process
 * in place.
 * 
 * @author selsky
 */
public class FindAllUsersForCampaignDao extends AbstractDao {
	private static Logger _logger = Logger.getLogger(FindAllUsersForCampaignDao.class);
	
	private String findAllUsersForCampaignSql = "SELECT DISTINCT user_id, login_id" +                 // distinct is used here in order to  
												" FROM user_role_campaign urc, user u, campaign c" +  // avoid multiple rows returned for  
 												" WHERE urc.campaign_id = c.id " +                    // users with multiple roles
												" AND urc.user_id = u.id" +
												" AND c.name = ?";
	
	public FindAllUsersForCampaignDao(DataSource dataSource) {
		super(dataSource);
	}
	
	/**
	 * Returns all of the distinct user ids for the current campaign id of the user in the AwRequest.
	 */
	@Override
	public void execute(AwRequest awRequest) {
		try {
		
			awRequest.setResultList(
				getJdbcTemplate().query(
					findAllUsersForCampaignSql,
					new Object[] {awRequest.getCampaignName()},
					new RowMapper() {
						public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
							SimpleUser su = new SimpleUser();
							su.setId(rs.getInt(1));
							su.setUserName(rs.getString(2));
							return su;
						}
					}
				)
			);
			
		} catch (org.springframework.dao.DataAccessException dae) {
			
			_logger.error("an error occurred running the following SQL '" + findAllUsersForCampaignSql + "' with the parameter " + 
				awRequest.getCampaignName() + ": " + dae.getMessage());
			
			throw new DataAccessException(dae);
			
		}
	}
}
