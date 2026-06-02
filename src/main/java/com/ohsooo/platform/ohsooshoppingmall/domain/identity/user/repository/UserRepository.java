package com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUserIdAndIsDeletedFalse(Long userId);
}
