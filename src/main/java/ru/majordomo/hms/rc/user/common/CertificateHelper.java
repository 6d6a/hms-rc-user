package ru.majordomo.hms.rc.user.common;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import java.io.ByteArrayInputStream;
import java.net.IDN;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CertificateHelper {

    private static CertificateFactory certificateFactory;

    static {
        java.security.Security.addProvider(
                new org.bouncycastle.jce.provider.BouncyCastleProvider()
        );
    }

    public static void validate(SSLCertificate certificate)
            throws ParameterValidationException, InternalApiException {
        List<Certificate> certificates = buildCertificates(certificate.getCert());

        checkPrivateKey(certificate.getKey(), certificates.get(0));

        checkDomainName(certificate.getName(), certificates.get(0));

        if (certificate.getChain() != null && !certificate.getChain().isEmpty()) {
            certificates.addAll(buildCertificates(certificate.getChain()));
        }

        checkChainOfCertificates(certificates);

        checkDates(certificates.get(0));
    }

    public static LocalDateTime getNotAfter(SSLCertificate certificate) throws ParameterValidationException {
        Iterator<Certificate> certsIterator = buildCertificates(certificate.getCert()).iterator();

        if (certsIterator.hasNext()) {
            X509Certificate x509Certificate = (X509Certificate) certsIterator.next();

            Instant instant = Instant.ofEpochMilli(x509Certificate.getNotAfter().getTime());

            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } else {
            throw new ParameterValidationException("Не удалось получить дату окончания X509 сертификата");
        }
    }

    private static void checkDates(Certificate certificate) {
        X509Certificate xCert = (X509Certificate) certificate;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime notBefore = LocalDateTime.ofInstant(xCert.getNotBefore().toInstant(), ZoneId.systemDefault());
        LocalDateTime notAfter = LocalDateTime.ofInstant(xCert.getNotAfter().toInstant(), ZoneId.systemDefault());

        if (notBefore.isAfter(now)) {
            throw new ParameterValidationException("Сертификат не действителен до " + notBefore.toLocalDate());
        } else if (notAfter.isBefore(now)) {
            throw new ParameterValidationException("Срок действия сертификата истёк " + notAfter.toLocalDate());
        }
    }

    private static PublicKey getPublicKey(PrivateKey privateKey) throws Exception {
        RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) privateKey;

        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                crtKey.getModulus(), crtKey.getPublicExponent());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(publicKeySpec);
    }

    static PrivateKey getPrivateKey(String string) throws Exception {
        String pkcs8Pem = string
                .replaceAll("-----(BEGIN|END)\\s(RSA\\s)?PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(pkcs8Pem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

    private static CertificateFactory getCertificateFactory() {
        if (certificateFactory == null) {
            try {
                certificateFactory = CertificateFactory.getInstance("X509");
            } catch (CertificateException e) {
                log.error("Can't create certificate factory, catch e {} message {}", e, e.getMessage());
                throw new InternalApiException();
            }
        }
        return certificateFactory;
    }

    static void checkDomainName(String domain, Certificate certificate) {
        String nameFromCert = "";
        try {
            X509Certificate x509 = (X509Certificate) certificate;
            for (String string : x509.getSubjectDN().getName().split(",")) {
                string = string.trim();
                if (string.startsWith("CN=")) {
                    nameFromCert = string.replaceAll("^CN=(www\\.)?", "");
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Can't get domain name from certificate, catch e {} message {}", e, e.getMessage());
            throw new ParameterValidationException("Не удалось получить имя домена из сертификата");
        }

        String domainInUnicode = IDN.toASCII(domain.toLowerCase());

        if (nameFromCert.startsWith("*.")) {
            String withoutWildcard = nameFromCert.substring(2);
            if (!domainInUnicode.endsWith(withoutWildcard)) {
                throw new ParameterValidationException(
                        "Имя домена сертификата *." + IDN.toUnicode(withoutWildcard) + " не совпадает с " + domain
                );
            }
        } else if (!domainInUnicode.equals(nameFromCert)
                && !domainInUnicode.replaceAll("^www\\.", "").equals(nameFromCert)
        ) {
            throw new ParameterValidationException(
                    "Имя домена сертификата " + IDN.toUnicode(nameFromCert) + " не совпадает с " + domain
            );
        }
    }

    static List<Certificate> buildCertificates(String certificates) {
        try {
            return new ArrayList<>(
                    getCertificateFactory().generateCertificates(
                            new ByteArrayInputStream(certificates.getBytes())
                    )
            );
        } catch (Exception e) {
            log.error("Can't create list of certificate from user's cert, catch e {} message {}", e, e.getMessage());
            throw new ParameterValidationException("Цепочка сертифиткатов невалидна");
        }
    }

    private static void checkPrivateKey(String privateKey, Certificate certificate) {
        PublicKey publicKey;
        try {
            publicKey = getPublicKey(getPrivateKey(privateKey));
        } catch (Exception e) {
            log.error("Can't build public key from private key, catch e {} message {}", e, e.getMessage());
            throw new ParameterValidationException("Приватный ключ невалиден");
        }

        if (!Arrays.equals(publicKey.getEncoded(), certificate.getPublicKey().getEncoded())) {
            throw new ParameterValidationException("Приватный ключ невалиден для сертификата");
        }
    }

    static void checkChainOfCertificates(List<Certificate> certificates) {
        if (certificates.isEmpty()) {
            throw new ParameterValidationException("Сертификат отсутствует");
        } else if (certificates.size() == 1) {
            try {
                certificates.get(0).verify(certificates.get(0).getPublicKey());
            } catch (Exception e) {
                throw new ParameterValidationException("Сертификат не является самоподписанным");
            }
        } else {
            try {
                Iterator<Certificate> iterator = certificates.iterator();
                Certificate previous = null;
                int validatedCount = 0;
                while (iterator.hasNext()) {
                    Certificate current = iterator.next();

                    if (previous != null) {
                        if (!Arrays.equals(previous.getPublicKey().getEncoded(), current.getPublicKey().getEncoded())) {
                            previous.verify(current.getPublicKey());
                            validatedCount++;
                        }
                    }
                    previous = current;
                }
                if (validatedCount < 1) {
                    throw new ParameterValidationException("Ни один сертификат в цепочке не был провалидирован");
                }
            } catch (Exception e) {
                log.error("Can't verify certificates {}, catch e {} message {}", certificates, e, e.getMessage());
                throw new ParameterValidationException("Сертификат или сертификаты удостоверяющего центра невалидны");
            }
        }
    }

    public static List<Certificate> buildSequenceCertificateChain(String data) {
        List<Certificate> certificates = buildCertificates(data);

        if (certificates.isEmpty()) {
            throw new ParameterValidationException("Не удалось создать цепочку сертификатов");
        } else if (certificates.size() == 1) {
            return certificates;
        }

        class Wrap {
            private final Certificate cur;
            private Wrap prev;
            private Wrap next;

            private Wrap(Certificate cur) {
                this.cur = cur;
            }
        }

        LinkedList<Certificate> chain = new LinkedList<>();

        Map<String, Certificate> uniqueCertMap = new HashMap<>();

        for (Certificate c : certificates) {
            uniqueCertMap.put(Arrays.toString(c.getPublicKey().getEncoded()), c);
        }

        Collection<Certificate> uniqueCertList = uniqueCertMap.values();
        List<Wrap> wraps = uniqueCertList.stream().map(Wrap::new).collect(Collectors.toList());

        for (int outer = 0; outer < wraps.size(); outer++) {
            for (int inner = 0; inner < wraps.size(); inner++) {
                if (inner == outer) continue;
                Certificate innerCert = wraps.get(inner).cur;
                Certificate outCert = wraps.get(outer).cur;

                try {
                    innerCert.verify(outCert.getPublicKey());
                    wraps.get(inner).next = wraps.get(outer);
                } catch (Exception e) {
                    try {
                        outCert.verify(innerCert.getPublicKey());
                        wraps.get(inner).prev = wraps.get(outer);
                    } catch (Exception ignore) {}
                }
            }
        }

        long withoutPrev = wraps.stream().filter(w -> w.prev == null).count();
        long withoutNext = wraps.stream().filter(w -> w.next == null).count();

        if (withoutNext != 1 || withoutPrev != 1) {
            throw new ParameterValidationException("Не полная цепочка сертификатов");
        }

        Wrap cur = wraps.stream().filter(w -> w.prev == null).findFirst().get();
        int count = 0;

        while (count < uniqueCertList.size()) {
            chain.add(cur.cur);
            if (cur.next == null) {
                break;
            }
            cur = cur.next;
        }

        if (chain.size() != uniqueCertList.size()) {
            throw new ParameterValidationException("Не полная цепочка сертификатов");
        }

        return chain;
    }

    public static String toPEM(Certificate cert) {
        return toPEM(Collections.singletonList(cert));
    }

    public static String toPEM(Collection<Certificate> certs) {
        try {
            StringBuilder result = new StringBuilder();
            for (Certificate cert : certs) {
                result.append("-----BEGIN CERTIFICATE-----\n");

                String encode = new String(
                        Base64.getEncoder().encode(cert.getEncoded())
                );

                while (encode.length() > 64) {
                    String next = encode.substring(0, 64);
                    result.append(next).append("\n");
                    encode = encode.substring(64);
                }

                if (encode.length() > 0) {
                    result.append(encode).append("\n");
                }

                result.append("-----END CERTIFICATE-----\n");
            }

            return result.toString();
        } catch (Exception e) {
            log.error("Can't convert certificate to PEM format, catch e {} message {}", e, e.getMessage());
            throw new ParameterValidationException("Не удалось записать сертификат");
        }
    }

    public static Map<String, String> getIssuerInfo(Certificate certificate) {
        Map<String, String> issuerInfo = new HashMap<>();
        try {
            String info = ((X509Certificate) certificate).getIssuerDN().getName();
            for(String s: info.split(", ")) {
                String[] split = s.split("=");
                if (split.length == 2) {
                    issuerInfo.put(split[0], split[1]);
                }
            }
        } catch (Exception ignore) {}
        return issuerInfo;
    }
}