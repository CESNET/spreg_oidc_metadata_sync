package cz.muni.ics.oidc.models;

import cz.muni.ics.oidc.Utils;
import cz.muni.ics.oidc.props.ConfProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;

@Component
public class CryptoBeans {

    @Bean
    public Cipher cipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        final String CIPHER_PARAMS = "AES/ECB/PKCS5PADDING";
        return Cipher.getInstance(CIPHER_PARAMS);
    }

    @Autowired
    @Bean
    public SecretKeySpec secretKeySpec(ConfProperties confProperties) throws NoSuchAlgorithmException {
        return Utils.generateSecretKeySpec(confProperties.getEncryptionSecret());
    }

}
