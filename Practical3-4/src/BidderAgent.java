import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BidderAgent extends Agent{
	// The list with the items that the bidder has to buy
	private ArrayList<Item> itemsToBuy;
	// The auctioneer that sells the items
	private AID auctioneer;
	// Bidder gui to add items
	private BidderGui myGui;
	
	protected void setup() {
		// Welcome message
		System.out.println("Hello! Bidder-Agent " + this.getAID().getName() + " is ready.");
		// Initialise list of items and auctioneer AID
		itemsToBuy = new ArrayList<Item>();
		auctioneer = new AID();
		// Create and show GUI
		myGui = new BidderGui(this);
		myGui.showGui();
		// Add Behaviours
		this.addBehaviour(new FindAuctioneerBehaviour());
		this.addBehaviour(new ItemRequestBehaviour());
	}
	
	protected void takeDown() {
		// Print bye message
		System.out.println("Bidder agent " + this.getAID().getName() + " terminating.");
		// Close the GUI
		myGui.dispose();
	}
	
	// Method called from the GUI to add items to the array list
	public void AddItemToBuy(final String name, final int price) {
		Item item = new Item(name,price);
		itemsToBuy.add(item);
	}
	
	// Behaviour that is going to look for the Auctioneer until it finds it
	private class FindAuctioneerBehaviour extends Behaviour{
		public void action() {
			// Search for auctioneer in the DF service
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("auctioneer");
			template.addServices(sd);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				if(result.length > 0) {
					auctioneer = result[0].getName();
				}
			} catch(FIPAException fe) {
				fe.printStackTrace();
			}	
		}
		public boolean done() {
			if(!auctioneer.getName().isEmpty()) {
				// Once the auctioneer is found resgister on it
				myAgent.addBehaviour(new RegisterMeBehaviour());
				return true;
			}
			return false;
		}
		
	}
	
	// One shot behaviour that is going to send an inform message to the auctioneer to register
	private class RegisterMeBehaviour extends OneShotBehaviour{
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(auctioneer);
			msg.setContent("Register");
			msg.setConversationId("Registration");
			msg.setReplyWith("inform" + System.currentTimeMillis());
			myAgent.send(msg);
			System.out.println("Bidder-Agent " + myAgent.getAID().getName() + " starting registration.");
		}
	}
	
	// Behaviour that is going to handle whether the bidder wants an item or not
	private class ItemRequestBehaviour extends CyclicBehaviour{
		public void action() {
			// Inform messages the auctioneer sends with the name of the item to bid
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				boolean itemFound = false;
				ACLMessage reply = msg.createReply();
				// Check if the content of the message is one of the items of the bidder list
				for(int i = 0; i < itemsToBuy.size(); i++) {
					if(itemsToBuy.get(i).Name.equals(msg.getContent())) {
						// If item is found agree to participate in the auction
						System.out.println("Agent " + myAgent.getAID().getName() + " wants this item.");
						reply.setPerformative(ACLMessage.AGREE);
						reply.setContent("AgreeToParticipate");
						itemFound = true;
						// Add the behaviour that is going to handle the auction only to those bidders that want to participate
						myAgent.addBehaviour(new BiddingBehaviour(i));
						break;
					}
				}
				// If the items is not found refuse to participate in the auction
				if(!itemFound) {
					System.out.println("Agent " + myAgent.getAID().getName() + " does not want this item.");
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("RefuseToParticipate");
				}
				myAgent.send(reply);
			} else {
				block();
			}
		}
	}
	
	// Behaviour that is going to handle the auction process
	private class BiddingBehaviour extends Behaviour{
		private int currentItem;		// Index of the item that is being bidded in the itemsToBuy list
		private boolean quitAuction;	// Boolean that is going to manage when the behaviour ends
		
		public BiddingBehaviour(int item) {
			currentItem = item;
			quitAuction = false;
		}
		
		public void action() {
			ACLMessage msg = myAgent.receive();
			if(msg != null) {
				// CFP messages are regarding the price of the item that is being bid
				if(msg.getPerformative() == ACLMessage.CFP) {
					ACLMessage reply = msg.createReply();
					// Get the current item price being bidded
					int currentItemPrice = Integer.parseInt(msg.getContent());
					int myMaxPrice = itemsToBuy.get(currentItem).MaxPrice;
					// Check if bidder can afford the item
					if(currentItemPrice > myMaxPrice) {
						// Refuse to continue the bid
						reply.setPerformative(ACLMessage.REFUSE);
						reply.setContent("RefuseToPayTheItemPrice");
						quitAuction = true;
						
					} else {
						// Place a bid between the currentItemPrice and the MaxPrice
						int priceToBidNext = (myMaxPrice + currentItemPrice) / 2;
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent(Integer.toString(priceToBidNext));
					}
					myAgent.send(reply);
				} else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
					// ACCEPT_PROPOSAL message declares the winner of the auction
					String itemName = itemsToBuy.get(currentItem).Name;
					System.out.println("Agent " + myAgent.getAID().getName() + " has bought " + itemName + " succesfully.");
					System.out.println("Agent " + myAgent.getAID().getName() + " removing " + itemName + " from list of items to buy.");
					System.out.println();
					itemsToBuy.remove(currentItem);
					quitAuction = true;
				}

			}else {
				block();
			}
		}
		
		public boolean done() {
			if(quitAuction) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	// Each item has a name and a price
	private class Item{
		public String Name;
		public int MaxPrice;
		
		public Item(String name, int price) {
			Name = name;
			MaxPrice = price;
		}
	}
}
