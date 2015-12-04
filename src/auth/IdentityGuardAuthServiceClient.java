package auth;
//===========================================================================
//
// Copyright 2004-2010 Entrust. All rights reserved.
//
// This file implements a sample Entrust IdentityGuard Authentication
// application
//
//===========================================================================

import com.entrust.identityGuard.authenticationManagement.wsv9.*;
import com.entrust.identityGuard.common.ws.ConsoleLoggerImpl;
import com.entrust.identityGuard.common.ws.TestConnectionImpl;
import com.entrust.identityGuard.common.ws.TimeInterval;
import com.entrust.identityGuard.common.ws.URIFailoverFactory;
import com.entrust.identityGuard.failover.wsv9.AuthenticationFailoverService_ServiceLocator;
import com.entrust.identityGuard.failover.wsv9.FailoverCallConfigurator;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * This application serves as a sample of the Entrust IdentityGuard
 * Authentication client library.
 * <p/>
 * It provides examples of the Authentication APIs via an interactive shell
 * that allows invoking any of the APIs to perform challenge and authentication
 * requests.
 * The APIs used in this samples are:
 * - ping
 * - getAnonymousChallenge
 * - getAnonymousChallengeForGroup
 * - authenticateAnonymousChallenge
 * - getAllowedAuthenticationTypes
 * - getAllowedAuthenticationTypesForGroup
 * - getGenericChallenge
 * - authenticateGenericChallenge
 * <p/>
 * Usage: <br>
 * IdentityGuardAuthServiceClient ([-host <host>] [-port <port>])|[-url <url>]
 */
public class IdentityGuardAuthServiceClient {

    /**
     * Shell prompt
     */
    private static final String PROMPT = "> ";

    /**
     * TAB for shell output
     */
    private static final String TAB = "    ";

    static private String dateFormat(Calendar c, String empty) {
        if (c == null) return empty;

        if (c.getTime().equals(new Date(0))) return empty;

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(c.getTime());
    }

    static private String status(Boolean val) {
        if (val == null) return "not performed";
        if (val.booleanValue()) {
            return "passed";
        }
        return "failed";
    }

    static private String convert(String val) {
        if (val == null) return "";
        return val;
    }

    static private Integer getInteger(String val, String param) {
        if (val == null) {
            return null;
        }
        try {
            Integer i = new Integer(val);
            return i;
        } catch (Exception e) {
            if (param != null) {
                System.out.println("The parameter " + param + " must be an integer value.");
            }
            return null;
        }
    }

    /**
     * read PEM data from the named file and return it as a string
     */
    private static String readDataFromFile(String file) throws Exception {
        String data = "";
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(file));
            String line = r.readLine();
            while (line != null) {
                line = line.trim();
                if (line.indexOf("----") == -1) {
                    data += line;
                }
                line = r.readLine();
            }
        } finally {
            if (r != null) r.close();
        }

        return data;
    }

    /**
     * read binary data from the named file
     */
    private static byte[] readBinaryDataFromFile(String file) throws Exception {
        File f = new File(file);
        if (!f.canRead()) {
            throw new Exception("File " + file + " does not exist.");
        }

        byte[] data = new byte[(int) f.length()];

        FileInputStream fi = null;
        try {
            fi = new FileInputStream(f);
            fi.read(data);
            return data;
        } finally {
            if (fi != null) fi.close();
        }

    }

    /**
     * welcome message
     */
    private static String welcome_msg =
            "\nWelcome to the Entrust IdentityGuard Authentication Service sample " +
                    "application.\n";

    private static String description =
            "This application provides example usages of the Entrust IdentityGuard\n" +
                    "Authentication API, an interface that can be used to retrieve challenge\n" +
                    "requests and authenticate user responses.\n" +
                    "\n" +
                    "The following authentication mechanisms can be implemented using\n" +
                    "the APIs:\n" +
                    "\n" +
                    "    - one-step authentication:\n" +
                    "        1. getAnonymousChallenge or getAnonymousChallengeForGroup\n" +
                    "        2. authenticateAnonymousChallenge\n" +
                    "    - two-step generic authentication\n" +
                    "        1. getGenericChallenge\n" +
                    "        2. authenticateGenericChallenge\n" +
                    "\n" +
                    "To display all available commands, type 'help'.\n";

    /**
     * The service url where the IG Authentication Web Service has been deployed
     */
    private static ArrayList ms_serviceURL = new ArrayList();

    /**
     * The last received anonymous challenge. It will be used for anonymous
     * challenge authentications
     */
    private static ChallengeSet ms_cachedAnonymousChallengeSet = null;

    /**
     * true if stack traces are to be shown for errors or false otherwise
     */
    private static boolean ms_debug = false;

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
     * Failover: Whether to show log messages or not
     */
    private static boolean verbose = false;

    /**
     * Main implementation allowing running of this class.
     *
     * @param args Accept the url pointing to the Entrust IdentityGuard
     *             authentication service.
     */
    public static void main(String[] args) {
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        try {
            // Check for service url override
            ArrayList<String> url = new ArrayList<String>();
            String host = null;
            String port = null;

            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-host")) {
                    if (url.size() > 0) {
                        log("Error: specifying both -host and -url " +
                                "is not allowed.");
                        System.exit(1);
                    }
                    if (host != null) {
                        log("Error: -host specified multiple times.");
                        System.exit(1);
                    }
                    i += 1;
                    if (i >= args.length) {
                        log("Error: -host missing argument.");
                        System.exit(1);
                    }
                    host = args[i];
                } else if (args[i].equalsIgnoreCase("-port")) {
                    if (url.size() > 0) {
                        log("Error: specifying both -port and -url is not allowed.");
                        System.exit(1);
                    }
                    if (port != null) {
                        log("Error: -port specified multiple times.");
                        System.exit(1);
                    }
                    i += 1;
                    if (i >= args.length) {
                        log("Error: -port missing argument.");
                        System.exit(1);
                    }
                    port = args[i];
                } else if (args[i].equalsIgnoreCase("-url")) {
                    if ((host != null) || (port != null)) {
                        log("Error: specifying both -host or -port " +
                                "and -url is not allowed.");
                        System.exit(1);
                    }

                    i += 1;
                    if (i >= args.length) {
                        log("Error: -url missing argument.");
                        System.exit(1);
                    }
                    url.add(args[i]);
                } else if (args[i].equalsIgnoreCase("-restoreTime")) {
                    i += 1;
                    if (i >= args.length) {
                        log("Error: -restoreTime missing argument.");
                        System.exit(1);
                    }
                    try {
                        restoreTimeToPreferred = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        log("Error: -restoreTime argument is not a valid integer.");
                        System.exit(1);
                    }
                } else if (args[i].equalsIgnoreCase("-holdoffTime")) {
                    i += 1;
                    if (i >= args.length) {
                        log("Error: -holdoffTime missing argument.");
                        System.exit(1);
                    }
                    try {
                        failedServerHoldoffTime = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        log("Error: -holdoffTime argument is not a valid integer.");
                        System.exit(1);
                    }
                } else if (args[i].equalsIgnoreCase("-numRetries")) {
                    i += 1;
                    if (i >= args.length) {
                        log("Error: -numRetries missing argument.");
                        System.exit(1);
                    }
                    try {
                        numberOfRetries = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        log("Error: -numRetries argument is not a valid integer.");
                        System.exit(1);
                    }
                } else if (args[i].equalsIgnoreCase("-retryDelay")) {
                    i += 1;
                    if (i >= args.length) {
                        log("Error: -retryDelay missing argument.");
                        System.exit(1);
                    }
                    try {
                        delayBetweenRetries = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        log("Error: -retryDelay argument is not a valid integer.");
                        System.exit(1);
                    }
                } else if (args[i].equalsIgnoreCase("-verbose")) {
                    verbose = true;
                } else if (args[i].equalsIgnoreCase("-help")) {
                    log("IdentityGuardAuthServiceClient [-help]\n" +
                            "   ([-host <host>] [-port <port>])|[-url <url>]+\n" +
                            "   [-restoreTime <secs>] [-holdoffTime <secs>]\n" +
                            "   [-numRetries <retries>] [-retryDelay <msecs>]\n" +
                            "   [-verbose]");
                    System.exit(0);
                } else {
                    log("Invalid argument: " + args[i]);
                    System.exit(1);
                }
            }

            if (url.size() > 0) {
                ms_serviceURL = url;
            } else {
                // Host specific settings
                // Use either one of these
                //if (host == null) host = "si-entrust1.US.SINET.SI.EDU";
                if (host == null) host = "si-entrust2.US.SINET.SI.EDU";

                if (port == null) port = "8443";
                ms_serviceURL.add("https://" + host + ":" + port +
                        "/IdentityGuardAuthService/services/AuthenticationServiceV9");
            }

            String[] igURLs = new String[ms_serviceURL.size()];

            for (int i = 0; i < ms_serviceURL.size(); i++) {
                igURLs[i] = (String) ms_serviceURL.get(i);
            }

            failoverFactory = new URIFailoverFactory(
                    igURLs,
                    new TimeInterval(restoreTimeToPreferred),
                    new TimeInterval(failedServerHoldoffTime),
                    (verbose) ? new ConsoleLoggerImpl() : null,
                    new TestConnectionImpl());

            log(welcome_msg);
            log(description);
            // Display service url in use
            log("Connected to Entrust IdentityGuard authentication service URL:\n"
                    + ms_serviceURL);

            // Start interactive mode with prompt
            System.out.print(PROMPT);
            String inputLine = r.readLine();
            while (inputLine != null) {
                Object[] params = getParams(inputLine);
                if (params.length >= 1) {
                    String cmd = (String) params[0];

                    if (cmd.equalsIgnoreCase("exit")) {
                        processExit();
                    } else if (cmd.equalsIgnoreCase("help")
                            || cmd.equalsIgnoreCase("?")) {
                        processHelp();
                    } else if (cmd.equalsIgnoreCase("ping")) {
                        processPing(params);
                    } else if (cmd.equalsIgnoreCase("getAnonymousChallenge")) {
                        processGetAnonymousChallenge(params);
                    } else if (cmd.equalsIgnoreCase("getAnonymousChallengeForGroup")) {
                        processGetAnonymousChallengeForGroup(params);
                    } else if (cmd.equalsIgnoreCase("authenticateAnonymousChallenge")) {
                        processAuthenticateAnonymousChallenge(params);
                    } else if (cmd.equalsIgnoreCase("getAllowedAuthenticationTypes")) {
                        processGetAllowedAuthenticationTypes(params);
                    } else if (cmd.equalsIgnoreCase(
                            "getAllowedAuthenticationTypesForGroup")) {
                        processGetAllowedAuthenticationTypesForGroup(params);
                    } else if ((cmd.equalsIgnoreCase("getGenericChallenge")) ||
                            (cmd.equalsIgnoreCase("ggc"))) {
                        processGetGenericChallenge(params);
                    } else if ((cmd.equalsIgnoreCase("agc")) ||
                            (cmd.equalsIgnoreCase("authenticateGenericChallenge"))) {
                        processAuthenticateGenericChallenge(params);
                    } else if (cmd.equalsIgnoreCase("debugOn")) {
                        processDebug(true);
                    } else if (cmd.equalsIgnoreCase("debugOff")) {
                        processDebug(false);
                    } else {
                        processHelp();
                    }
                }

                System.out.print(PROMPT);
                inputLine = r.readLine();
            }
            System.out.println();
        } catch (IOException e) {
            log(e);
            // Ignore exception
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e1) {
                    /* Ignore exception */
                }
            }
        }
    }

    private static void processExit() {
        System.out.println("Exit the Entrust IdentityGuard Authentication " +
                "Test Application.");
        System.exit(0);
    }

    private static void processDebug(boolean on) {
        ms_debug = on;
        System.out.println("Debug: " + on);
    }

    private static void processHelp() {
        System.out.println("Available commands:");
        System.out.println(TAB + PING_HELP);
        System.out.println(TAB + GET_ANON_CHALL_HELP);
        System.out.println(TAB + AUTH_ANON_HELP);
        System.out.println(TAB + GET_ALLOWED_AUTH_TYPES);
        System.out.println(TAB + GET_ALLOWED_AUTH_TYPES_FOR_GROUP);
        System.out.println(TAB + GET_GEN_CHALL_HELP);
        System.out.println(TAB + AUTH_GEN_HELP);
        System.out.println(TAB + "help");
        System.out.println(TAB + "debugOn");
        System.out.println(TAB + "debugOff");
        System.out.println(TAB + "exit");
    }

    private static final String PING_HELP = "ping";

    private static void processPing(Object[] params) {
        if (params.length != 1) {
            System.out.println("Usage:");
            System.out.println(TAB + PING_HELP);
        } else {
            ping();
        }
    }

    private static final String GET_ANON_CHALL_HELP = "getAnonymousChallenge";

    private static void processGetAnonymousChallenge(Object[] params) {
        if (params.length != 1) {
            System.out.println("Usage:");
            System.out.println(TAB + GET_ANON_CHALL_HELP);
        } else {
            getAnonymousChallenge();
        }
    }

    private static final String GET_ANON_CHALL_FOR_GROUP_HELP =
            "getAnonymousChallengeForGroup [<GROUP>]";

    private static void processGetAnonymousChallengeForGroup(Object[] params) {
        if (params.length == 1) {
            getAnonymousChallengeForGroup(null);
        } else if (params.length == 2) {
            getAnonymousChallengeForGroup((String) params[1]);
        } else {
            System.out.println("Usage:");
            System.out.println(TAB + GET_ANON_CHALL_FOR_GROUP_HELP);
        }
    }

    private static final String AUTH_ANON_HELP =
            "authenticateAnonymousChallenge <USERID> <RES>+ " +
                    "[-pvn <pvn>] [-newpvn <newpvn>] ";

    private static void processAuthenticateAnonymousChallenge(Object[] params) {
        if (params.length < 3) {
            System.out.println("Usage:");
            System.out.println(TAB + AUTH_ANON_HELP);
        } else {
            String userid = (String) params[1];

            String pvn = null;
            String newpvn = null;
            int actualResponseSize = 0;
            String[] maxChallengeResponse = new String[params.length - 2];

            for (int i = 2; i < params.length; i++) {
                String arg = (String) params[i];
                if (arg.equalsIgnoreCase("-pvn")) {
                    if (pvn != null) {
                        System.out.println("Error:  -pvn already specified.");
                        System.out.println(TAB + AUTH_ANON_HELP);
                        return;
                    }
                    i += 1;
                    if (i >= params.length) {
                        System.out.println("Error:  argument for -pvn " +
                                "missing.");
                        System.out.println(TAB + AUTH_ANON_HELP);
                        return;
                    }
                    pvn = (String) params[i];
                } else if (arg.equalsIgnoreCase("-newpvn")) {
                    if (newpvn != null) {
                        System.out.println("Error:  -newpvn already specified.");
                        System.out.println(TAB + AUTH_ANON_HELP);
                        return;
                    }
                    i += 1;
                    if (i >= params.length) {
                        System.out.println("Error:  argument for -newpvn " +
                                "missing.");
                        System.out.println(TAB + AUTH_ANON_HELP);
                        return;
                    }
                    newpvn = (String) params[i];
                } else {
                    maxChallengeResponse[actualResponseSize++] =
                            (String) params[i];
                }
            }

            if (actualResponseSize == 0) {
                System.out.println("Usage:");
                System.out.println(TAB + AUTH_ANON_HELP);
                return;
            }

            String[] challengeResponse = new String[actualResponseSize];
            for (int i = 0; i < actualResponseSize; i++) {
                challengeResponse[i] = (String) maxChallengeResponse[i];
            }
            authenticateAnonymousChallenge(
                    userid, challengeResponse, pvn, newpvn);
        }
    }

    private static final AuthenticationTypeEx getAuthenticationTypeEx(String val) {
        if (val.compareToIgnoreCase("GRID") == 0) {
            return AuthenticationTypeEx.GRID;
        } else if (val.compareToIgnoreCase("QA") == 0) {
            return AuthenticationTypeEx.QA;
        } else if (val.compareToIgnoreCase("OTP") == 0) {
            return AuthenticationTypeEx.OTP;
        } else if (val.compareToIgnoreCase("TOKENRO") == 0) {
            return AuthenticationTypeEx.TOKENRO;
        } else if (val.compareToIgnoreCase("TOKENCR") == 0) {
            return AuthenticationTypeEx.TOKENCR;
        } else if (val.compareToIgnoreCase("EXTERNAL") == 0) {
            return AuthenticationTypeEx.EXTERNAL;
        } else if (val.compareToIgnoreCase("PASSWORD") == 0) {
            return AuthenticationTypeEx.PASSWORD;
        } else if (val.compareToIgnoreCase("CERTIFICATE") == 0) {
            return AuthenticationTypeEx.CERTIFICATE;
        } else if (val.compareToIgnoreCase("BIOMETRIC") == 0) {
            return AuthenticationTypeEx.BIOMETRIC;
        } else if (val.compareToIgnoreCase("NONE") == 0) {
            return AuthenticationTypeEx.NONE;
        }
        return null;
    }

    private static final String GET_ALLOWED_AUTH_TYPES =
            "getAllowedAuthenticationTypes <USERID>";

    private static void processGetAllowedAuthenticationTypes(Object[] params) {
        String userid = null;

        for (int i = 1; i < params.length; i++) {
            String arg = (String) params[i];
            if (userid != null) {
                System.out.println("Error:  userid already specified.");
                System.out.println(TAB + GET_ALLOWED_AUTH_TYPES);
                return;
            }
            userid = arg;
        }

        if (userid == null) {
            System.out.println("Error:  userid missing.");
            System.out.println(TAB + GET_ALLOWED_AUTH_TYPES);
            return;
        }

        getAllowedAuthenticationTypes(userid);
    }

    private static final String GET_ALLOWED_AUTH_TYPES_FOR_GROUP =
            "getAllowedAuthenticationTypesForGroup [<group>]";

    private static void processGetAllowedAuthenticationTypesForGroup(
            Object[] params) {
        String group = null;

        for (int i = 1; i < params.length; i++) {
            String arg = (String) params[i];
            if (group != null) {
                System.out.println("Error:  group already specified.");
                System.out.println(TAB + GET_ALLOWED_AUTH_TYPES_FOR_GROUP);
                return;
            }
            group = arg;
        }

        getAllowedAuthenticationTypesForGroup(group);
    }

    private static final String GET_GEN_CHALL_HELP =
            "getGenericChallenge [-] <USERID> " +
                    "[-authtype GRID|QA|OTP|TOKENRO|TOKENCR|EXTERNAL|PASSWORD|CERTIFICATE|BIOMETRIC|NONE]|" +
                    "[-authtypelist (GRID|QA|OTP|TOKENRO|TOKENCR|EXTERNAL|PASSWORD|CERTIFICATE|BIOMETRIC|NONE)*] " +
                    "[-applicationName <name>] " +
                    "[-requiresPVN TRUE|FALSE] " +
                    "[-authTypesRequiringPVN (GRID|OTP|TOKENRO|TOKENCR)*] " +
                    "[-challengehistory (GRID|QA|OTP|TOKENRO|TOKENCR|EXTERNAL|PASSWORD|CERTIFICATE|BIOMETRIC|NONE)*] " +
                    "[-otpDelivery DEFAULT|<Contact Label>+] " +
                    "[-deliverForDynamicRefresh] " +
                    "[-onlySelectOTPAuthenticationIfDeliveryAvailable] " +
                    "[-passwordName <passwordName>] " +
                    "[-securitylevel NORMAL|ENHANCED] " +
                    "[-ipaddress <ipaddress>] " +
                    "[-machine [-label <label>] [-machinenonce <nonce>] " +
                    "[-sequence <nonce>] [-appdata ([-] <name> <value)*]" +
                    "[-registerCertificate] " +
                    "[-certificate <file>] " +
                    "[-tokenSets ([-] <set>)+] " +
                    "[-tokenMutualAuthChallenge <challenge>] " +
                    "[-gridchallengesize <number>] " +
                    "[-qachallengesize <number>] " +
                    "[-auth [-get ([-] <name>)*] [-remove ([-] <name>)*] " +
                    "[-set [-merge] ([-] <name> <value>)*]] " +
                    "[-transactionDetails ([-] <name> <value>)+] " +
                    "[-performDeliveryAndSignature [TRUE|FALSE]] " +
                    "[-requireDeliveryAndSignatureIfAvailable [TRUE|FALSE]] " +
                    "[-tokenTransactionMode CLASSIC|ONLINE] " +
                    "[-tokenChallengeSummary <summary>] " +
                    "[-performCertificateDelivery [TRUE|FALSE]] " +
                    "[-requireCertificateDelivery [TRUE|FALSE]] " +
                    "[-smartCredentialChallengeSummary <summary>]";

    private static void processGetGenericChallenge(Object[] params) {
        String userid = null;
        AuthenticationTypeEx authType = null;
        List authTypeList = null;
        Boolean requiresPVN = null;
        List authTypesRequiringPVN = null;
        List getauth = null;
        List removeauth = null;
        boolean mergeauth = false;
        List setauth = null;
        List challengeHistory = null;
        Integer gridChallengeSize = null;
        Integer qaChallengeSize = null;
        String ipAddress = null;
        MachineSecret machine = null;
        Boolean registerCertificate = null;
        String certificate = null;
        SecurityLevel securityLevel = null;
        String passwordName = null;
        Boolean useDefaultDelivery = null;
        String[] contactInfoLabel = null;
        Boolean deliverForDynamicRefresh = null;
        Boolean onlySelectOTPAuthenticationIfDeliveryAvailable = null;
        String appname = null;
        NameValue[] transactionDetails = null;
        List tokenSets = null;
        String tokenMutualAuthChallenge = null;
        Boolean performDeliveryAndSignature = null;
        Boolean requireDeliveryAndSignatureIfAvailable = null;
        TokenTransactionMode tokenTransactionMode = null;
        String tokenChallengeSummary = null;
        Boolean performCertificateDelivery = null;
        Boolean requireCertificateDelivery = null;
        String smartCredentialChallengeSummary = null;

        for (int i = 1; i < params.length; i++) {
            String arg = (String) params[i];
            if (arg.equalsIgnoreCase("-authtype")) {
                if (authType != null) {
                    System.out.println("Error:  -authtype already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                if (authTypeList != null) {
                    System.out.println("Error:  -authtypelist already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -authtype missing.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                authType = getAuthenticationTypeEx((String) params[i]);
                if (authType == null) {
                    System.out.println("Error:  invalid argument for -authtype.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
            } else if (arg.equalsIgnoreCase("-authtypelist")) {
                if (authType != null) {
                    System.out.println("Error:  -authtype already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                if (authTypeList != null) {
                    System.out.println("Error:  -authtypelist already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                authTypeList = new ArrayList();
                while (i + 1 < params.length) {
                    AuthenticationTypeEx a =
                            getAuthenticationTypeEx((String) params[i + 1]);
                    if (a == null) {
                        break;
                    }
                    authTypeList.add(a);
                    i += 1;
                }
            } else if (arg.equalsIgnoreCase("-applicationName")) {
                if (appname != null) {
                    System.out.println("Error:  -applicationName already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  -applicationName missing argument.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                appname = (String) params[i];
            } else if (arg.equalsIgnoreCase("-requirespvn")) {
                if (requiresPVN != null) {
                    System.out.println("Error:  -requirespvn already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  -requirespvn missing argument.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                requiresPVN = new Boolean((String) params[i]);
            } else if (arg.equalsIgnoreCase("-authTypesRequiringPVN")) {
                if (authTypesRequiringPVN != null) {
                    System.out.println("Error:  -authTypesRequiringPVN already " +
                            "specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                authTypesRequiringPVN = new ArrayList();
                while (i + 1 < params.length) {
                    AuthenticationTypeEx a =
                            getAuthenticationTypeEx((String) params[i + 1]);
                    if (a == null) {
                        break;
                    }
                    authTypesRequiringPVN.add(a);
                    i += 1;
                }
            } else if (arg.equalsIgnoreCase("-challengehistory")) {
                if (challengeHistory != null) {
                    System.out.println("Error:  -challengehistory already " +
                            "specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                challengeHistory = new ArrayList();
                while (i + 1 < params.length) {
                    AuthenticationTypeEx a =
                            getAuthenticationTypeEx((String) params[i + 1]);
                    if (a == null) {
                        break;
                    }
                    challengeHistory.add(a);
                    i += 1;
                }
            } else if (arg.equalsIgnoreCase("-otpDelivery")) {
                if ((useDefaultDelivery != null) ||
                        (contactInfoLabel != null)) {
                    System.out.println("Error:  -otpDelivery already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -otpDelivery " +
                            "missing.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                String val = (String) params[i];
                if (val.equalsIgnoreCase("DEFAULT")) {
                    useDefaultDelivery = Boolean.TRUE;
                } else {
                    ArrayList list = new ArrayList();
                    while (i < params.length) {
                        arg = (String) params[i];
                        if (arg.equals("-")) {
                            i += 1;
                            if (i >= params.length) {
                                System.out.println(
                                        "Error:  argument for -otpDelivery missing.");
                                System.out.println(TAB + GET_GEN_CHALL_HELP);
                                return;
                            }
                        } else if ((arg.length() > 0) && (arg.charAt(0) == '-')) {
                            i -= 1;
                            break;
                        }
                        list.add(arg);

                        i += 1;
                    }
                    contactInfoLabel =
                            (String[]) list.toArray(new String[list.size()]);
                }
            } else if (arg.equalsIgnoreCase("-deliverForDynamicRefresh")) {
                if (deliverForDynamicRefresh != null) {
                    System.out.println(
                            "Error:  -deliverForDynamicRefresh already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                deliverForDynamicRefresh = Boolean.TRUE;
            } else if (arg.equalsIgnoreCase(
                    "-onlySelectOTPAuthenticationIfDeliveryAvailable")) {
                if (onlySelectOTPAuthenticationIfDeliveryAvailable != null) {
                    System.out.println(
                            "Error:  -onlySelectOTPAuthenticationIfDeliveryAvailable " +
                                    "already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                onlySelectOTPAuthenticationIfDeliveryAvailable = Boolean.TRUE;
            } else if (arg.equalsIgnoreCase("-securitylevel")) {
                if (securityLevel != null) {
                    System.out.println("Error:  -securitylevel already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -securitylevel " +
                            "missing.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                try {
                    securityLevel = SecurityLevel.fromValue((String) params[i]);
                } catch (Exception e) {
                    System.out.println("Error:  invalid argument for " +
                            "-securitylevel.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
            } else if (arg.equalsIgnoreCase("-passwordName")) {
                if (passwordName != null) {
                    System.out.println("Error:  -passwordName already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -passwordName " +
                            "missing.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                passwordName = (String) params[i];
            } else if (arg.equalsIgnoreCase("-gridchallengesize")) {
                if (gridChallengeSize != null) {
                    System.out.println("Error:  -gridChallengesize already " +
                            "specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -gridchallengesize " +
                            "missing.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                gridChallengeSize = getInteger((String) params[i], "-gridchallengesize");
                if (gridChallengeSize == null) {
                    return;
                }
            } else if (arg.equalsIgnoreCase("-qachallengesize")) {
                if (qaChallengeSize != null) {
                    System.out.println("Error:  -qachallengesize already " +
                            "specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -qachallengesize " +
                            "missing.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                qaChallengeSize = getInteger((String) params[i], "-qachallengesize");
                if (qaChallengeSize == null) {
                    return;
                }
            } else if (arg.equalsIgnoreCase("-auth")) {
                if ((getauth != null) || (removeauth != null) ||
                        (setauth != null)) {
                    System.out.println("Error:  -auth already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                while (i + 1 < params.length) {
                    arg = (String) params[i + 1];
                    if (arg.equalsIgnoreCase("-get")) {
                        i += 1;
                        if (getauth != null) {
                            System.out.println("Error:  -auth -get already " +
                                    "specified.");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }
                        getauth = new ArrayList();
                        while (i + 1 < params.length) {
                            arg = (String) params[i + 1];
                            if ((arg.length() > 1) && (arg.charAt(0) == '-')) {
                                break;
                            }

                            if (arg.equals("-")) {
                                i += 1;
                                if (i + 1 >= params.length) {
                                    System.out.println("Error:  -auth -get missing " +
                                            "argument.");
                                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                                    return;
                                }
                                arg = (String) params[i + 1];
                            }

                            getauth.add(arg);
                            i += 1;
                        }
                    } else if (arg.equalsIgnoreCase("-remove")) {
                        i += 1;
                        if (removeauth != null) {
                            System.out.println("Error:  -auth -remove already " +
                                    "specified.");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }
                        removeauth = new ArrayList();
                        while (i + 1 < params.length) {
                            arg = (String) params[i + 1];
                            if ((arg.length() > 1) && (arg.charAt(0) == '-')) {
                                break;
                            }

                            if (arg.equals("-")) {
                                i += 1;
                                if (i + 1 >= params.length) {
                                    System.out.println("Error:  -auth -remove missing " +
                                            "argument.");
                                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                                    return;
                                }
                                arg = (String) params[i + 1];
                            }

                            removeauth.add(arg);
                            i += 1;
                        }
                    } else if (arg.equalsIgnoreCase("-set")) {
                        i += 1;
                        if (setauth != null) {
                            System.out.println("Error:  -auth -set already " +
                                    "specified.");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }

                        if (i + 1 < params.length) {
                            arg = (String) params[i + 1];
                            if (arg.equalsIgnoreCase("-merge")) {
                                mergeauth = true;
                                i += 1;
                            }
                        }

                        setauth = new ArrayList();
                        while (i + 1 < params.length) {
                            arg = (String) params[i + 1];
                            if ((arg.length() > 1) && (arg.charAt(0) == '-')) {
                                break;
                            }

                            if (arg.equals("-")) {
                                i += 1;
                                if (i + 1 >= params.length) {
                                    System.out.println("Error:  -auth -set missing " +
                                            "argument.");
                                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                                    return;
                                }
                                arg = (String) params[i + 1];
                            }

                            NameValue set = new NameValue();
                            set.setName(arg);

                            i += 1;
                            if (i + 1 >= params.length) {
                                System.out.println("Error:  -auth -set missing " +
                                        "argument.");
                                System.out.println(TAB + GET_GEN_CHALL_HELP);
                                return;
                            }

                            set.setValue((String) params[i + 1]);

                            setauth.add(set);
                            i += 1;
                        }
                    } else {
                        break;
                    }
                }
            } else if (arg.equalsIgnoreCase("-ipaddress")) {
                if (ipAddress != null) {
                    System.out.println("Error:  -ipaddress already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -ipaddress " +
                            "missing.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                ipAddress = (String) params[i];
            } else if (arg.equalsIgnoreCase("-registerCertificate")) {
                if (registerCertificate != null) {
                    System.out.println(
                            "Error:  -registerCertificate already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                registerCertificate = Boolean.TRUE;
            } else if (arg.equalsIgnoreCase("-certificate")) {
                if (certificate != null) {
                    System.out.println("Error:  -certificate already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -certificate " +
                            "missing.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                try {
                    certificate = readDataFromFile((String) params[i]);
                } catch (Exception e) {
                    System.out.println("Error:  -certificate invalid argument.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
            } else if (arg.equalsIgnoreCase("-machine")) {
                if (machine != null) {
                    System.out.println("Error:  -machine already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                machine = new MachineSecret();
                i += 1;
                while (i < params.length) {
                    arg = (String) params[i];
                    if (arg.equalsIgnoreCase("-label")) {
                        if (machine.getMachineLabel() != null) {
                            System.out.println("Error:  -machine -label" +
                                    "already specified.");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }
                        i += 1;
                        if (i >= params.length) {
                            System.out.println("Error:  missing argument for " +
                                    "-machine -label");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }
                        machine.setMachineLabel((String) params[i]);
                        i += 1;
                    } else if (arg.equalsIgnoreCase("-machinenonce")) {
                        if (machine.getMachineNonce() != null) {
                            System.out.println("Error:  -machine -machinenonce" +
                                    "already specified.");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }
                        i += 1;
                        if (i >= params.length) {
                            System.out.println("Error:  missing argument for " +
                                    "-machine -machinenonce");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }
                        machine.setMachineNonce((String) params[i]);
                        i += 1;
                    } else if (arg.equalsIgnoreCase("-sequence")) {
                        if (machine.getSequenceNonce() != null) {
                            System.out.println("Error:  -machine -sequence" +
                                    "already specified.");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }
                        i += 1;
                        if (i >= params.length) {
                            System.out.println("Error:  missing argument for " +
                                    "-machine -sequence");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }
                        machine.setSequenceNonce((String) params[i]);
                        i += 1;
                    } else if (arg.equalsIgnoreCase("-appdata")) {
                        if (machine.getApplicationData() != null) {
                            System.out.println("Error:  -machine -appdata" +
                                    "already specified.");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }
                        i += 1;
                        List list = new ArrayList();
                        while (i < params.length) {
                            NameValue nv = new NameValue();
                            nv.setName((String) params[i]);
                            if ((nv.getName().charAt(0) == '-') &&
                                    (!nv.getName().equals("-"))) {
                                i -= 1;
                                break;
                            }
                            if (nv.getName().equals("-")) {
                                i += 1;
                                if (i >= params.length) {
                                    System.out.println("Error: missing argument for " +
                                            "-machine -appdata.");
                                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                                    return;
                                }
                                nv.setName((String) params[i]);
                            }

                            i += 1;
                            if (i >= params.length) {
                                System.out.println("Error: missing value argument " +
                                        "for -machine -appdata.");
                                System.out.println(TAB + GET_GEN_CHALL_HELP);
                                return;
                            }
                            nv.setValue((String) params[i]);
                            list.add(nv);

                            i += 1;
                        }

                        NameValue[] appdata = new NameValue[list.size()];
                        list.toArray(appdata);
                        machine.setApplicationData(appdata);
                    } else {
                        i -= 1;
                        break;
                    }
                }
            } else if (arg.equalsIgnoreCase("-transactionDetails")) {
                if (transactionDetails != null) {
                    System.out.println("Error:  -transactionDetails " +
                            "already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;
                List list = new ArrayList();
                while (i < params.length) {
                    NameValue nv = new NameValue();
                    nv.setName((String) params[i]);
                    if ((nv.getName().charAt(0) == '-') &&
                            (!nv.getName().equals("-"))) {
                        i -= 1;
                        break;
                    }
                    if (nv.getName().equals("-")) {
                        i += 1;
                        if (i >= params.length) {
                            System.out.println("Error: missing argument for " +
                                    "-transactionDetails.");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }
                        nv.setName((String) params[i]);
                    }

                    i += 1;
                    if (i >= params.length) {
                        System.out.println("Error: missing value argument " +
                                "for -transactionDetails.");
                        System.out.println(TAB + GET_GEN_CHALL_HELP);
                        return;
                    }
                    nv.setValue((String) params[i]);
                    list.add(nv);

                    i += 1;
                }

                if (list.size() == 0) {
                    System.out.println("Error: missing value argument " +
                            "for -transactionDetails.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }

                transactionDetails = new NameValue[list.size()];
                list.toArray(transactionDetails);
            } else if (arg.equalsIgnoreCase("-tokenSets")) {
                if (tokenSets != null) {
                    System.out.println("Error:  -tokenSets already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;

                tokenSets = new ArrayList();
                while (i < params.length) {
                    arg = (String) params[i];

                    if (arg.equals("-")) {
                        i += 1;
                        if (i >= params.length) {
                            System.out.println(
                                    "Error:  argument for -tokenSets missing.");
                            System.out.println(TAB + GET_GEN_CHALL_HELP);
                            return;
                        }
                        arg = (String) params[i];
                    } else if ((arg.length() > 0) && (arg.charAt(0) == '-')) {
                        i -= 1;
                        break;
                    }

                    tokenSets.add(arg);
                    i += 1;
                }

                if (tokenSets.size() == 0) {
                    System.out.println("Error:  argument for -tokenSets missing.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
            } else if (arg.equalsIgnoreCase("-tokenMutualAuthChallenge")) {
                if (tokenMutualAuthChallenge != null) {
                    System.out.println("Error:  -tokenMutualAuthChallenge already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                i += 1;

                if (i >= params.length) {
                    System.out.println("Error:  argument for -tokenMutualAuthChallenge missing.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                tokenMutualAuthChallenge = (String) params[i];
            } else if (arg.equalsIgnoreCase("-performDeliveryAndSignature")) {
                if (performDeliveryAndSignature != null) {
                    System.out.println(
                            "Error:  -performDeliveryAndSignature already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                performDeliveryAndSignature = Boolean.TRUE;

                if (i + 1 < params.length) {
                    arg = (String) params[i + 1];
                    if (arg.equalsIgnoreCase("true")) {
                        performDeliveryAndSignature = Boolean.TRUE;
                        i += 1;
                    } else if (arg.equalsIgnoreCase("false")) {
                        performDeliveryAndSignature = Boolean.FALSE;
                        i += 1;
                    }
                }
            } else if (arg.equalsIgnoreCase(
                    "-requireDeliveryAndSignatureIfAvailable")) {
                if (requireDeliveryAndSignatureIfAvailable != null) {
                    System.out.println(
                            "Error:  -requireDeliveryAndSignatureIfAvailable already " +
                                    "specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                requireDeliveryAndSignatureIfAvailable = Boolean.TRUE;

                if (i + 1 < params.length) {
                    arg = (String) params[i + 1];
                    if (arg.equalsIgnoreCase("true")) {
                        requireDeliveryAndSignatureIfAvailable = Boolean.TRUE;
                        i += 1;
                    } else if (arg.equalsIgnoreCase("false")) {
                        requireDeliveryAndSignatureIfAvailable = Boolean.FALSE;
                        i += 1;
                    }
                }
            } else if (arg.equalsIgnoreCase("-tokenTransactionMode")) {
                if (tokenTransactionMode != null) {
                    System.out.println(
                            "Error:  -tokenTransactionMode already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }

                i += 1;
                if (i >= params.length) {
                    System.out.println(
                            "Error:  -tokenTransactionMode missing required argument.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                try {
                    String mode = (String) params[i];
                    tokenTransactionMode =
                            TokenTransactionMode.fromString(mode.toUpperCase());
                } catch (Exception e) {
                    System.out.println(
                            "Error:  -tokenTransactionMode invalid argument value.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
            } else if (arg.equalsIgnoreCase("-tokenChallengeSummary")) {
                if (tokenChallengeSummary != null) {
                    System.out.println(
                            "Error:  -tokenChallengeSummary already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }

                i += 1;
                if (i >= params.length) {
                    System.out.println(
                            "Error:  -tokenChallengeSummary missing required argument.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                tokenChallengeSummary = (String) params[i];

            } else if (arg.equalsIgnoreCase("-performCertificateDelivery")) {
                if (performCertificateDelivery != null) {
                    System.out.println(
                            "Error:  -performCertificateDelivery already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                performCertificateDelivery = Boolean.TRUE;

                if (i + 1 < params.length) {
                    arg = (String) params[i + 1];
                    if (arg.equalsIgnoreCase("true")) {
                        performCertificateDelivery = Boolean.TRUE;
                        i += 1;
                    } else if (arg.equalsIgnoreCase("false")) {
                        performCertificateDelivery = Boolean.FALSE;
                        i += 1;
                    }
                }
            } else if (arg.equalsIgnoreCase("-requireCertificateDelivery")) {
                if (requireCertificateDelivery != null) {
                    System.out.println(
                            "Error:  -requireCertificateDelivery already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                requireCertificateDelivery = Boolean.TRUE;

                if (i + 1 < params.length) {
                    arg = (String) params[i + 1];
                    if (arg.equalsIgnoreCase("true")) {
                        requireCertificateDelivery = Boolean.TRUE;
                        i += 1;
                    } else if (arg.equalsIgnoreCase("false")) {
                        requireCertificateDelivery = Boolean.FALSE;
                        i += 1;
                    }
                }
            } else if (arg.equalsIgnoreCase("-smartCredentialChallengeSummary")) {
                if (smartCredentialChallengeSummary != null) {
                    System.out.println(
                            "Error:  -smartCredentialChallengeSummary already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }

                i += 1;
                if (i >= params.length) {
                    System.out.println(
                            "Error:  -smartCredentialChallengeSummary missing required argument.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                smartCredentialChallengeSummary = (String) params[i];

            } else {
                if (arg.equals("-")) {
                    i += 1;
                    if (i >= params.length) {
                        System.out.println("Error:  userid missing.");
                        System.out.println(TAB + GET_GEN_CHALL_HELP);
                        return;
                    }
                    arg = (String) params[i];
                }
                if (userid != null) {
                    System.out.println("Error:  userid already specified.");
                    System.out.println(TAB + GET_GEN_CHALL_HELP);
                    return;
                }
                userid = arg;
            }
        }

        if (userid == null) {
            System.out.println("Error:  userid missing.");
            System.out.println(TAB + GET_GEN_CHALL_HELP);
            return;
        }

        AuthenticationTypeEx[] authTypeArray = null;
        if (authTypeList != null) {
            authTypeArray = new AuthenticationTypeEx[authTypeList.size()];
            authTypeList.toArray(authTypeArray);
        }
        String[] getAuthArray = null;
        if (getauth != null) {
            getAuthArray = new String[getauth.size()];
            getauth.toArray(getAuthArray);
        }
        String[] removeAuthArray = null;
        if (removeauth != null) {
            removeAuthArray = new String[removeauth.size()];
            removeauth.toArray(removeAuthArray);
        }
        NameValue[] setAuthArray = null;
        if (setauth != null) {
            setAuthArray = new NameValue[setauth.size()];
            setauth.toArray(setAuthArray);
        }
        AuthenticationTypeEx[] challengeHistoryArray = null;
        if (challengeHistory != null) {
            challengeHistoryArray =
                    new AuthenticationTypeEx[challengeHistory.size()];
            challengeHistory.toArray(challengeHistoryArray);
        }
        AuthenticationTypeEx[] authTypesRequiringPVNArray = null;
        if (authTypesRequiringPVN != null) {
            authTypesRequiringPVNArray =
                    new AuthenticationTypeEx[authTypesRequiringPVN.size()];
            authTypesRequiringPVN.toArray(authTypesRequiringPVNArray);
        }
        String[] tokenSetArray = null;
        if (tokenSets != null) {
            tokenSetArray = new String[tokenSets.size()];
            tokenSets.toArray(tokenSetArray);
        }
        getGenericChallenge(userid, authType, authTypeArray, appname,
                requiresPVN, authTypesRequiringPVNArray,
                gridChallengeSize, qaChallengeSize,
                challengeHistoryArray, useDefaultDelivery,
                contactInfoLabel, deliverForDynamicRefresh,
                onlySelectOTPAuthenticationIfDeliveryAvailable,
                passwordName, securityLevel, ipAddress, machine,
                registerCertificate, certificate,
                getAuthArray, removeAuthArray,
                mergeauth, setAuthArray, transactionDetails,
                performDeliveryAndSignature,
                requireDeliveryAndSignatureIfAvailable,
                tokenTransactionMode,
                tokenChallengeSummary,
                performCertificateDelivery,
                requireCertificateDelivery,
                smartCredentialChallengeSummary,
                tokenSetArray,
                tokenMutualAuthChallenge);
    }

    private static final String AUTH_GEN_HELP =
            "authenticateGenericChallenge [-] <USERID> [([-] <RES>)+] " +
                    "[-authtype GRID|QA|OTP|TOKENRO|TOKENCR|EXTERNAL|PASSWORD|CERTIFICATE|BIOMETRIC|NONE] " +
                    "[-challengesize <number>] " +
                    "[-applicationName <name>] " +
                    "[-challengehistory (GRID|QA|OTP|TOKENRO|TOKENCR|EXTERNAL|PASSWORD|CERTIFICATE|BIOMETRIC|NONE)*] " +
                    "[-qanumwronganswersallowed <number>] " +
                    "[-securitylevel NORMAL|ENHANCED] " +
                    "[-ipaddress <ipaddress>] " +
                    "[-certificate <file>] " +
                    "[-signatureData <data>+] " +
                    "[-tokenMutualAuth] " +
                    "[-registermachine | " +
                    "(-machine [-label <label>] [-machinenonce <nonce>] " +
                    "[-appdata ([-] <name> <value)*])]" +
                    "[-newpassword <password>] [-passwordName <passwordName>] " +
                    "[-pvn <pvn>] [-newpvn <newpvn>] [-pap] " +
                    "[-authTypesRequiringPVN (GRID|OTP|TOKENRO|TOKENCR)*] " +
                    "[-auth [-get ([-] <name>)*] [-remove ([-] <name>)*] " +
                    "[-set [-merge] ([-] <name> <value>)*]] " +
                    "[-serialNumber <serialNumber>] " +
                    "[-sets ([-] <set>)+] " +
                    "[-otpDelivery DEFAULT|<Contact Label>+] " +
                    "[-deliverForDynamicRefresh] " +
                    "[-transactionId <id>] " +
                    "[-transactionDetails ([-] <name> <value>)+] " +
                    "[-certificateSignatureFile <file>] " +
                    "[-performDeliveryAndSignature [TRUE|FALSE]] " +
                    "[-requireDeliveryAndSignatureIfAvailable [TRUE|FALSE]] " +
                    "[-biometricData <file>] " +
                    "[-transactionReceiptFile <file>]";

    private static void processAuthenticateGenericChallenge(Object[] params) {
        String userid = null;
        AuthenticationTypeEx authType = null;
        List challengeHistory = null;
        List authTypesRequiringPVN = null;
        String appname = null;
        List response = null;
        List getauth = null;
        List removeauth = null;
        boolean mergeauth = false;
        List setauth = null;
        Integer challengeSize = null;
        Integer qaNumWrongAnswersAllowed = null;
        String newPassword = null;
        String passwordName = null;
        String pvn = null;
        String newpvn = null;
        SecurityLevel securityLevel = null;
        String ipAddress = null;
        String certificate = null;
        MachineSecret machine = null;
        Boolean registerMachine = null;
        String[] signatureData = null;
        boolean pap = false;
        NameValue[] transactionDetails = null;
        String transactionId = null;
        String certificateSignatureFile = null;
        String transactionReceiptFile = null;
        Boolean performDeliveryAndSignature = null;
        Boolean requireDeliveryAndSignatureIfAvailable = null;
        String serialNumber = null;
        List sets = null;
        Boolean useDefaultDelivery = null;
        String[] contactInfoLabel = null;
        Boolean deliverForDynamicRefresh = null;
        byte[] biometricData = null;
        Boolean tokenMutualAuth = null;

        for (int i = 1; i < params.length; i++) {
            String arg = (String) params[i];
            if (arg.equalsIgnoreCase("-authtype")) {
                if (authType != null) {
                    System.out.println("Error:  -authtype already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -authtype missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                authType = getAuthenticationTypeEx((String) params[i]);
                if (authType == null) {
                    System.out.println("Error:  invalid argument for -authtype.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
            } else if (arg.equalsIgnoreCase("-serialNumber")) {
                if (serialNumber != null) {
                    System.out.println("Error:  -serialNumber already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println(
                            "Error:  argument for -serialNumber missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                serialNumber = (String) params[i];
            } else if (arg.equalsIgnoreCase("-sets")) {
                if (sets != null) {
                    System.out.println("Error:  -sets already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;

                sets = new ArrayList();
                while (i < params.length) {
                    arg = (String) params[i];

                    if (arg.equals("-")) {
                        i += 1;
                        if (i >= params.length) {
                            System.out.println("Error:  argument for -sets missing.");
                            System.out.println(TAB + AUTH_GEN_HELP);
                            return;
                        }
                        arg = (String) params[i];
                    } else if ((arg.length() > 0) && (arg.charAt(0) == '-')) {
                        i -= 1;
                        break;
                    }

                    sets.add(arg);
                    i += 1;
                }

                if (sets.size() == 0) {
                    System.out.println("Error:  argument for -sets missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
            } else if (arg.equalsIgnoreCase("-challengehistory")) {
                if (challengeHistory != null) {
                    System.out.println("Error:  -challengehistory already " +
                            "specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                challengeHistory = new ArrayList();
                while (i + 1 < params.length) {
                    AuthenticationTypeEx a =
                            getAuthenticationTypeEx((String) params[i + 1]);
                    if (a == null) {
                        break;
                    }
                    challengeHistory.add(a);
                    i += 1;
                }
            } else if (arg.equalsIgnoreCase("-authTypesRequiringPVN")) {
                if (authTypesRequiringPVN != null) {
                    System.out.println("Error:  -authTypesRequiringPVN already " +
                            "specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                authTypesRequiringPVN = new ArrayList();
                while (i + 1 < params.length) {
                    AuthenticationTypeEx a =
                            getAuthenticationTypeEx((String) params[i + 1]);
                    if (a == null) {
                        break;
                    }
                    authTypesRequiringPVN.add(a);
                    i += 1;
                }
            } else if (arg.equalsIgnoreCase("-challengesize")) {
                if (challengeSize != null) {
                    System.out.println("Error:  -challengesize already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -challengesize " +
                            "missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                challengeSize = getInteger((String) params[i], "-challengesize");
                if (challengeSize == null) {
                    return;
                }
            } else if (arg.equalsIgnoreCase("-applicationName")) {
                if (appname != null) {
                    System.out.println(
                            "Error:  -applicationName already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -applicationName " +
                            "missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                appname = (String) params[i];
            } else if (arg.equalsIgnoreCase("-newpassword")) {
                if (newPassword != null) {
                    System.out.println("Error:  -newpassword already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -newpassword " +
                            "missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                newPassword = (String) params[i];
            } else if (arg.equalsIgnoreCase("-passwordName")) {
                if (passwordName != null) {
                    System.out.println("Error:  -passwordName already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -passwordName " +
                            "missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                passwordName = (String) params[i];
            } else if (arg.equalsIgnoreCase("-securitylevel")) {
                if (securityLevel != null) {
                    System.out.println("Error:  -securitylevel already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -securitylevel " +
                            "missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                try {
                    securityLevel = SecurityLevel.fromValue((String) params[i]);
                } catch (Exception e) {
                    System.out.println("Error:  invalid argument for " +
                            "-securitylevel.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
            } else if (arg.equalsIgnoreCase("-ipaddress")) {
                if (ipAddress != null) {
                    System.out.println("Error:  -ipaddress already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -ipaddress " +
                            "missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                ipAddress = (String) params[i];
            } else if (arg.equalsIgnoreCase("-certificate")) {
                if (certificate != null) {
                    System.out.println("Error:  -certificate already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -certificate " +
                            "missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                try {
                    certificate = readDataFromFile((String) params[i]);
                } catch (Exception e) {
                    System.out.println("Error:  invalid argument for -certificate.");
                    return;
                }
            } else if (arg.equalsIgnoreCase("-signatureData")) {
                if (signatureData != null) {
                    System.out.println("Error:  -signatureData already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                List list = new ArrayList();
                i += 1;
                while (i < params.length) {
                    arg = (String) params[i];
                    if ((arg.length() > 0) && (arg.charAt(0) == '-')) {
                        i -= 1;
                        break;
                    }
                    list.add(arg);
                    i += 1;
                }

                if (list.size() == 0) {
                    System.out.println("Error:  argument for -signatureData " +
                            "missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                signatureData = new String[list.size()];
                list.toArray(signatureData);
            } else if (arg.equalsIgnoreCase("-tokenMutualAuth")) {
                if (tokenMutualAuth != null) {
                    System.out.println("Error:  -tokenMutualAuth already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                tokenMutualAuth = Boolean.TRUE;
            } else if (arg.equalsIgnoreCase("-pvn")) {
                if (pvn != null) {
                    System.out.println("Error:  -pvn already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -pvn " +
                            "missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                pvn = (String) params[i];
            } else if (arg.equalsIgnoreCase("-pap")) {
                if (pap) {
                    System.out.println("Error:  -pap already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                pap = true;
            } else if (arg.equalsIgnoreCase("-newpvn")) {
                if (newpvn != null) {
                    System.out.println("Error:  -newpvn already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -newpvn " +
                            "missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                newpvn = (String) params[i];
            } else if (arg.equalsIgnoreCase("-qanumwronganswersallowed")) {
                if (qaNumWrongAnswersAllowed != null) {
                    System.out.println("Error:  -qanumwronganswersallowed " +
                            "already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for " +
                            "-qanumwronganswersallowed missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                qaNumWrongAnswersAllowed =
                        getInteger((String) params[i], "-qanumwronganswersallowed");
                if (qaNumWrongAnswersAllowed == null) {
                    return;
                }
            } else if (arg.equalsIgnoreCase("-auth")) {
                if ((getauth != null) || (removeauth != null) ||
                        (setauth != null)) {
                    System.out.println("Error:  -auth already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                while (i + 1 < params.length) {
                    arg = (String) params[i + 1];
                    if (arg.equalsIgnoreCase("-get")) {
                        i += 1;
                        if (getauth != null) {
                            System.out.println("Error:  -auth -get already " +
                                    "specified.");
                            System.out.println(TAB + AUTH_GEN_HELP);
                            return;
                        }
                        getauth = new ArrayList();
                        while (i + 1 < params.length) {
                            arg = (String) params[i + 1];
                            if ((arg.length() > 1) && (arg.charAt(0) == '-')) {
                                break;
                            }

                            if (arg.equals("-")) {
                                i += 1;
                                if (i + 1 >= params.length) {
                                    System.out.println("Error:  -auth -get missing " +
                                            "argument.");
                                    System.out.println(TAB + AUTH_GEN_HELP);
                                    return;
                                }
                                arg = (String) params[i + 1];
                            }

                            getauth.add(arg);
                            i += 1;
                        }
                    } else if (arg.equalsIgnoreCase("-remove")) {
                        i += 1;
                        if (removeauth != null) {
                            System.out.println("Error:  -auth -remove already " +
                                    "specified.");
                            System.out.println(TAB + AUTH_GEN_HELP);
                            return;
                        }
                        removeauth = new ArrayList();
                        while (i + 1 < params.length) {
                            arg = (String) params[i + 1];
                            if ((arg.length() > 1) && (arg.charAt(0) == '-')) {
                                break;
                            }

                            if (arg.equals("-")) {
                                i += 1;
                                if (i + 1 >= params.length) {
                                    System.out.println("Error:  -auth -remove missing " +
                                            "argument.");
                                    System.out.println(TAB + AUTH_GEN_HELP);
                                    return;
                                }
                                arg = (String) params[i + 1];
                            }

                            removeauth.add(arg);
                            i += 1;
                        }
                    } else if (arg.equalsIgnoreCase("-set")) {
                        i += 1;
                        if (setauth != null) {
                            System.out.println("Error:  -auth -set already " +
                                    "specified.");
                            System.out.println(TAB + AUTH_GEN_HELP);
                            return;
                        }

                        if (i + 1 < params.length) {
                            arg = (String) params[i + 1];
                            if (arg.equalsIgnoreCase("-merge")) {
                                mergeauth = true;
                                i += 1;
                            }
                        }

                        setauth = new ArrayList();
                        while (i + 1 < params.length) {
                            arg = (String) params[i + 1];
                            if ((arg.length() > 1) && (arg.charAt(0) == '-')) {
                                break;
                            }

                            if (arg.equals("-")) {
                                i += 1;
                                if (i + 1 >= params.length) {
                                    System.out.println("Error:  -auth -set missing " +
                                            "argument.");
                                    System.out.println(TAB + AUTH_GEN_HELP);
                                    return;
                                }
                                arg = (String) params[i + 1];
                            }

                            NameValue set = new NameValue();
                            set.setName(arg);

                            i += 1;
                            if (i + 1 >= params.length) {
                                System.out.println("Error:  -auth -set missing " +
                                        "argument.");
                                System.out.println(TAB + AUTH_GEN_HELP);
                                return;
                            }

                            set.setValue((String) params[i + 1]);

                            setauth.add(set);
                            i += 1;
                        }
                    } else {
                        break;
                    }
                }
            } else if (arg.equalsIgnoreCase("-registermachine")) {
                if ((registerMachine != null) || (machine != null)) {
                    System.out.println("Error:  -registermachine or -machine " +
                            "already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                registerMachine = Boolean.TRUE;
            } else if (arg.equalsIgnoreCase("-machine")) {
                if ((registerMachine != null) || (machine != null)) {
                    System.out.println("Error:  -registermachine or -machine " +
                            "already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                machine = new MachineSecret();
                i += 1;
                while (i < params.length) {
                    arg = (String) params[i];
                    if (arg.equalsIgnoreCase("-label")) {
                        if (machine.getMachineLabel() != null) {
                            System.out.println("Error:  -machine -label" +
                                    "already specified.");
                            System.out.println(TAB + AUTH_GEN_HELP);
                            return;
                        }
                        i += 1;
                        if (i >= params.length) {
                            System.out.println("Error:  missing argument for " +
                                    "-machine -label");
                            System.out.println(TAB + AUTH_GEN_HELP);
                            return;
                        }
                        machine.setMachineLabel((String) params[i]);
                        i += 1;
                    } else if (arg.equalsIgnoreCase("-machinenonce")) {
                        if (machine.getMachineNonce() != null) {
                            System.out.println("Error:  -machine -machinenonce" +
                                    "already specified.");
                            System.out.println(TAB + AUTH_GEN_HELP);
                            return;
                        }
                        i += 1;
                        if (i >= params.length) {
                            System.out.println("Error:  missing argument for " +
                                    "-machine -machinenonce");
                            System.out.println(TAB + AUTH_GEN_HELP);
                            return;
                        }
                        machine.setMachineNonce((String) params[i]);
                        i += 1;
                    } else if (arg.equalsIgnoreCase("-appdata")) {
                        if (machine.getApplicationData() != null) {
                            System.out.println("Error:  -machine -appdata" +
                                    "already specified.");
                            System.out.println(TAB + AUTH_GEN_HELP);
                            return;
                        }
                        i += 1;
                        List list = new ArrayList();
                        while (i < params.length) {
                            NameValue nv = new NameValue();
                            nv.setName((String) params[i]);
                            if ((nv.getName().charAt(0) == '-') &&
                                    (!nv.getName().equals("-"))) {
                                i -= 1;
                                break;
                            }
                            if (nv.getName().equals("-")) {
                                i += 1;
                                if (i >= params.length) {
                                    System.out.println("Error: missing argument for " +
                                            "-machine -appdata.");
                                    System.out.println(TAB + AUTH_GEN_HELP);
                                    return;
                                }
                                nv.setName((String) params[i]);
                            }

                            i += 1;
                            if (i >= params.length) {
                                System.out.println("Error: missing value argument " +
                                        "for -machine -appdata.");
                                System.out.println(TAB + AUTH_GEN_HELP);
                                return;
                            }
                            nv.setValue((String) params[i]);
                            list.add(nv);

                            i += 1;
                        }

                        NameValue[] appdata = new NameValue[list.size()];
                        list.toArray(appdata);
                        machine.setApplicationData(appdata);
                    } else {
                        i -= 1;
                        break;
                    }
                }
            } else if (arg.equalsIgnoreCase("-transactionDetails")) {
                if (transactionDetails != null) {
                    System.out.println("Error:  -transactionDetails " +
                            "already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                List list = new ArrayList();
                while (i < params.length) {
                    NameValue nv = new NameValue();
                    nv.setName((String) params[i]);
                    if ((nv.getName().charAt(0) == '-') &&
                            (!nv.getName().equals("-"))) {
                        i -= 1;
                        break;
                    }
                    if (nv.getName().equals("-")) {
                        i += 1;
                        if (i >= params.length) {
                            System.out.println("Error: missing argument for " +
                                    "-transactionDetails.");
                            System.out.println(TAB + AUTH_GEN_HELP);
                            return;
                        }
                        nv.setName((String) params[i]);
                    }

                    i += 1;
                    if (i >= params.length) {
                        System.out.println("Error: missing value argument " +
                                "for -transactionDetails.");
                        System.out.println(TAB + AUTH_GEN_HELP);
                        return;
                    }
                    nv.setValue((String) params[i]);
                    list.add(nv);

                    i += 1;
                }

                if (list.size() == 0) {
                    System.out.println("Error: missing value argument " +
                            "for -transactionDetails.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }

                transactionDetails = new NameValue[list.size()];
                list.toArray(transactionDetails);
            } else if (arg.equalsIgnoreCase("-transactionId")) {
                if (transactionId != null) {
                    System.out.println("Error:  -transactionId " +
                            "already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error: missing argument " +
                            "for -transactionId.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                transactionId = (String) params[i];
            } else if (arg.equalsIgnoreCase("-certificateSignatureFile")) {
                if (certificateSignatureFile != null) {
                    System.out.println("Error:  -certificateSignatureFile " +
                            "already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error: missing argument " +
                            "for -certificateSignatureFile.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                certificateSignatureFile = (String) params[i];
            } else if (arg.equalsIgnoreCase("-transactionReceiptFile")) {
                if (transactionReceiptFile != null) {
                    System.out.println("Error:  -transactionReceiptFile " +
                            "already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error: missing argument " +
                            "for -transactionReceiptFile.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                transactionReceiptFile = (String) params[i];
            } else if (arg.equalsIgnoreCase("-performDeliveryAndSignature")) {
                if (performDeliveryAndSignature != null) {
                    System.out.println(
                            "Error:  -performDeliveryAndSignature already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                performDeliveryAndSignature = Boolean.TRUE;

                if (i + 1 < params.length) {
                    arg = (String) params[i + 1];
                    if (arg.equalsIgnoreCase("true")) {
                        performDeliveryAndSignature = Boolean.TRUE;
                        i += 1;
                    } else if (arg.equalsIgnoreCase("false")) {
                        performDeliveryAndSignature = Boolean.FALSE;
                        i += 1;
                    }
                }
            } else if (arg.equalsIgnoreCase(
                    "-requireDeliveryAndSignatureIfAvailable")) {
                if (requireDeliveryAndSignatureIfAvailable != null) {
                    System.out.println(
                            "Error:  -requireDeliveryAndSignatureIfAvailable already " +
                                    "specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                requireDeliveryAndSignatureIfAvailable = Boolean.TRUE;

                if (i + 1 < params.length) {
                    arg = (String) params[i + 1];
                    if (arg.equalsIgnoreCase("true")) {
                        requireDeliveryAndSignatureIfAvailable = Boolean.TRUE;
                        i += 1;
                    } else if (arg.equalsIgnoreCase("false")) {
                        requireDeliveryAndSignatureIfAvailable = Boolean.FALSE;
                        i += 1;
                    }
                }
            } else if (arg.equalsIgnoreCase("-otpDelivery")) {
                if ((useDefaultDelivery != null) ||
                        (contactInfoLabel != null)) {
                    System.out.println("Error:  -otpDelivery already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  argument for -otpDelivery " +
                            "missing.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                String val = (String) params[i];
                if (val.equalsIgnoreCase("DEFAULT")) {
                    useDefaultDelivery = Boolean.TRUE;
                } else {
                    ArrayList list = new ArrayList();
                    while (i < params.length) {
                        arg = (String) params[i];
                        if (arg.equals("-")) {
                            i += 1;
                            if (i >= params.length) {
                                System.out.println(
                                        "Error:  argument for -otpDelivery missing.");
                                System.out.println(TAB + AUTH_GEN_HELP);
                                return;
                            }
                        } else if ((arg.length() > 0) && (arg.charAt(0) == '-')) {
                            i -= 1;
                            break;
                        }
                        list.add(arg);

                        i += 1;
                    }
                    contactInfoLabel =
                            (String[]) list.toArray(new String[list.size()]);
                }
            } else if (arg.equalsIgnoreCase("-deliverForDynamicRefresh")) {
                if (deliverForDynamicRefresh != null) {
                    System.out.println(
                            "Error:  -deliverForDynamicRefresh already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                deliverForDynamicRefresh = Boolean.TRUE;
            } else if (arg.equalsIgnoreCase("-biometricData")) {
                if (biometricData != null) {
                    System.out.println(
                            "Error:  -biometricData already specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                if (response != null) {
                    System.out.println(
                            "Error:  both -biometricData and response specified.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                i += 1;
                if (i >= params.length) {
                    System.out.println("Error:  -biometricData missing argument.");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
                try {
                    biometricData = readBinaryDataFromFile((String) params[i]);
                } catch (Exception e) {
                    System.out.println(
                            "Error:  -biometricData invalid argument: " +
                                    e.getMessage() + ".");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }
            } else {
                if (arg.equals("-")) {
                    i += 1;

                    if (i >= params.length) {
                        System.out.println("Error:  missing argument.");
                        System.out.println(TAB + AUTH_GEN_HELP);
                        return;
                    }

                    arg = (String) params[i];
                } else if ((arg.length() > 0) && (arg.charAt(0) == '-')) {
                    System.out.println("Error:  unknown argument " + arg + ".");
                    System.out.println(TAB + AUTH_GEN_HELP);
                    return;
                }

                if (userid == null) {
                    userid = arg;
                } else {
                    if (biometricData != null) {
                        System.out.println(
                                "Error:  both -biometricData and response specified.");
                        System.out.println(TAB + AUTH_GEN_HELP);
                        return;
                    }

                    if (response == null) {
                        response = new ArrayList();
                    }
                    response.add(arg);
                }
            }
        }

        if (userid == null) {
            System.out.println("Error:  userid missing.");
            System.out.println(TAB + AUTH_GEN_HELP);
            return;
        }

        if (biometricData != null) {
            response = new ArrayList();
            response.add(Base64.encodeBase64String(biometricData).trim());
        }

        String[] respArray = null;
        if (response != null) {
            respArray = new String[response.size()];
            response.toArray(respArray);
        }

        String[] getAuthArray = null;
        if (getauth != null) {
            getAuthArray = new String[getauth.size()];
            getauth.toArray(getAuthArray);
        }
        String[] removeAuthArray = null;
        if (removeauth != null) {
            removeAuthArray = new String[removeauth.size()];
            removeauth.toArray(removeAuthArray);
        }
        NameValue[] setAuthArray = null;
        if (setauth != null) {
            setAuthArray = new NameValue[setauth.size()];
            setauth.toArray(setAuthArray);
        }
        AuthenticationTypeEx[] challengeHistoryArray = null;
        if (challengeHistory != null) {
            challengeHistoryArray =
                    new AuthenticationTypeEx[challengeHistory.size()];
            challengeHistory.toArray(challengeHistoryArray);
        }
        AuthenticationTypeEx[] authTypesRequiringPVNArray = null;
        if (authTypesRequiringPVN != null) {
            authTypesRequiringPVNArray =
                    new AuthenticationTypeEx[authTypesRequiringPVN.size()];
            authTypesRequiringPVN.toArray(authTypesRequiringPVNArray);
        }

        String[] setsArray = null;
        if (sets != null) {
            setsArray = new String[sets.size()];
            sets.toArray(setsArray);
        }

        authenticateGenericChallenge(userid, authType, respArray,
                challengeSize, appname,
                challengeHistoryArray,
                qaNumWrongAnswersAllowed,
                getAuthArray, removeAuthArray, mergeauth,
                setAuthArray, newPassword, passwordName,
                pvn, newpvn,
                authTypesRequiringPVNArray, pap,
                securityLevel, ipAddress, registerMachine,
                machine, certificate,
                signatureData,
                tokenMutualAuth,
                transactionId,
                transactionDetails,
                certificateSignatureFile,
                transactionReceiptFile, serialNumber,
                setsArray, useDefaultDelivery,
                contactInfoLabel, deliverForDynamicRefresh,
                performDeliveryAndSignature,
                requireDeliveryAndSignatureIfAvailable);
    }

    /**
     * Parse user input into tokens.
     * <p/>
     * Simple method that breaks the provided string into a
     * sequence of tokens, without breaking quoted strings.
     * Does not handle escaped quotes or nested quotes.
     *
     * @param inputLine user input
     *
     * @return array of tokens, or the original input line if
     *         it contains an open quote
     */
    private static Object[] getParams(String inputLine) {
        final char QUOTE = '"';
        int next = 0;
        ArrayList list = new ArrayList();
        int i = inputLine.indexOf(QUOTE);
        while (i != -1) {
            String temp = inputLine.substring(next, i).trim();
            if (temp.length() > 0) {
                list.addAll(tokenize(temp));
            }
            next = i + 1;
            i = inputLine.indexOf(QUOTE, next);
            if (i == -1) {
                return new String[]{inputLine};
            }
            temp = inputLine.substring(next, i);
            list.add(temp);
            next = i + 1;
            i = inputLine.indexOf(QUOTE, next);
        }
        list.addAll(tokenize(inputLine.substring(next).trim()));
        return list.toArray();
    }

    /**
     * Tokenize a string.
     *
     * @param str a string
     *
     * @return list of tokens
     */
    private static ArrayList tokenize(String str) {
        StringTokenizer st = new StringTokenizer(str);
        ArrayList list = new ArrayList();
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        return list;
    }


    /**
     * Get the authenticate service binding.
     *
     * @return The binding used to invoke service operations.
     *
     * @throws Exception If binding could not be created
     */
    private static AuthenticationServiceBindingStub getBinding()
            throws Exception {
        if (ms_serviceBinding == null) {
            FailoverCallConfigurator failoverConfig =
                    new FailoverCallConfigurator(numberOfRetries, delayBetweenRetries);

            AuthenticationService_ServiceLocator locator =
                    new AuthenticationFailoverService_ServiceLocator(failoverFactory,
                            (verbose) ? new ConsoleLoggerImpl() : null,
                            failoverConfig);
            ms_serviceBinding =
                    (AuthenticationServiceBindingStub) locator.getAuthenticationService();
        }
        return ms_serviceBinding;
    }

    /**
     * This method pings the authentication service
     * <p/>
     * Method invoked: ping()
     */
    public static void ping() {
        try {
            log("Running 'ping' ...");

            getBinding().ping(new NameValue[0]);
            log("success.");
        } catch (AuthenticationFault ex) {
            log(ex);
        } catch (Exception ex) {
            log(ex);
        }
    }

    /**
     * Retrieves an anonymous challenge.<br>
     * This method is used for one step authentication mechanism.<br>
     * <p/>
     * Method invoked: getAnonymousChallenge()
     */
    public static void getAnonymousChallenge() {
        try {
            log("Running getAnonymousChallenge' ...");

            GenericChallenge gc = getBinding().getAnonymousChallenge();
            ms_cachedAnonymousChallengeSet = gc.getGridChallenge();

            log(genString(ms_cachedAnonymousChallengeSet));
            log(genString(gc.getPVNInfo()));
        } catch (AuthenticationFault ex) {
            log(ex);
        } catch (Exception ex) {
            log(ex);
        }
    }

    /**
     * Retrieves an anonymous challenge for the given group.<br>
     * This method is used for one step authentication mechanism.<br>
     * <p/>
     * Method invoked: getAnonymousChallengeForGroup( <GROUP> )
     */
    public static void getAnonymousChallengeForGroup(String group) {
        try {
            log("Running getAnonymousChallengeForGroup' ...");

            GenericChallenge gc = getBinding().getAnonymousChallengeForGroup(
                    new GetAnonymousChallengeForGroupCallParms(group));
            ms_cachedAnonymousChallengeSet = gc.getGridChallenge();

            log(genString(ms_cachedAnonymousChallengeSet));
            log(genString(gc.getPVNInfo()));
        } catch (AuthenticationFault ex) {
            log(ex);
        } catch (Exception ex) {
            log(ex);
        }
    }

    /**
     * Authentication user using last retrieved anonymous challenge.<br>
     * This method is used in the one step authentication mechanism. The
     * getAnonymousChallenge or getAnonymousChallengeForGroup
     * method must be called before invoking this method.<br>
     * <p/>
     * Method invoked: authenticateAnonymousChallenge
     *
     * @param userid            The userid being authenticated
     * @param challengeResponse The user's challenge response
     */
    public static void authenticateAnonymousChallenge(
            String userid,
            String[] challengeResponse,
            String pvn,
            String newpvn) {
        try {
            if (ms_cachedAnonymousChallengeSet == null) {
                log("No anonymous challenge found. You must run " +
                        "getAnonymousChallenge or getAnonymousChallengeForGroup " +
                        "first.");
                return;
            }

            log("Using last anonymous challenge: " +
                    genString(ms_cachedAnonymousChallengeSet));
            log("Using given response: " + genString(challengeResponse));

            GenericAuthenticateParms parms = new GenericAuthenticateParms();
            parms.setNewPVN(newpvn);

            AuthenticationServiceBindingStub binding = getBinding();
            GenericAuthenticateResponse resp =
                    binding.authenticateAnonymousChallenge(
                            new AuthenticateAnonymousChallengeCallParms(
                                    userid,
                                    ms_cachedAnonymousChallengeSet,
                                    new Response(pvn, challengeResponse, null),
                                    parms));

            CardData ci = resp.getCardInfo();
            if (ci != null) {
                System.out.println("authenticated with card " +
                        ci.getSerialNumber());
                System.out.println("  expiry date:     " +
                        dateFormat(ci.getExpiryDate(), "never"));
                System.out.println("  superseded date: " +
                        dateFormat(ci.getSupersededDate(), "never"));
            } else {
                System.out.println("authenticated with PIN.");
            }

            log("Authentication successful");
        } catch (AuthenticationFault ex) {
            log("Authentication failed");
            log(ex);
        } catch (Exception ex) {
            log(ex);
        }
    }

    /**
     * display a machine secret
     */
    private static void dumpMachineSecret(MachineSecret machineSecret) {
        log("Machine Secret:");
        log("   Machine Label:  " + machineSecret.getMachineLabel());
        log("   Machine Nonce:  " + machineSecret.getMachineNonce());
        log("   Sequence Nonce: " + machineSecret.getSequenceNonce());
        log("   App Data:       ");
        if (machineSecret.getApplicationData() != null) {
            for (int i = 0; i < machineSecret.getApplicationData().length; i++) {
                log("     Name:         " +
                        machineSecret.getApplicationData()[i].getName());
                log("     Value:        " +
                        machineSecret.getApplicationData()[i].getValue());
            }
        }
    }

    /**
     * display machine secret policy
     */
    private static void dumpMachineSecretPolicy(
            MachineSecretPolicy machineSecretPolicy) {
        if (machineSecretPolicy == null) return;

        log("Machine Secret Policy:");
        log("   Machine Nonce Required:   " +
                machineSecretPolicy.isMachineNonceRequired());
        log("   Sequence Nonce Required:  " +
                machineSecretPolicy.isSequenceNonceRequired());
        log("   App. Data Required:       " +
                machineSecretPolicy.getAppDataRequired());
        log("   Max. Machine Secret Size: " +
                machineSecretPolicy.getMaxMachineSecretSize());
    }

    /**
     * display an IP location
     */
    static private void dumpIPLocation(IPLocation location) {
        log("IP Location:");
        log("   Private Address: " + location.isPrivateAddress());
        log("   IP Address:      " + location.getIPAddress());
        if (!location.isPrivateAddress()) {
            log("   Country:         " + location.getCountry());
            log("   Country Name:    " + convert(location.getCountryName()));
            log("   Region:          " + location.getRegion());
            log("   Region Name:     " + convert(location.getRegionName()));
            log("   City:            " + location.getCity());
            log("   ISP:             " + location.getISP());
            log("   Latitude:        " + location.getLatitude());
            log("   Longitude:       " + location.getLongitude());
        }
    }

    /**
     * display a transaction receipt
     */
    static private void dumpTransactionReceipt(TransactionReceiptInfo receipt,
                                               String transactionReceiptFile)
            throws Exception {
        log("Transaction Receipt:");
        log("   Signature Type:  " + receipt.getTransactionSignatureType());
        log("   Receipt:         " + receipt.getTransactionReceipt());
        if (receipt.getTransactionSignatureFault() != null) {
            log("   Signature Fault: " +
                    receipt.getTransactionSignatureFault().getErrorMessage());
        }
        if (transactionReceiptFile != null) {
            PrintWriter pw =
                    new PrintWriter(new FileOutputStream(transactionReceiptFile));
            pw.println(receipt.getTransactionReceipt());
            pw.close();
            log("   Receipt File:    " + transactionReceiptFile);
        }
        if (receipt.getTransactionSignatureFault() != null) {
            log("   Signature Fault: " +
                    receipt.getTransactionSignatureFault().getErrorMessage());
        }
    }

    /**
     * display an array of authentication types
     */
    private static void dumpAllowedTypes(String title,
                                         AuthenticationTypeEx[] allowed,
                                         boolean showDefault) {
        log(title + ":");
        if ((allowed == null) || (allowed.length == 0)) {
            log("   none");
        } else {
            for (int i = 0; i < allowed.length; i++) {
                if ((i == 0) && showDefault) {
                    log("   " + allowed[i].toString() + " (default)");
                } else {
                    log("   " + allowed[i].toString());
                }
            }
        }
    }

    /**
     * convert a DeliveryMechanism to a displayable string
     */
    private static String convert(DeliveryMechanism mech) {
        if (mech == null) return "";
        return mech.getContactInfoLabel() + "/" +
                mech.getDeliveryConfigurationName() +
                (mech.isDefaultContactInfo() ? "/default" : "");
    }

    /**
     * Retrieves allowed authentication types for a user.
     * <p/>
     * Method invoked: getAllowedAuthenticationTypes
     *
     * @param userid The userid for which the allowed authentication types are
     *               retrieved.
     */
    public static void getAllowedAuthenticationTypes(String userid) {
        try {
            log("Running getAllowedAuthenticationTypes using userid '" +
                    userid + "' ...");

            AllowedAuthenticationTypesEx allowed =
                    getBinding().getAllowedAuthenticationTypesEx(new
                            GetAllowedAuthenticationTypesExCallParms(userid));

            dumpAllowedTypes("Generic Authentication", allowed.getGenericAuth(),
                    true);
            dumpAllowedTypes("Enhanced Generic Authentication",
                    allowed.getEnhancedGenericAuth(),
                    true);
            dumpAllowedTypes("Machine Registration", allowed.getMachineAuth(),
                    true);
            dumpAllowedTypes("Auth Types Can View Secrets",
                    allowed.getAuthTypeCanViewSecrets(),
                    false);
            dumpAllowedTypes("Auth Types Can Modify Secrets",
                    allowed.getAuthTypeCanModifySecrets(),
                    false);
            dumpAllowedTypes("Auth Types Requiring PVN",
                    allowed.getAuthTypesRequiringPVN(),
                    false);
            log("PVN Length:               " + allowed.getPVNLength());
        } catch (AuthenticationFault ex) {
            log(ex);
        } catch (Exception ex) {
            log(ex);
        }
    }

    /**
     * Retrieves allowed authentication types for a group.
     * <p/>
     * Method invoked: getAllowedAuthenticationTypesForGroup
     *
     * @param group The group for which the allowed authentication types are
     *              retrieved.
     */
    public static void getAllowedAuthenticationTypesForGroup(String group) {
        try {
            log("Running getAllowedAuthenticationTypesForGroup using group '" +
                    group + "' ...");

            AllowedAuthenticationTypesEx allowed =
                    getBinding().getAllowedAuthenticationTypesForGroupEx(
                            new GetAllowedAuthenticationTypesForGroupExCallParms(group));

            dumpAllowedTypes("Generic Authentication", allowed.getGenericAuth(),
                    true);
            dumpAllowedTypes("Enhanced Generic Authentication",
                    allowed.getEnhancedGenericAuth(),
                    true);
            dumpAllowedTypes("Machine Registration", allowed.getMachineAuth(),
                    true);
            dumpAllowedTypes("Auth Types Can View Secrets",
                    allowed.getAuthTypeCanViewSecrets(),
                    false);
            dumpAllowedTypes("Auth Types Can Modify Secrets",
                    allowed.getAuthTypeCanModifySecrets(),
                    false);
            dumpAllowedTypes("Auth Types Requiring PVN",
                    allowed.getAuthTypesRequiringPVN(),
                    false);
            log("PVN Length:               " + allowed.getPVNLength());
        } catch (AuthenticationFault ex) {
            log(ex);
        } catch (Exception ex) {
            log(ex);
        }
    }

    /**
     * Retrieves a generic challenge for the user.<br>
     * This method is used for two step generic authentication mechanism.<br>
     * <p/>
     * Method invoked: getGenericChallenge
     *
     * @param userid                      The userid for which a challenge is requested
     * @param authType                    The challenge type to use.
     * @param authTypeList                A list of challenge types to use.
     * @param appname                     The application name.
     * @param requiresPVN                 A flag indicating if only auth types that require/don't require
     *                                    a PVN are considered from the authTypeList
     * @param gridChallengeSize           The grid challenge size to request
     * @param qaChallengeSize             The QA challenge size to request
     * @param challengeHistory            A list of authentication types in challenge history
     * @param useDefaultDelivery          If true, use the default delivery mechanism for OTP OOB
     *                                    delivery.
     * @param contactInfoLabel            If set, use this delivery mechanism for OTP OOB delivery.
     * @param deliverForDynamicRefresh    If true, IG should deliver the OTPs when dynamic refresh
     *                                    is enabled.
     * @param onlySelectOTPAuthenticationIfDeliveryAvailable
     *                                    If true, IG should select OTP authentication
     *                                    only if the user has delivery mechanisms available when
     *                                    delivery mechanisms aren't specified in the request
     * @param passwordName                If getting a password change, the password name to use.
     * @param securityLevel               Security level of this request
     * @param ipAddress                   IP address to use in risk-based authentication.
     * @param machineSecret               Machine secret to use in risk-based authentication.
     * @param registerCertificate         Flag indicating if certificate should be registered.
     * @param certificate                 Certificate to use in risk-based authentication.
     * @param getAuth                     A list of the auth secrets to return.  An empty list means
     *                                    return all.
     * @param removeAuth                  A list of the auth secrets to remove.  An empty list means
     *                                    remove all.
     * @param merge                       Instead of replacing the existing values with the values in
     *                                    setAuth, merge them
     * @param setAuth                     A list of the auth secrets to add.  An empty list means
     *                                    remove all.
     * @param transactionDetails          a list of transaction details.
     * @param performDeliveryAndSignature Flag indicating if Entrust IdentityGuard should perform
     *                                    delivery and signature for tokens that support it.
     * @param requireDeliveryAndSignatureIfAvailable
     *                                    Flag indicating if Entrust IdentityGuard should only consider
     *                                    tokens that support delivery and signature when selecting
     *                                    tokens.
     * @param tokenTransactionMode        Value indicating whether to perform CLASSIC or ONLINE
     *                                    transaction for tokens.
     * @param tokenChallengeSummary       Summary describing the purpose for a token challenge.
     * @param performCertificateDelivery  Flag indicating if Entrust IdentityGuard should perform
     *                                    delivery for smart credentials that support it when getting
     *                                    a certificate challenge.
     * @param requireCertificateDelivery  Flag indicating if Entrust IdentityGuard should only
     *                                    consider smart credentials that support delivery when getting
     *                                    a certificate challenge.
     * @param smartCredentialChallengeSummary
     *                                    Summary describing the purpose for a smart credential challenge.
     * @param tokenSets                   a list of token sets to restrict which tokens are considered
     *                                    when generating a token challenge.
     * @param tokenMutualAuthChallenge    If specified, a token mutual authentication challenge generated
     *                                    by the token.
     */
    public static void getGenericChallenge(
            String userid,
            AuthenticationTypeEx authType,
            AuthenticationTypeEx[] authTypeList,
            String appname,
            Boolean requiresPVN,
            AuthenticationTypeEx[] authTypesRequiringPVN,
            Integer gridChallengeSize,
            Integer qaChallengeSize,
            AuthenticationTypeEx[] challengeHistory,
            Boolean useDefaultDelivery,
            String[] contactInfoLabel,
            Boolean deliverForDynamicRefresh,
            Boolean onlySelectOTPAuthenticationIfDeliveryAvailable,
            String passwordName,
            SecurityLevel securityLevel,
            String ipAddress,
            MachineSecret machineSecret,
            Boolean registerCertificate,
            String certificate,
            String[] getAuth,
            String[] removeAuth,
            boolean merge,
            NameValue[] setAuth,
            NameValue[] transactionDetails,
            Boolean performDeliveryAndSignature,
            Boolean requireDeliveryAndSignatureIfAvailable,
            TokenTransactionMode tokenTransactionMode,
            String tokenChallengeSummary,
            Boolean performCertificateDelivery,
            Boolean requireCertificateDelivery,
            String smartCredentialChallengeSummary,
            String[] tokenSets,
            String tokenMutualAuthChallenge) {
        try {
            log("Running getGenericChallenge using userid '" + userid + "' ...");

            // return all authentication parms.  Note that policy may prevent
            // the secrets from being returned.
            AuthenticationSecretParms authparms = new AuthenticationSecretParms();
            if (getAuth != null) {
                if (getAuth.length == 0) {
                    authparms.setGetAllSecrets(Boolean.TRUE);
                } else {
                    authparms.setGetSecrets(getAuth);
                }
            }
            if (removeAuth != null) {
                authparms.setRemoveSecrets(removeAuth);
            }
            if (setAuth != null) {
                if (merge) authparms.setMergeSecrets(Boolean.TRUE);
                authparms.setSetSecrets(setAuth);
            }
            GenericChallengeParmsEx parms = new GenericChallengeParmsEx();
            parms.setAuthSecretParms(authparms);
            parms.setAuthenticationType(authType);
            parms.setAuthenticationTypeList(authTypeList);
            parms.setApplicationName(appname);
            parms.setPasswordName(passwordName);
            parms.setRequiresPVN(requiresPVN);
            parms.setAuthTypesRequiringPVN(authTypesRequiringPVN);
            parms.setGridChallengeSize(gridChallengeSize);
            parms.setQAChallengeSize(qaChallengeSize);
            parms.setChallengeHistory(challengeHistory);
            parms.setSecurityLevel(securityLevel);
            parms.setIPAddress(ipAddress);
            parms.setRegisterCertificate(registerCertificate);
            parms.setCertificate(certificate);
            parms.setMachineSecret(machineSecret);
            parms.setUseDefaultDelivery(useDefaultDelivery);
            parms.setContactInfoLabel(contactInfoLabel);
            parms.setDeliverForDynamicRefresh(deliverForDynamicRefresh);
            parms.setOnlySelectOTPAuthenticationIfDeliveryAvailable(
                    onlySelectOTPAuthenticationIfDeliveryAvailable);
            parms.setTransactionDetails(transactionDetails);
            parms.setPerformDeliveryAndSignature(performDeliveryAndSignature);
            parms.setRequireDeliveryAndSignatureIfAvailable(
                    requireDeliveryAndSignatureIfAvailable);
            parms.setTokenTransactionMode(tokenTransactionMode);
            parms.setTokenChallengeSummary(tokenChallengeSummary);
            parms.setPerformCertificateDelivery(performCertificateDelivery);
            parms.setRequireCertificateDelivery(requireCertificateDelivery);
            parms.setSmartCredentialChallengeSummary(
                    smartCredentialChallengeSummary);
            parms.setTokenSets(tokenSets);
            parms.setTokenMutualAuthenticationChallenge(tokenMutualAuthChallenge);
            GenericChallengeEx challengeSet =
                    getBinding().getGenericChallengeEx(
                            new GetGenericChallengeExCallParms(userid, parms));

            log("Challenge Request Result: " +
                    challengeSet.getChallengeRequestResult());

            RiskScoringResult riskScore = challengeSet.getRiskScoringResult();
            if (riskScore != null) {
                log("Risk Score Result:");
                Boolean ipPassed = riskScore.getIPAuthenticationPassed();
                if (ipPassed == null) {
                    log("   IP Authentication not performed.");
                } else {
                    log("   IP Authentication Passed:   " + ipPassed);
                    IPAuthenticationStatus ipstatus =
                            riskScore.getIPAuthenticationStatus();
                    if (ipstatus != null) {
                        log("   IP Authentication Status:");
                        log("      Expected Locations:         " +
                                status(ipstatus.getExpectedLocations()));
                        log("      IP Blacklist:               " +
                                status(ipstatus.getIPBlacklist()));
                        log("      Country Blacklist:          " +
                                status(ipstatus.getCountryBlacklist()));
                        log("      Velocity:                   " +
                                status(ipstatus.getVelocity()));
                        log("      Location History:           " +
                                status(ipstatus.getLocationHistory()));
                    }
                }
                Boolean maPassed = riskScore.getMachineAuthenticationPassed();
                if (maPassed == null) {
                    log("   Machine Authentication not performed.");
                } else {
                    log("   Machine Authentication Passed:    " + maPassed);
                    MachineAuthenticationStatus mastatus =
                            riskScore.getMachineAuthenticationStatus();
                    if (mastatus != null) {
                        log("   Machine Authentication Status:");
                        log("      Machine Nonce Failed:          " +
                                mastatus.isMachineNonceFailed());
                        log("      Sequence Nonce Failed:         " +
                                mastatus.isSequenceNonceFailed());
                        log("      App. Data Failed:              " +
                                mastatus.isAppDataFailed());
                        log("      Num. Required App. Data:       " +
                                mastatus.getNumRequiredApplicationData());
                        log("      Num. Failed App. Data:         " +
                                mastatus.getNumFailedApplicationData());
                        log("      Secret Expired:                " +
                                mastatus.isSecretExpired());
                    }
                }
                Boolean caPassed = riskScore.getCertificateAuthenticationPassed();
                if (caPassed == null) {
                    log("   Certificate authentication not performed.");
                } else {
                    log("   Certificate Authentication Passed:    " + caPassed);
                    CertificateAuthenticationStatus castatus =
                            riskScore.getCertificateAuthenticationStatus();
                    if (castatus != null) {
                        log("   Certificate Authentication Status:");
                        log("      Certificate Registered:       " +
                                castatus.isCertificateRegistered());
                        log("      Certificate Valid:            " +
                                castatus.isCertificateValid());
                    }
                }
                ExternalRiskScoreResult externalResult =
                        riskScore.getExternalRiskScoreStatus();
                if (externalResult == null) {
                    log("   External Risk not performed.");
                } else {
                    log("   External Risk Result:                 " +
                            externalResult);
                    log("   External Risk Score:                  " +
                            riskScore.getExternalRiskScore());
                }
            }

            if (challengeSet.getChallengeRequestResult().equals(
                    ChallengeRequestResult.CHALLENGE)) {
                if (challengeSet.getType().equals(AuthenticationTypeEx.GRID)) {
                    ChallengeSet gridChallenge = challengeSet.getGridChallenge();
                    log("Grid Challenge:");
                    if (gridChallenge != null) {
                        if (gridChallenge.getCardSerialNumbers() != null) {
                            for (int i = 0;
                                 i < gridChallenge.getCardSerialNumbers().length; i++) {
                                log("  Card:                         " +
                                        gridChallenge.getCardSerialNumbers()[i]);
                            }
                        }
                        log("  Card Cell Alphabet:           " +
                                gridChallenge.getCardCellAlphabet());
                        log("  Card Cell Size:               " +
                                gridChallenge.getCardCellSize());
                        if (gridChallenge.isUserHasTemporaryPin()) {
                            log("  User has temporary PIN.");
                        }
                        log("  Temporary PIN Cell Alphabet:  " +
                                gridChallenge.getTemporaryPinCellAlphabet());
                        log("  Temporary PIN Challenge Size: " +
                                gridChallenge.getTemporaryPinChallengeSize());
                        log("  Temporary PIN Cell Size:      " +
                                gridChallenge.getTemporaryPinCellSize());
                        log(genString(gridChallenge));
                    }
                } else if (challengeSet.getType().equals(AuthenticationTypeEx.QA)) {
                    log("QA Challenge:");
                    String[] qa = challengeSet.getQAChallenge();
                    for (int i = 0; i < qa.length; i++) {
                        log("   Question: " + qa[i]);
                    }
                } else if (challengeSet.getType().equals(AuthenticationTypeEx.OTP)) {
                    log("OTP Challenge.");
                    OTPChallenge otp = challengeSet.getOTPChallenge();
                    if ((otp.getDeliveryMechanismUsed() != null) &&
                            (otp.getDeliveryMechanismUsed().length > 0)) {
                        log("OTP Successfully Delivered: ");
                        for (int i = 0; i < otp.getDeliveryMechanismUsed().length; i++) {
                            System.out.println(
                                    "   " +
                                            convert(otp.getDeliveryMechanismUsed()[i]));
                        }
                    }
                    if ((otp.getDeliveryMechanismFailed() != null) &&
                            (otp.getDeliveryMechanismFailed().length > 0)) {
                        log("OTP Failed Delivery: ");
                        for (int i = 0; i < otp.getDeliveryMechanismFailed().length; i++) {
                            System.out.println(
                                    "   " +
                                            convert(otp.getDeliveryMechanismFailed()[i]) + ": " +
                                            otp.getDeliveryMechanismFailureReason()[i].getMessage());
                        }
                    }
                    if (otp.getDeliveryMechanism() != null
                            && otp.getDeliveryMechanism().length > 0) {
                        log("Available OTP Delivery mechanisms:");
                        for (int i = 0; i < otp.getDeliveryMechanism().length; i++) {
                            log("   " + convert(otp.getDeliveryMechanism()[i]));
                        }
                    }
                    if (otp.getManualDeliveryRequired() != null) {
                        log("Manual delivery: " + otp.getManualDeliveryRequired());
                    }
                    log("Dynamic Refresh: " + otp.isDynamicRefresh());
                    if (otp.isDynamicRefresh()) {
                        if ((otp.getNeedsDeliveryForChallenge() != null) &&
                                (otp.getNeedsDeliveryForChallenge().booleanValue())) {
                            log("   Needs Delivery for Challenge.");
                        }
                        if ((otp.getNeedsDeliveryForAuthenticate() != null) &&
                                (otp.getNeedsDeliveryForAuthenticate().booleanValue())) {
                            log("   Needs Delivery for Authenticate.");
                        }
                    }
                } else if ((challengeSet.getType().equals(
                        AuthenticationTypeEx.TOKENRO)) ||
                        (challengeSet.getType().equals(
                                AuthenticationTypeEx.TOKENCR))) {
                    log("Token Challenge.");
                    TokenChallengeEx tokchall = challengeSet.getTokenChallenge();
                    if (tokchall != null) {
                        if (tokchall.getChallenge() != null) {
                            log("  Challenge:                      " +
                                    tokchall.getChallenge());
                        }
                        if (tokchall.getTokens() != null) {
                            TokenSetParser tokenParser =
                                    new TokenSetParser(tokchall.getTokens());
                            Iterator setit = tokenParser.getTokenSets().iterator();
                            while (setit.hasNext()) {
                                String set = (String) setit.next();
                                if (set.equals("")) {
                                    log("  Default Token Set:");
                                } else {
                                    log("  Token Set:                      " + set);
                                }
                                Iterator tokit =
                                        tokenParser.getTokensForSet(set).iterator();
                                while (tokit.hasNext()) {
                                    TokenDataEx tok = (TokenDataEx) tokit.next();
                                    log("     Token:                       " +
                                            tok.getVendorId() + " " +
                                            tok.getSerialNumber());
                                    if (challengeSet.getType().equals(
                                            AuthenticationTypeEx.TOKENRO)) {
                                        log("         Supports Data Signature:         " +
                                                tok.isSupportsDataSignature());
                                        log("         Supports Mutual Authentication:  " +
                                                tok.isSupportsMutualAuthentication());
                                        log("         Supports Delivery and Signature: " +
                                                tok.isSupportsDeliveryAndSignature());
                                        log("         Supports Online Transactions:    " +
                                                tok.isSupportsOnlineTransactions());
                                        log("         Supports Offline Transactions:   " +
                                                tok.isSupportsOfflineTransactions());
                                        if (tok.getMutualAuthenticationResponse() != null) {
                                            log("         Mutual Authentication Response:  " +
                                                    tok.getMutualAuthenticationResponse());
                                        }
                                        log("         Delivery Status:                 " +
                                                tok.getDeliveryStatus());
                                    }
                                }
                            }
                        }
                        if (tokchall.getTransactionMode() != null) {
                            log("  Transasction Mode:              " +
                                    tokchall.getTransactionMode());
                            if (TokenTransactionMode.ONLINE.equals(
                                    tokchall.getTransactionMode())) {
                                log("  Transasction Id:                " +
                                        tokchall.getTransactionId());
                                log("  Transasction Create Date:       " +
                                        dateFormat(tokchall.getCreateDate(), ""));
                                log("  Transasction Lifetime:          " +
                                        tokchall.getLifetime());
                            }
                        }
                        if ((tokchall.getHasTemporaryPIN() != null) &&
                                (tokchall.getHasTemporaryPIN().booleanValue())) {
                            log("  User has temporary PIN.");
                        }
                        log("  Temporary PIN Cell Alphabet:    " +
                                tokchall.getTemporaryPINCellAlphabet());
                        log("  Temporary PIN Challenge Size:   " +
                                tokchall.getTemporaryPINChallengeSize());
                        log("  Temporary PIN Cell Size:        " +
                                tokchall.getTemporaryPINCellSize());
                    }
                } else if (challengeSet.getType().equals(AuthenticationTypeEx.EXTERNAL)) {
                    log("External Challenge.");
                    ExternalChallenge ec = challengeSet.getExternalChallenge();
                    if (ec.getChallenge() != null) {
                        log("   Challenge: " + ec.getChallenge());
                    }
                } else if (challengeSet.getType().equals(AuthenticationTypeEx.NONE)) {
                    log("NONE Challenge.");
                } else if (challengeSet.getType().equals(AuthenticationTypeEx.PASSWORD)) {
                    log("Password Challenge.");
                    PasswordChallenge pc = challengeSet.getPasswordChallenge();
                    log("   Password Change Required: " +
                            pc.getChangeRequired().toString());
                    log("   Password Expiry Date: " + dateFormat(pc.getExpiryDate(), "never"));
                    log("   Password Change Allowed After Date: " +
                            dateFormat(pc.getAllowChangeAfterDate(), "illegal value"));
                    PasswordRules pr = pc.getPasswordRules();
                    log("   Password Rules:");
                    log("      Upper Case:              " + pr.getUpperCase());
                    log("      Lower Case:              " + pr.getLowerCase());
                    log("      Digit:                   " + pr.getNumber());
                    log("      Special:                 " + pr.getSpecialChar());
                    log("      Minimum Length:          " + pr.getMinimumLength());
                    log("      Maximum Repeated Chars.: " + pr.getMaxRepeatedChars());
                } else if (challengeSet.getType().equals(
                        AuthenticationTypeEx.CERTIFICATE)) {
                    log("Certificate Challenge.");
                    CertificateChallenge cc = challengeSet.getCertificateChallenge();
                    log("   Certificate Challenge:    " +
                            cc.getChallenge());
                    log("   Hashing Algorithm:        " +
                            cc.getHashingAlgorithm());
                    log("   Available Certificates:");
                    if (cc.getCertificates() != null) {
                        for (int i = 0; i < cc.getCertificates().length; i++) {
                            log("    Issuer DN:               " +
                                    cc.getCertificates()[i].getIssuerDN());
                            log("    Subject DN:              " +
                                    cc.getCertificates()[i].getSubjectDN());
                            log("    Serial Number:           " +
                                    cc.getCertificates()[i].getSerialNumber());
                            log("    Issue Date:              " +
                                    dateFormat(
                                            cc.getCertificates()[i].getIssueDate(), ""));
                            log("    Expiry Date:             " +
                                    dateFormat(
                                            cc.getCertificates()[i].getExpiryDate(), ""));
                        }
                    }
                    log("   Available SmartCredentials:");
                    if (cc.getSmartCredentials() != null) {
                        for (int i = 0; i < cc.getSmartCredentials().length; i++) {
                            log("      Id:                          " +
                                    cc.getSmartCredentials()[i].getSmartCredentialId());
                            log("      Friendly Name:               " +
                                    cc.getSmartCredentials()[i].getFriendlyName());
                            log("      Mobile:                      " +
                                    cc.getSmartCredentials()[i].isMobile());
                            log("      Supports Security Challenge: " +
                                    cc.getSmartCredentials()[i].isSupportsSecurityChallenge());
                            log("      Delivery Status:             " +
                                    cc.getSmartCredentials()[i].getDeliveryStatus());
                        }
                    }
                    if (cc.getTransactionId() != null) {
                        log("   Transaction Id:           " + cc.getTransactionId());
                    }
                    log("   Create Date:              " +
                            dateFormat(cc.getCreateDate(), ""));
                    log("   Lifetime (seconds):       " + cc.getLifetime());
                } else if (challengeSet.getType().equals(
                        AuthenticationTypeEx.BIOMETRIC)) {
                    log("Biometric Challenge.");
                    BiometricChallenge bc = challengeSet.getBiometricChallenge();
                    if (bc.getBiometrics() != null) {
                        for (int i = 0; i < bc.getBiometrics().length; i++) {
                            log("    Vendor Id:               " +
                                    bc.getBiometrics()[i].getVendorId());
                            log("    Type:                    " +
                                    bc.getBiometrics()[i].getType());
                            log("    Vendor Manufacturer:     " +
                                    bc.getBiometrics()[i].getVendorManufacturer());
                        }
                    }
                }
            }

            if (challengeSet.getTransactionId() != null) {
                log("Transaction Id: " + challengeSet.getTransactionId());
            }

            machineSecret = challengeSet.getMachineSecret();
            if (machineSecret != null) {
                dumpMachineSecret(machineSecret);
            }
            dumpMachineSecretPolicy(challengeSet.getMachineSecretPolicy());

            IPLocation ipLocation = challengeSet.getIPLocation();
            if (ipLocation != null) {
                dumpIPLocation(ipLocation);
            }

            CertificateData cert = challengeSet.getCertificate();
            if (cert != null) {
                log("Certificate used for RBA:");
                log("    Issuer DN:               " + cert.getIssuerDN());
                log("    Subject DN:              " + cert.getSubjectDN());
                log("    Serial Number:           " + cert.getSerialNumber());
                log("    Issue Date:              " +
                        dateFormat(cert.getIssueDate(), ""));
                log("    Expiry Date:             " +
                        dateFormat(cert.getExpiryDate(), ""));
            }

            NameValue[] authsec = challengeSet.getAuthenticationSecrets();
            if (authsec != null) {
                log("Authentication secrets:");
                for (int i = 0; i < authsec.length; i++) {
                    log("   " + authsec[i].getName());
                }
            } else {
                log("No authentication secrets returned.");
            }

            log(genString(challengeSet.getPVNInfo()));

            log("Disable Challenge Retention: " +
                    challengeSet.getDisableChallengeRetention());

            log("Group: " + challengeSet.getGroup());
        } catch (AuthenticationFault ex) {
            log(ex);
        } catch (Exception ex) {
            log(ex);
        }
    }

    /**
     * Authenticates generic challenge for user.<br>
     * This method is used in the generic authentication mechanism. The
     * getGenericChallenge method must * be called before invoking this
     * method.<br>
     * <p/>
     * Method invoked: authenticateGenericChallenge
     *
     * @param userId                      The userid being authenticated
     * @param authType                    The challenge type being used
     * @param challengeResponse           The user's challenge response
     * @param challengeSize               The size of the challenge to authenticate
     * @param appname                     The application name.
     * @param challengeHistory            A list of authentication types the application has already
     *                                    authenticated
     * @param getAuth                     A list of the auth secrets to return.  An empty list means
     *                                    return all.
     * @param removeAuth                  A list of the auth secrets to remove.  An empty list means
     *                                    remove all.
     * @param merge                       Instead of replacing the existing values with the values in
     *                                    setAuth, merge them
     * @param setAuth                     A list of the auth secrets to add.  An empty list means
     *                                    remove all.
     * @param newPassword                 The new password will replace the old password for the case
     *                                    where a password change is required.
     * @param passwordName                The name of the password to use when authenticating a password
     *                                    challenge.
     * @param pvn                         The PVN value
     * @param newPVN                      The new PVN value
     * @param authTypesRequiringPVN       A list of authentication types that require PVN in addition
     *                                    to those specified in policy.
     * @param securityLevel               The security level of this request.
     * @param ipAddress                   The ipAddress this client authenticated from
     * @param registerMachine             If set to true, a new machine secret will be registered.
     * @param machine                     If not null, a machine secret to be registered or updated.
     * @param certificate                 If not null, a certificate to be registered.
     * @param signatureData               If not null, the data to be validated as part of a TOKENRO
     *                                    authentication.
     * @param tokenMutualAuth             If set to true, the response will be validated as a token
     *                                    mutual authentication response.
     * @param transactionId               The transaction id.
     * @param transactionDetails          The transaction details.
     * @param certificateSignatureFile    If a certificate signature is returned, this argument specifies
     *                                    the file to which it will be written.
     * @param serialNumber                A serial number to specify which grid or token is used when
     *                                    authenticating a grid or token response.
     * @param sets                        A list of sets to restrict which tokens are considered when
     *                                    authenticating a token response.
     * @param useDefaultDelivery          If true, OTPs generated during authentication will be delivered
     *                                    to the user's default contact
     * @param contactLabel                If set and useDefaultDelivery is not true, OTPs generated
     *                                    during authentication will be delivered to the specified
     *                                    contacts.
     * @param deliverForDynamicRefresh    If true, OTPs will be delivered if dynamic refresh is enabled
     *                                    even if new OTPs were not generated.
     * @param performDeliveryAndSignature Flag indicating if Entrust IdentityGuard should perform
     *                                    delivery and signature for tokens that support it.
     * @param requireDeliveryAndSignatureIfAvailable
     *                                    Flag indicating if Entrust IdentityGuard should only consider
     *                                    tokens that support delivery and signature when authenticating
     *                                    token response.
     */
    public static void authenticateGenericChallenge(
            String userId,
            AuthenticationTypeEx authType,
            String[] challengeResponse,
            Integer challengeSize,
            String appname,
            AuthenticationTypeEx[] challengeHistory,
            Integer qaNumWrongAnswersAllowed,
            String[] getAuth,
            String[] removeAuth,
            boolean merge,
            NameValue[] setAuth,
            String newPassword,
            String passwordName,
            String pvn,
            String newpvn,
            AuthenticationTypeEx[] authTypesRequiringPVN,
            boolean pap,
            SecurityLevel securityLevel,
            String ipAddress,
            Boolean registerMachine,
            MachineSecret machineSecret,
            String certificate,
            String[] signatureData,
            Boolean tokenMutualAuth,
            String transactionId,
            NameValue[] transactionDetails,
            String certificateSignatureFile,
            String transactionReceiptFile,
            String serialNumber,
            String[] sets,
            Boolean useDefaultDelivery,
            String[] contactInfoLabel,
            Boolean deliverForDynamicRefresh,
            Boolean performDeliveryAndSignature,
            Boolean requireDeliveryAndSignatureIfAvailable) {
        try {
            AuthenticationServiceBindingStub binding = getBinding();
            AuthenticationSecretParms authparms = new AuthenticationSecretParms();
            if (getAuth != null) {
                if (getAuth.length == 0) {
                    authparms.setGetAllSecrets(Boolean.TRUE);
                } else {
                    authparms.setGetSecrets(getAuth);
                }
            }
            if (removeAuth != null) {
                authparms.setRemoveSecrets(removeAuth);
            }
            if (setAuth != null) {
                if (merge) authparms.setMergeSecrets(Boolean.TRUE);
                authparms.setSetSecrets(setAuth);
            }
            GenericAuthenticateParmsEx parms = new GenericAuthenticateParmsEx();
            parms.setAuthenticationType(authType);
            parms.setChallengeSize(challengeSize);
            parms.setApplicationName(appname);
            parms.setNumWrongAnswersAllowed(qaNumWrongAnswersAllowed);
            parms.setAuthSecretParms(authparms);
            parms.setChallengeHistory(challengeHistory);
            parms.setNewPassword(newPassword);
            parms.setPasswordName(passwordName);
            parms.setNewPVN(newpvn);
            parms.setAuthTypesRequiringPVN(authTypesRequiringPVN);
            parms.setSecurityLevel(securityLevel);
            parms.setIPAddress(ipAddress);
            parms.setRegisterMachineSecret(registerMachine);
            parms.setMachineSecret(machineSecret);
            parms.setCertificate(certificate);
            parms.setDataSignatureValues(signatureData);
            parms.setTokenMutualAuthentication(tokenMutualAuth);
            parms.setUseDefaultDelivery(useDefaultDelivery);
            parms.setContactInfoLabel(contactInfoLabel);
            parms.setDeliverForDynamicRefresh(deliverForDynamicRefresh);
            Response response = null;
            if (pap) {
                String resp = "";
                if (challengeResponse != null) {
                    for (int i = 0; i < challengeResponse.length; i++) {
                        resp += challengeResponse[i];
                    }
                }
                response =
                        new Response(
                                pvn,
                                null,
                                new RadiusResponse(
                                        new PAPResponse(resp.getBytes("UTF8")),
                                        null,
                                        null,
                                        null));
            } else {
                response = new Response(pvn, challengeResponse, null);
            }
            parms.setTransactionId(transactionId);
            if (certificateSignatureFile != null) {
                parms.setReturnCertificateResponse(Boolean.TRUE);
            }
            parms.setTransactionDetails(transactionDetails);
            parms.setPerformDeliveryAndSignature(performDeliveryAndSignature);
            parms.setRequireDeliveryAndSignatureIfAvailable(
                    requireDeliveryAndSignatureIfAvailable);
            parms.setSerialNumber(serialNumber);
            parms.setTokenSets(sets);
            GenericAuthenticateResponseEx resp =
                    binding.authenticateGenericChallengeEx(
                            new AuthenticateGenericChallengeExCallParms(
                                    userId,
                                    response,
                                    parms));

            if (resp.getGroup() != null) {
                System.out.println("Group of authenticating user is " +
                        resp.getGroup());
            }

            if (resp.getFullName() != null) {
                System.out.println("Full name of authenticating user is " +
                        resp.getFullName());
            }

            CardData ci = resp.getCardInfo();
            if (ci != null) {
                System.out.println("authenticated with card " +
                        ci.getSerialNumber());
                System.out.println("  expiry date:     " +
                        dateFormat(ci.getExpiryDate(), "never"));
                System.out.println("  superseded date: " +
                        dateFormat(ci.getSupersededDate(), "never"));
            }
            TokenDataEx ti = resp.getTokenInfo();
            if (ti != null) {
                System.out.print("authenticated with token " +
                        ti.getVendorId() + " " + ti.getSerialNumber());
                if (ti.getTokenSet().equals("")) {
                    System.out.println(" in the default set.");
                } else {
                    System.out.println(" with the set " + ti.getTokenSet() + ".");
                }
            }
            BiometricData biometricInfo = resp.getBiometricInfo();
            if (biometricInfo != null) {
                System.out.println("Authenticated with biometric:");
                System.out.println("    Vendor Id:               " +
                        biometricInfo.getVendorId());
                System.out.println("    Type:                    " +
                        biometricInfo.getType());
                System.out.println("    Vendor Manufacturer:     " +
                        biometricInfo.getVendorManufacturer());
            }
            CertificateData certInfo = resp.getCertificateInfo();
            if (certInfo != null) {
                System.out.println("Authenticated with certificate:");
                System.out.println("    Issuer DN:               " +
                        certInfo.getIssuerDN());
                System.out.println("    Subject DN:              " +
                        certInfo.getSubjectDN());
                System.out.println("    Serial Number:           " +
                        certInfo.getSerialNumber());
                System.out.println("    Issue Date:              " +
                        dateFormat(certInfo.getIssueDate(), ""));
                System.out.println("    Expiry Date:             " +
                        dateFormat(certInfo.getExpiryDate(), ""));
            }
            SmartCredentialData scInfo = resp.getSmartCredentialInfo();
            if (scInfo != null) {
                System.out.println("Authenticated with smart credential:");
                System.out.println("    Id:                      " +
                        scInfo.getSmartCredentialId());
                System.out.println("    Friendly Name:           " +
                        scInfo.getFriendlyName());
                if ((scInfo.getResponse() != null) &&
                        (certificateSignatureFile != null)) {
                    PrintWriter pw =
                            new PrintWriter(
                                    new FileOutputStream(certificateSignatureFile));
                    pw.println(scInfo.getResponse());
                    pw.close();
                    System.out.println("    Signature File:          " +
                            certificateSignatureFile);
                }
            }
            PasswordInfo pi = resp.getPasswordInfo();
            if (pi != null) {
                System.out.println("password expiry: " +
                        dateFormat(pi.getExpiryDate(), "never"));
                System.out.println("password change allowed after date: " +
                        dateFormat(pi.getAllowChangeAfterDate(), "illegal value"));
            }
            NameValue[] authsec = resp.getAuthenticationSecrets();
            if (authsec != null) {
                log("Authentication secrets:");
                for (int i = 0; i < authsec.length; i++) {
                    log("   " + authsec[i].getName());
                }
            } else {
                log("No authentication secrets returned.");
            }
            AuthenticationTypeEx[] updatedHistory = resp.getChallengeHistory();
            if (updatedHistory != null) {
                log("Challenge history:");
                for (int i = 0; i < updatedHistory.length; i++) {
                    log("   " + updatedHistory[i].toString());
                }
            }
            MachineSecret newMachine = resp.getMachineSecret();
            if (newMachine != null) {
                dumpMachineSecret(newMachine);
            }
            IPLocation ipLocation = resp.getIPLocation();
            if (ipLocation != null) {
                dumpIPLocation(ipLocation);
            }
            CertificateData registeredCert = resp.getCertificateRegistered();
            if (registeredCert != null) {
                System.out.println("Registered certificate:");
                System.out.println("    Issuer DN:               " +
                        registeredCert.getIssuerDN());
                System.out.println("    Subject DN:              " +
                        registeredCert.getSubjectDN());
                System.out.println("    Serial Number:           " +
                        registeredCert.getSerialNumber());
                System.out.println("    Issue Date:              " +
                        dateFormat(registeredCert.getIssueDate(), ""));
                System.out.println("    Expiry Date:             " +
                        dateFormat(registeredCert.getExpiryDate(), ""));
            }
            if ((resp.getDeliveryMechanismUsed() != null) &&
                    (resp.getDeliveryMechanismUsed().length > 0)) {
                log("OTP Successfully Delivered: ");
                for (int i = 0; i < resp.getDeliveryMechanismUsed().length; i++) {
                    System.out.println(
                            "   " +
                                    convert(resp.getDeliveryMechanismUsed()[i]));
                }
            }
            if ((resp.getDeliveryMechanismFailed() != null) &&
                    (resp.getDeliveryMechanismFailed().length > 0)) {
                log("OTP Failed Delivery: ");
                for (int i = 0; i < resp.getDeliveryMechanismFailed().length; i++) {
                    System.out.println(
                            "   " +
                                    convert(resp.getDeliveryMechanismFailed()[i]) + ": " +
                                    resp.getDeliveryMechanismFailureReason()[i].getMessage());
                }
            }
            if (resp.getOTPDynamicRefresh() != null) {
                log("OTP Dynamic Refresh: " + resp.getOTPDynamicRefresh());
            }
            if (resp.getOTPNewGenerated() != null) {
                log("OTP New Generated:   " + resp.getOTPNewGenerated());
            }

            TransactionReceiptInfo receipt = resp.getTransactionReceiptInfo();
            if (receipt != null) {
                dumpTransactionReceipt(receipt, transactionReceiptFile);
            }

            if (resp.getLastAuth() != null) {
                System.out.println("Last Auth Date: " +
                        dateFormat(resp.getLastAuth().getDate(), ""));
                System.out.println("Last Auth Type: " +
                        resp.getLastAuth().getType());
            }

            if (resp.getLastFailedAuth() != null) {
                System.out.println(
                        "Last Failed Auth Date: " +
                                dateFormat(resp.getLastFailedAuth().getDate(), ""));
                System.out.println(
                        "Last Failed Auth Type: " +
                                resp.getLastFailedAuth().getType());
            }

            log("Authentication successful");

            AuthenticationFault warningFault = resp.getWarningFault();
            if (warningFault != null) {
                // shared secrets operations error will not throw exceptions,
                // log the error here
                log("Error encountered updating secrets:");
                log("  " + warningFault.getErrorMessage());
            }
        } catch (AuthenticationPasswordChangeRequiredFault ex) {
            log("Password Change Required exception:");
            log("  Either the password is expired or the changeRequired flag is set.");

            PasswordRules pr = ex.getPasswordRules();
            log("   Password Rules:");
            log("      Upper Case:              " + pr.getUpperCase());
            log("      Lower Case:              " + pr.getLowerCase());
            log("      Digit:                   " + pr.getNumber());
            log("      Special:                 " + pr.getSpecialChar());
            log("      Minimum Length:          " + pr.getMinimumLength());
            log("      Maximum Repeated Chars.: " + pr.getMaxRepeatedChars());
        } catch (AuthenticationFault ex) {
            log("Authentication failed");
            log(ex);
        } catch (Exception ex) {
            log(ex);
        }
    }

    /**
     * Utility to generate string output from a challengeSet.
     *
     * @param challengeSet The challenge set
     *
     * @return the generated String
     */
    private static String genString(ChallengeSet challengeSet) {
        StringBuffer buf = new StringBuffer("ChallengeSet = ");

        Challenge[] challArr = challengeSet.getChallenge();
        for (int i = 0; i < challArr.length; i++) {
            if (i != 0) {
                buf.append(" ");
            }
            Challenge chall = challArr[i];
            buf.append('[');
            buf.append((char) (chall.getColumn() + (int) 'A'));
            buf.append(',');
            buf.append(chall.getRow() + 1);
            buf.append(']');
        }

        return buf.toString();
    }

    /**
     * Utility to generate string output from PVNInfo.
     *
     * @param pvnInfo The PVNInfo
     *
     * @return the generated String
     */
    private static String genString(PVNInfo pvnInfo) {
        StringBuffer buf = new StringBuffer("PVNInfo: ");
        if (pvnInfo == null) {
            buf.append("null");
        } else {
            buf.append("rqd(" + String.valueOf(pvnInfo.isRequired()) + ") ");
            buf.append("lgth(" + String.valueOf(pvnInfo.getLength()) + ") ");
            if (pvnInfo.getAvailable() == null) {
                buf.append("avl(null) ");
            } else {
                buf.append("avl(" + pvnInfo.getAvailable().toString() + ") ");
            }
            if (pvnInfo.getChangeRequired() == null) {
                buf.append("chgRqd(null) ");
            } else {
                buf.append("chgRqd(" + pvnInfo.getChangeRequired().toString() +
                        ") ");
            }
        }
        return buf.toString();
    }

    /**
     * Utility to generate string output from a challenge response.
     *
     * @param challengeResponse The challenge response values
     *
     * @return the generated String
     */
    private static String genString(String[] challengeResponse) {
        StringBuffer buf = new StringBuffer("ChallengeResponse = ");

        for (int i = 0; i < challengeResponse.length; i++) {
            if (i != 0) {
                buf.append(',');
            }
            String string = challengeResponse[i];
            buf.append(string);
        }

        return buf.toString();
    }

    /**
     * Log message.
     *
     * @param msg Informatin to log
     */
    private static void log(String msg) {
        System.out.println(msg.toString());
    }

    /**
     * Log exception.
     *
     * @param exception The exception to log
     */
    private static void log(Exception exception) {
        System.out.println("Error occurred: " + exception.getMessage());
        if (ms_debug) {
            exception.printStackTrace();
        }
    }

    /**
     * Log exception.
     *
     * @param exception The exception to log
     */

    private static void log(AuthenticationFault exception) {
        System.out.println("Error occurred: " + exception.getMessage());
        if (ms_debug) {
            if (exception.getParams() != null) {
                System.out.println("Params:");
                for (int i = 0; i < exception.getParams().length; i++) {
                    System.out.println("   " + exception.getParams()[i]);
                }
            }
            exception.printStackTrace();
        }
    }
}
