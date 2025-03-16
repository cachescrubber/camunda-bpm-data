package io.holunda.camunda.bpm.data.example.process;

import io.holunda.camunda.bpm.data.builder.VariableMapBuilder;
import io.holunda.camunda.bpm.data.example.domain.Order;
import io.holunda.camunda.bpm.data.example.domain.OrderPosition;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.mock.Mocks;
import org.operaton.bpm.spring.boot.starter.test.helper.StandaloneInMemoryTestConfiguration;
import org.operaton.spin.plugin.impl.SpinProcessEnginePlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;

import static io.holunda.camunda.bpm.data.example.process.OrderApproval.Elements.*;
import static io.holunda.camunda.bpm.data.example.process.OrderApproval.*;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

@Deployment(resources = "order_approval.bpmn")
public class OrderApprovalProcessTest {

  @RegisterExtension
  public static final ProcessEngineExtension engine = ProcessEngineExtension
    .builder()
    .useProcessEngine(new StandaloneInMemoryTestConfiguration(new SpinProcessEnginePlugin()).buildProcessEngine())
    .build();

  private OrderApprovalInstanceFactory factory;


  @BeforeEach
  public void register() {
    factory = new OrderApprovalInstanceFactory(engine.getRuntimeService());
    OrderApproval config = new OrderApproval();
    MockOrderApproval approval = new MockOrderApproval();
    Mocks.register("guardExecutionListener", config.guardExecutionListener());
    Mocks.register("guardTaskListener", config.guardTaskListener());
    Mocks.register("loadOrder", approval.loadOrder());
    Mocks.register("writeOrderTotal", approval.writeOrderTotal());
    Mocks.register("calculateOrderPositions", approval.calculateOrderPositions());
  }

  @Test
  public void shouldDeploy() {
    // empty method body checks deployment
  }

  @Test
  public void shouldStartAsync() {
    OrderApprovalInstance instance = factory.start("1");

    assertThat(instance.get()).isStarted();
    assertThat(instance.get()).isWaitingAt(element(start_order_created));
  }

  @Test
  public void shouldStartAndWaitInUserTask() {
    OrderApprovalInstance instance = factory.start("1");

    assertThat(instance.get()).isStarted();

    // pass async on start
    execute(job());

    assertThat(instance.get()).isWaitingAt(element(user_approve_order));
  }

  @Test
  public void shouldStartAndWaitInUserTaskAndApprove() {
    OrderApprovalInstance instance = factory.start("1");

    assertThat(instance.get()).isStarted();

    // pass async on start
    execute(job());

    // complete user task
    complete(task(), new VariableMapBuilder().set(ORDER_APPROVED, true).build());
    // pass async oafter user task
    execute(job());

    assertThat(instance.get()).isEnded();
    assertThat(instance.get()).hasPassed(element(end_order_approved));
  }

  @Test
  public void shouldStartAndWaitInUserTaskAndReject() {
    OrderApprovalInstance instance = factory.start("1");

    assertThat(instance.get()).isStarted();

    // pass async on start
    execute(job());

    // complete user task
    complete(task(), new VariableMapBuilder().set(ORDER_APPROVED, false).build());
    // pass async after user task
    execute(job());

    assertThat(instance.get()).isEnded();
    assertThat(instance.get()).hasPassed(element(end_order_rejected));
  }


  /**
   * Stub for the test.
   */
  static class MockOrderApproval {
    public JavaDelegate loadOrder() {
      return execution -> {
        ORDER.on(execution).set(new Order("1", Date.from(Instant.now()), Arrays.asList(
          new OrderPosition("Pencil", BigDecimal.valueOf(1.99), 3L),
          new OrderPosition("Sheet", BigDecimal.valueOf(0.17), 3L)
        )));
      };
    }

    public JavaDelegate calculateOrderPositions() {
      return execution -> {
        ORDER_TOTAL.on(execution).set(BigDecimal.valueOf(6.48));
      };
    }

    public JavaDelegate writeOrderTotal() {
      return execution -> {
      };
    }
  }

}
