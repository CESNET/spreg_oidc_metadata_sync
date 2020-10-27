package cz.muni.ics.oidc;

import cz.muni.ics.oidc.models.ClientDetailsEntity;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Repository
@NoArgsConstructor
@AllArgsConstructor
public class ClientRepository {

    @PersistenceContext(unitName = "defaultPersistenceUnit")
    @NonNull
    private EntityManager manager;

    public ClientDetailsEntity getById(Long id) {
        return manager.find(ClientDetailsEntity.class, id);
    }

    public ClientDetailsEntity getClientByClientId(String clientId) {
        TypedQuery<ClientDetailsEntity> query = manager.createNamedQuery(
                ClientDetailsEntity.QUERY_BY_CLIENT_ID, ClientDetailsEntity.class);
        query.setParameter(ClientDetailsEntity.PARAM_CLIENT_ID, clientId);
        return this.getSingleResult(query.getResultList());
    }

    @Transactional
    public ClientDetailsEntity saveClient(ClientDetailsEntity client) {
        return this.saveOrUpdate(manager, client);
    }

    @Transactional
    public void deleteClient(ClientDetailsEntity client) {
        ClientDetailsEntity found = this.getById(client.getId());
        if (found != null) {
            manager.remove(found);
        } else {
            throw new IllegalArgumentException("Client not found: " + client);
        }
    }

    @Transactional
    public ClientDetailsEntity updateClient(Long id, ClientDetailsEntity client) {
        client.setId(id);
        return this.saveOrUpdate(manager, client);
    }

    // private

    private ClientDetailsEntity getSingleResult(List<ClientDetailsEntity> list) {
        switch(list.size()) {
            case 0:
                return null;
            case 1:
                return list.get(0);
            default:
                throw new IllegalStateException("Expected single result, got " + list.size());
        }
    }

    private ClientDetailsEntity saveOrUpdate(EntityManager entityManager, ClientDetailsEntity entity) {
        ClientDetailsEntity tmp = entityManager.merge(entity);
        entityManager.flush();
        return tmp;
    }

}

