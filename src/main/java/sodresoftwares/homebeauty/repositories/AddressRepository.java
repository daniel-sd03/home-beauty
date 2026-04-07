package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.homebeauty.model.Address;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, String> {
    List<Address> findByUserId(String userId);
}
