import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.core.Runtime;

public class Application {
	
	public static void main(String[] args) {
		//Setup the JADE environment
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);
		try {
			//Start the agent controller, which is itself an agent (rma)
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			
			// Tutorial
			// Now start our own SimpleAgent, called Fred
			/*AgentController myAgent = myContainer.createNewAgent("Fred", SimpleAgent.class.getCanonicalName(), null);
			myAgent.start();
			// Now start the TimerAgent, called Chronos
			AgentController myTimerAgent = myContainer.createNewAgent("Chronos", TimerAgent.class.getCanonicalName(), null);
			myTimerAgent.start();
			// Now start a TickerAgent, called Tickles
			AgentController myTickerAgent = myContainer.createNewAgent("Tickles", TickerAgent.class.getCanonicalName(), null);
			myTickerAgent.start();*/
			
			// Exercise 1,2 and 3
			/*String[] names = {"Thor", "Fred", "Jorge", "Manuel", "Ron", "Harry", "Hermione", "Rosalia", "Manolo", "Ruberto"};
			for(int i = 0; i < 10; i++)
			{
				AgentController myAgent = myContainer.createNewAgent(names[i], SimpleAgent.class.getCanonicalName(), null);
				myAgent.start();
			}*/
			
			// Exercise 5a
			/*AgentController myAgent = myContainer.createNewAgent("Fred", SimpleAgent2.class.getCanonicalName(), null);
			myAgent.start();*/
			
			// Exercise 5b
			/*AgentController myTimeAgent = myContainer.createNewAgent("Chronos", TimeAgent.class.getCanonicalName(), null);
			myTimeAgent.start();*/
			
			// Exercise 5c
			/*AgentController myFSMAgent = myContainer.createNewAgent("Terminator", FSMAgent.class.getCanonicalName(), null);
			myFSMAgent.start();*/
			
			// Exercise 5d
			AgentController myComplexAgent = myContainer.createNewAgent("Complexor", ComplexBehaviourAgent.class.getCanonicalName(), null);
			myComplexAgent.start();
			
			
			
		}catch(Exception e) {
			System.out.println("Exception starting agent: " + e.toString());
		}
	}

}
