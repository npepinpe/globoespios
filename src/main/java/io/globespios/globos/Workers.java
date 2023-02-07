package io.globespios.globos;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class Workers {
  private static final Logger LOG = LoggerFactory.getLogger(Workers.class);

  private final ZeebeClient client;

  @Autowired
  public Workers(final ZeebeClient client) {
    this.client = client;
  }

  @ZeebeWorker(
      type = "io.globespios.prepararItinerario",
      maxJobsActive = 30,
      fetchVariables = {"key", "itinerary", "reminderDelay"})
  public void prepareItinerary(final JobClient client, final ActivatedJob job) {
    final var request = job.getVariablesAsType(PrepareItinerary.class);
    LOG.info("Preparing itinerary for balloon with ID {}", request.key);

    final var parsedDelay = Duration.parse(request.reminderDelay);
    if (parsedDelay.compareTo(Duration.ofMinutes(5)) > 0) {
      LOG.debug("Clamping duration of reminder {} down to 5m", parsedDelay);
      CompletableFuture.delayedExecutor(1, TimeUnit.MINUTES)
          .execute(
              () ->
                  this.client
                      .newSetVariablesCommand(job.getProcessInstanceKey())
                      .variables(Map.of("recordarDemora", "5m"))
                      .send()
                      .join());
    }

    client.newCompleteCommand(job).send().join();
  }

  @ZeebeWorker(
      type = "io.globespios.prepararGlobo",
      maxJobsActive = 30,
      fetchVariables = {"key", "equipment", "reminderDelay"})
  public void prepareBalloon(final JobClient client, final ActivatedJob job) {
    final var request = job.getVariablesAsType(PrepareBalloon.class);
    LOG.info("Preparing equipment for balloon with ID {}", request.key);
    client.newCompleteCommand(job).send().join();
  }

  @ZeebeWorker(
      type = "io.globespios.recordar",
      maxJobsActive = 30,
      fetchVariables = {"key"})
  public void remind(final JobClient client, final ActivatedJob job) {
    final var request = job.getVariablesAsType(RemindConfirmation.class);
    LOG.info("Reminder to confirm launch for balloon with ID {}", request.key);
    client.newCompleteCommand(job).send().join();

    LOG.info("Confirming launch of balloon with ID {}", request.key);
    this.client
        .newPublishMessageCommand()
        .messageName("io.globespios.confirmar")
        .correlationKey(String.valueOf(request.key))
        .send()
        .join();
  }

  @ZeebeWorker(
      type = "io.globespios.lanzar",
      maxJobsActive = 30,
      fetchVariables = {"key", "equipment", "itinerary"})
  public void launch(final JobClient client, final ActivatedJob job) {
    final var request = job.getVariablesAsType(Launch.class);
    LOG.info(
        "Launching balloon with ID {}, equipment {}, and itinerary {}",
        request.key,
        request.equipment,
        request.itinerary);

    client.newCompleteCommand(job).send().join();
  }

  record PrepareItinerary(String key, List<String> itinerary, String reminderDelay) {}

  record PrepareBalloon(String key, List<String> equipment, String reminderDelay) {}

  record Launch(String key, List<String> equipment, List<String> itinerary) {}

  record RemindConfirmation(String key) {}
}
