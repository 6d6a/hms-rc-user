package ru.majordomo.hms.rc.user.test.api.http;

import com.fasterxml.jackson.databind.ObjectMapper;

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

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.DTO.QuotaReport;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
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
public class UnixAccountRestControllerTest {

    private MockMvc mockMvc;
    private String resourceName = "unix-account";
    private List<UnixAccount> batchOfUnixAccount = new ArrayList<>();

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
    private RestDocumentationResultHandler doc;

    @Autowired
    private WebApplicationContext ctx;
    @Autowired
    private UnixAccountRepository repository;

    @Before
    public void setUp() throws Exception {
        this.doc = document("unix-account/{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        batchOfUnixAccount = ResourceGenerator.generateBatchOfUnixAccounts();
        repository.saveAll((Iterable) batchOfUnixAccount);
    }

    @Test
    public void updateQuota() throws Exception {
        QuotaReport report = new QuotaReport();
        report.setQuotaUsed(100000L);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(report);
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(
                "/" + resourceName + "/" + batchOfUnixAccount.get(0).getId() + "/quota-report"
        ).contentType(APPLICATION_JSON_UTF8).accept(APPLICATION_JSON_UTF8).content(json);
        mockMvc.perform(request).andExpect(status().isAccepted());
    }

    @Test
    public void readOneAndPrint() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/"
                + batchOfUnixAccount.get(0).getId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(print());
    }

    @Test
    public void readOne() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/"
                + batchOfUnixAccount.get(0).getId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(doc)
                .andDo(doc.document(
                        responseFields(
                                fieldWithPath("@type").description("ClassName ресурса"),
                                fieldWithPath("id").description("Внутренний ID ресурса"),
                                fieldWithPath("accountId").description("ID аккаунта владельца ресурса"),
                                fieldWithPath("name").description("Имя UNIX account'а"),
                                fieldWithPath("switchedOn").description("Флаг того, активен ли аккаунт"),
                                fieldWithPath("homeDir").description("Домашняя директория пользователя"),
                                fieldWithPath("uid").description("UID пользователя"),
                                fieldWithPath("serverId").description("ID сервера, на котором расположен ящик"),
                                subsectionWithPath("crontab[]").description("Список заданий в для cron"),
                                fieldWithPath("quota").description("Максимальный совокупный размер файлов пользователя"),
                                fieldWithPath("quotaUsed").description("Фактический совокупный размер файлов пользователя"),
                                fieldWithPath("writable").description("Флаг, указывающий на то, возможна ли запись новых файлов пользователем"),
                                fieldWithPath("passwordHash").description("Криптованный хеш пароля"),
                                subsectionWithPath("keyPair").description("Пара ключей для доступа по SSH"),
                                fieldWithPath("sendmailAllowed").description("Разрешена ли отправка почты из скриптов аккаунта"),
                                fieldWithPath("locked").description("Доступность ресурса для изменения"),
                                fieldWithPath("infected").description("Наличие незакрытых отчётов о вредоносном коде"),
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
        String accountId = batchOfUnixAccount.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].name").value(batchOfUnixAccount.get(0).getName()))
                .andExpect(jsonPath("$[0].accountId").value(accountId))
                .andExpect(jsonPath("$[0].switchedOn").value(batchOfUnixAccount.get(0).getSwitchedOn()))
                .andExpect(jsonPath("$[0].homeDir").value(batchOfUnixAccount.get(0).getHomeDir()))
                .andExpect(jsonPath("$[0].uid").value(batchOfUnixAccount.get(0).getUid()))
                .andExpect(jsonPath("$[0].serverId").value(batchOfUnixAccount.get(0).getServerId()))
                .andExpect(jsonPath("$[0].crontab").isArray())
                .andExpect(jsonPath("$[0].quota").value(batchOfUnixAccount.get(0).getQuota()))
                .andExpect(jsonPath("$[0].quotaUsed").value(batchOfUnixAccount.get(0).getQuotaUsed()))
                .andExpect(jsonPath("$[0].writable").value(batchOfUnixAccount.get(0).getWritable()))
                .andExpect(jsonPath("$[0].passwordHash").value(batchOfUnixAccount.get(0).getPasswordHash()))
                .andExpect(jsonPath("$[0].keyPair").isMap())
                .andExpect(jsonPath("$[0].keyPair.privateKey").value(batchOfUnixAccount.get(0).getKeyPair().getPrivateKey()))
                .andExpect(jsonPath("$[0].keyPair.publicKey").value(batchOfUnixAccount.get(0).getKeyPair().getPublicKey()));
    }

    @Test
    public void readAllByServerId() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/filter?serverId=" + batchOfUnixAccount.get(0).getServerId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(print());
    }

    @Test
    public void readByIdAndByAccountId() throws Exception {
        String accountId = batchOfUnixAccount.get(0).getAccountId();
        String unixAccountId = batchOfUnixAccount.get(0).getId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/" + unixAccountId).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(batchOfUnixAccount.get(0).getName()))
                .andExpect(jsonPath("accountId").value(accountId))
                .andExpect(jsonPath("switchedOn").value(batchOfUnixAccount.get(0).getSwitchedOn()))
                .andExpect(jsonPath("homeDir").value(batchOfUnixAccount.get(0).getHomeDir()))
                .andExpect(jsonPath("uid").value(batchOfUnixAccount.get(0).getUid()))
                .andExpect(jsonPath("serverId").value(batchOfUnixAccount.get(0).getServerId()))
                .andExpect(jsonPath("crontab").isArray())
                .andExpect(jsonPath("quota").value(batchOfUnixAccount.get(0).getQuota()))
                .andExpect(jsonPath("quotaUsed").value(batchOfUnixAccount.get(0).getQuotaUsed()))
                .andExpect(jsonPath("writable").value(batchOfUnixAccount.get(0).getWritable()))
                .andExpect(jsonPath("passwordHash").value(batchOfUnixAccount.get(0).getPasswordHash()))
                .andExpect(jsonPath("keyPair").isMap())
                .andExpect(jsonPath("keyPair.privateKey").value(batchOfUnixAccount.get(0).getKeyPair().getPrivateKey()))
                .andExpect(jsonPath("keyPair.publicKey").value(batchOfUnixAccount.get(0).getKeyPair().getPublicKey()));
    }

}
