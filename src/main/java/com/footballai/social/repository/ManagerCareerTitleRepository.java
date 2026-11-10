package com.footballai.social.repository;
import com.footballai.social.model.entity.ManagerCareerTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ManagerCareerTitleRepository extends JpaRepository<ManagerCareerTitle,UUID> {
    List<ManagerCareerTitle> findAllByManagerIdIn(List<UUID> managerIds);
    List<ManagerCareerTitle> findAllByManagerIdOrderByAwardedOnDesc(UUID managerId);
}
