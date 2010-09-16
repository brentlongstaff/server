-- MySQL DDL statements for the AndWellness database

CREATE DATABASE andwellness CHARACTER SET utf8 COLLATE utf8_general_ci;
USE andwellness;

-- --------------------------------------------------------------------
-- Campaign id and name. Allows users to be bound to a campaign without  
-- having a campaign configuration in the system. 
-- --------------------------------------------------------------------
CREATE TABLE campaign (
  id smallint(4) unsigned NOT NULL auto_increment,
  name varchar(250) NOT NULL,
  label varchar(500) default NULL, -- can be used as a separate display label
  PRIMARY KEY (id),
  UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Links surveys (stored in raw XML format) to a campaign. 
-- --------------------------------------------------------------------
CREATE TABLE campaign_configuration (
  id smallint(4) unsigned NOT NULL auto_increment,
  campaign_id smallint(4) unsigned NOT NULL,
  version varchar(250) NOT NULL,
  xml mediumtext NOT NULL, -- the max length for mediumtext is roughly 5.6 million UTF-8 chars
  PRIMARY KEY (id),
  UNIQUE (campaign_id, version),
  CONSTRAINT FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------------------------
-- System users.
-- -----------------------------------------------------------------------
CREATE TABLE user (
  id smallint(6) unsigned NOT NULL auto_increment,
  login_id varchar(15) NOT NULL,
  password varchar(100) NOT NULL,
  enabled bit NOT NULL,
  new_account bit NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ---------------------------------------------------------------------
-- Due to IRB standards, we store personally identifying information
-- separately from the user's login credentials. ** This table is currently
-- unused, but it is kept around in order to avoid changing the command
-- line registration process. **
-- ---------------------------------------------------------------------
CREATE TABLE user_personal (
  id smallint(6) unsigned NOT NULL auto_increment,
  email_address varchar(320),
  json_data text,
  PRIMARY KEY (id)
  -- we will have to check the uniqueness of new email addresses within the application itself because 
  -- the length of the UTF-8 encoded email address exceeds the maximum size for indexing in MySQL.
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- ---------------------------------------------------------------------
-- Link user to user_personal. This is a one-to-one mapping managed by
-- the application i.e., a user cannot have more than one user_personal
-- entry.
-- ---------------------------------------------------------------------
CREATE TABLE user_user_personal (
	user_id smallint(6) unsigned NOT NULL,
	user_personal_id smallint(6) unsigned NOT NULL,
	PRIMARY KEY (user_id, user_personal_id),
	CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE,
	CONSTRAINT FOREIGN KEY (user_personal_id) REFERENCES user_personal (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- User roles.
-- --------------------------------------------------------------------
CREATE TABLE user_role (
  id tinyint(1) unsigned NOT NULL auto_increment,
  label varchar(50) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Bind users to roles and campaigns. A user can have a different role
-- for each campaign they belong to.
-- --------------------------------------------------------------------
CREATE TABLE user_role_campaign (
  id smallint(6) unsigned NOT NULL auto_increment,
  user_id smallint(6) unsigned NOT NULL,
  campaign_id smallint(4) unsigned NOT NULL,
  user_role_id tinyint(1) unsigned NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (user_role_id) REFERENCES user_role (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Stores survey responses for a user in a campaign 
-- --------------------------------------------------------------------
CREATE TABLE survey_response (
  id integer unsigned NOT NULL auto_increment,
  user_id smallint(6) unsigned NOT NULL,
  campaign_configuration_id smallint(4) unsigned NOT NULL,
  
  time_stamp timestamp NOT NULL,
  epoch_millis bigint unsigned NOT NULL, 
  phone_timezone varchar(32) NOT NULL,
  latitude double,
  longitude double,
  json text NOT NULL, -- the max length for text is 21845 UTF-8 chars
  
  PRIMARY KEY (id),
  INDEX (user_id, campaign_configuration_id),
  UNIQUE (user_id, campaign_configuration_id, epoch_millis), -- handle duplicate survey uploads
  
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE,    
  CONSTRAINT FOREIGN KEY (campaign_configuration_id) REFERENCES campaign_configuration (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Prompt types. Types are strings to be used as hints in determining 
-- how to process prompt response data. 
-- --------------------------------------------------------------------
CREATE TABLE prompt_type (
    id tinyint unsigned NOT NULL auto_increment,
    type varchar(50) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Stores individual prompt responses for a user in a campaign. Both
-- the entire survey response and each prompt response in for a survey
-- are stored.
-- --------------------------------------------------------------------
CREATE TABLE prompt_response (
  id integer unsigned NOT NULL auto_increment,
  user_id smallint(6) unsigned NOT NULL,
  survey_response_id integer unsigned NOT NULL,
  prompt_type_id tinyint unsigned NOT NULL,
  
  prompt_id varchar(250) NOT NULL,  -- defined by the prompt id from the survey XML //promptId
  response text NOT NULL,   -- the data format is defined by the prompt type, most likely it will be JSON or a UUID (for images)
   
  PRIMARY KEY (id),
  INDEX (user_id, survey_response_id),
  
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (survey_response_id) REFERENCES survey_response (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (prompt_type_id) REFERENCES prompt_type (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Points a UUID to a URL-based resource (such as an image) 
-- --------------------------------------------------------------------
CREATE TABLE url_based_resource (
    id  integer unsigned NOT NULL auto_increment,
    uuid char (36) NOT NULL,
    url text,
    UNIQUE (uuid), -- disallow duplicates and index on UUID
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- High-frequency "mode only" mobility data. Mobility data is *not*
-- linked to a campaign.
-- --------------------------------------------------------------------
CREATE TABLE mobility_mode_only_entry (
  id bigint unsigned NOT NULL auto_increment,
  user_id smallint(6) unsigned NOT NULL,
  time_stamp timestamp NOT NULL,
  epoch_millis bigint unsigned NOT NULL,
  phone_timezone varchar(32) NOT NULL,
  latitude double,
  longitude double,
  accuracy double,
  provider varchar(250),
  mode varchar(30) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (user_id, epoch_millis), -- enforce no-duplicates rule at the table level
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- High-frequency "mode + features" mobility data. Mobility data is 
-- *not* linked to a campaign.
-- --------------------------------------------------------------------
CREATE TABLE mobility_mode_features_entry (
  id integer unsigned NOT NULL auto_increment,
  user_id smallint(6) unsigned NOT NULL,
  time_stamp timestamp NOT NULL,
  epoch_millis bigint unsigned NOT NULL,
  phone_timezone varchar(32) NOT NULL,
  latitude double,
  longitude double,
  accuracy double,
  provider varchar(250),
  mode varchar(30) NOT NULL,
  speed double NOT NULL,
  variance double NOT NULL,
  average double NOT NULL,
  fft varchar(300) NOT NULL, -- A comma separated list of 10 FFT floating-point values. The reason the array is not unpacked  
                             -- into separate columns is because the data will not be used outside of a debugging scenario.
                             -- It is simply stored the way it is sent by the phone (as a JSON array). 
  PRIMARY KEY (id),
  UNIQUE INDEX (user_id, epoch_millis),
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ---------------------------------------------------------------------------------
-- 5 minute summary of mobility data from mobility_mode_only_entry and
-- mobility_mode_features_entry
-- ---------------------------------------------------------------------------------
CREATE TABLE mobility_entry_five_min_summary (
  id integer unsigned NOT NULL auto_increment,
  user_id smallint(6) unsigned NOT NULL,
  time_stamp timestamp NOT NULL,
  phone_timezone varchar(32) NOT NULL,
  mode varchar(30) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE INDEX (user_id,time_stamp, mode),
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ---------------------------------------------------------------------------------
-- Daily summary of mobility data from mobility_mode_only_entry and
-- mobility_mode_features_entry
-- ---------------------------------------------------------------------------------
CREATE TABLE mobility_entry_daily_summary (
  id integer unsigned NOT NULL auto_increment,
  user_id smallint(6) unsigned NOT NULL,
  entry_date date NOT NULL,
  mode varchar(30) NOT NULL,
  duration smallint (5) unsigned NOT NULL,
  PRIMARY KEY (id),
  UNIQUE INDEX (user_id, entry_date),
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
