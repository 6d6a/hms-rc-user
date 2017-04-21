package ru.majordomo.hms.rc.user.importing;

import org.apache.commons.lang.StringEscapeUtils;
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
    private final PersonRepository personRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public PersonDBImportService(
            @Qualifier("billingNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            PersonRepository personRepository,
            ApplicationEventPublisher publisher
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.personRepository = personRepository;
        this.publisher = publisher;
    }

    public void pull() {
        String query = "SELECT a.id " +
                "FROM client c " +
                "JOIN account a USING(client_id) " +
                "ORDER BY a.id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new PersonImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        String query = "SELECT a.id, " +
                "c.Client_ID, c.name, c.phone, c.phone2, c.email, c.email2, c.email3, " +
                "c.passport, c.HB, c.address, c.address_post, c.inn, c.kpp, c.ogrn, c.okpo, c.okved, " +
                "c.bank_rekv, c.bank_name, c.bill, c.coor_bill, c.bik, c.face, c.company_name " +
                "FROM client c " +
                "JOIN account a USING(client_id) " +
                "WHERE a.id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        namedParameterJdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );
    }

    private Person rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found Person for id: " + rs.getString("id") + " name: " + rs.getString("name"));

        Person person = new Person();
        person.setId("person_" + rs.getString("Client_ID"));
        person.setAccountId(rs.getString("id"));
        person.setSwitchedOn(true);
        person.setName(rs.getString("name"));

        String phone = rs.getString("phone");
        if (phone != null && !phone.equals("")) {
            person.addPhoneNumber(phone);
        }

        phone = rs.getString("phone2");
        if (phone != null && !phone.equals("")) {
            person.addPhoneNumber(phone);
        }

        String email = rs.getString("email");
        if (email != null && !email.equals("")) {
            person.addEmailAddress(email);
        }

        email = rs.getString("email2");
        if (email != null && !email.equals("")) {
            person.addEmailAddress(email);
        }

        email = rs.getString("email3");
        if (email != null && !email.equals("")) {
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
                e.printStackTrace();
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

        if (rs.getString("HB") != null && !rs.getString("HB").equals("0000-00-00")) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate birthday;
            try {
                birthday = LocalDate.parse(rs.getString("HB"), dateTimeFormatter);
                passport.setBirthday(birthday);
            } catch (DateTimeParseException e) {
                e.printStackTrace();
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
        String bankAccount = rs.getString("bill");
        String bankCorrespondentAccount = rs.getString("coor_bill");
        String bankBik = rs.getString("bik");

        if (bankRekv != null && !bankRekv.equals("") && (bankName != null && !bankName.equals(""))) {
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

        legalEntity.setAddress(rs.getString("address"));

        return legalEntity;
    }
}
