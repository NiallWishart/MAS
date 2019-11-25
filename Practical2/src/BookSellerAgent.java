import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class BookSellerAgent extends Agent{
	// Catalogue of books for sales (maps title to price)
	private Hashtable catalogue;
	// Stock of the books
	private Hashtable stock;
	//  The GUI that can be used by the user to add books in the catalogue
	private BookSellerGui myGui;
	
	// Agent initialization
	protected void setup() {
		// Create the catalogue
		catalogue = new Hashtable();
		// Create the stock 
		stock = new Hashtable();
		// Create and show the GUI
		myGui = new BookSellerGui(this);
		myGui.showGui();
		
		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-selling");
		sd.setName("JADE-book-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		// Add the behaviour serving requests for offer from buyer agents
		addBehaviour(new OfferRequestServer());
		
		// Add the behaviour serving purchase orders from buyers agents
		addBehaviour(new PurchaseOrdersServer());
	}
	
	// Agent clean-up
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}catch (FIPAException fe){
			fe.printStackTrace();
		}
		// Close the GUI
		myGui.dispose();
		// Print dismissal message
		System.out.println("Seller-agent " + getAID().getName() + " terminating:");
	}
	
	// Invoked by the GUI when the user adds a new book for sale
	public void updateCatalogue(final String title, final int price, final int stockNumber) {
		addBehaviour(new OneShotBehaviour () {
			public void action() {
				catalogue.put(title,  new Integer(price));
				if(stockNumber > 0) {
					stock.put(title, new Integer(stockNumber));
				} else {
					stock.put(title, new Integer(1));
				}
			}
		});
	}
	
	/*
	 * Inner class, this is the behaviour to serve incoming requests for offer from buyer agents. If the requested book
	 * is in the local catalogue the seller agent replies with a PROPOSE messsage specifying the price. Otherwise, a REFUSE
	 * message is sent back.
	 */

	private class OfferRequestServer extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				// Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();
				
				Integer price = (Integer) catalogue.get(title);
				if(price != null) {
					// Requested book is available for sale
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}else {
					// Requested book is not available for sale
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}else {
				block();
			}
		}
		
	}
	
	private class PurchaseOrdersServer extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				// ACCEPT_PROPOSAL message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();
				Integer price = (Integer) catalogue.get(title);
				stock.replace(title, new Integer((Integer) stock.get(title) - 1));
				// Check the stock
				if((Integer) stock.get(title) == 0) {
					catalogue.remove(title);
				}
				if(price != null) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(title+" sold to agent " + msg.getSender().getName());
				}else {
					// The requested book has been sold to another buyer in the meanwhile
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}else {
				block();
			}
		}
	}

}
