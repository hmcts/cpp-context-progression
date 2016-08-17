package uk.gov.moj.progression.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DefendantRepository extends EntityRepository<Defendant, UUID> {
    @Override
    List<Defendant> findAll();
}
