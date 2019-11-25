import jade.core.Profile;

import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.core.Runtime;

public class Application {
	
	public static void main(String[] args) {
		// Setup JADE environment
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);
		try {
			// Start the agent controller which is itself an agent
			AgentController rma = myContainer.createNewAgent("rma","jade.tools.rma.rma", null);
			rma.start();
			// Now start your own BookBuyerAgent called buyer
			String[] books = {"Java"};
			AgentController myAgent = myContainer.createNewAgent("buyerA", BookBuyerAgent.class.getCanonicalName(), books);
			myAgent.start();
			AgentController buyerAgentB = myContainer.createNewAgent("buyerB", BookBuyerAgent.class.getCanonicalName(), books);
			buyerAgentB.start();
			// Start seller agents
			AgentController sellerAgentA = myContainer.createNewAgent("sellerA", BookSellerAgent.class.getCanonicalName(), null);
			sellerAgentA.start();
			/*AgentController sellerAgentB = myContainer.createNewAgent("sellerB", BookSellerAgent.class.getCanonicalName(), null);
			sellerAgentB.start();
			AgentController sellerAgentC = myContainer.createNewAgent("sellerC", BookSellerAgent.class.getCanonicalName(), null);
			sellerAgentC.start();
			AgentController sellerAgentD = myContainer.createNewAgent("sellerD", BookSellerAgent.class.getCanonicalName(), null);
			sellerAgentD.start();
			AgentController sellerAgentE = myContainer.createNewAgent("sellerE", BookSellerAgent.class.getCanonicalName(), null);
			sellerAgentE.start();*/
		} catch(Exception e){
			System.out.println("Exception starting agent: " + e.toString());
		}
	}

}
