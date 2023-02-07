package io.globespios.globos;

import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeDeployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackageClasses = ProcessApplication.class)
@EnableZeebeClient
@ZeebeDeployment(resources = "classpath*:/models/*.*")
public class ProcessApplication {
  public static void main(String[] args) {
    SpringApplication.run(ProcessApplication.class, args);
  }
}
