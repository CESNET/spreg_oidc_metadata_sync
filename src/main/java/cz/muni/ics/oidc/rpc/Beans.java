package cz.muni.ics.oidc.rpc;

import cz.muni.ics.oidc.models.ConnectorProperties;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
public class Beans {

    @Autowired
    @Bean
    public RestTemplate restTemplate(ConnectorProperties connectorProperties) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(connectorProperties.getRequestTimeout())
                .setConnectTimeout(connectorProperties.getConnectTimeout())
                .setSocketTimeout(connectorProperties.getSocketTimeout())
                .build();

        PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager();
        poolingConnectionManager.setMaxTotal(connectorProperties.getMaxConnections());
        poolingConnectionManager.setDefaultMaxPerRoute(connectorProperties.getMaxConnectionsPerRoute());

        ConnectionKeepAliveStrategy connectionKeepAliveStrategy = (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator
                    (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();

                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return 20000L;
        };

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(poolingConnectionManager)
                .setKeepAliveStrategy(connectionKeepAliveStrategy)
                .build();

        HttpComponentsClientHttpRequestFactory poolingRequestFactory = new HttpComponentsClientHttpRequestFactory();
        poolingRequestFactory.setHttpClient(httpClient);

        // basic auth
        List<ClientHttpRequestInterceptor> interceptors =
                Collections.singletonList(new BasicAuthenticationInterceptor(connectorProperties.getPerunUser(),
                        connectorProperties.getPerunPassword()));
        InterceptingClientHttpRequestFactory authenticatingRequestFactory =
                new InterceptingClientHttpRequestFactory(poolingRequestFactory, interceptors);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(authenticatingRequestFactory);
        return restTemplate;
    }

}
