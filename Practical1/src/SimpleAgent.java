import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import java.util.concurrent.ThreadLocalRandom;

public class SimpleAgent extends Agent {
	
	// Count down for the agent to call its name
	int counter = 10;
	// Number between 60 and 120 (1 and 2 minutes)
	int deleteCounter;
	
	// This method is called when the agent is launched
	protected void setup() {
		// Set the deleteCounter to a random value between 60 and 120
		deleteCounter = ThreadLocalRandom.current().nextInt(60, 120 + 1);
		// Create a new TickerBehaviour
				addBehaviour(new TickerBehaviour(this, 1000){
					// Call onTick every 1000ms
					protected void onTick() {
						// Count down
						if(counter > 0) {
							counter--;
						} else {
							// Print out a welcome message
							System.out.println("Hello! Agent " + getAID().getName()+ " is ready.");
							counter = 10;
						}
						if(deleteCounter > 0) {
							deleteCounter--;
						} else {
							// Delete the agent
							System.out.println("Agent " + getAID().getName()+ " is deleted.");
							myAgent.doDelete();
						}
					}
					
				});
	}

}
