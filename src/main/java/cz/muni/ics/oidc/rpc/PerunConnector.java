package cz.muni.ics.oidc.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import cz.muni.ics.oidc.exception.PerunConnectionException;
import cz.muni.ics.oidc.exception.PerunUnknownException;
import cz.muni.ics.oidc.props.ConnectorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

/**
 * Connector for calling Perun RPC
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 * @author Pavol Pluta <pavol.pluta1@gmail.com>
 */
@Component
@Slf4j
@Validated
public class PerunConnector {

    private final String perunUrl;
    private final RestTemplate restTemplate;

    @Autowired
    public PerunConnector(@NotNull RestTemplate restTemplate,
                          @NotNull ConnectorProperties properties)
    {
        this.restTemplate = restTemplate;
        this.perunUrl = properties.getPerunUrl() + '/' + properties.getSerializer();
    }

    /**
     * Make post call to Perun RPC
     * @param manager String value representing manager to be called. Use constants from this class.
     * @param method Method to be called (i.e. getUserById)
     * @param map Map of parameters to be passed as request body
     * @return Response from Perun
     * @throws PerunUnknownException Thrown as wrapper of unknown exception thrown by Perun interface.
     * @throws PerunConnectionException Thrown when problem with connection to Perun interface occurs.
     */
    public JsonNode post(@NotBlank String manager, @NotBlank String method, @NotNull Map<String, Object> map)
            throws PerunUnknownException, PerunConnectionException
    {
        String actionUrl = this.perunUrl + '/' + manager + '/' + method;

        // make the call
        try {
            log.trace("Calling perun RPC:\n URL: {},\n params: {}", actionUrl, map);
            long startTime = currentTimeMillis();
            JsonNode result = restTemplate.postForObject(actionUrl, map, JsonNode.class);
            long endTime = currentTimeMillis();
            long responseTime = endTime - startTime;
            log.trace("POST call proceeded in {} ms.",responseTime);
            log.trace("Calling perun RPC:\n URL: {},\n params: {}\n returns: {}", actionUrl, map, result);
            return result;
        } catch (HttpClientErrorException ex) {
            return handleHttpClientErrorException(ex, actionUrl);
        } catch (Exception e) {
            throw new PerunConnectionException("Error when contacting Perun RPC", e);
        }
    }

    private JsonNode handleHttpClientErrorException(HttpClientErrorException ex, String actionUrl)
            throws PerunUnknownException {
        MediaType contentType = null;
        if (ex.getResponseHeaders() != null) {
            contentType = ex.getResponseHeaders().getContentType();
        }

        String body = ex.getResponseBodyAsString();

        if (contentType != null && "json".equalsIgnoreCase(contentType.getSubtype())) {
            try {
                JsonNode json = new ObjectMapper().readValue(body,JsonNode.class);
                if (json.has("errorId") && json.has("name")) {
                    switch (json.get("name").asText()) {
                        case "ExtSourceNotExistsException":
                        case "FacilityNotExistsException":
                        case "GroupNotExistsException":
                        case "MemberNotExistsException":
                        case "ResourceNotExistsException":
                        case "VoNotExistsException":
                        case "UserNotExistsException":
                        case "GroupAlreadyExistsException":
                        case "FacilityAlreadyExistsException":
                            return JsonNodeFactory.instance.nullNode();
                    }
                }
            } catch (IOException e) {
                log.error("cannot parse error message from JSON", e);
                throw new PerunUnknownException("Error when contacting Perun RPC", ex);
            }
        }

        log.error("HTTP ERROR {} URL {} Content-Type: {}", ex.getRawStatusCode(), actionUrl, contentType, ex);
        throw new PerunUnknownException("Error when contacting Perun RPC", ex);
    }

}

