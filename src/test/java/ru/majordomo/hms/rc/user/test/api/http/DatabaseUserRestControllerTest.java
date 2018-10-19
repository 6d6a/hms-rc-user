package ru.majordomo.hms.rc.user.test.api.http;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.amqp.AMQPBrokerConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;
import ru.majordomo.hms.rc.user.test.config.rest.ConfigRestControllers;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ConfigStaffResourceControllerClient.class,
                ConfigDomainRegistrarClient.class,

                FongoConfig.class,
                RedisConfig.class,
                DatabaseConfig.class,

                ConfigRestControllers.class,

                ConfigGovernors.class,
                AMQPBrokerConfig.class
        },
        webEnvironment = RANDOM_PORT
)
public class DatabaseUserRestControllerTest {

    private MockMvc mockMvc;
    private String resourceName = "database-user";
    private List<DatabaseUser> batchOfDatabaseUsers = new ArrayList<>();

    @Value("${default.database.serviceName}")
    private String defaultDatabaseService;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
    private RestDocumentationResultHandler doc;

    @Autowired
    private WebApplicationContext ctx;
    @Autowired
    private DatabaseUserRepository repository;

    @Before
    public void setUp() throws Exception {
        this.doc = document(resourceName + "/{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        batchOfDatabaseUsers = ResourceGenerator.generateBatchOfDatabaseUsers();
        repository.saveAll(batchOfDatabaseUsers);
    }

    @After
    public void deleteAll() {
        repository.deleteAll();
    }

    @Test
    public void readOne() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/"
                + batchOfDatabaseUsers.get(0).getId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(doc)
                .andDo(doc.document(
                        responseFields(
                                fieldWithPath("@type").description("ClassName ресурса"),
                                fieldWithPath("id").description("Внутренний ID ресурса"),
                                fieldWithPath("accountId").description("ID аккаунта владельца ресурса"),
                                fieldWithPath("name").description("Имя базы данных"),
                                fieldWithPath("switchedOn").description("Флаг того, активна ли база данных"),
                                fieldWithPath("type").description("Тип базы данных"),
                                fieldWithPath("passwordHash").description("Хэш пароля пользователя"),
                                fieldWithPath("serviceId").description("ID сервиса в RC-STAFF"),
                                fieldWithPath("allowedIPAddresses").description("IP-адреса, с которых возможен доступ"),
                                fieldWithPath("locked").description("Доступность ресурса для изменения"),
                                fieldWithPath("willBeDeleted").description("Запланировано удаление ресурса"),
                                fieldWithPath("maxCpuTimePerSecond").description("Количество процессорного времени в секунду")
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
        String accountId = batchOfDatabaseUsers.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].name").value(batchOfDatabaseUsers.get(0).getName()))
                .andExpect(jsonPath("$[0].accountId").value(accountId))
                .andExpect(jsonPath("$[0].switchedOn").value(batchOfDatabaseUsers.get(0).getSwitchedOn()))
                .andExpect(jsonPath("$[0].passwordHash").value(batchOfDatabaseUsers.get(0).getPasswordHash()))
                .andExpect(jsonPath("$[0].type").value(batchOfDatabaseUsers.get(0).getType().toString()));
    }

    @Test
    public void readByIdByAndAccountId() throws Exception {
        String accountId = batchOfDatabaseUsers.get(0).getAccountId();
        String databaseUserId = batchOfDatabaseUsers.get(0).getId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/" + databaseUserId).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(batchOfDatabaseUsers.get(0).getName()))
                .andExpect(jsonPath("accountId").value(accountId))
                .andExpect(jsonPath("switchedOn").value(batchOfDatabaseUsers.get(0).getSwitchedOn()))
                .andExpect(jsonPath("passwordHash").value(batchOfDatabaseUsers.get(0).getPasswordHash()))
                .andExpect(jsonPath("type").value(batchOfDatabaseUsers.get(0).getType().toString()));
    }
}
