package cz.muni.ics.oidc.data;

import cz.muni.ics.oidc.models.MitreidClient;
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
    public void saveClient(MitreidClient client) {
        this.saveOrUpdate(manager, client);
    }

    @Transactional
    public void deleteClient(MitreidClient client) {
        MitreidClient found = this.getById(client.getId());
        if (found != null) {
            manager.remove(found);
        } else {
            throw new IllegalArgumentException("Client not found: " + client);
        }
    }

    @Transactional
    public void updateClient(Long id, MitreidClient client) {
        client.setId(id);
        this.saveOrUpdate(manager, client);
    }

    public MitreidClient getById(Long id) {
        return manager.find(MitreidClient.class, id);
    }

    public MitreidClient getClientByClientId(String clientId) {
        TypedQuery<MitreidClient> query = manager.createNamedQuery(
                MitreidClient.QUERY_BY_CLIENT_ID, MitreidClient.class);
        query.setParameter(MitreidClient.PARAM_CLIENT_ID, clientId);
        return this.getSingleResult(query.getResultList());
    }

    public Set<String> getAllClientIds() {
        TypedQuery<String> q = manager.createNamedQuery(MitreidClient.QUERY_ALL_CLIENT_IDS, String.class);
        return new HashSet<>(q.getResultList());
    }

    @Transactional
    public int deleteByClientIds(Set<String> clientIds) {
        Query q = manager.createNamedQuery(MitreidClient.DELETE_BY_CLIENT_IDS);
        q.setParameter(MitreidClient.PARAM_CLIENT_ID_SET, clientIds);
        return q.executeUpdate();
    }

    // private

    private MitreidClient getSingleResult(List<MitreidClient> list) {
        switch(list.size()) {
            case 0:
                return null;
            case 1:
                return list.get(0);
            default:
                throw new IllegalStateException("Expected single result, got " + list.size());
        }
    }

    private void saveOrUpdate(EntityManager entityManager, MitreidClient entity) {
        entityManager.merge(entity);
        entityManager.flush();
    }

}

