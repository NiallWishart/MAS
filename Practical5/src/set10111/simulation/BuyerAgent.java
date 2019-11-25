package set10111.simulation;

import java.util.ArrayList;
import java.util.HashMap;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BuyerAgent extends Agent {
	private ArrayList<AID> sellers = new ArrayList<>();
	private ArrayList<String>  booksToBuy = new ArrayList<>();
	private HashMap<String,ArrayList<Offer>> currentOffers = new HashMap<>();
	private AID tickerAgent;
	private int numQueriesSent;
	private int totalSpent;
	@Override
	protected void setup() {
		// Initial total spend
		totalSpent = 0;
		//add this agent to the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("buyer");
		sd.setName(getLocalName() + "-buyer-agent");
		dfd.addServices(sd);
		try{
			DFService.register(this, dfd);
		}
		catch(FIPAException e){
			e.printStackTrace();
		}
		//add books to buy
		booksToBuy.add("Java for Dummies");
		booksToBuy.add("JADE: the Inside Story");
		booksToBuy.add("Multi-Agent Systems for Everybody");
		booksToBuy.add("The Witcher");
		
		addBehaviour(new TickerWaiter(this));
	}


	@Override
	protected void takeDown() {
		//Deregister from the yellow pages
		try{
			DFService.deregister(this);
		}
		catch(FIPAException e){
			e.printStackTrace();
		}
	}

	public class TickerWaiter extends CyclicBehaviour {

		//behaviour to wait for a new day
		public TickerWaiter(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
					MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt); 
			if(msg != null) {
				if(tickerAgent == null) {
					tickerAgent = msg.getSender();
				}
				if(msg.getContent().equals("new day")) {
					//spawn new sequential behaviour for day's activities
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					//sub-behaviours will execute in the order they are added
					dailyActivity.addSubBehaviour(new FindSellers(myAgent));
					dailyActivity.addSubBehaviour(new SendEnquiries(myAgent));
					dailyActivity.addSubBehaviour(new CollectOffers(myAgent));
					dailyActivity.addSubBehaviour(new ManageOffers());
					dailyActivity.addSubBehaviour(new ConfirmPurchases());
					//dailyActivity.addSubBehaviour(new EndDay(myAgent));
					myAgent.addBehaviour(dailyActivity);
				}
				else {
					// Print message
					System.out.println("Total amount of money spent is " + Integer.toString(totalSpent) +".");
					// Penalties for books not bought
					int penalty = 0;
					if(!booksToBuy.isEmpty()) {
						for(String book : booksToBuy) {
							System.out.println("Book " + book + " not bought.");
							penalty += 30;
						}
					}
					System.out.println("Penalty for books left to buy: " + Integer.toString(penalty) + ".");
					//termination message to end simulation
					myAgent.doDelete();
				}
			}
			else{
				block();
			}
		}

	}

	public class FindSellers extends OneShotBehaviour {

		public FindSellers(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			DFAgentDescription sellerTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("seller");
			sellerTemplate.addServices(sd);
			try{
				sellers.clear();
				DFAgentDescription[] agentsType1  = DFService.search(myAgent,sellerTemplate); 
				for(int i=0; i<agentsType1.length; i++){
					sellers.add(agentsType1[i].getName()); // this is the AID
				}
			}
			catch(FIPAException e) {
				e.printStackTrace();
			}

		}

	}

	public class SendEnquiries extends OneShotBehaviour {

		public SendEnquiries(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			//send out a call for proposals for each book
			numQueriesSent = 0;
			for(String bookTitle : booksToBuy) {
				ACLMessage enquiry = new ACLMessage(ACLMessage.CFP);
				enquiry.setContent(bookTitle);
				enquiry.setConversationId(bookTitle);
				for(AID seller : sellers) {
					enquiry.addReceiver(seller);
					numQueriesSent++;
				}
				myAgent.send(enquiry);
				
			}

		}
	}

	public class CollectOffers extends Behaviour {
		private int numRepliesReceived = 0;
		
		public CollectOffers(Agent a) {
			super(a);
			currentOffers.clear();
		}

		
		@Override
		public void action() {
			boolean received = false;
			for(String bookTitle : booksToBuy) {
				MessageTemplate mt = MessageTemplate.MatchConversationId(bookTitle);
				ACLMessage msg = myAgent.receive(mt);
				if(msg != null) {
					received = true;
					numRepliesReceived++;
					if(msg.getPerformative() == ACLMessage.PROPOSE) {
						//we have an offer
						//the first offer for a book today
						if(!currentOffers.containsKey(bookTitle)) {
							ArrayList<Offer> offers = new ArrayList<>();
							offers.add(new Offer(msg.getSender(),
									Integer.parseInt(msg.getContent())));
							currentOffers.put(bookTitle, offers);
						}
						//subsequent offers
						else {
							ArrayList<Offer> offers = currentOffers.get(bookTitle);
							offers.add(new Offer(msg.getSender(),
									Integer.parseInt(msg.getContent())));
						}
							
					}

				}
			}
			if(!received) {
				block();
			}
		}

		

		@Override
		public boolean done() {
			return numRepliesReceived == numQueriesSent;
		}

		@Override
		public int onEnd() {
			//print the offers
			for(String book : booksToBuy) {
				if(currentOffers.containsKey(book)) {
					ArrayList<Offer> offers = currentOffers.get(book);
					for(Offer o : offers) {
						System.out.println(book + "," + o.getSeller().getLocalName() + "," + o.getPrice());
					}
				}
				else {
					System.out.println("No offers for " + book);
				}
			}
			return 0;
		}

	}
	
	public class ManageOffers extends OneShotBehaviour{
		public void action() {
			// Set the number of queries sent to 0
			numQueriesSent = 0;
			// Go over all the books to buy
			for(String book : booksToBuy) {
				// Check if there are offers for the current book
				if(!currentOffers.isEmpty() && currentOffers.containsKey(book)) {
					AID bestSeller = null;
					int bestPrice = 0;
					ArrayList<AID> rejectedSellers = new ArrayList<AID>();
					// Find the best seller between all the offers
					for(Offer offer : currentOffers.get(book)) {
						AID currentSeller = offer.getSeller();
						int currentPrice = offer.getPrice();
						if(bestSeller == null) {
							bestSeller = currentSeller;
							bestPrice = currentPrice;
							continue;
						} else if(bestPrice > currentPrice) {
							// Add previous best seller to the rejected sellers list
							rejectedSellers.add(bestSeller);
							bestSeller = currentSeller;
							bestPrice = currentPrice;
							continue;
						}
						// Current seller is worse than best seller, add it to the rejected sellers list
						rejectedSellers.add(currentSeller);
					}
					// Send an accept proposal to the best seller and reject proposal to the rest
					ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					accept.setConversationId(book);
					accept.addReceiver(bestSeller);
					myAgent.send(accept);
					// Increase the number of accept proposals sent
					numQueriesSent++;
					// If there was only one seller then there won't be any rejected sellers
					if(!rejectedSellers.isEmpty()) {
						ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
						reject.setConversationId(book);
						for(AID seller : rejectedSellers) {
							reject.addReceiver(seller);
						}
						myAgent.send(reject);
					}
				}
			}
		}
	}
	
	public class ConfirmPurchases extends Behaviour{
		int numRepliesReceived = 0;
		public void action() {
			// Listen to message from sellers that will confirm the purchase
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchContent("purchase successful"));
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				// Get the book being purchased
				String book = msg.getConversationId();
				// Find the offer that corresponds to the sender of the message
				int price = 0;
				for(Offer offer : currentOffers.get(book)) {
					if(offer.getSeller().equals(msg.getSender())) {
						price = offer.getPrice();
					}
				}
				// Print message
				System.out.println("Book " + book + " purchased by " + Integer.toString(price) + " from " + msg.getSender().getName() + ".");
				// Increase number of replies received
				numRepliesReceived++;
				// Remove book from lists of books to buy
				booksToBuy.remove(book);
				// Add book cost to total 
				totalSpent += price;
			} else {
				block();
			}
		}
		public boolean done() {
			return numRepliesReceived == numQueriesSent;
		}
		
		public int onEnd() {
			myAgent.addBehaviour(new EndDay(myAgent));
			return 0;
		}
	}
	
	
	
	public class EndDay extends OneShotBehaviour {
		
		public EndDay(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(tickerAgent);
			msg.setContent("done");
			myAgent.send(msg);
			//send a message to each seller that we have finished
			ACLMessage sellerDone = new ACLMessage(ACLMessage.INFORM);
			sellerDone.setContent("done");
			for(AID seller : sellers) {
				sellerDone.addReceiver(seller);
			}
			myAgent.send(sellerDone);
		}
		
	}

}





