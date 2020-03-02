package ru.majordomo.hms.rc.user.test.api.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fppt.jedismock.RedisServer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
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


import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.MailboxRepository;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.DTO.QuotaReport;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.Person;
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
public class MailboxRestControllerTest {

    private MockMvc mockMvc;
    private String resourceName = "mailbox";
    private List<Mailbox> batchOfMailboxes = new ArrayList<>();

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
    private RestDocumentationResultHandler doc;

    @Autowired
    private WebApplicationContext ctx;
    @Autowired
    private MailboxRepository repository;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private UnixAccountRepository unixAccountRepository;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private static RedisServer redisServer;

    @Before
    public void setUp() throws Exception {
        if (redisServer == null) {
            LettuceConnectionFactory redisConnectionFactory = (LettuceConnectionFactory) this.redisConnectionFactory;
            redisServer = new RedisServer(redisConnectionFactory.getPort());
            redisServer.start();
        }
        this.doc = document("domain/{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        batchOfMailboxes = ResourceGenerator.generateBatchOfMailboxes();
        List<UnixAccount> unixAccounts = ResourceGenerator.generateBatchOfUnixAccounts(batchOfMailboxes);
        unixAccountRepository.saveAll(unixAccounts);
        for (Mailbox mailbox: batchOfMailboxes) {
            Person person = mailbox.getDomain().getPerson();
            Domain domain = mailbox.getDomain();

            personRepository.save(person);
            domain.setPersonId(person.getId());

            domainRepository.save(domain);
            mailbox.setDomainId(domain.getId());

            mailbox.setUid(unixAccounts.get(0).getUid());
            mailbox.setMailSpool("/homebig/" + domain.getName() + "/" + mailbox.getName());
        }

        repository.saveAll((Iterable) batchOfMailboxes);
    }

    @Test
    public void readOne() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/"
                + batchOfMailboxes.get(0).getId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(doc)
                .andDo(doc.document(
                        responseFields(
                                fieldWithPath("@type").description("ClassName ресурса"),
                                fieldWithPath("id").description("Внутренний ID ресурса"),
                                fieldWithPath("accountId").description("ID аккаунта владельца ресурса"),
                                fieldWithPath("domainId").description("ID домена"),
                                fieldWithPath("name").description("Имя ящика без указания домена"),
                                fieldWithPath("comment").description("Комментарий для ящика"),
                                fieldWithPath("switchedOn").description("Флаг того, активен ли ящик"),
                                fieldWithPath("passwordHash").description("Хэш пароля"),
                                subsectionWithPath("domain").description("Домен, на котором создан ящик"),
                                fieldWithPath("redirectAddresses").description("Список переадресаций с текущего почтового ящика"),
                                fieldWithPath("blackList").description("Список адресов, почта с которых не должна доставляться"),
                                fieldWithPath("whiteList").description("Список адресов, с которых почта должна доставляться в любом случае"),
                                fieldWithPath("mailFromAllowed").description("Разрешено ли отправлять почту по SMTP"),
                                fieldWithPath("antiSpamEnabled").description("Включен ли антиспам и антивирус"),
                                fieldWithPath("serverId").description("ID сервера, на котором расположен ящик"),
                                fieldWithPath("quota").description("Максимальный размер ящика. Назначается сервером, изменение невозможно"),
                                fieldWithPath("quotaUsed").description("Фактический размер ящика"),
                                fieldWithPath("isAggregator").description("Флаг, указывающий на то, будут ли в него доставляться письма для несуществующих ящиков домена"),
                                fieldWithPath("mailSpool").description("Служебное поле. Назначается сервером, изменение невозможно."),
                                fieldWithPath("uid").description("Служебное поле. Назначается сервером, изменение невозможно."),
                                fieldWithPath("writable").description("Флаг, указывающий на то, будут ли доставляться новые письма в ящик"),
                                fieldWithPath("spamFilterAction").description("Действие SPAM-фильтра"),
                                fieldWithPath("spamFilterMood").description("Придирчивость SPAM-фильтра"),
                                fieldWithPath("locked").description("Доступность ресурса для изменения"),
                                fieldWithPath("willBeDeleted").description("Запланировано удаление ресурса"),
                                fieldWithPath("allowedIps").description("Рарзрешённые IP")
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
        String accountId = batchOfMailboxes.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].name").value(batchOfMailboxes.get(0).getName()))
                .andExpect(jsonPath("$[0].accountId").value(accountId))
                .andExpect(jsonPath("$[0].switchedOn").value(batchOfMailboxes.get(0).getSwitchedOn()))
                .andExpect(jsonPath("$[0].domain").isMap())
                .andExpect(jsonPath("$[0].domain.id").value(batchOfMailboxes.get(0).getDomain().getId()))
                .andExpect(jsonPath("$[0].blackList").value(batchOfMailboxes.get(0).getBlackList()))
                .andExpect(jsonPath("$[0].whiteList").value(batchOfMailboxes.get(0).getWhiteList()))
                .andExpect(jsonPath("$[0].antiSpamEnabled").value(batchOfMailboxes.get(0).getAntiSpamEnabled()))
                .andExpect(jsonPath("$[0].serverId").value(batchOfMailboxes.get(0).getServerId()))
                .andExpect(jsonPath("$[0].quota").value(batchOfMailboxes.get(0).getQuota()))
                .andExpect(jsonPath("$[0].quotaUsed").value(batchOfMailboxes.get(0).getQuotaUsed()))
                .andExpect(jsonPath("$[0].writable").value(batchOfMailboxes.get(0).getWritable()));
    }

    @Test
    public void readByIdAndByAccountId() throws Exception {
        String accountId = batchOfMailboxes.get(0).getAccountId();
        String mailboxId = batchOfMailboxes.get(0).getId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/" + mailboxId).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(batchOfMailboxes.get(0).getName()))
                .andExpect(jsonPath("accountId").value(accountId))
                .andExpect(jsonPath("switchedOn").value(batchOfMailboxes.get(0).getSwitchedOn()))
                .andExpect(jsonPath("domain").isMap())
                .andExpect(jsonPath("domain.id").value(batchOfMailboxes.get(0).getDomain().getId()))
                .andExpect(jsonPath("blackList").value(batchOfMailboxes.get(0).getBlackList()))
                .andExpect(jsonPath("whiteList").value(batchOfMailboxes.get(0).getWhiteList()))
                .andExpect(jsonPath("antiSpamEnabled").value(batchOfMailboxes.get(0).getAntiSpamEnabled()))
                .andExpect(jsonPath("serverId").value(batchOfMailboxes.get(0).getServerId()))
                .andExpect(jsonPath("quota").value(batchOfMailboxes.get(0).getQuota()))
                .andExpect(jsonPath("quotaUsed").value(batchOfMailboxes.get(0).getQuotaUsed()))
                .andExpect(jsonPath("writable").value(batchOfMailboxes.get(0).getWritable()));
    }

    @Test
    public void readAllByServerId() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/filter?serverId=" + batchOfMailboxes.get(0).getServerId()).accept(APPLICATION_JSON_UTF8);
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
                "/" + resourceName + "/" + batchOfMailboxes.get(0).getId() + "/quota-report"
        ).contentType(APPLICATION_JSON_UTF8).accept(APPLICATION_JSON_UTF8).content(json);
        mockMvc.perform(request).andExpect(status().isAccepted());
    }

    @AfterClass
    public static void enough() throws Exception {
            redisServer.stop();
    }
}
