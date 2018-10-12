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

import ru.majordomo.hms.rc.user.repositories.FTPUserRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.FTPUser;
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
        webEnvironment = RANDOM_PORT,
        properties = {
                "resources.quotable.warnPercent.mailbox=90"
        }

)
public class FTPUserRestControllerTest {

    private MockMvc mockMvc;
    private String resourceName = "ftp-user";
    private List<FTPUser> batchOfFTPUsers = new ArrayList<>();

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
    private RestDocumentationResultHandler doc;

    @Autowired
    private WebApplicationContext ctx;
    @Autowired
    private FTPUserRepository repository;
    @Autowired
    private UnixAccountRepository unixAccountRepository;

    @Before
    public void setUp() throws Exception {
        this.doc = document(resourceName + "/{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        batchOfFTPUsers = ResourceGenerator.generateBatchOfFTPUsers();
        for (FTPUser ftpUser : batchOfFTPUsers) {
            ftpUser.setUnixAccountId(ftpUser.getUnixAccount().getId());
            unixAccountRepository.save(ftpUser.getUnixAccount());
            repository.save(ftpUser);
        }
    }

    @Test
    public void readOne() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/"
                + batchOfFTPUsers.get(0).getId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(doc)
                .andDo(doc.document(
                        responseFields(
                                fieldWithPath("@type").description("ClassName ресурса"),
                                fieldWithPath("id").description("Внутренний ID ресурса"),
                                fieldWithPath("accountId").description("ID аккаунта владельца ресурса"),
                                fieldWithPath("name").description("FQDN"),
                                fieldWithPath("switchedOn").description("Флаг того, активен ли домен"),
                                fieldWithPath("homeDir").description("Домашняя директория FTP пользователя"),
                                fieldWithPath("passwordHash").description("Хэш пользователя"),
                                subsectionWithPath("unixAccount").description("Аккаунт пользователя на сервере"),
                                fieldWithPath("allowedIPAddresses").description("Список адресов, с которых возможен доступ на сервер"),
                                fieldWithPath("allowWebFtp").description("Доступность FTP-сервера через WebFTP, если указаны разрешенные IP-адреса"),
                                fieldWithPath("locked").description("Доступность ресурса для изменения"),
                                fieldWithPath("willBeDeleted").description("Запланировано удаление ресурса")
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
        String accountId = batchOfFTPUsers.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].name").value(batchOfFTPUsers.get(0).getName()))
                .andExpect(jsonPath("$[0].accountId").value(accountId))
                .andExpect(jsonPath("$[0].switchedOn").value(batchOfFTPUsers.get(0).getSwitchedOn()))
                .andExpect(jsonPath("$[0].passwordHash").value(batchOfFTPUsers.get(0).getPasswordHash()))
                .andExpect(jsonPath("$[0].homeDir").value(batchOfFTPUsers.get(0).getHomeDir()))
                .andExpect(jsonPath("$[0].unixAccount").isMap())
                .andExpect(jsonPath("$[0].unixAccount.id").value(batchOfFTPUsers.get(0).getUnixAccount().getId()));
    }

    @Test
    public void readByIdAndByAccountId() throws Exception {
        String accountId = batchOfFTPUsers.get(0).getAccountId();
        String ftpUserId = batchOfFTPUsers.get(0).getId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/" + ftpUserId).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(batchOfFTPUsers.get(0).getName()))
                .andExpect(jsonPath("accountId").value(accountId))
                .andExpect(jsonPath("switchedOn").value(batchOfFTPUsers.get(0).getSwitchedOn()))
                .andExpect(jsonPath("passwordHash").value(batchOfFTPUsers.get(0).getPasswordHash()))
                .andExpect(jsonPath("homeDir").value(batchOfFTPUsers.get(0).getHomeDir()))
                .andExpect(jsonPath("unixAccount").isMap())
                .andExpect(jsonPath("unixAccount.id").value(batchOfFTPUsers.get(0).getUnixAccount().getId()));
    }

    @Test
    public void readByName() throws Exception {
        FTPUser ftpUser = batchOfFTPUsers.get(0);
        String accountId = ftpUser.getAccountId();
        String name = ftpUser.getName();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/filter?name=" + name).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(ftpUser.getName()))
                .andExpect(jsonPath("switchedOn").value(ftpUser.getSwitchedOn()))
                .andExpect(jsonPath("passwordHash").value(ftpUser.getPasswordHash()))
                .andExpect(jsonPath("homeDir").value(ftpUser.getHomeDir()))
                .andExpect(jsonPath("unixAccount.id").value(ftpUser.getUnixAccountId()));
    }

    @Test
    public void readByUnavailableName() throws Exception {
        FTPUser ftpUser = batchOfFTPUsers.get(0);
        String accountId = ftpUser.getAccountId();
        String name = ftpUser.getName();

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/filter?name=ksjdgasgkj" + name).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isNotFound());
    }

    @Test
    public void readByAlienAccountIdAndOwnName() throws Exception {
        FTPUser ftpUser = batchOfFTPUsers.get(0);
        FTPUser alienFtpUser = batchOfFTPUsers.get(1);
        String name = ftpUser.getName();

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(
                "/" + alienFtpUser.getAccountId() + "/" + resourceName + "/filter?name=" + name)
                .accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isNotFound());
    }

    @Test
    public void readByOwnAccountIdAndOwnName() throws Exception {
        FTPUser ftpUser = batchOfFTPUsers.get(0);
        String name = ftpUser.getName();

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(
                "/" + ftpUser.getAccountId() + "/" + resourceName + "/filter?name=" + name)
                .accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(ftpUser.getName()))
                .andExpect(jsonPath("switchedOn").value(ftpUser.getSwitchedOn()))
                .andExpect(jsonPath("passwordHash").value(ftpUser.getPasswordHash()))
                .andExpect(jsonPath("homeDir").value(ftpUser.getHomeDir()))
                .andExpect(jsonPath("unixAccount.id").value(ftpUser.getUnixAccountId()));
    }

    @Test
    public void countByAccountId() throws Exception {
        String accountId = batchOfFTPUsers.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/count").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("count").value(1))
                .andDo(doc)
                .andDo(doc.document(responseFields(fieldWithPath("count").description("Количество ресурсов WebSite для указанного accountId"))));
    }
}
