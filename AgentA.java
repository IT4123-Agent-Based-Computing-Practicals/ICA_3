package ICA3;

import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class AgentA extends Agent {

    //find the ledger agent
    private AID findAgentFromDF(String agetType) {
        try {
            DFAgentDescription agentDescriptionTemplate = new DFAgentDescription();
            ServiceDescription agentServiceDescription = new ServiceDescription();
            agentServiceDescription.setType(agetType);
            agentDescriptionTemplate.addServices(agentServiceDescription);
            DFAgentDescription[] availableAgents = DFService.search(this, agentDescriptionTemplate);
            if(availableAgents.length > 0) {
                AID agentId = availableAgents[0].getName();
                System.err.println("Agent A found (" + agetType + ")...");
                return agentId;
            }
            else {
                System.err.println("Agent not found...");
                return null;
            }
        } 
        catch (FIPAException fe) {
            System.err.println("Error in agent finding...");
        }
        return null;
    }

    //communication-send msg
    private void sendMessageFromClient(ACLMessage reply, AID recvId) {
        reply.addReceiver(recvId);//msg.addReceiver(getAID("test"));
        send(reply);
        System.out.println("client A send msg...");
    }

    //communication-receive msg base on the performative
    private void identifyIntensionAndRespond(ACLMessage reply) {
        switch (reply.getPerformative()) {
            case ACLMessage.REQUEST:
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(reply.getContent());//start transaction
                System.out.println("client A got the token " + reply.getContent() + "...");
                sendMessageFromClient(msg, getAID("B"));
                break;
            default:
                System.out.print("No valied performative...");
        }
    }

    //start the agent A
    @Override
    protected void setup() {
        System.out.println(getLocalName() + " is ready");

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                AID agentId = findAgentFromDF("ledger");
                if(agentId != null) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setContent("false");
                    sendMessageFromClient(msg, agentId);
                }
                else {
                    System.out.println("Cannot communicate: Agent not found...");
                }

            }
        });

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage reply = receive();
                if(reply != null) {
                    identifyIntensionAndRespond(reply);
                }
                else {
                    block();
                } 
            }
        });
    }
}
