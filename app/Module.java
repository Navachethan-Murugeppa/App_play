import com.google.inject.AbstractModule;
import play.libs.akka.AkkaGuiceSupport;
import actors.SupervisorActor;

/**
 * Module: Configures the application’s dependencies, specifically the SupervisorActor,
 * using Guice for dependency injection and Akka for actor management.
 *
 * <p>Author: Priyadarshine Kumar 40293041</p>
 */
public class Module extends AbstractModule implements AkkaGuiceSupport {

    /**
     * Configures the bindings for the application, specifically binding the SupervisorActor
     * to be injected by Guice and managed by Akka.
     */
    @Override
    protected void configure() {
        // Bind the SupervisorActor to the actor system with the name "supervisorActor"
        bindActor(SupervisorActor.class, "supervisorActor");
    }
}
