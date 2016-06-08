package uk.gov.moj.progression.persistence.repository;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

public class EntityManagerProducer {

    @PersistenceUnit
    private EntityManagerFactory emf;

    // TODO: Switch to a container based entity manager instead of application
    // based.
    @Produces // you can also make this @RequestScoped
    public EntityManager create() {
        return emf.createEntityManager();
    }

    public void close(@Disposes EntityManager em) {
        if (em.isOpen()) {
            em.close();
        }
    }
}