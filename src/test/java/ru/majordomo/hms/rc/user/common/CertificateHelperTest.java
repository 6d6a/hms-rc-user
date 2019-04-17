package ru.majordomo.hms.rc.user.common;

import org.junit.Test;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

public class CertificateHelperTest {

    @Test
    public void privateTestCreateTest() throws Exception {
        CertificateHelper.getPrivateKey(getPrivateKeyString());
    }

    @Test(expected = ParameterValidationException.class)
    public void buildInvalidCertTest() {
        CertificateHelper.buildCertificates("dgkjasdgj" + getCertificateString());
    }

    @Test(expected = ParameterValidationException.class)
    public void buildInvalidChainTest() {
        CertificateHelper.buildCertificates("dgkjasdgj" + getChainString());
    }

    @Test
    public void buildValidCertTest() {
        CertificateHelper.buildCertificates(getCertificateString());
        CertificateHelper.buildCertificates(getCertificateString() + getChainString());
        CertificateHelper.buildCertificates(getChainString());
    }

    @Test
    public void checkChain() {
        CertificateHelper.checkChainOfCertificates(
                CertificateHelper.buildCertificates(
                        getCertificateString() + getChainString()
                )
        );
    }

    @Test(expected = ParameterValidationException.class)
    public void checkOnlyCertWithoutChain() {
        CertificateHelper.checkChainOfCertificates(
                CertificateHelper.buildCertificates(
                        getCertificateString()
                )
        );
    }

    @Test
    public void checkValidDomainNameTest() {
        CertificateHelper.checkDomainName(
                getDomainName(),
                CertificateHelper.buildCertificates(getCertificateString()).get(0)
        );
    }

    @Test(expected = ParameterValidationException.class)
    public void checkInvalidDomainNameTest() {
        CertificateHelper.checkDomainName(
                "tadsgasdg" + getDomainName(),
                CertificateHelper.buildCertificates(getCertificateString()).get(0)
        );
    }

    @Test
    public void checkSelfSignedCert() {
        CertificateHelper.checkChainOfCertificates(
                CertificateHelper.buildCertificates(
                        getSelfSignedCert()
                )
        );
    }

    @Test
    public void validateSelfSignedCert() {
        SSLCertificate ssl = new SSLCertificate();
        ssl.setCert(getSelfSignedCert());
        ssl.setKey(getSelfSignedPrivateKey());
        ssl.setName("majordomo.ru");

        CertificateHelper.validate(ssl);
    }

    @Test
    public void subjectAlternativeNamesTest() {
        CertificateHelper.checkDomainName(
                "testmytest.ru",
                CertificateHelper.buildCertificates(getCertWithAlternativeNames()).get(0)
        );
    }

    private String getDomainName() {
        return "funke-store.ru";
    }

    private String getCertificateString() {
        return "-----BEGIN CERTIFICATE-----\n" +
                "MIIFaTCCBFGgAwIBAgISAzMh6hqU2k72jglGd0VtwVs0MA0GCSqGSIb3DQEBCwUA\n" +
                "MEoxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MSMwIQYDVQQD\n" +
                "ExpMZXQncyBFbmNyeXB0IEF1dGhvcml0eSBYMzAeFw0xODEyMDkxNTE5MzNaFw0x\n" +
                "OTAzMDkxNTE5MzNaMBkxFzAVBgNVBAMTDmZ1bmtlLXN0b3JlLnJ1MIIBIjANBgkq\n" +
                "hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqRJ5kYAeDeRHYO5rm7dpZyr4s6bhcyco\n" +
                "AJvgWEuZKRDN+12XRDS4y2Vk8QiYqBrH4of6nfjAMnsu23kQmgyhE9szY/hWrzgU\n" +
                "iuCnhubjqCbmHwJwc+glczfmesoZkXIEgkS0NtMbdJYhBBxLGlz6vtf2vV3USSQz\n" +
                "TXO5sekkfXHiYkNL8uq7s2pJFKyFwqtt56tWCGJduoE09cuSFgAwx5xjibZs7y+v\n" +
                "qZD11KqeG2O8u6J9vKouFqboOohn/CpTnc3Gw3Oi7ZVv6bZxv0vkDR3qgp83c7hG\n" +
                "2Uz776RfDf5tUxEOTZWsHGoUy36BBLP81PxZwE2vOjbKTfmvIaYV5QIDAQABo4IC\n" +
                "eDCCAnQwDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEF\n" +
                "BQcDAjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBRFjKV0jlQAEL3A1f7Fah1f7FcF\n" +
                "CTAfBgNVHSMEGDAWgBSoSmpjBH3duubRObemRWXv86jsoTBvBggrBgEFBQcBAQRj\n" +
                "MGEwLgYIKwYBBQUHMAGGImh0dHA6Ly9vY3NwLmludC14My5sZXRzZW5jcnlwdC5v\n" +
                "cmcwLwYIKwYBBQUHMAKGI2h0dHA6Ly9jZXJ0LmludC14My5sZXRzZW5jcnlwdC5v\n" +
                "cmcvMC0GA1UdEQQmMCSCDmZ1bmtlLXN0b3JlLnJ1ghJ3d3cuZnVua2Utc3RvcmUu\n" +
                "cnUwTAYDVR0gBEUwQzAIBgZngQwBAgEwNwYLKwYBBAGC3xMBAQEwKDAmBggrBgEF\n" +
                "BQcCARYaaHR0cDovL2Nwcy5sZXRzZW5jcnlwdC5vcmcwggEFBgorBgEEAdZ5AgQC\n" +
                "BIH2BIHzAPEAdwDiaUuuJujpQAnohhu2O4PUPuf+dIj7pI8okwGd3fHb/gAAAWeT\n" +
                "xEnGAAAEAwBIMEYCIQDGVeJ4IBDO62pbzwmDG58l2n8adQDL9wnM/Ng21EZuNAIh\n" +
                "APtjb4soFl4478Gzx+4ofVblM0hcl5AH+9THXI6FjGoSAHYAKTxRllTIOWW6qlD8\n" +
                "WAfUt2+/WHopctykwwz05UVH9HgAAAFnk8RH0gAABAMARzBFAiAcLnqkFYoCDsop\n" +
                "l6x6G8pru8tM2W2w+MuCMzC/lFcUPwIhALI1A4R+SBfBDJdYi/Vi4m/QFepFs9jb\n" +
                "QRTYbi8JZz1RMA0GCSqGSIb3DQEBCwUAA4IBAQA3QyqAx80rKm6ndqgktGU6pFcs\n" +
                "1HKf+5wKKZ5Qr7CwlCqTSWUpAxC5jFL3ea0JJkNk3MjQL8T8GKYmvDJwIh+BZE8m\n" +
                "++7l0cRwJzLECw1u+5MEc4OCjXouDl0t1N5CavsEYG8DhxxldXvH0d/i+/4wKyAc\n" +
                "TswOy+cjnK2mm2SDql4OLjumuO3y+4Y4oNZloU4X9u4+sJBj+IbvWDvMWWOjCYS1\n" +
                "wjTuvq+EjlwRj5nyVgM0SjaYUmsYTH479zdb5e595J/K/QIIZruzXgpKG/m5e+Np\n" +
                "IN9I8KGrBKWVtVWgl1ucQSngx9EdDuGgNyAzUQB0DLKyVmM1bZ+Kv7uCzIrB\n" +
                "-----END CERTIFICATE-----\n";
    }

    private String getChainString() {
        return "-----BEGIN CERTIFICATE-----\n" +
                "MIIFaTCCBFGgAwIBAgISAzMh6hqU2k72jglGd0VtwVs0MA0GCSqGSIb3DQEBCwUA\n" +
                "MEoxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MSMwIQYDVQQD\n" +
                "ExpMZXQncyBFbmNyeXB0IEF1dGhvcml0eSBYMzAeFw0xODEyMDkxNTE5MzNaFw0x\n" +
                "OTAzMDkxNTE5MzNaMBkxFzAVBgNVBAMTDmZ1bmtlLXN0b3JlLnJ1MIIBIjANBgkq\n" +
                "hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqRJ5kYAeDeRHYO5rm7dpZyr4s6bhcyco\n" +
                "AJvgWEuZKRDN+12XRDS4y2Vk8QiYqBrH4of6nfjAMnsu23kQmgyhE9szY/hWrzgU\n" +
                "iuCnhubjqCbmHwJwc+glczfmesoZkXIEgkS0NtMbdJYhBBxLGlz6vtf2vV3USSQz\n" +
                "TXO5sekkfXHiYkNL8uq7s2pJFKyFwqtt56tWCGJduoE09cuSFgAwx5xjibZs7y+v\n" +
                "qZD11KqeG2O8u6J9vKouFqboOohn/CpTnc3Gw3Oi7ZVv6bZxv0vkDR3qgp83c7hG\n" +
                "2Uz776RfDf5tUxEOTZWsHGoUy36BBLP81PxZwE2vOjbKTfmvIaYV5QIDAQABo4IC\n" +
                "eDCCAnQwDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEF\n" +
                "BQcDAjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBRFjKV0jlQAEL3A1f7Fah1f7FcF\n" +
                "CTAfBgNVHSMEGDAWgBSoSmpjBH3duubRObemRWXv86jsoTBvBggrBgEFBQcBAQRj\n" +
                "MGEwLgYIKwYBBQUHMAGGImh0dHA6Ly9vY3NwLmludC14My5sZXRzZW5jcnlwdC5v\n" +
                "cmcwLwYIKwYBBQUHMAKGI2h0dHA6Ly9jZXJ0LmludC14My5sZXRzZW5jcnlwdC5v\n" +
                "cmcvMC0GA1UdEQQmMCSCDmZ1bmtlLXN0b3JlLnJ1ghJ3d3cuZnVua2Utc3RvcmUu\n" +
                "cnUwTAYDVR0gBEUwQzAIBgZngQwBAgEwNwYLKwYBBAGC3xMBAQEwKDAmBggrBgEF\n" +
                "BQcCARYaaHR0cDovL2Nwcy5sZXRzZW5jcnlwdC5vcmcwggEFBgorBgEEAdZ5AgQC\n" +
                "BIH2BIHzAPEAdwDiaUuuJujpQAnohhu2O4PUPuf+dIj7pI8okwGd3fHb/gAAAWeT\n" +
                "xEnGAAAEAwBIMEYCIQDGVeJ4IBDO62pbzwmDG58l2n8adQDL9wnM/Ng21EZuNAIh\n" +
                "APtjb4soFl4478Gzx+4ofVblM0hcl5AH+9THXI6FjGoSAHYAKTxRllTIOWW6qlD8\n" +
                "WAfUt2+/WHopctykwwz05UVH9HgAAAFnk8RH0gAABAMARzBFAiAcLnqkFYoCDsop\n" +
                "l6x6G8pru8tM2W2w+MuCMzC/lFcUPwIhALI1A4R+SBfBDJdYi/Vi4m/QFepFs9jb\n" +
                "QRTYbi8JZz1RMA0GCSqGSIb3DQEBCwUAA4IBAQA3QyqAx80rKm6ndqgktGU6pFcs\n" +
                "1HKf+5wKKZ5Qr7CwlCqTSWUpAxC5jFL3ea0JJkNk3MjQL8T8GKYmvDJwIh+BZE8m\n" +
                "++7l0cRwJzLECw1u+5MEc4OCjXouDl0t1N5CavsEYG8DhxxldXvH0d/i+/4wKyAc\n" +
                "TswOy+cjnK2mm2SDql4OLjumuO3y+4Y4oNZloU4X9u4+sJBj+IbvWDvMWWOjCYS1\n" +
                "wjTuvq+EjlwRj5nyVgM0SjaYUmsYTH479zdb5e595J/K/QIIZruzXgpKG/m5e+Np\n" +
                "IN9I8KGrBKWVtVWgl1ucQSngx9EdDuGgNyAzUQB0DLKyVmM1bZ+Kv7uCzIrB\n" +
                "-----END CERTIFICATE-----\n" +
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIEkjCCA3qgAwIBAgIQCgFBQgAAAVOFc2oLheynCDANBgkqhkiG9w0BAQsFADA/\n" +
                "MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT\n" +
                "DkRTVCBSb290IENBIFgzMB4XDTE2MDMxNzE2NDA0NloXDTIxMDMxNzE2NDA0Nlow\n" +
                "SjELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUxldCdzIEVuY3J5cHQxIzAhBgNVBAMT\n" +
                "GkxldCdzIEVuY3J5cHQgQXV0aG9yaXR5IFgzMIIBIjANBgkqhkiG9w0BAQEFAAOC\n" +
                "AQ8AMIIBCgKCAQEAnNMM8FrlLke3cl03g7NoYzDq1zUmGSXhvb418XCSL7e4S0EF\n" +
                "q6meNQhY7LEqxGiHC6PjdeTm86dicbp5gWAf15Gan/PQeGdxyGkOlZHP/uaZ6WA8\n" +
                "SMx+yk13EiSdRxta67nsHjcAHJyse6cF6s5K671B5TaYucv9bTyWaN8jKkKQDIZ0\n" +
                "Z8h/pZq4UmEUEz9l6YKHy9v6Dlb2honzhT+Xhq+w3Brvaw2VFn3EK6BlspkENnWA\n" +
                "a6xK8xuQSXgvopZPKiAlKQTGdMDQMc2PMTiVFrqoM7hD8bEfwzB/onkxEz0tNvjj\n" +
                "/PIzark5McWvxI0NHWQWM6r6hCm21AvA2H3DkwIDAQABo4IBfTCCAXkwEgYDVR0T\n" +
                "AQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAYYwfwYIKwYBBQUHAQEEczBxMDIG\n" +
                "CCsGAQUFBzABhiZodHRwOi8vaXNyZy50cnVzdGlkLm9jc3AuaWRlbnRydXN0LmNv\n" +
                "bTA7BggrBgEFBQcwAoYvaHR0cDovL2FwcHMuaWRlbnRydXN0LmNvbS9yb290cy9k\n" +
                "c3Ryb290Y2F4My5wN2MwHwYDVR0jBBgwFoAUxKexpHsscfrb4UuQdf/EFWCFiRAw\n" +
                "VAYDVR0gBE0wSzAIBgZngQwBAgEwPwYLKwYBBAGC3xMBAQEwMDAuBggrBgEFBQcC\n" +
                "ARYiaHR0cDovL2Nwcy5yb290LXgxLmxldHNlbmNyeXB0Lm9yZzA8BgNVHR8ENTAz\n" +
                "MDGgL6AthitodHRwOi8vY3JsLmlkZW50cnVzdC5jb20vRFNUUk9PVENBWDNDUkwu\n" +
                "Y3JsMB0GA1UdDgQWBBSoSmpjBH3duubRObemRWXv86jsoTANBgkqhkiG9w0BAQsF\n" +
                "AAOCAQEA3TPXEfNjWDjdGBX7CVW+dla5cEilaUcne8IkCJLxWh9KEik3JHRRHGJo\n" +
                "uM2VcGfl96S8TihRzZvoroed6ti6WqEBmtzw3Wodatg+VyOeph4EYpr/1wXKtx8/\n" +
                "wApIvJSwtmVi4MFU5aMqrSDE6ea73Mj2tcMyo5jMd6jmeWUHK8so/joWUoHOUgwu\n" +
                "X4Po1QYz+3dszkDqMp4fklxBwXRsW10KXzPMTZ+sOPAveyxindmjkW8lGy+QsRlG\n" +
                "PfZ+G6Z6h7mjem0Y+iWlkYcV4PIWL1iwBi8saCbGS5jN2p8M+X+Q7UNKEkROb3N6\n" +
                "KOqkqm57TH2H3eDJAkSnh6/DNFu0Qg==\n" +
                "-----END CERTIFICATE-----\n";
    }

    private String getPrivateKeyString() {
        return "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIIEpAIBAAKCAQEAqRJ5kYAeDeRHYO5rm7dpZyr4s6bhcycoAJvgWEuZKRDN+12X\n" +
                "RDS4y2Vk8QiYqBrH4of6nfjAMnsu23kQmgyhE9szY/hWrzgUiuCnhubjqCbmHwJw\n" +
                "c+glczfmesoZkXIEgkS0NtMbdJYhBBxLGlz6vtf2vV3USSQzTXO5sekkfXHiYkNL\n" +
                "8uq7s2pJFKyFwqtt56tWCGJduoE09cuSFgAwx5xjibZs7y+vqZD11KqeG2O8u6J9\n" +
                "vKouFqboOohn/CpTnc3Gw3Oi7ZVv6bZxv0vkDR3qgp83c7hG2Uz776RfDf5tUxEO\n" +
                "TZWsHGoUy36BBLP81PxZwE2vOjbKTfmvIaYV5QIDAQABAoIBAQCoZsN5vm+xDIhg\n" +
                "Lvo13qj43p9bacQzS3QXryr1J0+FzLwuPBlYEV9jEDpLrnFHN22S4DgrMsJVRcb1\n" +
                "Sg/UMPIb8gCs+YV1/1jOv1d+Een96cjaaDaT7E2pBqvl7/kpmrSNAFu0I++733FB\n" +
                "Q+E2gBgtELUuBxBUTd9frP3wDDWRT8eZHpJv5BwiazyQPfdHyu/Wf+fMzg/++39h\n" +
                "RwfVcTiiGSzM3kJbOciGVAlnYfJNa8UIIagIeaRwK0hBAi7I8fXGrUG3HQrhIlCq\n" +
                "reXp2buwomxZipiOrz9FYKZCdj0RvNg2OleIZ+RSXte74at8knlBwqPB9cw3TXsg\n" +
                "SydUhGz9AoGBAN14k6CzhsJzSjnzuQ8bNYgcinB5kLjg3YztSNJkadpZxlNcD05c\n" +
                "e84/Pzcm/Db818jlIZRxPe/ImUZ03bXo33FBR88+A8wFh+XM+84iF3D0Z+EAuevX\n" +
                "F63Sb04Qan6HhA7+f/Zzeo1WSiexf8AKXJqTX0eWYlaR1xUdxTFInPjnAoGBAMNu\n" +
                "ieh84qRguqySwxQuEt75y125ryYmaNUoY5cPAMsOgtANu8Q80tsAuFjAI7oiKorV\n" +
                "XhLiXkhEUhCVfIOPGmaWqcFWP1bYUGv6PzAfnL0RLk4zbfbIhjReZ1J9qawZdikf\n" +
                "qgROlYnmPkGu1/FYCDZT77ZBVXrb4B2sI5kh+CVTAoGAcon3n7b5TPufOdWIsf1a\n" +
                "dCh+mKjmZhc1vbrCXL092YVwgpAsAlTmWsZvBTEOY8nkN6v47/KZG7fdPYkXWQs0\n" +
                "9DfU0aO7BvsdS4X1/Ke5A9wDFWTwwr4dKvKYSgFHcmTXsYGB4I3cpdgoxdqQvVse\n" +
                "hXsOXdGyzh7i47Lf1xs8w6UCgYEAhqiSw6vRapvv5FsXqb75Z8RMmV8JTUsn7r/u\n" +
                "pzZCQKfWAM+FsniKfTZoz1ZNB8Lk91Z/hJmPh1H0DdNCwU+ITGnBvCLWb9uKxmve\n" +
                "wCT4FdpRwrZzLZkLx+fIX7GQiLHelgVoW6FeLm+ENDPqPTSALBb30+f+ozH/odJr\n" +
                "kOc0GVsCgYAuESZpppqWfMp39CznJGjJma2UlvcZeRGBgkDIPcnv9Y6+gg11Sw9n\n" +
                "JC534ERUsI2qC6mDpcU1qZ9kSyVJKsbwHodBbtXSqnv/fuU1dRLHvhTOh7vFOW2h\n" +
                "Bui1tohNoLV8b3/h01rDxvwxwY2boadmkox0vlfbp/TyzZMXEq/rDw==\n" +
                "-----END RSA PRIVATE KEY-----\n";
    }

    private String getSelfSignedCert() {
        return "-----BEGIN CERTIFICATE-----\n" +
                "MIIDwjCCAqoCCQCXEaEcHuaMhDANBgkqhkiG9w0BAQsFADCBojELMAkGA1UEBhMC\n" +
                "UlUxFzAVBgNVBAgMDkxlbmluZ3JhZHNrYXlhMRkwFwYDVQQHDBBTYWludC1QZXRl\n" +
                "cnNidXJnMRYwFAYDVQQKDA1NYWpvcmRvbW8gTFREMQswCQYDVQQLDAJNajEVMBMG\n" +
                "A1UEAwwMbWFqb3Jkb21vLnJ1MSMwIQYJKoZIhvcNAQkBFhRzdXBwb3J0QG1ham9y\n" +
                "ZG9tby5ydTAeFw0xOTAyMTkxMzI1MDVaFw00NjA3MDcxMzI1MDVaMIGiMQswCQYD\n" +
                "VQQGEwJSVTEXMBUGA1UECAwOTGVuaW5ncmFkc2theWExGTAXBgNVBAcMEFNhaW50\n" +
                "LVBldGVyc2J1cmcxFjAUBgNVBAoMDU1ham9yZG9tbyBMVEQxCzAJBgNVBAsMAk1q\n" +
                "MRUwEwYDVQQDDAxtYWpvcmRvbW8ucnUxIzAhBgkqhkiG9w0BCQEWFHN1cHBvcnRA\n" +
                "bWFqb3Jkb21vLnJ1MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu7NZ\n" +
                "LF+Z4LCmie49jUY4f/Z5WbwFUjdtSsoaDLVXoPSDAjOrNfMv5fv1INE9IMLg2xn+\n" +
                "QlIdjcY/IxhpwIV/7duCwiPpxqbrnPbeApITMP7mw4jxDMRG+oDYPxvpq50VH/WK\n" +
                "uAoEIFuQ+7jpK8hAAmUV5JaPq1A9ucotn2Qmc3pejzxcvYhisjEKKz0N0+fUYNc8\n" +
                "FLsJZsJTI4cY9mD2YDc4+zcsQ7hdR0S9n3qxCLJvOPPlBcAz74k6PMkMZWt4uZ04\n" +
                "gbMyjv+PQyWVDadhAbp5iTVzsTTYohcJjfqVwRS1nm7fyEF7JKzFF/vq+aGGLOTW\n" +
                "pCJekPRSCIMPyBCFuwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQCn/nq9eHh74n4K\n" +
                "iSA4OW42AnKavs5F5PWnsgDZUgjBSO1hiRcpIWlWMNiK9aOGMHMP3H+BOqBeD+ih\n" +
                "wUnQkI46lBxe1o3QnX1x8RQYDkg4IKcyWQ+H9h1GdMD4S+EpRmvXsIVLSu5FemVc\n" +
                "9Q6ITNo9nQlVjv+E5zDnAWluh8kAJiN8xifxCyt60Dq5SSDsYL+2X13hx3xevezB\n" +
                "Me2jUvQE4ENfX/g5lkPewRywKs+LEr3W8NrQ5O6zdNyWa8lnSut7vnfGd0K2XHCl\n" +
                "1GUY3T8H6cfhEaiTgUpLnPTGeFlkd2GhutlZKDH5dhDhPzkMd69ZmhIPwnYrF9Nr\n" +
                "8fEtY4DE\n" +
                "-----END CERTIFICATE-----";
    }

    private String getSelfSignedPrivateKey() {
        return "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIIEpAIBAAKCAQEAu7NZLF+Z4LCmie49jUY4f/Z5WbwFUjdtSsoaDLVXoPSDAjOr\n" +
                "NfMv5fv1INE9IMLg2xn+QlIdjcY/IxhpwIV/7duCwiPpxqbrnPbeApITMP7mw4jx\n" +
                "DMRG+oDYPxvpq50VH/WKuAoEIFuQ+7jpK8hAAmUV5JaPq1A9ucotn2Qmc3pejzxc\n" +
                "vYhisjEKKz0N0+fUYNc8FLsJZsJTI4cY9mD2YDc4+zcsQ7hdR0S9n3qxCLJvOPPl\n" +
                "BcAz74k6PMkMZWt4uZ04gbMyjv+PQyWVDadhAbp5iTVzsTTYohcJjfqVwRS1nm7f\n" +
                "yEF7JKzFF/vq+aGGLOTWpCJekPRSCIMPyBCFuwIDAQABAoIBAQCNajJlHCHzByRy\n" +
                "BuyY467tueTzlpTrvJa7Ikk+CeJbgdyya/sySmZr/8hPUVWcuTZv3MWTcgqSJiqr\n" +
                "ZllGk04vBpMdcbFP2bLqL5RUm3maEHIMH8B8veQ8F5bU9VK32UFxMq2wutshBdvV\n" +
                "yxrhwZ14MHCuKEFV8L6Qm6KD1BCkLuISKP5zZyVIrTYXK+eHCenct3/7U5Xm9aip\n" +
                "6Em8n3TbyZGtpWj03d5tWxGjwn1hSwJxlYY5baHQf6U6weVEUge0aGSlwDE1J5xm\n" +
                "pHUQmIcAYd6mspwj6akOexfosaszNK/c0yd7w6h6cyiZS0SS6Vh1AgxgsJxg7Xp1\n" +
                "eg8A5O8BAoGBAPCVKe390+kgIg1QqgryrHcYIWNjA/4TaRow6BL+6F1o5l+N9106\n" +
                "FDWS/CvVGmDfGBIAVsqzpAc6HbLr2nzleJBRjhDUyvM/JLpcdzP7rI3MDPQwr7Lq\n" +
                "d0MMN7FW4cBKmbThws6cPMn2yPUCKJizKrkPnw4l+BsMOsMwawUHbcTnAoGBAMe6\n" +
                "owU/l89uID+xawiWpcqVr48ZJEo75SuC4B/SwdbgGLOlCpmk5evrEVOUhE9QN3ZK\n" +
                "ktvcYGN4BorUS81aKpdoo65SS4I6p85bT9UZNZnXZQXhzpKzy/OIFJGZw55a8J4Y\n" +
                "Z7CI3teuKVBFAcmTCuaB18b8RdHx18cSjdC1v4oNAoGAN5Ic5040PqsfcnfpbzHy\n" +
                "yGeYfr6GU4/kPRqC4LBkt/7dB8FhG/WoaV+URAOrXijqBG5zncq03r570bJM+4B9\n" +
                "wsgzyot8fYdeUnW9SxKdHvu9GkKMswUZmzEdRCdPaf1RV8i2GMdZ+0S30nA3NF5e\n" +
                "RfTnxnravlSq1h8++SBoKI0CgYB8pVl/uQRBC/VMSqkFnx6wo2IA6CvhuCd5VCAT\n" +
                "PDCErS49Ts6ivGpRJU6W3qUD6ofyZu/oDkyJRyquEXeNHKxf+YnNDMba60g2XH4h\n" +
                "+62b6PS/CwEkRNkPjulnRJL0jMSFpa1wWrrX13UpByfqaL3wxS+om1/negqzzjx6\n" +
                "+DIlMQKBgQCWded+o3/MNwYXJVlzHGwDL3YwYp1wGW1phh6ehS57yFCLqMLsqt2T\n" +
                "XLMQFO4YMbKCPxuiCdrF/l3aI4a7dC68Fc0QInF8hkERvsrZwC/1ToFmSuINThyc\n" +
                "0O6HgrESvLLJWMVpE4ssEoMfB97Y4mtPahQGgFq2r0mqdyNk25N0ww==\n" +
                "-----END RSA PRIVATE KEY-----";
    }

    private String getCertWithAlternativeNames() {
        return "-----BEGIN CERTIFICATE-----\n" +
                "MIIElTCCA32gAwIBAgIUIYhSFrD95T9sN16Trv/vada1fcUwDQYJKoZIhvcNAQEL\n" +
                "BQAwgYsxCzAJBgNVBAYTAlVTMRkwFwYDVQQKExBDbG91ZEZsYXJlLCBJbmMuMTQw\n" +
                "MgYDVQQLEytDbG91ZEZsYXJlIE9yaWdpbiBTU0wgQ2VydGlmaWNhdGUgQXV0aG9y\n" +
                "aXR5MRYwFAYDVQQHEw1TYW4gRnJhbmNpc2NvMRMwEQYDVQQIEwpDYWxpZm9ybmlh\n" +
                "MB4XDTE5MDQxMjEzNDQwMFoXDTM0MDQwODEzNDQwMFowYjEZMBcGA1UEChMQQ2xv\n" +
                "dWRGbGFyZSwgSW5jLjEdMBsGA1UECxMUQ2xvdWRGbGFyZSBPcmlnaW4gQ0ExJjAk\n" +
                "BgNVBAMTHUNsb3VkRmxhcmUgT3JpZ2luIENlcnRpZmljYXRlMIIBIjANBgkqhkiG\n" +
                "9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5fjRYWQzVlNUjJBpskJLpXoyqT3DI/T63nNd\n" +
                "zZl77DQfoz9EIvIOy35FQ3EHuzNiuq+oLzkaUPGK3f0jaHPvEDvHvWl6/cqpbp3j\n" +
                "zqV+9+myQytdUaNxQ2DjoOqtDGJC21U0ty2aC+wHolZaM3UCAzttbwZ+oidFdyec\n" +
                "kBZDgEwZuWRqGtHSvxNfDbGaQZI+GGmN2DRJSdj1uaqOEWkQqKX1B1is8Fx14Yqm\n" +
                "pT2RPZ65xEFNyA3kRxnPJCEsiToo2mHx9+099z8z4I00Vt7f1b7NG+DmcwaWwcjs\n" +
                "YAje9Kx2kSnC7rAU97WjU4+aKJMg8LnGA8xXo8Uku7Rvdjx3SQIDAQABo4IBFzCC\n" +
                "ARMwDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcD\n" +
                "ATAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBS4whPJfaqBpfR3FWrRA9PqyDhlsDAf\n" +
                "BgNVHSMEGDAWgBQk6FNXXXw0QIep65TbuuEWePwppDBABggrBgEFBQcBAQQ0MDIw\n" +
                "MAYIKwYBBQUHMAGGJGh0dHA6Ly9vY3NwLmNsb3VkZmxhcmUuY29tL29yaWdpbl9j\n" +
                "YTAYBgNVHREEETAPgg10ZXN0bXl0ZXN0LnJ1MDgGA1UdHwQxMC8wLaAroCmGJ2h0\n" +
                "dHA6Ly9jcmwuY2xvdWRmbGFyZS5jb20vb3JpZ2luX2NhLmNybDANBgkqhkiG9w0B\n" +
                "AQsFAAOCAQEAbeImyC7DVgmd8DN96b9Oh6hNw6nchLfBMTh1k9nLkHf6QB21M2+U\n" +
                "Dw3XNWyXrToHK49F/zgdXp5vuQEviukgcWm/1OGifRtJ7dz87seMq2vwcJgGhfVJ\n" +
                "pwBKnByoRKsjylg6kX2D+9yFuW1cyopIXuwEeS7f7kxWNhScmUp0upJXBE++BTFB\n" +
                "TbimgKoUo9Y6Kt82HPkKVJNZnSIKgHH7os/hu4rQkcEHmLl5znlZ0FwkMca61MdD\n" +
                "BAwTUS0HnYoqsIp/sDyGwlqZIxxd0otySNZFJOs7Vh0nsKvDTRsKBqwAYWZc/ijN\n" +
                "GnuBTGyz7ou/5aygWbIhNGwaSnOiw5tjWg==\n" +
                "-----END CERTIFICATE-----\n";
    }
}
