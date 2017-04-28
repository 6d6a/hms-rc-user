package ru.majordomo.hms.rc.user.importing;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.majordomo.hms.rc.user.common.PhoneNumberManager;
import ru.majordomo.hms.rc.user.event.person.PersonCreateEvent;
import ru.majordomo.hms.rc.user.event.person.PersonImportEvent;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.LegalEntity;
import ru.majordomo.hms.rc.user.resources.Passport;
import ru.majordomo.hms.rc.user.resources.Person;

@Service
public class PersonDBImportService implements ResourceDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PersonDBImportService.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private NamedParameterJdbcTemplate registrantNamedParameterJdbcTemplate;
    private final PersonRepository personRepository;
    private final ApplicationEventPublisher publisher;
    private final EmailValidator emailValidator = EmailValidator.getInstance(true, true); //allowLocal, allowTLD

    @Autowired
    public PersonDBImportService(
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            @Qualifier("registrantNamedParameterJdbcTemplate") NamedParameterJdbcTemplate registrantNamedParameterJdbcTemplate,
            PersonRepository personRepository,
            ApplicationEventPublisher publisher
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.registrantNamedParameterJdbcTemplate = registrantNamedParameterJdbcTemplate;
        this.personRepository = personRepository;
        this.publisher = publisher;
    }

    public void pull() {
        String query = "SELECT a.id " +
                "FROM client c " +
                "JOIN account a USING(client_id) " +
                "GROUP BY a.id " +
                "ORDER BY a.id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new PersonImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        String query = "SELECT a.id, " +
                "c.Client_ID, c.name as client_name, c.phone, c.phone2, c.email, c.email2, c.email3, " +
                "c.passport, '' as passport_number, '' as passport_org, '' as passport_date, " +
                "c.HB as birthdate, c.address as legal_address, c.address_post, c.inn, c.kpp, c.ogrn, c.okpo, c.okved, " +
                "c.bank_rekv, c.bank_name, c.bill as bank_account, c.coor_bill as bank_account_loro, c.bik as bank_bik, c.face, c.company_name " +
                "FROM client c " +
                "JOIN account a USING(client_id) " +
                "WHERE a.id = :accountId " +
                "GROUP BY a.id ";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );

        pullRegistrantPersons(accountId);
    }

    private Person rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found Person for id: " + rs.getString("id") +
                " name: " + rs.getString("client_name"));

        Person person = new Person();
        person.setId("person_" + rs.getString("Client_ID"));
        person.setAccountId(rs.getString("id"));
        person.setSwitchedOn(true);
        person.setName(!rs.getString("client_name").equals("") ?
                rs.getString("client_name") :
                "person_" + rs.getString("id") + "_" + rs.getString("Client_ID")
        );

        String phone = rs.getString("phone");
        if (phone != null && !phone.equals("") && PhoneNumberManager.phoneValid(phone)) {
            person.addPhoneNumber(phone);
        }

        phone = rs.getString("phone2");
        if (phone != null && !phone.equals("") && PhoneNumberManager.phoneValid(phone)) {
            person.addPhoneNumber(phone);
        }

        String email = rs.getString("email");
        if (email != null && !email.equals("") && emailValidator.isValid(email)) {
            person.addEmailAddress(email);
        }

        email = rs.getString("email2");
        if (email != null && !email.equals("") && emailValidator.isValid(email)) {
            person.addEmailAddress(email);
        }

        email = rs.getString("email3");
        if (email != null && !email.equals("") && emailValidator.isValid(email)) {
            person.addEmailAddress(email);
        }

        String postalAddress = rs.getString("address_post");
        if (postalAddress != null && !postalAddress.equals("")) {
            person.setPostalAddress(postalAddress);
        }

        person.setCountry("RU");

        String personType = rs.getString("face");

        if (personType.equals("ph")) {
            person.setPassport(getPassportFromResultSet(rs));
        } else if (personType.equals("ju")) {
            person.setLegalEntity(getLegalEntityFromString(rs));
        }

        publisher.publishEvent(new PersonCreateEvent(person));

        return null;
    }

    private void pullRegistrantPersons(String accountId) {
        String query = "SELECT a.id, " +
                "cpr.nic_in_registrant, cpr.deleted " +
                "FROM account a " +
                "JOIN client_persons_rpc cpr USING(client_id) " +
                "WHERE a.id = :accountId AND cpr.deleted = 0";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMapRegistrantPerson
        );
    }

    private Person rowMapRegistrantPerson(ResultSet rs, int rowNum) throws SQLException {
        String nicHandle = rs.getString("nic_in_registrant");

        logger.debug("Found Registrant Person for id: " + rs.getString("id") +
                " nic_in_registrant: " + nicHandle);

        Person person = new Person();
        person.setId("person_" + rs.getString("id") + "_" + nicHandle);
        person.setAccountId(rs.getString("id"));
        person.setSwitchedOn(true);
        person.setNicHandle(rs.getString("nic_in_registrant"));

        String query = "SELECT c.client_id, c.nic_handle, c.type " +
                "FROM clients c  " +
                "WHERE c.parent_client_id = 2  AND c.nic_handle = :nic_handle AND c.deleted IS NULL";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("nic_handle", nicHandle);

        registrantNamedParameterJdbcTemplate.query(query,
                namedParameters1,
                ((rs1, rowNum1) -> {
                    String clientType = rs1.getString("type");
                    String contactsTableName = "";
                    String contactsColumns = "";
                    switch (clientType) {
                        case "individual":
                            contactsTableName = "contacts_individuals";
                            contactsColumns = "'RU' as country_id, cc.email, " +
                                    "CONCAT_WS(' ', cc.lastname, cc.firstname, cc.middlename) AS client_name, " +
                                    "cc.birthdate, '' as passport, cc.passport_number, cc.passport_org, cc.passport_date, " +
                                    "CONCAT_WS(', ', cc.postal_address_zip, cc.postal_address_city, cc.postal_address_street) AS postal_address, " +
                                    "cc.phone_1, cc.phone_2, cc.email_additional_1, cc.email_additional_2";
                            break;
                        case "individual_foreign":
                            contactsTableName = "contacts_individuals_foreigns";
                            contactsColumns = "cc.country_id, cc.email, " +
                                    "CONCAT_WS(' ', cc.lastname, cc.firstname, cc.middlename) AS client_name, " +
                                    "cc.birthdate, cc.document as passport, '' as passport_number, '' as passport_org, '' as passport_date, " +
                                    "'' as legal_address, " +
                                    "CONCAT_WS(', ', cc.postal_address_zip, cc.postal_address_city, cc.postal_address_street) AS postal_address, " +
                                    "cc.phone_1, cc.phone_2, cc.email_additional_1, cc.email_additional_2";
                            break;
                        case "company":
                            contactsTableName = "contacts_companies";
                            contactsColumns = "'RU' as country_id, cc.email, " +
                                    "CONCAT_WS(' ', cc.form, cc.name) AS client_name, " +
                                    "cc.inn, cc.kpp, cc.ogrn, '' as okpo, '' as okved, " +
                                    "CONCAT_WS(' ', cc.director_lastname, cc.director_firstname, cc.director_middlename) AS director_name, " +
                                    "cc.bank_name, cc.bank_account, cc.bank_account_loro, cc.bank_bik, '' as bank_rekv, " +
                                    "CONCAT_WS(', ', cc.legal_address_zip, cc.legal_address_city, cc.legal_address_street) AS legal_address, " +
                                    "CONCAT_WS(', ', cc.postal_address_zip, cc.postal_address_city, cc.postal_address_street) AS postal_address, " +
                                    "cc.phone_1, cc.phone_2, cc.email_additional_1, cc.email_additional_2";
                            break;
                        case "company_foreign":
                            contactsTableName = "contacts_companies_foreigns";
                            contactsColumns = "cc.country_id, cc.email, cc.name AS client_name, " +
                                    "'' as kpp, '' as ogrn, '' as okpo, '' as okved, '' as inn, " +
                                    "CONCAT_WS(' ', cc.director_lastname, cc.director_firstname, cc.director_middlename) AS director_name, " +
                                    "cc.legal_address_zip, cc.legal_address_city, cc.legal_address_street, " +
                                    "'' as legal_address, " +
                                    "'' as bank_name, '' as bank_account, '' as bank_account_loro, '' as bank_bik, '' as bank_rekv, " +
                                    "CONCAT_WS(', ', cc.postal_address_zip, cc.postal_address_city, cc.postal_address_street) AS postal_address, " +
                                    "cc.phone_1, cc.phone_2, cc.email_additional_1, cc.email_additional_2";
                            break;
                        case "entrepreneur":
                            contactsTableName = "contacts_entrepreneurs";
                            contactsColumns = "'RU' as country_id, cc.email, " +
                                    "CONCAT_WS(' ', cc.lastname, cc.firstname, cc.middlename) AS client_name, " +
                                    "'' as kpp, '' as okpo, cc.inn, cc.ogrn, '' as okved, " +
                                    "'' as bank_name, '' as bank_account, '' as bank_account_loro, '' as bank_bik, '' as bank_rekv, " +
                                    "'' as legal_address, " +
                                    "cc.birthdate, '' as passport, cc.passport_number, cc.passport_org, cc.passport_date, " +
                                    "CONCAT_WS(', ', cc.postal_address_zip, cc.postal_address_city, cc.postal_address_street) AS postal_address, " +
                                    "cc.phone_1, cc.phone_2, cc.email_additional_1, cc.email_additional_2";
                            break;
                        case "entrepreneur_foreign":
                            contactsTableName = "contacts_entrepreneurs_foreigns";
                            contactsColumns = "cc.country_id, cc.email, " +
                                    "CONCAT_WS(' ', cc.lastname, cc.firstname, cc.middlename) AS client_name, " +
                                    "'' as kpp, '' as ogrn, '' as okpo, '' as okved, '' as inn, " +
                                    "'' as bank_name, '' as bank_account, '' as bank_account_loro, '' as bank_bik, '' as bank_rekv, " +
                                    "'' as legal_address, " +
                                    "cc.birthdate, cc.document as passport, '' as passport_number, '' as passport_org, '' as passport_date, " +
                                    "CONCAT_WS(', ', cc.postal_address_zip, cc.postal_address_city, cc.postal_address_street) AS postal_address, " +
                                    "cc.phone_1, cc.phone_2, cc.email_additional_1, cc.email_additional_2";
                            break;
                    }

                    String queryContact = "SELECT " + contactsColumns + " " +
                            "FROM " + contactsTableName + " cc " +
                            "WHERE cc.client_id = :client_id";
                    SqlParameterSource namedParametersContact = new MapSqlParameterSource("client_id", rs1.getString("client_id"));

                    registrantNamedParameterJdbcTemplate.query(queryContact,
                            namedParametersContact,
                            ((rs2, rowNum2) -> {
                                person.setName(rs2.getString("client_name"));

                                String phone = rs2.getString("phone_1");
                                if (phone != null && !phone.equals("") && PhoneNumberManager.phoneValid(phone)) {
                                    person.addPhoneNumber(phone);
                                }

                                phone = rs2.getString("phone_2");
                                if (phone != null && !phone.equals("") && PhoneNumberManager.phoneValid(phone)) {
                                    person.addPhoneNumber(phone);
                                }

                                String email = rs2.getString("email");
                                if (email != null && !email.equals("") && emailValidator.isValid(email)) {
                                    person.addEmailAddress(email);
                                }

                                email = rs2.getString("email_additional_1");
                                if (email != null && !email.equals("") && emailValidator.isValid(email)) {
                                    person.addEmailAddress(email);
                                }

                                email = rs2.getString("email_additional_2");
                                if (email != null && !email.equals("") && emailValidator.isValid(email)) {
                                    person.addEmailAddress(email);
                                }

                                String postalAddress = rs2.getString("postal_address");
                                if (postalAddress != null && !postalAddress.equals("")) {
                                    person.setPostalAddress(postalAddress);
                                }

                                String country = rs2.getString("country_id");
                                if (country != null && !country.equals("")) {
                                    person.setCountry(country);
                                } else {
                                    person.setCountry("RU");
                                }

                                switch (clientType) {
                                    case "individual":
                                    case "individual_foreign":
                                        person.setPassport(getPassportFromResultSet(rs2));
                                        break;
                                    case "company":
                                    case "company_foreign":
                                        person.setLegalEntity(getLegalEntityFromString(rs2));
                                        break;
                                    case "entrepreneur":
                                    case "entrepreneur_foreign":
                                        person.setLegalEntity(getLegalEntityFromString(rs2));
                                        person.setPassport(getPassportFromResultSet(rs2));
                                        break;
                                }
                                return null;
                            })
                    );

                    publisher.publishEvent(new PersonCreateEvent(person));

                    return null;
                })
        );

        return null;
    }

    public boolean importToMongo() {
        personRepository.deleteAll();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<Person> persons = personRepository.findByAccountId(accountId);

        if (persons != null && !persons.isEmpty()) {
            personRepository.delete(persons);
        }

        pull(accountId);
        return true;
    }

    private Passport getPassportFromResultSet(ResultSet rs) throws SQLException {
        Passport passport = new Passport();

        String passportFromDatabase = rs.getString("passport");

        if (passportFromDatabase != null && !passportFromDatabase.equals("")) {
            logger.debug("passportFromDatabase: " + passportFromDatabase);

            passportFromDatabase = passportFromDatabase.replaceAll("\r\n", " ");

            // Очистка паспорта от HTML-спецсимволов
            passportFromDatabase = passportFromDatabase.replaceAll("(?u)&#([0-9]+|[a-z]+);", "");

            // Номер паспорта
            Pattern p = Pattern.compile("(?u)([A-Z0-9]+[A-Z0-9\\s]*).*");
            Matcher m = p.matcher(passportFromDatabase);

            if (m.matches()) {
                passport.setNumber(m.group(1).replaceAll("(?u)\\s+", ""));
                passportFromDatabase = passportFromDatabase.replaceAll(m.group(1), "");
            }

            // Дата выдачи паспорта
            p = Pattern.compile("(?u).*(\\d{2}\\.\\d{2}\\.\\d{4}).*");
            m = p.matcher(passportFromDatabase);

            if (m.matches()) {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                LocalDate issuedDate;
                try {
                    issuedDate = LocalDate.parse(m.group(1), dateTimeFormatter);
                    passport.setIssuedDate(issuedDate);
                } catch (DateTimeParseException e) {
                    logger.error("can not parse issuedDate from passportFromDatabase: " + passportFromDatabase +
                            " for name: " + rs.getString("client_name"));
                }
                passportFromDatabase = passportFromDatabase.replaceAll(m.group(1), "");
            }

            // Очиска информации о паспорте от лишних слов и знаков
            if (passport.getIssuedDate() != null || passport.getNumber() != null) {
                passportFromDatabase = passportFromDatabase.replaceAll("(?uim)(выдан|дата|место|номер|выдачи|паспорта|паспорт|серия)", "");
                passportFromDatabase = passportFromDatabase.replaceAll("(?um)\\s{2,}", "");
                passportFromDatabase = passportFromDatabase.replaceAll("^[ ,.]", "");
                passportFromDatabase = passportFromDatabase.replaceAll("[ ,.]$", "");
            }

            passport.setIssuedOrg(passportFromDatabase);

            String birthDate = null;
            try {
                birthDate = rs.getString("birthdate");
            } catch (SQLException e) {
                logger.error("can not get birthdate from rs for name: " + rs.getString("client_name"));
            }

            if (birthDate != null && !birthDate.equals("0000-00-00")) {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate birthday;
                try {
                    birthday = LocalDate.parse(birthDate, dateTimeFormatter);
                    passport.setBirthday(birthday);
                } catch (DateTimeParseException e) {
                    logger.error("can not parse birthdate from rs for name: " + rs.getString("client_name"));
                }
            }
        } else {
            String passportNumber = rs.getString("passport_number");
            String passportOrg = rs.getString("passport_org");
            String passportDate = null;
            try {
                passportDate = rs.getString("passport_date");
            } catch (SQLException e) {
                logger.error("can not parse passport_date from rs for name: " + rs.getString("client_name"));
            }

            logger.debug("passportNumber: " + passportNumber + " passportOrg: " + passportOrg + " passportDate: " + passportDate);

            if (passportNumber != null && !passportNumber.equals("")) {
                passport.setNumber(passportNumber);
            }

            if (passportOrg != null && !passportOrg.equals("")) {
                passport.setIssuedOrg(passportOrg);
            }

            if (passportDate != null && !passportDate.equals("") && !passportDate.equals("0000-00-00")) {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate issuedDate;
                try {
                    issuedDate = LocalDate.parse(passportDate, dateTimeFormatter);
                    passport.setIssuedDate(issuedDate);
                } catch (DateTimeParseException e) {
                    logger.error("can not parse issuedDate from " + passportDate + " for name: " + rs.getString("client_name"));
                }
            }
        }

        return passport;
    }

    private LegalEntity getLegalEntityFromString(ResultSet rs) throws SQLException {
        LegalEntity legalEntity = new LegalEntity();

        legalEntity.setInn(rs.getString("inn"));
        legalEntity.setKpp(rs.getString("kpp"));
        legalEntity.setOgrn(rs.getString("ogrn"));
        legalEntity.setOkpo(rs.getString("okpo"));
        legalEntity.setOkvedCodes(rs.getString("okved"));

        String bankRekv = rs.getString("bank_rekv");
        String bankName = rs.getString("bank_name");
        String bankAccount = rs.getString("bank_account");
        String bankCorrespondentAccount = rs.getString("bank_account_loro");
        String bankBik = rs.getString("bank_bik");

        if (bankRekv != null && !bankRekv.equals("") && (bankName == null || bankName.equals(""))) {
            bankRekv = StringEscapeUtils.unescapeHtml(bankRekv);
            bankRekv = bankRekv.replace("\\", "");

            // Выборка расчетного счета
            Pattern p = Pattern.compile("(?uim)(4[0-9]{19})");
            Matcher m = p.matcher(bankRekv);

            if (m.matches()) {
                legalEntity.setBankAccount(m.group(1));
            }

            // Выборка кор. счета
            p = Pattern.compile("(?uim)(3[0-9]{19})");
            m = p.matcher(bankRekv);

            if (m.matches()) {
                legalEntity.setCorrespondentAccount(m.group(1));
            }

            // Выборка БИК
            p = Pattern.compile("(?uim)(0[0-9]{8})");
            m = p.matcher(bankRekv);

            if (m.matches()) {
                legalEntity.setBik(m.group(1));
            }

            // Выборка названия банка
            String[] splitedBankRekv = bankRekv.split("(?uim)\r?\n", 2);
            legalEntity.setBankName(splitedBankRekv[0]);
        } else {
            if (bankName != null) {
                bankName = bankName.replace("\\", "");
            }
            legalEntity.setBankName(bankName);
            legalEntity.setBankAccount(bankAccount);
            legalEntity.setCorrespondentAccount(bankCorrespondentAccount);
            legalEntity.setBik(bankBik);
        }

        legalEntity.setAddress(rs.getString("legal_address"));

        return legalEntity;
    }
}
