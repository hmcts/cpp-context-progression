package uk.gov.moj.cpp.progression.persistence.repository;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

import uk.gov.moj.cpp.progression.persistence.entity.Address;
/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Repository
public interface AddressRepository extends EntityRepository<Address, UUID> {
}
