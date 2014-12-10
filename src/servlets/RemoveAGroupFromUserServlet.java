package servlets;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.Rdn;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.LdapTool;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;

/**
 * Servlet implementation class AddOrganisationServlet
 * 
 * Handle assigning a group to an organization
 * This servlet is refered from Organisations.jsp
 */
public class RemoveAGroupFromUserServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
       

	/**
	 * 
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.debug("RemoveAGroupFromUserServlet about to process Post request: " + request.getQueryString());
		
		//Get user DN 
		String userDN = request.getParameter("userDN").trim(); 
		//Get desired group DN
		String groupDN = request.getParameter("groupDN").trim();
		
		NamingEnumeration namingEnum = null;
		HttpSession session = request.getSession(true);
		List<String> ohGroupsThisUserCanAccess = (List<String>)session.getAttribute(AdminServlet.OHGROUPS_ALLOWED_ACCESSED);
		Set<String> baseGroups = null;
		LdapTool lt = null;
		
		// create xml string that stores data that need to be responded to client
		StringBuffer sfXml = new StringBuffer();
		response.setContentType("text/xml");
	    sfXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
	    sfXml.append("<response>");
		
		try {
			
			lt = new LdapTool();
			//remove group from user
			lt.removeUserFromAGroup(userDN, groupDN);
			
			Attributes attrs = lt.getUserAttributes(userDN); // all the groups that are memberOf userDN
			Attribute attr = attrs.get("memberOf");
			
			if(ohGroupsThisUserCanAccess == null){
				baseGroups = lt.getBaseGroups(); // all the groups stored in LDAP, used to create a list of notMemberOf
			} else {
				baseGroups = lt.getBaseGroupsWithGivenOHGroupsAllowedToBeAccessed(ohGroupsThisUserCanAccess);
			}
			
			if(lt!=null) lt.close();
			
			if(attr != null){
				namingEnum = attr.getAll();
				// remove all the memberOf groups from baseGroups
				// so, the result, baseGroups will contains only those groups that userDN is not a memberOf that groups
				while(namingEnum.hasMore()){
					String thisDN = (String) namingEnum.next();
					thisDN = (String) Rdn.unescapeValue(thisDN);
					String name = LdapTool.getCNValueFromDN(thisDN);
					baseGroups.remove(name);
					
					String value = String.format("<memberOf> <dn>%s</dn> <name>%s</name> </memberOf>", 
							StringEscapeUtils.escapeXml(thisDN), StringEscapeUtils.escapeXml(name));
					sfXml.append(value);
				}
			}
			
			// assign those notMemberOfGroups into the xml response string
			for(String bsGroup : baseGroups){
				String value = String.format("<notMemberOf> %s </notMemberOf>", StringEscapeUtils.escapeXml(bsGroup));
				sfXml.append(value);
			}
			
			// If removal is successful, response with a "passed" tag.
			String value = String.format("<passed></passed>");
		    sfXml.append(value);
		    
			logger.debug(String.format("Group '%s' has been removed from user '%s' successfully.", 
					StringEscapeUtils.escapeXml(groupDN), StringEscapeUtils.escapeXml(userDN)));
			
		} catch (Exception e){
			// if failed
			String value = String.format("<failed>Reason of the failure: %s.</failed>", StringEscapeUtils.escapeXml(e.getMessage()));
			sfXml.append(value);
			
			logger.debug("Removal of user: '" + userDN + "' from group " + groupDN + " has failed.", e);
		}

		sfXml.append("</response>");
	    response.getWriter().write(sfXml.toString());
	    response.getWriter().flush();
	    response.getWriter().close();
	}

}

