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
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.Passport;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.test.config.ConfigPersonRestController;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConfigPersonRestController.class, webEnvironment = RANDOM_PORT)
public class PersonRestControllerTest {

    private MockMvc mockMvc;
    private String resourceName = "person";
    private List<Person> batchOfPersons;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
    private RestDocumentationResultHandler doc;

    @Autowired
    private WebApplicationContext ctx;
    @Autowired
    private PersonRepository repository;

    @Before
    public void setUp() {
        this.doc = document("config-template/{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
    }

    private void generateBatchOfPersons() {
        Person parovozov = new Person();
        parovozov.setName("Паровозов Аркадий Локомотивович");
        parovozov.addEmailAddress("arkady@parovozov.ru");
        parovozov.addEmailAddress("parovozov@gmail.com");
        parovozov.addPhoneNumber("+79110000911");
        parovozov.addPhoneNumber("+79110000001");

        Passport parovozovPassport = new Passport();
        parovozovPassport.setBirthday();
    }

    @Test
    public void readAll() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(doc)
                .andDo(doc.document(
                        responseFields(
                                fieldWithPath("[].id").description("Внутренний ID ресурса"),
                                fieldWithPath("[].name").description("ФИО или название организации"),
                                fieldWithPath("[].switchedOn").description("Флаг того, можно ли использовать персону"),
                                fieldWithPath("[].phoneNumbers").description("Список контактных телефонных номеров"),
                                fieldWithPath("[].emailAddresses").description("Список контактных email адресов"),
                                fieldWithPath("[].passport").description("Паспортные данные персоны. Если null - значит это юр. лицо"),
                                fieldWithPath("[].legalEntity").description("Информация об организации. Если null - значит это физ. лицо")
                        )
                ));
    }
}
