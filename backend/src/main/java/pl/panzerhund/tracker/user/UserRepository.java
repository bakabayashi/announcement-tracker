package pl.panzerhund.tracker.user;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.panzerhund.tracker.user.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByEmail(String email);
}
