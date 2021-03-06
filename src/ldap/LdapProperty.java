package ldap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * used to read the properties configured in ldap.properties
 *
 */
public class LdapProperty {
	private static Logger logger = Logger.getRootLogger();

	/**
	 * get the value of the given properties name
	 * @param name : properties name
	 * @return the value of the given properties name
	 */
	public static String getProperty(String name){
		logger.debug("getting property from ldap.properties: " + name);
		
		String value = null;
		Properties props = new Properties();
		File home = new File(getCatalinaBase());
		File conf = new File(home, "conf");
		File properties = new File(conf, "ldap.properties");
        try{
        	FileInputStream fis = new FileInputStream(properties);
        	props.load(fis);
        	value = props.getProperty(name);
        	props.clear();
        	fis.close();
		}catch(FileNotFoundException fe){
			props.setProperty("error", "LDAP " + ErrorConstants.CONFIG_FILE_NOTFOUND);
			logger.error("LDAP" + ErrorConstants.CONFIG_FILE_NOTFOUND, fe);
			
		}catch(Exception ex){
			props.setProperty("error", "LDAP " + ErrorConstants.CONFIG_FILE_NOTFOUND);
			logger.error("LDAP" + ErrorConstants.CONFIG_FILE_NOTFOUND, ex);
		}
        
        logger.debug("finished getting property from ldap.properties: " + name);
		return value;
	}

	
	/**
	 * get all the properties names that are available in the ldap.properties
	 * @return an enum of all the properties names that are available in the ldap.properties
	 */
	public static Enumeration<?> propertyNames(){
		logger.debug("getting all properties from ldap.properties");
		
		Enumeration<?> pvalues = null;
		Properties props = new Properties();
		File home = new File(getCatalinaBase());
		File conf = new File(home, "conf");
		File properties = new File(conf, "ldap.properties");
        try{
        	FileInputStream fis = new FileInputStream(properties);
        	props.load(fis);
        	pvalues = props.propertyNames();
        	fis.close();
		}catch(FileNotFoundException fe){
			props.setProperty("error", "LDAP " + ErrorConstants.CONFIG_FILE_NOTFOUND);
			logger.error("LDAP" + ErrorConstants.CONFIG_FILE_NOTFOUND, fe);
			
		}catch(Exception ex){
			props.setProperty("error", "LDAP " + ErrorConstants.CONFIG_FILE_NOTFOUND);
			logger.error("LDAP" + ErrorConstants.CONFIG_FILE_NOTFOUND, ex);
		}
        
        logger.debug("finished getting all properties from ldap.properties");
		return pvalues;
	}
	
	private static String getCatalinaBase() {
        return System.getProperty("catalina.base", getCatalinaHome());
    }
	
    private static String getCatalinaHome() {
        return System.getProperty("catalina.home",
                                  System.getProperty("user.dir"));
    }
}