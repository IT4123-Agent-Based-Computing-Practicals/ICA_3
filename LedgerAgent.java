package ICA3;

import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import java.util.*;

public class LedgerAgent extends Agent {

    //data structure
    Map<String, AID> tokenStore = new HashMap<>();

    //register the Ledger agent to directory facilitator(DF)
    private void registerAgentInDF() {
        try{
            DFAgentDescription agentDescription = new DFAgentDescription();
            agentDescription.setName(getAID());

            ServiceDescription agentServiceDescription = new ServiceDescription();
            agentServiceDescription.setType("ledger");
            agentServiceDescription.setName(getLocalName() + "-ledger");
            agentDescription.addServices(agentServiceDescription);

            DFService.register(this, agentDescription);
            System.out.println("Agent registert successfully...");
        } 
        catch(FIPAException fx) {
            System.out.println("Error occure-registration failed...");
        }
    }

    //create a random token
    private String createToken() {
        String token = "TXN-" + UUID.randomUUID().toString();
        return token;
    }

    //communication-send msg
    private void sendMessageFromServerToClient(ACLMessage reply, AID recvId) {
        reply.addReceiver(recvId);
        send(reply);
        System.out.println("server send the reply...");
    }

    //communication-receive msg base on the performative
    private void identifyIntensionAndRespond(ACLMessage msg) {
        AID senderAgentId = msg.getSender();
        switch(msg.getPerformative()) {
            case ACLMessage.REQUEST:
                if(msg.getContent().contains("false")) {
                    String token = createToken();
                    tokenStore.put(token, senderAgentId);
                    ACLMessage reply = new ACLMessage(ACLMessage.REQUEST);
                    reply.setContent(token);
                    sendMessageFromServerToClient(reply, senderAgentId);
                }
                else {
                    String token = msg.getContent();
                    if(tokenStore.containsKey(token)) {
                        tokenStore.remove(token);
                        ACLMessage reply = new ACLMessage(ACLMessage.CONFIRM);
                        reply.setContent(token);
                        sendMessageFromServerToClient(reply, senderAgentId);
                    }
                    else {
                        ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);
                        reply.setContent("Token is not available...");
                       
                        sendMessageFromServerToClient(reply, senderAgentId);
                    }
                }
                break;
            default:
                System.err.println("No valied performative...");
        }
    }

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " is ready");
        registerAgentInDF();

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

    @Override
    protected void takeDown(){
        try {
            DFService.deregister(this);
        } 
        catch (FIPAException fx) {
            System.out.println("Agent de registration failed");
        }
        System.out.println("Agent de register successfull...");
    }
}

// javac -cp lib\jade.jar -d classes ICA3\*.java

// java -cp lib\jade.jar;classes jade.Boot -agents test:ICA3.LedgerAgent;A:ICA3.AgentA;B:ICA3.AgentB