package ru.sosgps.wayrecall.initialization;

import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.*;

public class LetsEncryptRenewer {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LetsEncryptRenewer.class);

    public LetsEncryptRenewer(Properties properties) {
        this.keyStoreDir = Paths.get(System.getenv("WAYRECALL_HOME"), "security").toFile();
        this.acmeServerUri = properties.getProperty("global.security.acme.server");
        this.domains = Optional.ofNullable(properties.getProperty("global.security.acme.securedomains"))
                .map(str -> Arrays.asList(str.split(",\\s*"))).orElse(Collections.emptyList());
    }


    private final String acmeServerUri;
    final File keyStoreDir;
    private final List<String> domains;

    public File getCertChainFile() {
        return new File(keyStoreDir, "cert-chain.crt");
    }

    public File getKeyPairPemFile() {
        return new File(keyStoreDir, "domainKeyPair.pem");
    }

    public File getPrivateKeyFile() {
        return new File(keyStoreDir, "domain.pem.priv");
    }

    public boolean updateCertificate() throws IOException, AcmeException, InterruptedException {
        if (this.acmeServerUri == null || this.acmeServerUri.equalsIgnoreCase("null") || domains.isEmpty()) {
            log.info("no ACME configured");
            return false;
        }
        Session session = new Session(acmeServerUri);
        keyStoreDir.mkdirs();
        log.info("keyStoreDir = " + keyStoreDir.getAbsolutePath());
        KeyPair accountKeyPair = getOrCreateKeyPair(new File(keyStoreDir, "accountkeypair.pem"));

        Login login = new AccountBuilder()
                .addContact("mailto:lkuka@yandex.ru")
                .agreeToTermsOfService()
                .useKeyPair(accountKeyPair)
                .createLogin(session);

        Account account = login.getAccount();

        log.info("accountLocation = " + account.getLocation());

        Order order = account.newOrder()
                .domains(domains)
                .create();

        log.info("running challenges for domains: " + domains);

        List<Authorization> authorizations = order.getAuthorizations();
        if (getCertChainFile().exists() && authorizations.stream().allMatch(it -> it.getStatus() == Status.VALID)) {
            log.info("certificate exists no need to update");
            return false;
        }

        for (Authorization auth : authorizations) {
            if (auth.getStatus() != Status.VALID) {
                processAuth(auth);
            }
        }

        log.info("challanges passed");

        KeyPair domainKeyPair = getOrCreateKeyPair(getKeyPairPemFile());

        try (FileWriter fileWriter = new FileWriter(getPrivateKeyFile())) {
            fileWriter.append("-----BEGIN PRIVATE KEY------\n");
            fileWriter.append(DatatypeConverter.printBase64Binary(domainKeyPair.getPrivate().getEncoded()));
            fileWriter.append('\n');
            fileWriter.append("-----END PRIVATE KEY------\n");
        }

        CSRBuilder csrb = new CSRBuilder();
        for (String domain : domains) {
            csrb.addDomain(domain);
        }
        csrb.setOrganization("KSB Stels");
        csrb.sign(domainKeyPair);
        byte[] csr = csrb.getEncoded();

        csrb.write(new FileWriter(new File(keyStoreDir, "domain.cert")));

        log.info("executing the order");
        order.execute(csr);
        log.info("waiting for the order update");

        while (true) {
            Status status = order.getStatus();
            log.info("order status : " + status);
            if (status == Status.INVALID) throw new RuntimeException(order.getError().asJSON().toString());
            if (status == Status.VALID) break;
            Thread.sleep(3000L);
            order.update();
        }
        Certificate cert = order.getCertificate();

        try (FileWriter fw = new FileWriter(getCertChainFile())) {
            cert.writeCertificate(fw);
        }
        return true;
    }

    private volatile String token = null;
    private volatile String content = null;

    private void processAuth(Authorization auth) throws AcmeException, InterruptedException {
        Http01Challenge challenge = auth.findChallenge(Http01Challenge.class);

        token = challenge.getToken();
        content = challenge.getAuthorization();

        try {
            challenge.trigger();
            while (true) {
                Status status = auth.getStatus();
                log.info("auth status = " + status);
                if (status == Status.VALID) break;
                Thread.sleep(3000L);
                auth.update();
            }
        } finally {
            token = null;
            content = null;
        }

    }

    private static KeyPair getOrCreateKeyPair(File keyPairPemFile) throws IOException {
        KeyPair keyPair;
        if (keyPairPemFile.exists()) {
            try (FileReader fr = new FileReader(keyPairPemFile)) {
                keyPair = KeyPairUtils.readKeyPair(fr);
            }
        } else {
            keyPair = KeyPairUtils.createKeyPair(2048);
            try (FileWriter fw = new FileWriter(keyPairPemFile)) {
                KeyPairUtils.writeKeyPair(keyPair, fw);
            }
        }
        return keyPair;
    }

    public String httpChallengePath() {
        return "/.well-known/acme-challenge/";
    }

    public HttpServlet httpChallengeServlet() {
        return new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                String pathInfo = req.getPathInfo();
                String servletPath = req.getServletPath();
                if (token == null || !servletPath.endsWith(token)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                log.debug("pathInfo = " + pathInfo + ", servletPath = " + servletPath + ", token = " + token);
                resp.setContentType("text/plain");

                try (PrintWriter writer = resp.getWriter()) {
                    writer.append(content);
                }
            }
        };
    }

}
