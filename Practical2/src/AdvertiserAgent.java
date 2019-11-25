

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AdvertiserAgent extends Agent {
	// Catalogue of books
	private Hashtable catalogue;
	// Gui for the advertiser
	
	// Agent initialisation
	public void setup() {
		catalogue = new Hashtable();
		
		// Register advertiser in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("advertiser");
		sd.setName("JADE-book-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	// Behaviour that is going to listen for notifications from seller
	private class SellerNotification extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage mes = myAgent.receive(mt);
			if(mes != null) {
				// Add the seller ID, book title and book price to the catalogue
				String[] content = mes.getContent().split(",");
				String sellerAID = mes.getSender().getName();
				// If the seller AID is already in the catalogue
				if(catalogue.containsKey(sellerAID)) {
					SellerCatalogue updatedCatalogue = (SellerCatalogue) catalogue.get(sellerAID);
					// A check should be done to see if the book is already added
					updatedCatalogue.BooksAndPrices.put(content[0], content[1]);
					catalogue.replace(sellerAID, updatedCatalogue);
				} else {
					SellerCatalogue sellerCatalogue = new SellerCatalogue();
					sellerCatalogue.BooksAndPrices.put(content[0], content[1]);
					catalogue.put(sellerAID, sellerCatalogue);
				}
			} else {
				block();
			}
		}
	}
	
	// Behaviour that is going to listen for CFP from buyers
	private class BuyerRequestBook extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage mes = myAgent.receive(mt);
			if(mes != null) {
				// Get book title from message
				ACLMessage reply = mes.createReply();
				String bookToFind = mes.getContent();
				// Find the book in the catalogue
				Iterator catalogueIterator = catalogue.entrySet().iterator();
				String bestSeller = "";
				int bestPrice = 0;
				while(catalogueIterator.hasNext()) {
					Map.Entry element = (Map.Entry)catalogueIterator.next();
					// Check all the sellers
					String currentSeller = (String) element.getKey();
					SellerCatalogue currentCatalogue = (SellerCatalogue) element.getValue();
					int currentPrice = (Integer) currentCatalogue.BooksAndPrices.get(bookToFind);
					if(currentCatalogue.BooksAndPrices.contains(bookToFind)) {
						// Stablish the best seller
						if(bestSeller.isEmpty() || bestPrice > currentPrice) {
							bestSeller = currentSeller;
							bestPrice = currentPrice;
						}
					}
				}
				// If the book has not been found send a refuse message
				if(bestSeller.isEmpty()) {
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				} else {
					// Send the AID of the seller and the book price
					reply.setPerformative(ACLMessage.PROPOSE);
					String sellerInformation = bestSeller +"," + bestPrice;
					reply.setContent(sellerInformation);
				}
			}
		}
	}
	
	// Agent clean-up
	public void takeDown() {
		// Deregister from the yellow pages
		try {
				DFService.deregister(this);
		}catch (FIPAException fe){
				fe.printStackTrace();
		}
	}
	
	// Inner class Book
	private class SellerCatalogue{
		public Hashtable BooksAndPrices;
	}

}
