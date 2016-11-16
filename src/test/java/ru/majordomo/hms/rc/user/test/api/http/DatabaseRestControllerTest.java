package ru.majordomo.hms.rc.user.test.api.http;

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
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.rest.ConfigDatabaseRestController;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ConfigStaffResourceControllerClient.class, ConfigDatabaseRestController.class}, webEnvironment = RANDOM_PORT)
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
            databaseUserRepository.save((Iterable) database.getDatabaseUsers());
            for (DatabaseUser databaseUser: database.getDatabaseUsers()) {
                database.addDatabaseUserId(databaseUser.getId());
            }
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
                                fieldWithPath("id").description("Внутренний ID ресурса"),
                                fieldWithPath("name").description("Имя базы данных"),
                                fieldWithPath("switchedOn").description("Флаг того, активна ли база данных"),
                                fieldWithPath("serverId").description("ID сервера, на котором расположена база"),
                                fieldWithPath("type").description("Тип базы данных"),
                                fieldWithPath("quota").description("Максимальный размер базы данных в килобайтах"),
                                fieldWithPath("quotaUsed").description("Фактический размер базы в килобайтах"),
                                fieldWithPath("writable").description("Флаг доступности записи."),
                                fieldWithPath("databaseUsers").description("Список пользователей этой базы")
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
                .andExpect(jsonPath("$[0].switchedOn").value(batchOfDatabases.get(0).getSwitchedOn()))
                .andExpect(jsonPath("$[0].serverId").value(batchOfDatabases.get(0).getServerId()))
                .andExpect(jsonPath("$[0].type").value(batchOfDatabases.get(0).getType().toString()))
                .andExpect(jsonPath("$[0].quota").value(batchOfDatabases.get(0).getQuota()))
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
                .andExpect(jsonPath("switchedOn").value(batchOfDatabases.get(0).getSwitchedOn()))
                .andExpect(jsonPath("serverId").value(batchOfDatabases.get(0).getServerId()))
                .andExpect(jsonPath("type").value(batchOfDatabases.get(0).getType().toString()))
                .andExpect(jsonPath("quota").value(batchOfDatabases.get(0).getQuota()))
                .andExpect(jsonPath("quotaUsed").value(batchOfDatabases.get(0).getQuotaUsed()))
                .andExpect(jsonPath("writable").value(batchOfDatabases.get(0).getWritable()))
                .andExpect(jsonPath("databaseUsers").isArray())
                .andExpect(jsonPath("databaseUsers.[0].id").value(batchOfDatabases.get(0).getDatabaseUsers().get(0).getId()));
    }

    @Test
    public void countByAccountId() throws Exception {
        String accountId = batchOfDatabases.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/count").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("count").value(1));
    }
}
