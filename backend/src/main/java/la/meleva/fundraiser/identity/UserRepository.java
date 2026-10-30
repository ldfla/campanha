package la.meleva.fundraiser.identity;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByPublicId(String publicId);

    boolean existsByPhoneNumber(String phoneNumber);
}
