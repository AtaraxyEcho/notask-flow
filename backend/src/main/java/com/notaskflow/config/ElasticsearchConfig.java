package com.notaskflow.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.notaskflow.repository.FileSearchRepository;
import com.notaskflow.repository.NoteSearchRepository;
import java.time.Duration;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch 配置。
 *
 * @author LIN
 */
@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackageClasses = {NoteSearchRepository.class, FileSearchRepository.class})
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String[] uris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.elasticsearch.connection-timeout:5s}")
    private Duration connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:30s}")
    private Duration socketTimeout;

    /**
     * 创建 Elasticsearch REST 客户端。
     *
     * @return Elasticsearch REST 客户端
     */
    @Bean(destroyMethod = "close")
    public RestClient elasticsearchRestClient() {
        RestClientBuilder builder = RestClient.builder(resolveHttpHosts());
        builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(toMilliseconds(connectionTimeout))
                .setSocketTimeout(toMilliseconds(socketTimeout)));

        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        return builder.build();
    }

    /**
     * 创建 Elasticsearch Java API 客户端。
     *
     * @param restClient Elasticsearch REST 客户端
     * @return Elasticsearch Java API 客户端
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    /**
     * 创建 Spring Data Elasticsearch 操作模板。
     *
     * @param elasticsearchClient Elasticsearch Java API 客户端
     * @return Elasticsearch 操作模板
     */
    @Bean
    public ElasticsearchOperations elasticsearchTemplate(ElasticsearchClient elasticsearchClient) {
        return new ElasticsearchTemplate(elasticsearchClient);
    }

    private HttpHost[] resolveHttpHosts() {
        HttpHost[] httpHosts = Arrays.stream(uris)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(this::normalizeUri)
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        if (httpHosts.length == 0) {
            return new HttpHost[] {HttpHost.create("http://localhost:9200")};
        }

        return httpHosts;
    }

    private String normalizeUri(String uri) {
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            return uri;
        }
        return "http://" + uri;
    }

    private int toMilliseconds(Duration timeout) {
        return Math.toIntExact(timeout.toMillis());
    }

//    /**
//     * 启动时初始化笔记搜索索引及映射。
//     *
//     * @param elasticsearchOperations Elasticsearch 操作模板
//     * @return 应用启动任务
//     */
//    @Bean
//    public ApplicationRunner elasticsearchIndexInitializer(ElasticsearchOperations elasticsearchOperations) {
//        return args -> initializeIndex(elasticsearchOperations.indexOps(NoteSearchDocument.class));
//    }
//
//    private void initializeIndex(IndexOperations indexOperations) {
//        try {
//            if (indexOperations.exists()) {
//                indexOperations.putMapping();
//                return;
//            }
//            boolean created = indexOperations.createWithMapping();
//            if (!created) {
//                log.warn("笔记 Elasticsearch 索引初始化失败");
//            }
//        } catch (RuntimeException exception) {
//            log.warn("笔记 Elasticsearch 索引初始化异常，将在业务操作时重试", exception);
//        }
//    }
}
