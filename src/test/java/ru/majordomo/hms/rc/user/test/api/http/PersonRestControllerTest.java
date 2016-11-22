package ru.majordomo.hms.rc.user.test.api.http;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.rest.ConfigPersonRestController;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ConfigPersonRestController.class, ConfigStaffResourceControllerClient.class}, webEnvironment = RANDOM_PORT)
public class PersonRestControllerTest {

    private MockMvc mockMvc;
    private String resourceName = "person";
    private List<Person> batchOfPersons = new ArrayList<>();

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
    private RestDocumentationResultHandler doc;

    @Autowired
    private WebApplicationContext ctx;
    @Autowired
    private PersonRepository repository;

    @Before
    public void setUp() {
        this.doc = document("person/{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        batchOfPersons = ResourceGenerator.generateBatchOfPerson();
        for (Person person: batchOfPersons) {
            repository.save(person);
        }
    }

    @Test
    public void readOneIndividualPerson() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/"
                + batchOfPersons.get(0).getId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(doc)
                .andDo(doc.document(
                        responseFields(
                                fieldWithPath("id").description("Внутренний ID ресурса"),
                                fieldWithPath("name").description("ФИО или название организации"),
                                fieldWithPath("switchedOn").description("Флаг того, активна ли персона"),
                                fieldWithPath("phoneNumbers").description("Список телефонных номеров"),
                                fieldWithPath("emailAddresses").description("Список контакных email адресов"),
                                fieldWithPath("passport").description("Паспортные данные"),
                                fieldWithPath("passport.number").description("Серия и номер паспорта"),
                                fieldWithPath("passport.issuedOrg").description("Наименование организации, выдавшей паспорт"),
                                fieldWithPath("passport.issuedDate").description("Дата выдачи паспорта"),
                                fieldWithPath("passport.birthday").description("Дата рождения в формате yyyy-MM-dd"),
                                fieldWithPath("passport.mainPage").description("Страница паспорта с фотографией"),
                                fieldWithPath("passport.registerPage").description("Страница с регистрацией"),
                                fieldWithPath("passport.address").description("Адрес регистрации"),
                                fieldWithPath("legalEntity").description("Реквизиты организации. Здесь null, т.к. объект является физ. лицом"),
                                fieldWithPath("country").description("Код страны, резидентом которой является персона"),
                                fieldWithPath("postalAddress").description("Адрес, по которому можно направлять почтовые уведомления"),
                                fieldWithPath("owner").description("Является ли эта персона владельцем аккаунта"),
                                fieldWithPath("nicHandle").description("nicHandle")
                        )
                ));
    }

    @Test
    public void readOneLegalEntityPerson() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/"
                + batchOfPersons.get(1).getId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(doc)
                .andDo(doc.document(
                        responseFields(
                                fieldWithPath("id").description("Внутренний ID ресурса"),
                                fieldWithPath("name").description("ФИО или название организации"),
                                fieldWithPath("switchedOn").description("Флаг того, активна ли персона"),
                                fieldWithPath("phoneNumbers").description("Список телефонных номеров"),
                                fieldWithPath("emailAddresses").description("Список контакных email адресов"),
                                fieldWithPath("passport").description("Паспортные данные. Здесь null, т.к. объект является юр. лицом"),
                                fieldWithPath("legalEntity.inn").description("ИНН организации"),
                                fieldWithPath("legalEntity.okpo").description("ОКПО организации"),
                                fieldWithPath("legalEntity.kpp").description("КПП организации"),
                                fieldWithPath("legalEntity.ogrn").description("ОГРН организации"),
                                fieldWithPath("legalEntity.okvedCodes").description("Список кодов ОКВЭД организации"),
                                fieldWithPath("legalEntity.address").description("Адрес регистрации"),
                                fieldWithPath("postalAddress").description("Адрес, по которому можно направлять почтовые уведомления"),
                                fieldWithPath("owner").description("Является ли эта персона владельцем аккаунта"),
                                fieldWithPath("country").description("Код страны, резидентом которой является организация"),
                                fieldWithPath("nicHandle").description("nicHandle")
                        )
                ));
    }

    @Test
    public void readAll() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(doc);
    }

    @Test
    public void readAllByAccountId() throws Exception {
        String accountId = batchOfPersons.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].name").value(batchOfPersons.get(0).getName()))
                .andExpect(jsonPath("$[0].switchedOn").value(batchOfPersons.get(0).getSwitchedOn()))
                .andExpect(jsonPath("$[0].phoneNumbers").value(batchOfPersons.get(0).getPhoneNumbers()))
                .andExpect(jsonPath("$[0].emailAddresses").value(batchOfPersons.get(0).getEmailAddresses()))
                .andExpect(jsonPath("$[0].passport").isMap())
                .andExpect(jsonPath("$[0].passport.number").value(batchOfPersons.get(0).getPassport().getNumber()))
                .andExpect(jsonPath("$[0].passport.issuedOrg").value(batchOfPersons.get(0).getPassport().getIssuedOrg()))
                .andExpect(jsonPath("$[0].passport.issuedDate").value(batchOfPersons.get(0).getPassport().getIssuedDate().toString()))
                .andExpect(jsonPath("$[0].passport.birthday").value(batchOfPersons.get(0).getPassport().getBirthday().toString()))
                .andExpect(jsonPath("$[0].passport.mainPage").value(batchOfPersons.get(0).getPassport().getMainPage()))
                .andExpect(jsonPath("$[0].passport.registerPage").value(batchOfPersons.get(0).getPassport().getRegisterPage()))
                .andExpect(jsonPath("$[0].passport.address").value(batchOfPersons.get(0).getPassport().getAddress()))
                .andExpect(jsonPath("$[0].legalEntity").value(batchOfPersons.get(0).getLegalEntity()))
                .andExpect(jsonPath("$[0].postalAddress").value(batchOfPersons.get(0).getPostalAddress()))
                .andExpect(jsonPath("$[0].owner").value(batchOfPersons.get(0).getOwner()))
                .andExpect(jsonPath("$[0].country").value(batchOfPersons.get(0).getCountry()))
                .andExpect(jsonPath("$[0].nicHandle").value(batchOfPersons.get(0).getNicHandle()));
    }

    @Test
    public void readByIdAndByAccountId() throws Exception {
        String accountId = batchOfPersons.get(0).getAccountId();
        String personId = batchOfPersons.get(0).getId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/" + personId).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(batchOfPersons.get(0).getName()))
                .andExpect(jsonPath("switchedOn").value(batchOfPersons.get(0).getSwitchedOn()))
                .andExpect(jsonPath("phoneNumbers").value(batchOfPersons.get(0).getPhoneNumbers()))
                .andExpect(jsonPath("emailAddresses").value(batchOfPersons.get(0).getEmailAddresses()))
                .andExpect(jsonPath("passport").isMap())
                .andExpect(jsonPath("passport.number").value(batchOfPersons.get(0).getPassport().getNumber()))
                .andExpect(jsonPath("passport.issuedOrg").value(batchOfPersons.get(0).getPassport().getIssuedOrg()))
                .andExpect(jsonPath("passport.issuedDate").value(batchOfPersons.get(0).getPassport().getIssuedDate().toString()))
                .andExpect(jsonPath("passport.birthday").value(batchOfPersons.get(0).getPassport().getBirthday().toString()))
                .andExpect(jsonPath("passport.mainPage").value(batchOfPersons.get(0).getPassport().getMainPage()))
                .andExpect(jsonPath("passport.registerPage").value(batchOfPersons.get(0).getPassport().getRegisterPage()))
                .andExpect(jsonPath("passport.address").value(batchOfPersons.get(0).getPassport().getAddress()))
                .andExpect(jsonPath("legalEntity").value(batchOfPersons.get(0).getLegalEntity()))
                .andExpect(jsonPath("postalAddress").value(batchOfPersons.get(0).getPostalAddress()))
                .andExpect(jsonPath("owner").value(batchOfPersons.get(0).getOwner()))
                .andExpect(jsonPath("country").value(batchOfPersons.get(0).getCountry()))
                .andExpect(jsonPath("nicHandle").value(batchOfPersons.get(0).getNicHandle()));
    }

    @Test
    public void readByAccountIdAndByOwner() throws Exception {
        String accountId = batchOfPersons.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/owner").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(batchOfPersons.get(0).getName()))
                .andExpect(jsonPath("switchedOn").value(batchOfPersons.get(0).getSwitchedOn()))
                .andExpect(jsonPath("phoneNumbers").value(batchOfPersons.get(0).getPhoneNumbers()))
                .andExpect(jsonPath("emailAddresses").value(batchOfPersons.get(0).getEmailAddresses()))
                .andExpect(jsonPath("passport").isMap())
                .andExpect(jsonPath("passport.number").value(batchOfPersons.get(0).getPassport().getNumber()))
                .andExpect(jsonPath("passport.issuedOrg").value(batchOfPersons.get(0).getPassport().getIssuedOrg()))
                .andExpect(jsonPath("passport.issuedDate").value(batchOfPersons.get(0).getPassport().getIssuedDate().toString()))
                .andExpect(jsonPath("passport.birthday").value(batchOfPersons.get(0).getPassport().getBirthday().toString()))
                .andExpect(jsonPath("passport.mainPage").value(batchOfPersons.get(0).getPassport().getMainPage()))
                .andExpect(jsonPath("passport.registerPage").value(batchOfPersons.get(0).getPassport().getRegisterPage()))
                .andExpect(jsonPath("passport.address").value(batchOfPersons.get(0).getPassport().getAddress()))
                .andExpect(jsonPath("legalEntity").value(batchOfPersons.get(0).getLegalEntity()))
                .andExpect(jsonPath("postalAddress").value(batchOfPersons.get(0).getPostalAddress()))
                .andExpect(jsonPath("owner").value(batchOfPersons.get(0).getOwner()))
                .andExpect(jsonPath("country").value(batchOfPersons.get(0).getCountry()))
                .andExpect(jsonPath("nicHandle").value(batchOfPersons.get(0).getNicHandle()));
    }
}
