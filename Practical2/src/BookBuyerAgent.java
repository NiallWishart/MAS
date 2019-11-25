import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;

public class BookBuyerAgent extends Agent {
	// Title of the book to buy
	private String targetBookTitle;
	// The list of knwon seller agents
	private AID[] sellerAgents;
	
	// Agent initialization
	protected void setup() {
		// Print welcome message
		System.out.println("Hello! Buyer-agent " + this.getAID().getName()+" is ready.");
		// Get the title of the book to buy
		Object[] args = getArguments();
		if(args != null && args.length > 0) {
			targetBookTitle = (String) args[0];
			System.out.println("Trying to buy " + targetBookTitle);
			// Add ticker behaviour that schedules a request to seller agents every minute
			this.addBehaviour(new TickerBehaviour(this, 60000) {
				protected void onTick() {
					// Update the list of seller agents
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("book-selling");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						sellerAgents = new AID[result.length];
						for(int i = 0; i < result.length; ++i) {
							sellerAgents[i] = result[i].getName();
						}
					}catch (FIPAException fe) {
						fe.printStackTrace();
					}
					myAgent.addBehaviour(new RequestPerformer());
				}
			});
		}else {
			// Agent terminates when there is no book to buy
			System.out.println("No book title specified");
			doDelete();
		}
	}
	
	// Agent clean-up operations
	protected void takeDown() {
		// Printout dismissal message
		System.out.println("Buyer-agent " + this.getAID().getName() + " terminating.");
	}
	
	/*
	 * Innver class, this behaviour is used to request seller agents the target book
	 */
	private class RequestPerformer extends Behaviour{
		private AID bestSeller;		// AID of the agent that provides the best offer
		private int bestPrice;		// The best offered price
		private int repliesCnt = 0;	// The number of replies from seller agents
		private MessageTemplate mt;	// The template to receive replies
		private int step = 0;
		
		public void action() {
			switch(step) {
			case 0:
				// Send the CFP to all seller agents
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for(int i = 0; i < sellerAgents.length; ++i) {
					cfp.addReceiver(sellerAgents[i]);
				}
				cfp.setContent(targetBookTitle);
				cfp.setConversationId("book-trade");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if(reply != null) {
					// Reply received
					if(reply.getPerformative() == ACLMessage.PROPOSE) {
						// THis is an offer
						int price = Integer.parseInt(reply.getContent());
						if(bestSeller == null || price < bestPrice) {
							// This is the best offer at present
							bestPrice = price;
							bestSeller = reply.getSender();
						}
					}
					repliesCnt++;
					if(repliesCnt >= sellerAgents.length) {
						// All agents have replied
						step = 2;
					}
				}else {
					block();
				}
				break;
			case 2:
				// Send the purchase order to the seller that provided the best offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeller);
				order.setContent(targetBookTitle);
				order.setConversationId("book-trade");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if(reply != null) {
					// Purchase order reply received
					if(reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase succesful. We can terminate
						System.out.println(targetBookTitle+" succesfully purchased.");
						System.out.println("Price = " + bestPrice);
						myAgent.doDelete();
					}
					step = 4;
				} else {
					block();
				}
				break;
			}
		}
		
		public boolean done() {
			return ((step == 2 && bestSeller == null) || step == 4);
		}
	}

}
