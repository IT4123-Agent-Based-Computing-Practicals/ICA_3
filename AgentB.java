package ICA3;

import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class AgentB extends Agent {

    //find the ledger agent
    private AID findAgentFromDF(String agentType) {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription agentServiceDescription = new ServiceDescription();
            agentServiceDescription.setType(agentType);
            template.addServices(agentServiceDescription);
            DFAgentDescription[] availableAgents = DFService.search(this, template);
            if(availableAgents.length > 0) {
                AID agentId = availableAgents[0].getName();
                System.err.println("Agent B found ("+ agentType +")...");
                return agentId;
            }
            else {
                return null;
            }
        } 
        catch (FIPAException fe) {
            System.err.println("Error in agent finding...");
            return null;
        } 
    }

    //communication-send msg
    private void sendMessageFromClient(ACLMessage reply, AID recvId) {
        reply.addReceiver(recvId);//msg.addReceiver(getAID("test"));
        send(reply);
        System.out.println("client B send msg...");
    }
    
    //communication-receive msg base on the performative
    private void identifyIntensionAndRespond(ACLMessage msg) {
        switch(msg.getPerformative()) {
            case ACLMessage.INFORM:
                AID agentId = findAgentFromDF("ledger");
                System.out.println("Agent B got the token from the agent A " + msg.getContent() + "...");
                if(agentId != null) {
                    ACLMessage reply = new ACLMessage(ACLMessage.REQUEST);
                    reply.setContent(msg.getContent());
                    sendMessageFromClient(reply, agentId);
                }
                else {
                    System.out.println("Cannot communicate: Agent not found...");
                }
                break;
            case ACLMessage.CONFIRM:
                System.out.println("Token veryfy can proceed the transaction...");
                break;
            case ACLMessage.REFUSE:
                System.out.println("Token not veryfy cannot proceed the transaction...");
                break;
            default:
                System.out.print("No valied performative...");
        }
    }

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " is ready");

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if(msg != null) {
                    identifyIntensionAndRespond(msg);
                }
                else {
                    block();
                }
            }
        });
    }
}
