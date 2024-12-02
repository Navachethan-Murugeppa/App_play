import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.actor.ActorRef;


/**
 * A simple actor that throws a RuntimeException for testing supervisor strategy.
 */
public class TestActor extends AbstractActor {
    private int restartCounter = 0;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, msg -> {
                    if (msg.equals("Test message") && restartCounter == 0) {
                        System.out.println("Received message: " + msg);
                        throw new RuntimeException("Test Exception");  // This triggers the supervisor's strategy
                    } else {
                        System.out.println("Received message: " + msg);

                        restartCounter++;
                        getSender().tell("Message received after restart", getSelf());
                    }
                })
                .build();
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        System.out.println("Actor started");
    }

    @Override
    public void postRestart(Throwable reason) throws Exception {
        super.postRestart(reason);
        System.out.println("Actor restarted");
    }
}
