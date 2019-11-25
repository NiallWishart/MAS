import java.util.concurrent.TimeUnit;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;

public class TickerAgent extends Agent{
	
	// Get the current time - this will be the time that the agents was lunched at
	long t0 = System.currentTimeMillis();
	
	Behaviour loop;
	protected void setup()
	{
		System.out.println(t0);
		loop = new TickerBehaviour( this, 300) {
			protected void onTick()
			{
				// Print elapsed time since launch
				System.out.println(System.currentTimeMillis()-t0 + ": " + myAgent.getLocalName());
				// Delete agent after running 1 minute
				if(TimeUnit.MILLISECONDS.toSeconds((System.currentTimeMillis()-t0)) >= 60)
				{
					myAgent.doDelete();
				}
			}
		};
		addBehaviour(loop);
	}
}
