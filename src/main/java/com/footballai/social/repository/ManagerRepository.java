package com.footballai.social.repository;
import com.footballai.social.model.entity.Manager;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;
public interface ManagerRepository extends JpaRepository<Manager,UUID>, JpaSpecificationExecutor<Manager> { }
