package auth;
//===========================================================================
//
// Copyright 2004-2011 Entrust. All rights reserved.
//
// This file implements a sample application that manages an Entrust
// Security Manager profile which is used to perform Entrust IdentityGuard
// certificate authentication.
//
//===========================================================================

import com.entrust.identityGuard.authenticationManagement.wsv9.*;
import com.entrust.toolkit.KeyAndCertificateSource;
import com.entrust.toolkit.PKCS7EncodeStream;
import com.entrust.toolkit.User;
import com.entrust.toolkit.credentials.*;
import com.entrust.toolkit.security.provider.Initializer;
import com.entrust.toolkit.util.AuthorizationCode;
import com.entrust.toolkit.util.ManagerTransport;
import com.entrust.toolkit.util.SecureStringBuffer;
import com.entrust.toolkit.x509.directory.JNDIDirectory;
import iaik.asn1.structures.AlgorithmID;
import iaik.pkcs.PKCS7CertList;
import iaik.utils.Base64OutputStream;
import iaik.utils.Util;
import iaik.x509.X509Certificate;

import java.io.*;
import java.net.URL;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

public class IGCertAuth {
    private static final String cmdlist =
            "create, recover, export, authenticate";

    private static void error(String syntax, String msg) {
        System.out.println(msg);
        if (syntax != null) {
            System.out.println("syntax: " + syntax);
        }
        System.exit(1);
    }

    private static void error(String msg) {
        error(null, msg);
    }

    private static void dumpFault(AuthenticationFault e, String ident) {
        if (e == null) return;
        System.out.println(ident + "AuthenticationFault: " +
                e.getErrorCode().toString() + " " +
                e.getInternalCode() + " " +
                e.getErrorMessage());
    }

    /**
     * create a profile with the given refnum/authcode
     */
    private static void create(Parms parms, String[] args) throws Exception {
        String syntax = "create <refnum> <authcode>";

        if (args.length != 3) {
            error(syntax, "create: invalid arguments.");
        }


        if (parms.manager_host == null) {
            error("create: igcertauth.manager.host property not set.");
        }
        if (parms.manager_port == null) {
            error("create: igcertauth.manager.port property not set.");
        }
        User user = new User();
        ManagerTransport transport =
                new ManagerTransport(
                        parms.manager_host,
                        Integer.parseInt(parms.manager_port));

        JNDIDirectory directory = null;
        if (parms.manager_ldap != null) {
            directory = new JNDIDirectory(parms.manager_ldap);
        }
        user.setConnections(directory, transport);

        if (parms.manager_file == null) {
            error("create: igcertauth.manager.file property not set.");
        }
        if (parms.manager_password == null) {
            error("create: igcertauth.manager.password property not set.");
        }
        CredentialWriter writer = null;
        if (parms.manager_format.equals("p12")) {
            writer =
                    new PKCS12Writer(new FileOutputStream(parms.manager_file), 2000);
        } else if (parms.manager_format.equals("epf")) {
            writer = new FilenameProfileWriter(parms.manager_file);
        } else {
            error("create: keystore format not supported.");
        }
        user.setCredentialWriter(writer);
        user.login(
                new CredentialCreator(
                        new SecureStringBuffer(args[1]),
                        new AuthorizationCode(new StringBuffer(args[2]))),
                new SecureStringBuffer(parms.manager_password));
    }

    /**
     * recover a profile with the given refnum/authcode
     */
    private static void recover(Parms parms, String[] args) throws Exception {
        String syntax = "recover <refnum> <authcode>";

        if (args.length != 3) {
            error(syntax, "recover: invalid arguments.");
        }


        if (parms.manager_host == null) {
            error("recover: igcertauth.manager.host property not set.");
        }
        if (parms.manager_port == null) {
            error("recover: igcertauth.manager.port property not set.");
        }
        User user = new User();
        ManagerTransport transport =
                new ManagerTransport(
                        parms.manager_host,
                        Integer.parseInt(parms.manager_port));

        JNDIDirectory directory = null;
        if (parms.manager_ldap != null) {
            directory = new JNDIDirectory(parms.manager_ldap);
        }
        user.setConnections(directory, transport);

        if (parms.manager_file == null) {
            error("recover: igcertauth.manager.file property not set.");
        }
        if (parms.manager_password == null) {
            error("recover: igcertauth.manager.password property not set.");
        }
        CredentialWriter writer = null;
        if (parms.manager_format.equals("p12")) {
            writer =
                    new PKCS12Writer(new FileOutputStream(parms.manager_file), 2000);
        } else if (parms.manager_format.equals("epf")) {
            writer = new FilenameProfileWriter(parms.manager_file);
        } else {
            error("recover: keystore format not supported.");
        }
        user.setCredentialWriter(writer);
        user.login(
                new CredentialRecoverer(
                        new SecureStringBuffer(args[1]),
                        new AuthorizationCode(new StringBuffer(args[2]))),
                new SecureStringBuffer(parms.manager_password));
    }

    /**
     * export the verification certificate from the profile
     */
    private static void export(Parms parms, String[] args) throws Exception {
        String syntax = "export [-cert|-p7] <file>";

        boolean p7 = false;
        boolean cert = false;
        String file = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-cert")) {
                if (cert) {
                    error(syntax, "export: -cert specified twice.");
                } else if (p7) {
                    error(syntax, "export: -cert and -p7 both specified.");
                }
                cert = true;
            } else if (args[i].equalsIgnoreCase("-p7")) {
                if (p7) {
                    error(syntax, "export: -p7 specified twice.");
                } else if (cert) {
                    error(syntax, "export: -cert and -p7 both specified.");
                }
                p7 = true;
            } else if (file == null) {
                file = args[i];
            } else {
                error(syntax, "export: unexpected argument " + args[i] + ".");
            }
        }

        if (file == null) {
            error(syntax, "export: file not specified.");
        }

        if (parms.manager_file == null) {
            error("export: igcertauth.manager.file property not set.");
        }
        if (parms.manager_password == null) {
            error("export: igcertauth.manager.password property not set.");
        }

        PKCS7CertList p7List = null;
        X509Certificate userCert = null;
        if (parms.manager_format.equals("keystore")) {
            if (parms.manager_alias == null) {
                error("export: igcertauth.manager.alias property not set.");
            }
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(
                    new FileInputStream(parms.manager_file),
                    parms.manager_password.toCharArray());

            if (p7) {
                // get the certificate chain for the alias from the KeyStore
                // and convert it into JavaTK X509Certificate objects and
                // then load it into a P7
                Certificate[] chain = ks.getCertificateChain(parms.manager_alias);
                X509Certificate[] xchain = new X509Certificate[chain.length];
                for (int i = 0; i < chain.length; i++) {
                    xchain[i] = new X509Certificate(chain[i].getEncoded());
                }

                // put the cert chain into a P7 structure
                p7List = new PKCS7CertList();
                p7List.setCertificateList(xchain);
            } else {
                // get the certificate from the KeyStore and convert it to the
                // JavaTK format
                userCert =
                        new X509Certificate(
                                ks.getCertificate(parms.manager_alias).getEncoded());
            }
        } else {
            CredentialReader reader = null;
            if (parms.manager_format.equals("p12")) {
                reader = new PKCS12Reader(new FileInputStream(parms.manager_file));
            } else if (parms.manager_format.equals("epf")) {
                reader = new FilenameProfileReader(parms.manager_file);
            }

            User user = new User();

            // if we have LDAP information, login online
            if (parms.manager_ldap != null) {
                user.setConnections(new JNDIDirectory(parms.manager_ldap), null);
            }

            user.login(
                    reader,
                    new SecureStringBuffer(new StringBuffer(parms.manager_password)));

            if (p7) {
                // get the CA certificates
                X509Certificate[] cas = user.getCaCertificateChain();

                // create a new array with space for the user cert
                X509Certificate[] certChain = new X509Certificate[cas.length + 1];

                // copy the CA chain to the end of the new array
                System.arraycopy(cas, 0, certChain, 1, cas.length);

                certChain[0] = user.getVerificationCertificate();

                // put the cert chain into a P7 structure
                p7List = new PKCS7CertList();
                p7List.setCertificateList(certChain);
            } else {
                userCert = user.getVerificationCertificate();
            }
        }

        // output the cert or p7 we extracted above
        PrintWriter pw = new PrintWriter(new FileOutputStream(file));
        if (userCert != null) {
            pw.println(
                    Util.toASCIIString(Util.Base64Encode(userCert.getEncoded())));
        } else {
            // encode and output the P7
            pw.println(
                    Util.toASCIIString(Util.Base64Encode(p7List.toByteArray())));
        }
        pw.close();
    }

    /**
     * generate the IG response for the given certificate challenge
     * using the signing key of the given user.  The hash algorithm and
     * challenge specified in the challenge can be overridden with values
     * provided by the user.  This capability can be used to verify that
     * certificate validation fails if a different hash algorithm or challenge
     * string are used during certificate validation.
     */
    private static String signChallenge(KeyAndCertificateSource keyAndCert,
                                        String overrideHeader,
                                        String overrideHash,
                                        String overrideChallenge,
                                        CertificateChallenge challenge)
            throws Exception {
        // if the hash wasn't overridden by the user, use the value from the
        // challenge
        if (overrideHash == null) {
            overrideHash = challenge.getHashingAlgorithm();
        }
        // if the challenge wasn't overridden by the user, use the value from the
        // challenge
        if (overrideChallenge == null) {
            overrideChallenge = challenge.getChallenge();
        }
        // if the challenge wasn't overridden by the user, use the default
        // value
        if (overrideHeader == null) {
            overrideHeader = "Entrust IdentityGuard";
        }

        System.out.println("Certificate challenge:");
        System.out.println("   Hashing Alg.: " + overrideHash);
        System.out.println("   Header:       " + overrideHeader);
        System.out.println("   Challenge:    " + overrideChallenge);

        // first we hash the challenge using the given algorithm
        MessageDigest digest =
                MessageDigest.getInstance(overrideHash);
        String data = overrideHeader + ":" + overrideChallenge;
        digest.update(data.getBytes("UTF-8"));
        byte[] challengeHash = digest.digest();

        // sign the hash
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Base64OutputStream b64os = new Base64OutputStream(baos);
        PKCS7EncodeStream encoder =
                new PKCS7EncodeStream(keyAndCert, b64os, PKCS7EncodeStream.SIGN_ONLY);
        encoder.setDigestAlgorithm(AlgorithmID.sha1);
        encoder.write(challengeHash);
        encoder.close();

        // return the Base-64 encoded signature
        return new String(baos.toByteArray(), "ISO8859_1");
    }

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

    static private String dateFormat(Calendar c) {
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(c.getTime());
    }

    private static void authenticate(Parms parms, String[] args)
            throws Exception {
        String syntax = "authenticate <userid> [-hash <hash] " +
                "[-challenge <challenge>] [-header <header> " +
                "[-certificate <file>]";

        String userid = null;
        String certificate = null;
        String overrideHeader = null;
        String overrideChallenge = null;
        String overrideHash = null;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-certificate")) {
                if (certificate != null) {
                    error(syntax, "authenticate: -certificate specified twice.");
                }
                i += 1;
                if (i >= args.length) {
                    error(syntax, "authenticate: no argument for -certificate.");
                }
                certificate = readDataFromFile(args[i]);
            } else if (args[i].equalsIgnoreCase("-header")) {
                if (overrideHeader != null) {
                    error(syntax, "authenticate: -header specified twice.");
                }
                i += 1;
                if (i >= args.length) {
                    error(syntax, "authenticate: no argument for -header.");
                }
                overrideHeader = args[i];
            } else if (args[i].equalsIgnoreCase("-hash")) {
                if (overrideHash != null) {
                    error(syntax, "authenticate: -hash specified twice.");
                }
                i += 1;
                if (i >= args.length) {
                    error(syntax, "authenticate: no argument for -hash.");
                }
                overrideHash = args[i];
            } else if (args[i].equalsIgnoreCase("-challenge")) {
                if (overrideChallenge != null) {
                    error(syntax, "authenticate: -challenge specified twice.");
                }
                i += 1;
                if (i >= args.length) {
                    error(syntax, "authenticate: no argument for -challenge.");
                }
                overrideChallenge = args[i];
            } else if (userid == null) {
                userid = args[i];
            } else {
                error(syntax, "authenticate: unexpected argument " + args[i] + ".");
            }
        }

        if (userid == null) {
            error(syntax, "authenticate: <userid> not specified.");
        }

        // connect to the IG auth service
        if (parms.ig_url == null) {
            error("create: igcertauth.manager.password property not set.");
        }
        AuthenticationService_ServiceLocator locator =
                new AuthenticationService_ServiceLocator();
        AuthenticationServiceBindingStub binding =
                (AuthenticationServiceBindingStub) locator.getAuthenticationService(
                        new URL(parms.ig_url));

        // get a certificate challenge for the specified user
        GenericChallengeParms challengeParms = new GenericChallengeParms();
        challengeParms.setAuthenticationType(AuthenticationType.CERTIFICATE);
        GenericChallenge challenge =
                binding.getGenericChallenge(
                        new GetGenericChallengeCallParms(userid, challengeParms));

        // login to the profile used to sign the challenge
        if (parms.manager_file == null) {
            error("create: igcertauth.manager.file property not set.");
        }
        if (parms.manager_password == null) {
            error("create: igcertauth.manager.password property not set.");
        }

        KeyAndCertificateSource keyAndCertificateSource = null;
        if (parms.manager_format.equals("keystore")) {
            if (parms.manager_alias == null) {
                error("authenticate: igcertauth.manager.alias property not set.");
            }

            // open the keystore
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(
                    new FileInputStream(parms.manager_file),
                    parms.manager_password.toCharArray());

            // get the private key from the keystore
            PrivateKey privKey =
                    (PrivateKey) ks.getKey(
                            parms.manager_alias,
                            parms.manager_password.toCharArray());


            // get the certificate from the keystore and convert it to JavaTK
            // format
            X509Certificate cert =
                    new X509Certificate(
                            ks.getCertificate(parms.manager_alias).getEncoded());

            // set the key and certificate source with the privkey and cert
            keyAndCertificateSource = new KeyAndCertificateSource();
            keyAndCertificateSource.setSigningInfo(privKey, cert);
        } else {
            // setup reader for profile type
            CredentialReader reader = null;
            if (parms.manager_format.equals("p12")) {
                reader = new PKCS12Reader(new FileInputStream(parms.manager_file));
            } else if (parms.manager_format.equals("epf")) {
                reader = new FilenameProfileReader(parms.manager_file);
            }

            // login to user
            User user = new User();

            // if we have LDAP information, login online
            if (parms.manager_ldap != null) {
                user.setConnections(new JNDIDirectory(parms.manager_ldap), null);
            }

            user.login(
                    reader,
                    new SecureStringBuffer(new StringBuffer(parms.manager_password)));

            // setup key and cert source using user
            keyAndCertificateSource = new KeyAndCertificateSource(user);
        }

        // generate the response
        String response =
                signChallenge(keyAndCertificateSource,
                        overrideHeader, overrideHash, overrideChallenge,
                        challenge.getCertificateChallenge());

        // authenticate
        GenericAuthenticateParms authenticateParms =
                new GenericAuthenticateParms();
        authenticateParms.setAuthenticationType(AuthenticationType.CERTIFICATE);
        authenticateParms.setCertificate(certificate);
        GenericAuthenticateResponse authResponse =
                binding.authenticateGenericChallenge(
                        new AuthenticateGenericChallengeCallParms(
                                userid,
                                new Response(null, new String[]{response}, null),
                                authenticateParms));

        CertificateData certInfo = authResponse.getCertificateInfo();
        if (certInfo != null) {
            System.out.println("Certificate used for authentication:");
            System.out.println("   Issuer DN:     " + certInfo.getIssuerDN());
            System.out.println("   Subject DN:    " + certInfo.getSubjectDN());
            System.out.println("   Serial Number: " + certInfo.getSerialNumber());
            System.out.println("   Issue Date:    " +
                    dateFormat(certInfo.getIssueDate()));
            System.out.println("   Expiry Date:   " +
                    dateFormat(certInfo.getExpiryDate()));
        }
        CertificateData certReg = authResponse.getCertificateRegistered();
        if (certReg != null) {
            System.out.println("Certificate registered:");
            System.out.println("   Issuer DN:     " + certReg.getIssuerDN());
            System.out.println("   Subject DN:    " + certReg.getSubjectDN());
            System.out.println("   Serial Number: " + certReg.getSerialNumber());
            System.out.println("   Issue Date:    " +
                    dateFormat(certReg.getIssueDate()));
            System.out.println("   Expiry Date:   " +
                    dateFormat(certReg.getExpiryDate()));
        }
    }

    private static class Parms {
        String ig_url = null;
        String manager_file = null;
        String manager_password = null;
        String manager_alias = null;
        String manager_ldap = null;
        String manager_host = null;
        String manager_port = null;
        String manager_format = null;
    }

    public static void main(String args[]) throws Exception {
        if (args.length == 0) {
            error("missing arguments.");
        }

        Properties p = new Properties();
        p.load(ClassLoader.getSystemClassLoader().
                getResourceAsStream("igcertauth.properties"));

        Parms parms = new Parms();
        parms.ig_url = p.getProperty("igcertauth.identityguard.url");
        if (parms.ig_url != null) {
            System.out.println("igcertauth.identityguard.url = " + parms.ig_url);
        }

        parms.manager_file = p.getProperty("igcertauth.manager.file");
        if (parms.manager_file != null) {
            System.out.println("igcertauth.manager.file = " + parms.manager_file);
        }

        parms.manager_password = p.getProperty("igcertauth.manager.password");
        if (parms.manager_password != null) {
            System.out.println("igcertauth.manager.password = " +
                    parms.manager_password);
        }

        parms.manager_alias = p.getProperty("igcertauth.manager.alias");
        if (parms.manager_alias != null) {
            System.out.println("igcertauth.manager.alias = " +
                    parms.manager_alias);
        }

        parms.manager_ldap = p.getProperty("igcertauth.manager.ldap");
        if (parms.manager_ldap != null) {
            System.out.println("igcertauth.manager.ldap = " + parms.manager_ldap);
        }

        parms.manager_host = p.getProperty("igcertauth.manager.host");
        if (parms.manager_host != null) {
            System.out.println("igcertauth.manager.host = " + parms.manager_host);
        }

        parms.manager_port = p.getProperty("igcertauth.manager.port");
        if (parms.manager_port != null) {
            System.out.println("igcertauth.manager.port = " + parms.manager_port);
        }

        parms.manager_format = p.getProperty("igcertauth.manager.format");
        if (parms.manager_format != null) {
            if ((!parms.manager_format.equals("p12")) &&
                    (!parms.manager_format.equals("epf")) &&
                    (!parms.manager_format.equals("keystore"))) {
                error(parms.manager_format +
                        " is an invalid value for parms.manager.format.");
            }
        }
        System.out.println("igcertauth.manager.format = " + parms.manager_format);

        // initialize the Entrust JavaTK
        Initializer.getInstance().setProviders();


        try {
            if (args[0].equalsIgnoreCase("create")) {
                create(parms, args);
            } else if (args[0].equalsIgnoreCase("recover")) {
                recover(parms, args);
            } else if (args[0].equalsIgnoreCase("export")) {
                export(parms, args);
            } else if (args[0].equalsIgnoreCase("authenticate")) {
                authenticate(parms, args);
            } else {
                System.out.println("Allowed commands: " + cmdlist);
                error("unknown command " + args[0]);
            }
        } catch (AuthenticationFault e) {
            dumpFault(e, "");
        }
    }
}
