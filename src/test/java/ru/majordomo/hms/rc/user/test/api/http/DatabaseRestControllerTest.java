package ru.majordomo.hms.rc.user.test.api.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.repositories.DatabaseRepository;
import ru.majordomo.hms.rc.user.repositories.DatabaseUserRepository;
import ru.majordomo.hms.rc.user.resources.DTO.QuotaReport;
import ru.majordomo.hms.rc.user.resources.Database;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
public class DatabaseRestControllerTest {

    private MockMvc mockMvc;
    private String resourceName = "database";
    private List<Database> batchOfDatabases = new ArrayList<>();

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
    private RestDocumentationResultHandler doc;

    @Autowired
    private WebApplicationContext ctx;
    @Autowired
    private DatabaseRepository repository;
    @Autowired
    private DatabaseUserRepository databaseUserRepository;

    @Before
    public void setUp() throws Exception {
        this.doc = document("database/{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        batchOfDatabases = ResourceGenerator.generateBatchOfDatabases();
        for (Database database : batchOfDatabases) {
            databaseUserRepository.saveAll((Iterable) database.getDatabaseUsers());
            for (DatabaseUser databaseUser: database.getDatabaseUsers()) {
                database.addDatabaseUserId(databaseUser.getId());
            }
            database.setServiceId(ObjectId.get().toString());
            repository.save(database);
        }
    }

    @Test
    public void readOne() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/"
                + batchOfDatabases.get(0).getId()).accept(APPLICATION_JSON_UTF8);
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
                                fieldWithPath("serviceId").description("ID сервиса, к которому привязана база данных"),
                                fieldWithPath("type").description("Тип базы данных"),
                                fieldWithPath("quota").description("Максимальный размер базы данных"),
                                fieldWithPath("quotaUsed").description("Фактический размер базы в килобайтах"),
                                fieldWithPath("writable").description("Флаг доступности записи."),
                                subsectionWithPath("databaseUsers[]").description("Список пользователей этой базы"),
                                fieldWithPath("locked").description("Доступность ресурса для изменения"),
                                fieldWithPath("willBeDeleted").description("Запланировано удаление ресурса"),
                                fieldWithPath("willBeDeletedAfter").description("Будет удалено после указанной даты")
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
        String accountId = batchOfDatabases.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].name").value(batchOfDatabases.get(0).getName()))
                .andExpect(jsonPath("$[0].accountId").value(accountId))
                .andExpect(jsonPath("$[0].switchedOn").value(batchOfDatabases.get(0).getSwitchedOn()))
                .andExpect(jsonPath("$[0].serviceId").value(batchOfDatabases.get(0).getServiceId()))
                .andExpect(jsonPath("$[0].type").value(batchOfDatabases.get(0).getType().toString()))
                .andExpect(jsonPath("$[0].quotaUsed").value(batchOfDatabases.get(0).getQuotaUsed()))
                .andExpect(jsonPath("$[0].writable").value(batchOfDatabases.get(0).getWritable()))
                .andExpect(jsonPath("$[0].databaseUsers").isArray())
                .andExpect(jsonPath("$[0].databaseUsers.[0].id").value(batchOfDatabases.get(0).getDatabaseUsers().get(0).getId()));
    }

    @Test
    public void readByIdAndByAccountId() throws Exception {
        String accountId = batchOfDatabases.get(0).getAccountId();
        String databaseId = batchOfDatabases.get(0).getId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/" + databaseId).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(batchOfDatabases.get(0).getName()))
                .andExpect(jsonPath("accountId").value(accountId))
                .andExpect(jsonPath("switchedOn").value(batchOfDatabases.get(0).getSwitchedOn()))
                .andExpect(jsonPath("serviceId").value(batchOfDatabases.get(0).getServiceId()))
                .andExpect(jsonPath("type").value(batchOfDatabases.get(0).getType().toString()))
                .andExpect(jsonPath("quotaUsed").value(batchOfDatabases.get(0).getQuotaUsed()))
                .andExpect(jsonPath("writable").value(batchOfDatabases.get(0).getWritable()))
                .andExpect(jsonPath("databaseUsers").isArray())
                .andExpect(jsonPath("databaseUsers.[0].id").value(batchOfDatabases.get(0).getDatabaseUsers().get(0).getId()));
    }

    @Test
    public void readAllByDatabaseUserId() throws Exception {
        String accountId = batchOfDatabases.get(0).getAccountId();
        String databaseUserId = batchOfDatabases.get(0).getDatabaseUserIds().get(0);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get("/" + accountId + "/" + resourceName + "/filter?databaseUserId=" + databaseUserId)
                .accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].name").value(batchOfDatabases.get(0).getName()))
                .andExpect(jsonPath("$[0].databaseUsers.[0].id").value(batchOfDatabases.get(0).getDatabaseUsers().get(0).getId()));
    }

    @Test
    public void countByAccountId() throws Exception {
        String accountId = batchOfDatabases.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/count").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("count").value(1))
                .andDo(doc)
                .andDo(doc.document(responseFields(fieldWithPath("count").description("Количество ресурсов WebSite для указанного accountId"))));
    }

    @Test
    public void readAllByServerId() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/filter?serviceId=" + batchOfDatabases.get(0).getServiceId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(print());
    }

    @Test
    public void updateQuota() throws Exception {
        QuotaReport report = new QuotaReport();
        report.setQuotaUsed(100000L);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(report);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(
                "/" + resourceName + "/" + batchOfDatabases.get(0).getId() + "/quota-report"
        ).contentType(APPLICATION_JSON_UTF8).accept(APPLICATION_JSON_UTF8).content(json);
        mockMvc.perform(request).andExpect(status().isAccepted());
    }
}
