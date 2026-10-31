package la.meleva.fundraiser.drawing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface DrawRepository extends JpaRepository<Draw,Long>{Optional<Draw> findByPublicId(String publicId);}
