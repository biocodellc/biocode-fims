package auth;

import fimsExceptions.FimsRuntimeException;
import fimsExceptions.ServerErrorException;
import com.entrust.identityGuard.authenticationManagement.wsv9.*;
import com.entrust.identityGuard.common.ws.TestConnectionImpl;
import com.entrust.identityGuard.common.ws.TimeInterval;
import com.entrust.identityGuard.common.ws.URIFailoverFactory;
import com.entrust.identityGuard.failover.wsv9.AuthenticationFailoverService_ServiceLocator;
import com.entrust.identityGuard.failover.wsv9.FailoverCallConfigurator;
import utils.SettingsManager;

import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;

/**
 * Authenticate users against Entrust Identity Guard Server
 */
public class EntrustIGAuthentication {
    private static AuthenticationTypeEx authtype = AuthenticationTypeEx.QA;
    private static String[] igURLs;
    static SettingsManager sm;

    /**
     * The binding used to make the service calls
     */
    private static AuthenticationServiceBindingStub ms_serviceBinding = null;

    /**
     * The Failover URI factory
     */
    private static URIFailoverFactory failoverFactory;

    /**
     * Failover: Restore time to preferred server (seconds)
     */
    private static int restoreTimeToPreferred = 3600;

    /**
     * Failover: Holdoff time before rechecking a failed server (seconds)
     */
    private static int failedServerHoldoffTime = 600;

    /**
     * Failover: Number of retries to attempt
     */
    private static int numberOfRetries = 1;

    /**
     * Failover: Delay between retries (ms)
     */
    private static int delayBetweenRetries = 500;

    /**
     * Load settings manager
     */
    static {
        // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();
        // Get the IG servers from property file
        String igURIs = sm.retrieveValue("igServers");
        if (igURIs.contains(",")) igURLs = igURIs.split(",");
        else {
            igURLs = new String[1];
            igURLs[0] = igURIs;
        }

        failoverFactory = new URIFailoverFactory(
                igURLs,
                new TimeInterval(restoreTimeToPreferred),
                new TimeInterval(failedServerHoldoffTime),
                new TestConnectionImpl());
    }

    /**
     * Returns the generic challenge for the user. This is used for 2-factor authentication.
     *
     * @param userId The userid for which a challenge is requested
     */
    public String[] getGenericChallenge(String userId) {
        GenericChallengeParmsEx parms = new GenericChallengeParmsEx();
        // set the authtype to QA
        parms.setAuthenticationType(authtype);
        // TODO: allow an unspecified amount of QA challenge questions?
        // limit the QA challenge size to 2 currently as that is all that we allow.
        parms.setQAChallengeSize(2);

        try {
            GenericChallengeEx challengeSet =
                    getBinding().getGenericChallengeEx(
                            new GetGenericChallengeExCallParms(userId, parms));

            if (challengeSet.getChallengeRequestResult().equals(
                    ChallengeRequestResult.CHALLENGE) &&
                    challengeSet.getType() == authtype) {
                return challengeSet.getQAChallenge();
            } else {
                throw new ServerErrorException();
            }
        } catch (AuthenticationFault ex) {
            if (ex.getErrorCode() == ErrorCode.USER_LOCKED) {
                throw new FimsRuntimeException("User account is locked. Please try again later", "user account is locked", 401, ex);
            } else {
                // TODO: this throws "Server Error" if user actually doesn't exist it should say something like "Invalid User/Password Combination"
                throw new ServerErrorException("Server Error","Either user doesn't exist or there was a problem connecting to the server", ex);
            }
        } catch (RemoteException ex) {
            throw new ServerErrorException("Server Error","Either user doesn't exist or there was a problem connecting to the server", ex);
        }
    }

    /**
     * Authenticates generic challenge for user.<br>
     * This method is used in the generic authentication mechanism. The
     * getGenericChallenge method must * be called before invoking this
     * method.
     */
    public boolean authenticateGenericChallange(String userId, String[] challengeResponse) {
        GenericAuthenticateParmsEx parms = new GenericAuthenticateParmsEx();
        // set the authtype to QA
        parms.setAuthenticationType(authtype);

        try {
            Response response = new Response(null, challengeResponse, null);
            GenericAuthenticateResponseEx resp =
                    getBinding().authenticateGenericChallengeEx(
                            new AuthenticateGenericChallengeExCallParms(
                                    userId,
                                    response,
                                    parms));

        } catch (AuthenticationFault ex) {
            if (ex.getErrorCode() == ErrorCode.USER_NO_CHALLENGE) {
                throw new FimsRuntimeException("Error while logging in. Please try again.", null, 400, ex);
            } else if (ex.getErrorCode() == ErrorCode.USER_LOCKED || ex.getErrorCode() == ErrorCode.AUTH_FAILED_USER_LOCKED) {
                String msg;
                Integer entrustLockout = Integer.parseInt(sm.retrieveValue("entrustLockout"));
                if (ex.getErrorCode() == ErrorCode.USER_LOCKED) {
                    msg = "User account is locked. Your account will unlock " + entrustLockout + "mins after fist becoming locked.";
                } else {
                    msg = "User account is locked. You account will unlock in " + entrustLockout + "mins.";
                }
                throw new FimsRuntimeException(msg, "user account is locked", 401, ex);
            } else if (ex.getErrorCode() == ErrorCode.INVALID_RESPONSE) {
                // Parse remaining attempts from exception message
                String remainingAttempts =  ex.getMessage().split("Invalid response to a challenge. ")[1].substring(0, 1);
                throw new FimsRuntimeException("One or more answers are incorrect. " + remainingAttempts + " attempts remaining.",
                                                "Invalid Challenge Response", 401, ex);
            } else {
                throw new ServerErrorException(ex);
            }
        } catch (RemoteException ex) {
            throw new ServerErrorException(ex);
        }

        return true;
    }

    /**
     * Get the authenticate service binding.
     *
     * @return The binding used to invoke service operations.
     *
     * @throws ServerErrorException If binding could not be created
     */
    private static AuthenticationServiceBindingStub getBinding() {
        if (ms_serviceBinding == null) {
            FailoverCallConfigurator failoverConfig =
                    new FailoverCallConfigurator(numberOfRetries, delayBetweenRetries);

            AuthenticationService_ServiceLocator locator =
                    new AuthenticationFailoverService_ServiceLocator(failoverFactory,
                            failoverConfig);
            try {
                ms_serviceBinding =
                        (AuthenticationServiceBindingStub) locator.getAuthenticationService();
            } catch (ServiceException ex) {
                throw new ServerErrorException(
                        "Server Error", "Problem with Entrust Identity Guard Connection. It is likely we can't locate the server.", ex);
            }
        }
        return ms_serviceBinding;
    }

}
