package servlets;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
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
 * Handle assigning a group to a user
 * This servlet is refered from UserDetails.jsp
 */
public class AddGroupUserServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Logger logger = Logger.getRootLogger();
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AddGroupUserServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Redirect to 'OrganisationDetails.jsp'
		String redirectURL = response.encodeRedirectURL("OrganisationDetails.jsp");
		response.sendRedirect(redirectURL);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.debug("AddGroupUserServlet about to process Post request: " + request.getParameterMap());
		
		NamingEnumeration namingEnum = null;
		HttpSession session = request.getSession(true);
		List<String> ohGroupsThisUserCanAccess = (List<String>)session.getAttribute(AdminServlet.OHGROUPS_ALLOWED_ACCESSED);
		Set<String> baseGroups = null;
		Attribute attr = null;
		LdapTool lt = null;
		boolean userAdded = false;
		
		// create xml string that stores data for responding to client
		StringBuffer sfXml = new StringBuffer();
		response.setContentType("text/xml");
	    sfXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
	    sfXml.append("<response>");
	    
	  //Get dn name of this user
	  	String dn = request.getParameter("dn").trim(); 
	  	//Get name of this group (this is not a dn-name of this group)
	  	String group = null;
	  	boolean getThisUserMemberOf = false;
	  	
	  	// if the request contains parameter:  getAllMemberOf="true"
	  	// it means that the javascript trying to get all the memberOf list in order to refresh the page.
	  	// otherwise, it means the client's javascript trying to add a group into memberOf list
	  	if(request.getParameter("getAllMemberOf")!=null && request.getParameter("getAllMemberOf").trim().equalsIgnoreCase("true")){
	  		getThisUserMemberOf = true;
	  	} else {
	  		group = request.getParameter("groupselect").trim();
	  	}
	  		
	    
	    
		try {
			lt = new LdapTool();
			
			if(group!=null && !getThisUserMemberOf){
				//Add organisation as group
				userAdded = lt.addUserToGroup(dn, lt.getDNFromGroup(group));
			} else {
				// this is the case that we just want to get the list of this user's MemberOf
				// so, we are not adding any group to the list and we just set this boolean to true
				userAdded = true;
			}
			
			// baseGroups will be used to list all groups that this user doesn't belong to
			// namingEnum will be used to list all groups that this user belong to
			Attributes attrs = lt.getUserAttributes(dn);
			attr = attrs.get("memberOf");
			
			if(ohGroupsThisUserCanAccess == null){
				baseGroups = lt.getBaseGroups(); // all the groups stored in LDAP, used to create a list of notMemberOf
			} else {
				baseGroups = lt.getBaseGroupsWithGivenOHGroupsAllowedToBeAccessed(ohGroupsThisUserCanAccess);
			}
				
			if(lt!=null) lt.close();
		} catch (Exception e){
			// preapring a failed response to client
			String value = String.format("<failed>Addition of organisation '%s' to group %s has failed. Reason of the failure: %s.</failed>", 
					StringEscapeUtils.escapeXml(dn), StringEscapeUtils.escapeXml(group), StringEscapeUtils.escapeXml(e.getMessage()));
		    sfXml.append(value);

			logger.debug("Addition of organisation '" + dn + "' to group " + group + " has failed.");
			
			sfXml.append("</response>");
		    response.getWriter().write(sfXml.toString());
		    response.getWriter().flush();
		    response.getWriter().close();
			return;
		}

		// If adding as group successful
		if (userAdded && baseGroups != null) {
			try {
				if(attr != null){
					namingEnum = attr.getAll();
					// add memberOf into the xml that will response to client
					while(namingEnum.hasMore()){
						String thisDN = (String) namingEnum.next();
						thisDN = (String) Rdn.unescapeValue(thisDN);
						String name = LdapTool.getCNValueFromDN(thisDN);
						// remove memberOf from baseGroups (so, after this loop baseGroups cotains only notMemberOf)
						baseGroups.remove(name);
						String value = String.format("<memberOf> <dn>%s</dn> <name>%s</name> </memberOf>", 
								StringEscapeUtils.escapeXml(thisDN), StringEscapeUtils.escapeXml(name));
						sfXml.append(value);
					}
				}
			} catch (NamingException e) {
				// preapring a failed response to client
				String value = String.format("<failed>Addition of organisation '%s' to group %s has been done successfully. But, the groups list cannot be generated because of the groups iteration has failed. Please refresh the page.</failed>", 
						StringEscapeUtils.escapeXml(dn), StringEscapeUtils.escapeXml(group));
			    sfXml.append(value);
			}
			
			// add all notMemberOf into xml that will response to client
			for(String bsGroup : baseGroups){
				String value = String.format("<notMemberOf> %s </notMemberOf>", StringEscapeUtils.escapeXml(bsGroup));
				sfXml.append(value);
			}
	
		    
		    String value = String.format("<passed>'User %s' has been successfully added to group %s.</passed>", 
		    		StringEscapeUtils.escapeXml(dn), StringEscapeUtils.escapeXml(group));
		    sfXml.append(value);
		    
			logger.debug("Organisation has been added to group.");

		// Otherwise, log the error and preapring a failed response to client
		} else {
			String value = String.format("<failed>Addition of organisation '%s' to group %s has failed.</failed>", 
					StringEscapeUtils.escapeXml(dn), StringEscapeUtils.escapeXml(group));
		    sfXml.append(value);
		    
			logger.debug("Addition of organisation '" + dn + "' to group "
					+ group + " has failed.");
		}
		
		
	    sfXml.append("</response>");
	    response.getWriter().write(sfXml.toString());
	    response.getWriter().flush();
	    response.getWriter().close();
	    

		
	}

}

