package edu.ucla.cens.awserver.dao;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.StringUtils;

/**
 * DAO for saving a media resource to the filesystem and inserting a row in the url_based_resource table that contains a URL to the 
 * resource.
 * 
 * When this class creates directories, it relies on the OS for persmissions set up.
 * 
 * @author selsky
 */
public class UrlBasedResourceDao extends AbstractUploadDao {
	private Object lock = new Object();
	private Pattern _numberRegexp = Pattern.compile("[0-9]+");
	private Pattern _numberRegexpJpg = Pattern.compile("[0-9]+\\.jpg"); // TODO - the file extension should not be hardcoded because
	                                                                    // it prevents this class from being used for other file types
	private static Logger _logger = Logger.getLogger(UrlBasedResourceDao.class);
	private File _currentWriteDir;
	private String _currentFileName;
	private String _fileExtension;
	private int _maxNumberOfDirs;
	private int _maxNumberOfFiles;
	private String _initialFileName; // e.g., 000
	private String _initialDir;      // e.g., 0000
	private static final String _insertSql = "insert into url_based_resource (user_id, uuid, url, client) values (?,?,?,?)";
	
	/**
	 * Creates an instance that uses the provided DataSource for database access; rootDirectory as the root for filesystem
	 * storage; fileExtension for naming saved files with the appropriate extension; maxNumberOfDirs for the maximum number of
	 * storage subdirectories; and maxNumberOfFiles for the maximum number of files per directory. The rootDirectory is used to 
	 * create the initial directory for storage: rootDirectory/000/000/000. 
	 */
	public UrlBasedResourceDao(DataSource dataSource, String rootDirectory, String fileExtension, 
			int maxNumberOfDirs, int maxNumberOfFiles) {
		
		super(dataSource);
		if(StringUtils.isEmptyOrWhitespaceOnly(rootDirectory)) {
			throw new IllegalArgumentException("rootDirectory is required");
		}
		if(StringUtils.isEmptyOrWhitespaceOnly(fileExtension)) {
			throw new IllegalArgumentException("fileExtension is required");
		}
		if(maxNumberOfDirs <= 0 || maxNumberOfDirs > 1000) {
			throw new IllegalArgumentException("maxNumberOfDirs must be greater than zero and <= 1000");
		}
		if(maxNumberOfFiles <= 0 || maxNumberOfFiles > 1000) {
			throw new IllegalArgumentException("maxNumberOfFiles must be greater than zero and <= 1000");
		}
		
		_fileExtension = fileExtension.startsWith(".") ? fileExtension : "." + fileExtension;
		_maxNumberOfDirs = (maxNumberOfDirs % 10 == 0) ? maxNumberOfDirs - 1: maxNumberOfDirs;
		_maxNumberOfFiles = (maxNumberOfFiles % 10 == 0) ? maxNumberOfFiles - 1: maxNumberOfFiles;
		
		_initialFileName = initialName(_maxNumberOfFiles);
		
		init(rootDirectory);
	}
	
	/**
	 * Persists media data to a filesystem location and places a URL to the data into the url_based_resource table. The database
	 * write and filesystem write are handled as one transaction in order to handle duplicate uploads gracefully.
	 */
	@Override
	public void execute(AwRequest awRequest) {
		
		synchronized(lock) { // synch the whole process to ensure each media file gets a unique URL
		
			if(_logger.isDebugEnabled()) {
				_logger.debug("saving a media file to the filesystem and a reference to it in url_based_resource");
			}
			
			final int userId = awRequest.getUser().getId();
			final String client = awRequest.getClient();
			final String uuid = awRequest.getMediaId();
			
			final String url = "file://" + _currentWriteDir + "/" + _currentFileName + _fileExtension;
			
			if(_logger.isDebugEnabled()) {
				_logger.debug("url to file: " + url);
			}
			
			OutputStream outputStream = null;
			
			// Wrap this upload in a transaction 
			DefaultTransactionDefinition def = new DefaultTransactionDefinition();
			def.setName("media upload");
			
			PlatformTransactionManager transactionManager = new DataSourceTransactionManager(getDataSource());
			TransactionStatus status = transactionManager.getTransaction(def); // begin transaction
			
			// a Savepoint could be used here, but since there is only one row to be inserted a 
			// regular rollback() will do the trick.
				
			try {
				
				// first, save the id and location to the db
				
				getJdbcTemplate().update( 
					new PreparedStatementCreator() {
						public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
							PreparedStatement ps = connection.prepareStatement(_insertSql);
							ps.setInt(1, userId);
							ps.setString(2, uuid);
							ps.setString(3, url);
							ps.setString(4, client);
							return ps;
						}
					}
				);
				
				// now save to the file system
				
				File f = new File(new URI(url));
				
				if(! f.createNewFile()) { // bad!! This means the file already exists, but there was no row for it in 
					                      // url_based_resource
					rollback(transactionManager, status);
					f = null;
					throw new DataAccessException("file already exists: " + url); 
				}
				
				outputStream = new BufferedOutputStream(new FileOutputStream(f));
				byte[] bytes = awRequest.getMedia();
				int length = bytes.length;
				int offset = 0;
				int writeLen = 1024;
				int total = 0;
				
				while(total < length) {
					int amountToWrite = writeLen < (length - total) ? writeLen : (length - total);
					outputStream.write(bytes, offset, amountToWrite);
					offset += amountToWrite;
					total += writeLen;
				}
				
				outputStream.close();
				outputStream = null;
				
				// now set the write directory and file name for the next file
				setNextDirAndFile();
				
				// done 
				transactionManager.commit(status);
				
			} catch (DataIntegrityViolationException dive) {
				
				if(isDuplicate(dive)) {
					
					if(_logger.isDebugEnabled()) {
						_logger.info("found a duplicate media upload message. uuid: " + uuid);
					}
					
					handleDuplicate(awRequest, 1); // 1 is passed here because there is only one media resource uploaded at a time
					rollback(transactionManager, status);
					
				} else {
				
					// some other integrity violation occurred - bad!! All of the data to be inserted must be validated
					// before this DAO runs so there is either missing validation or somehow an auto_incremented key
					// has been duplicated
					
					_logger.error("caught DataAccessException", dive);
					rollback(transactionManager, status);
					throw new DataAccessException(dive);
				}
				
			} catch (org.springframework.dao.DataAccessException dae) {
				
				_logger.error("caught DataAccessException when attempting to run the SQL + '" + _insertSql + "' with the following "
						+ "params: " + userId + ", " + uuid + ", " + url, dae);
				rollback(transactionManager, status);
				throw new DataAccessException(dae);
	
			} catch (IOException ioe) {
				
				_logger.error("caught IOException", ioe);
				rollback(transactionManager, status);
				throw new DataAccessException(ioe);

			} catch (URISyntaxException use) {
				
				_logger.error("caught URISyntaxException", use);
				rollback(transactionManager, status);
				throw new DataAccessException(use);
				
			} catch(TransactionException te) {
				
				_logger.error("caught TransactionException when attempting to run the SQL + '" + _insertSql + "' with the following "
					+ "params: " + userId + ", " + uuid + ", " + url, te);
				rollback(transactionManager, status); // attempt to rollback even though the exception was thrown at the transaction level
				throw new DataAccessException(te);
			}
			
			finally { // explicit cleanup for exceptional cases				
				if(null != outputStream) {
					try {
						
						outputStream.close();
						outputStream = null;
						
					} catch (IOException ioe) {
						
						_logger.error("caught IOException trying to close an output stream", ioe);
					}
				}
			}
		}
	}
	
	/**
	 * Attempts to rollback a transaction. 
	 */
	private void rollback(PlatformTransactionManager transactionManager, TransactionStatus transactionStatus) {
		
		try {
			
			_logger.error("rolling back a failed media upload transaction");
			transactionManager.rollback(transactionStatus);
			
		} catch (TransactionException te) {
			
			_logger.error("failed to rollback media upload transaction", te);
			throw new DataAccessException(te);
		}
	}
	
	/**
	 * Sets the current write directory and the current file name by checking the filesystem subtree under the rootDir.
	 */
	private void init(String rootDir) { // e.g., /opt/aw/userdata/images
		
		synchronized(lock) {
			
			File rootDirectory = new File(rootDir);
			
			if(! rootDirectory.isDirectory()) {
				throw new IllegalArgumentException(rootDir + " is not a directory");
			}
			
			File f = null;
			_initialDir = initialName(_maxNumberOfDirs);
			
			if(rootDir.endsWith("/")) {
				f = new File(rootDirectory.getAbsolutePath() + _initialDir + "/" + _initialDir + "/" + _initialDir);
			} else {
				f = new File(rootDirectory.getAbsolutePath() + "/" + _initialDir + "/" + _initialDir + "/" + _initialDir);
			}
			
			if(f.exists()) { // If the initial dir exists, fast-forward to the most recent storage location and use that location  
				             // for the initialization properties
			
				try {
					_currentWriteDir = findStartDir(f.getAbsolutePath());
				} catch (IOException ioe) {
					throw new IllegalStateException(ioe);
				}
				
				_currentFileName = findStartFile(_currentWriteDir);
				
			} else { // first time creating the filesystem subtree
				
				if(! f.mkdirs()) { 
					
					throw new IllegalStateException("cannot create " + f.getAbsolutePath() 
						+ " some of the intermediate dirs may have been created");
				}
				
				_currentWriteDir = f;
				_currentFileName = _initialFileName;
			}
			
			_logger.info("current write dir " + _currentWriteDir.getAbsolutePath());
			_logger.info("current file name " + _currentFileName);
			
		}
	}
	
	/**
	 * Sets up the file name and directory for the next file to be written. This method contains no internal synchronization
	 * and relies on the synchronized block in execute().
	 */
	private void setNextDirAndFile() throws IOException {
		
		if(! directoryHasMaxNumberOfFiles(_currentWriteDir)) { 
			
			_currentFileName = incrementName(_currentFileName, _maxNumberOfFiles);
			
		} else {
			
			File parentDir = _currentWriteDir.getParentFile();
			
			if(directoryHasMaxNumberOfSubdirectories(parentDir)) { // since the current directory is full, a new directory will have
				                                                   // to be created, but only if the number of directories isn't
				                                                   // maxed out
				
				if(directoryHasMaxNumberOfSubdirectories(parentDir.getParentFile())) { 
					// bad!!! the whole subtree is full. if this happens, it means there have been manual changes on the filesystem 
					// or we have a serious amount of data (depending on maxNumberOfFiles and maxNumberOfDirs)
					
					_logger.fatal("media storage filesystem subtree is full!");
					throw new IOException("media storage filesystem subtree is full!");
					
				}
				
				_currentWriteDir = new File(incrementDir(parentDir).getAbsolutePath() + "/" + _initialDir);
				_currentWriteDir.mkdir();
				_currentFileName = _initialFileName;
				
			} else {
				
				_currentWriteDir = incrementDir(_currentWriteDir);
				_currentFileName = _initialFileName;
			}
		}
	}
	
	private boolean directoryHasMaxNumberOfFiles(File dir) {
		NumberFileNameFilter filter = new NumberFileNameFilter(); // this can be an instance var
		if(dir.listFiles(filter).length < _maxNumberOfFiles) { _logger.info(dir.getAbsolutePath() + ":" + dir.listFiles(filter).length);
			return false;
		}
		return true;
	}
	
	private boolean directoryHasMaxNumberOfSubdirectories(File dir) {
		DirectoryFilter filter = new DirectoryFilter(); // this can be an instance var
		if(dir.listFiles(filter).length < _maxNumberOfDirs) {
			return false;
		}
		return true;
	}
	
	private String incrementName(String name, int max) {
		String s = String.valueOf(Integer.parseInt(name) + 1); // Integer.parseInt will gracefully truncate leading zeros
		int len = s.length();
		int zeroPadSize = String.valueOf(max).length() - len;
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < zeroPadSize; i++) {
			builder.append("0");
		}
		return builder.append(s).toString();
	}
	
	private File incrementDir(File dir) throws IOException {
		String path = dir.getAbsolutePath();
		path = path.substring(0, path.lastIndexOf("/") + 1);
		
		String name = incrementName(dir.getName(), _maxNumberOfDirs);
		File f = new File(path + name);
		
		if(! f.mkdir()) {
			throw new IOException("cannot make directory or directory already exists: " + f.getAbsolutePath());
		}
		return f;
	}
	
	private String initialName(int max) {
		int len = String.valueOf(max).length();
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < len; i++) {
			builder.append("0");
		}
		return builder.toString();
	}
	
	private File findStartDir(String path) throws IOException {
		File f = new File(path);
		
		if(! f.exists()) {
			if(f.mkdir()) {
				return f;
			} else {
				throw new IllegalStateException("cannot mkdir " + path);
			}
		} else {
			
			if(directoryHasMaxNumberOfFiles(f)) {
				
				File parentDir = f.getParentFile();
				
				if(directoryHasMaxNumberOfSubdirectories(parentDir)) {
					
					if(directoryHasMaxNumberOfSubdirectories(parentDir.getParentFile())) {
						
						_logger.fatal("media storage filesystem subtree is full!");
						throw new IllegalStateException("media storage filesystem subtree is full!");
						
					} else {
						
						// _logger.info(incrementDir(parentDir).getAbsolutePath() + "/" + _initialDir);
						return findStartDir(incrementDir(parentDir).getAbsolutePath() + "/" + _initialDir);
						
					}
					
				} else {
					
					return findStartDir(incrementSearchDir(f.getAbsolutePath()));
				}
				
			} else {
				return f;
			}
		}
	}
	
	private String incrementSearchDir(String path) {
		String[] splitPath = path.split("/");
		int leafDir = Integer.parseInt(splitPath[splitPath.length - 1]);
		int nodeDir = Integer.parseInt(splitPath[splitPath.length - 2]);
		
		if(leafDir == _maxNumberOfFiles) {
			
			nodeDir++;
			leafDir = 0;
			
		} else {
			
			leafDir++;
		}
		
		String newLeafDir = zeropad(leafDir, _maxNumberOfFiles);
		String newNodeDir = zeropad(nodeDir, _maxNumberOfDirs);
		StringBuilder newPath = new StringBuilder();
		
		for(int i = 0; i < splitPath.length - 2; i++) {
			newPath.append(splitPath[i]);
			newPath.append("/");
		}
		newPath.append(newNodeDir).append("/").append(newLeafDir);
		return newPath.toString();
	}
	
	private String zeropad(int name, int max) {
		int nameLen = String.valueOf(name).length();
		int maxLen = String.valueOf(max).length();
		int numberOfZeros = maxLen - nameLen;
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < numberOfZeros; i++) {
			builder.append("0");
		}
		builder.append(String.valueOf(name));
		return builder.toString();
	}
	
	private String findStartFile(File dir) {
		NumberFileNameFilter filter = new NumberFileNameFilter(); // this can be an instance var
		File[] files = dir.listFiles(filter);
		
		if(files.length > 0) {
			Arrays.sort(files);
			File f = files[files.length - 1];
			// TODO jpg should not be hardcoded!
			return incrementName(f.getName().substring(0, f.getName().lastIndexOf(".jpg")), _maxNumberOfFiles);
		} else {
			return _initialFileName;
		}
	}
	
	/* These FilenameFilters are tricky (and poorly named). The File object passed to accept() is the parent directory of a file 
	 * with the String name */
	
	public class DirectoryFilter implements FilenameFilter {
		public boolean accept(File f, String name) {
			return _numberRegexp.matcher(name).matches();
		}
	}
	
	public class NumberFileNameFilter implements FilenameFilter {
		public boolean accept(File f, String name) {
			return _numberRegexpJpg.matcher(name).matches();
		}
	}
}
