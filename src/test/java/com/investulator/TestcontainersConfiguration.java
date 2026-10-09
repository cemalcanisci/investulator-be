package com.investulator;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  // Same major/minor as the MySQL installed on the dev machine.
  private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:9.4");

  @Bean
  @ServiceConnection
  MySQLContainer mysqlContainer() {
    return new MySQLContainer(MYSQL_IMAGE);
  }
}
