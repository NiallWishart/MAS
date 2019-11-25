import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.core.Runtime;

public class Application {
	
	public static void main(String[] args) {
		// Setup jade environment
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);
		try {
			// Start agent controller
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			// Read CSV file with the items for the auction
			File csvFile = new File("C:\\Users\\niall_000\\Downloads\\Book1.csv");
			BufferedReader br = new BufferedReader(new FileReader(csvFile));
			String line ="";
			boolean firstLine = true;
			ArrayList<String> items = new ArrayList<String>();
			// Store items from csv into array
			while((line = br.readLine()) != null) {
				// The first line are not items
				if(!firstLine) {
					String[] lineContent = line.split(",");
					items.add(lineContent[1] + " " + lineContent[2]);
				}
				firstLine = false;
			}
			// Start auctioneer agent
			AgentController auctioneer = myContainer.createNewAgent("auctioneer", AuctioneerAgent.class.getCanonicalName(), items.toArray());
			auctioneer.start();
			// Start bidder agents
			AgentController bidderA = myContainer.createNewAgent("bidderA", BidderAgent.class.getCanonicalName(), null);
			bidderA.start();
			AgentController bidderB = myContainer.createNewAgent("bidderB", BidderAgent.class.getCanonicalName(), null);
			bidderB.start();
			AgentController bidderC = myContainer.createNewAgent("bidderC", BidderAgent.class.getCanonicalName(), null);
			bidderC.start();
			
			
		} catch(Exception e) {
			System.out.println("Exception starting agnet: " + e.toString());
		}
	}
}
