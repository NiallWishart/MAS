import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AuctioneerAgent extends Agent {
	// The items that the auctioneer needs to sell
	private ArrayList<Item> catalogue;
	// List with all the bidders
	private ArrayList<AID> bidders;
	// Auctioneer gui
	private AuctioneerGui myGui;
	
	protected void setup() {
		// Welcome message
		System.out.println("Hello! Auctioneer-agent" + this.getAID().getName() + " is ready.");
		// Initialise bidders and catalogue
		catalogue = new ArrayList<Item>();
		bidders = new ArrayList<AID>();
		// Load catalogue of items, items come from the args
		Object[] args = this.getArguments();
		for(int i = 0; i < args.length; i++) {
			// Each args is form by the name and the starting price separated by " "
			String[] content = args[i].toString().split(" ");
			// The ID of each item is going to be i
			Item itm = new Item(content[0], Integer.toString(i), Integer.parseInt(content[1]));
			// Add the item to the array list
			catalogue.add(itm);
		}
		// Create and show gui
		myGui = new AuctioneerGui(this);
		myGui.showGui();
		// Register auctioneer with the DF agent
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(this.getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("auctioneer");
		sd.setName("Item-bid-selling");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}catch (FIPAException fe) {
			fe.printStackTrace();
		}
		this.addBehaviour(new RegisterBidderBehaviour());
	}
	
	protected void takeDown() {
		// Print bye message
		System.out.println("Auctioner agent " + this.getAID().getName() + " terminating.");
		// Close the GUI
		myGui.dispose();
	}
	
	// Method called from Auctioneer GUI to start an auction
	public void StartAuction() {
		this.addBehaviour(new StartAuctionBehaviour());
	}
	
	// Behaviour that is going to add bidders to the list
	private class RegisterBidderBehaviour extends CyclicBehaviour {
		public void action() {
			// Inform messages from bidders mean register
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage message = myAgent.receive(mt);
			if(message != null) {
				if(message.getContent().equals("Register")) {
					// Get the AID of the sender and add it to the bidders list
					bidders.add(message.getSender());
					System.out.println("Bidder " + message.getSender().getName() + " registered.");
				}
			}else {
				block();
			}
		}
	}
	
	// This behaviour is going to send the next item to bid to the bidders
	private class StartAuctionBehaviour extends OneShotBehaviour {
		public void action() {
			// Create message with the first item on the itemslist and send it to all the bidders
			ACLMessage inf = new ACLMessage(ACLMessage.INFORM);
			for(int i = 0; i < bidders.size(); i++) {
				inf.addReceiver(bidders.get(i));
			}
			inf.setContent(catalogue.get(0).Name);
			inf.setConversationId("item");
			inf.setReplyWith("inform" + System.currentTimeMillis());
			myAgent.send(inf);
			System.out.println("Bid for item " + catalogue.get(0).Name + " starting.");
			// Auction behaviour
			myAgent.addBehaviour(new AuctionBehaviour());
		}
	}
	
	// Behaviour that handles the auction
	private class AuctionBehaviour extends Behaviour{
		private ArrayList<AID> 	participants = new ArrayList<AID>();	// AID of the bidders that are going to participate in the auction
		private AID highestBidder;										// Highest bidder in the last bid
		private int highestBid;											// Highest price in the last bid
		private int currentPrice = catalogue.get(0).StartPrice; 		// Current price for the item
		private int repliesCnt = 0;
		private int step = 0;
		public void action() {
			switch(step) {
			case(0):
				// Receive replies from bidders
				ACLMessage reply = myAgent.receive();
				if(reply != null) {
					if(reply.getPerformative() == ACLMessage.AGREE || reply.getPerformative() == ACLMessage.REFUSE) {
						repliesCnt++;
					}
					// If the bidder agrees to participate in the auction then add him to the list of participants
					if(reply.getPerformative() == ACLMessage.AGREE) {
						participants.add(reply.getSender());
						System.out.println("Bidder " + reply.getSender().getName() + " wants to participate in the auction.");
					}
				}
				// All bidders have replied
				if(repliesCnt >= bidders.size()) {
					step = 1;
					repliesCnt = 0;
				}
				break;
			case(1):
				// Send to bidders that want to participate in the auction the current price of the item
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for(int i = 0; i < participants.size(); i++) {
					cfp.addReceiver(participants.get(i));
				}
				cfp.setContent(Integer.toString(currentPrice));
				cfp.setConversationId("Price");
				cfp.setReplyWith("Price" + System.currentTimeMillis());
				myAgent.send(cfp);
				System.out.println("Current bid price for item " + catalogue.get(0).Name + " is " + Integer.toString(currentPrice) + ".");
				step = 2;
				break;
			case(2):
				// Receive response from bidders
				ACLMessage bidReply = myAgent.receive();
				if(bidReply != null) {
					// Bidder can afford current price
					if(bidReply.getPerformative() == ACLMessage.PROPOSE) {
						int bidPriceReply = Integer.parseInt(bidReply.getContent());
						// Update highest bid and highest bidder
						if(bidPriceReply > highestBid || highestBidder == null) {
							highestBid = bidPriceReply;
							highestBidder = bidReply.getSender();
						}
						repliesCnt++;
						// Bidder cannot afford current price
					} else if(bidReply.getPerformative() == ACLMessage.REFUSE) {
						// Remove bidder from participants
						participants.remove(bidReply.getSender());
						repliesCnt++;
					}
				}
				// All participants have replied
				if(repliesCnt >= participants.size()) {
					if(participants.size() >1) {
						// If more than one bidders remains update current price to be the highest bid and repeat the process
						currentPrice = highestBid;
						step = 1;
					} else {
						step = 3;
					}
				}
				break;
			case(3):
				// Send message to highest bidder about having bought the item
				ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				accept.addReceiver(highestBidder);
				accept.setContent(catalogue.get(0).Name);
				accept.setConversationId("auction");
				accept.setReplyWith("accept"+System.currentTimeMillis());
				myAgent.send(accept);
				step = 4;
				break;
			}
		}
		public boolean done() {
			// No bidder wants the item
			if(step == 1 && participants.isEmpty()) {
				System.out.println("No Bidder wants item " + catalogue.get(0).Name + ".");
			} else if(step == 3 && highestBidder == null) {
				System.out.println("No bidder can pay the starting price of item " + catalogue.get(0).Name + ".");
			} else if (step == 4) {
				System.out.println("Item " + catalogue.get(0).Name + " sold to bidder " + highestBidder.getName() + ".");
			} else {
				return false;
			}
			// Remove first item from list (current item being bidded)
			catalogue.remove(0);
			System.out.println("Removing item from catalogue.");
			// If there are more items to sell, start a new auction
			if(!catalogue.isEmpty()) {
				System.out.println("Auction finished.");
				System.out.println();
			} else {
				System.out.println("No more items to sell on the catalogue.");
				myAgent.doDelete();
			}
			return true;
		}
	}
	
	
	
	// Each item that the auctioneer stores has a name and an ID
	private class Item{
		public String Name;
		public String ID;
		public int StartPrice;
		
		public Item(String name, String id, int startPrice) {
			Name = name;
			ID = id;
			StartPrice = startPrice;
		}
	}

}
