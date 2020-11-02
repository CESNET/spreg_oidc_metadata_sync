package cz.muni.ics.oidc.data;

import cz.muni.ics.oidc.models.ClientDetailsEntity;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@NoArgsConstructor
@AllArgsConstructor
public class ClientRepository {

    @PersistenceContext(unitName = "defaultPersistenceUnit")
    @NonNull
    private EntityManager manager;

    @Transactional
    public void saveClient(ClientDetailsEntity client) {
        this.saveOrUpdate(manager, client);
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
    public void updateClient(Long id, ClientDetailsEntity client) {
        client.setId(id);
        this.saveOrUpdate(manager, client);
    }

    public ClientDetailsEntity getById(Long id) {
        return manager.find(ClientDetailsEntity.class, id);
    }

    public ClientDetailsEntity getClientByClientId(String clientId) {
        TypedQuery<ClientDetailsEntity> query = manager.createNamedQuery(
                ClientDetailsEntity.QUERY_BY_CLIENT_ID, ClientDetailsEntity.class);
        query.setParameter(ClientDetailsEntity.PARAM_CLIENT_ID, clientId);
        return this.getSingleResult(query.getResultList());
    }

    public Set<String> getAllClientIds() {
        TypedQuery<String> q = manager.createNamedQuery(ClientDetailsEntity.QUERY_ALL_CLIENT_IDS, String.class);
        return new HashSet<>(q.getResultList());
    }

    public int deleteByClientIds(Set<String> clientIds) {
        Query q = manager.createQuery(ClientDetailsEntity.DELETE_BY_CLIENT_IDS);
        q.setParameter(ClientDetailsEntity.PARAM_CLIENT_ID_SET, clientIds);
        return q.executeUpdate();
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

    private void saveOrUpdate(EntityManager entityManager, ClientDetailsEntity entity) {
        entityManager.merge(entity);
        entityManager.flush();
    }

}

