package uk.gov.moj.cpp.progression.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.entity.Person;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.QueryParam;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;
/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Repository
public interface PersonRepository extends EntityRepository<Person, UUID> {

    /**
     * Find all {@link Person} by lastName (case-insensitive).
     *
     * @param lastName of the people to retrieve.
     * @return List of matching people. Never returns null.
     */
    @Query(value = "FROM Person p WHERE UPPER(p.lastName) LIKE UPPER(?1)")
    List<Person> findByLastNameIgnoreCase(final String lastName);


    @Query(value = "FROM Person p WHERE upper(p.firstName) = upper(?1) and upper(p.lastName) = upper(?2) and " +
            "upper(replace(p.address.postCode,' ','')) = upper(replace(?3,' ','')) and p.dateOfBirth = ?4 ")
    List<Person> findPersonByCriteria(String firstName, String lastName, String postCode,
                                      LocalDate dateOfBirth);

    @Query(value = "FROM Person p where p.id in (:personIds)")
    List<Person> findPersonsByIds(@QueryParam("personIds") final List<UUID> personIds);
}
