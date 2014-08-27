package settings;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filter which checks for the user in the session, if no user is present, the redirect to login page
 */
public class secureFilter implements Filter {
    private FilterConfig fc = null;

    public void init (FilterConfig fc)
        throws ServletException {
        this.fc = fc;
    }

    public void destroy() {
        this.fc = null;
    }

    public void doFilter (ServletRequest req,
                          ServletResponse res,
                          FilterChain filterchain)
        throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession();
        System.out.println("here: " + session.getAttribute("user") + " that should be a user");
        if (session.getAttribute("user") == null) {
            response.sendRedirect("/fims/rest/authenticationService/login");
        }
        filterchain.doFilter(req, res);
    }
}

