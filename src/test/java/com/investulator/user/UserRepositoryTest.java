package com.investulator.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.investulator.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Test
  void savesUserWithGeneratedIdAndTimestamps() {
    User saved = userRepository.saveAndFlush(new User("user@example.com", "hash", "User"));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
  }

  @Test
  void storesEmailInLowerCase() {
    userRepository.saveAndFlush(new User("User@Example.COM", "hash", "User"));

    assertThat(userRepository.findByEmail("user@example.com"))
        .hasValueSatisfying(user -> assertThat(user.getEmail()).isEqualTo("user@example.com"));
  }

  @Test
  void reportsWhetherEmailExists() {
    userRepository.saveAndFlush(new User("user@example.com", "hash", "User"));

    assertThat(userRepository.existsByEmail("user@example.com")).isTrue();
    assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
  }

  @Test
  void rejectsDuplicateEmail() {
    userRepository.saveAndFlush(new User("user@example.com", "hash", "User"));

    assertThatThrownBy(
            () -> userRepository.saveAndFlush(new User("USER@example.com", "hash2", "Other")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void refreshesUpdatedAtOnUpdate() throws InterruptedException {
    User saved = userRepository.saveAndFlush(new User("user@example.com", "hash", "User"));
    Thread.sleep(5);

    saved.onUpdate();
    userRepository.saveAndFlush(saved);

    assertThat(saved.getUpdatedAt()).isAfter(saved.getCreatedAt());
    assertThat(saved.getPasswordHash()).isEqualTo("hash");
    assertThat(saved.getDisplayName()).isEqualTo("User");
  }
}
