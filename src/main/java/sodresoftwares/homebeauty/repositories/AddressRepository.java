package sodresoftwares.homebeauty.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sodresoftwares.homebeauty.model.Address;

public interface AddressRepository extends JpaRepository<Address, String> {
}
