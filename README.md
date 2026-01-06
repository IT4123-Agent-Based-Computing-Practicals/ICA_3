# 🤖 JADE Token Management System

A simple multi-agent system demonstrating token-based communication using JADE framework.

## 📋 Overview

Three agents work together to create, transfer, and verify tokens:

- **🏦 LedgerAgent**: The token bank (creates & verifies tokens)
- **👤 AgentA**: The requester (gets token first)
- **👥 AgentB**: The receiver (verifies token)

## 🔄 Flow Diagram

```
AgentA → LedgerAgent: "Give me a token!"
       ← "Here's TXN-123"

AgentA → AgentB: "Token TXN-123"

AgentB → LedgerAgent: "Is TXN-123 valid?"
       ← "✅ Confirmed!"
```

## 🚀 Quick Start

### Compile

```bash
javac -cp lib\jade.jar -d classes ICA3\*.java
```

### Run

```bash
java -cp lib\jade.jar;classes jade.Boot -agents test:ICA3.LedgerAgent;A:ICA3.AgentA;B:ICA3.AgentB
```

## � Output Screenshots

### Console Output

<img width="971" height="499" alt="Output" src="https://github.com/user-attachments/assets/3bbaed9e-9cb7-4228-8351-4b76b144450c" />

---

## �💡 Key Code Snippets

### 🏦 LedgerAgent - Token Creation

```java
private String createToken() {
    String token = "TXN-" + UUID.randomUUID().toString();
    return token;
}
```

### 🏦 LedgerAgent - Registration

```java
ServiceDescription agentServiceDescription = new ServiceDescription();
agentServiceDescription.setType("ledger");
DFService.register(this, agentDescription);
```

### 👤 AgentA - Request Token

```java
ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
msg.setContent("false");
sendMessageFromClient(msg, agentId);
```

### 👥 AgentB - Verify Token

```java
ACLMessage reply = new ACLMessage(ACLMessage.REQUEST);
reply.setContent(msg.getContent()); // Send token for verification
sendMessageFromClient(reply, agentId);
```

## 📬 Message Types

| Type      | Purpose                      | Emoji |
| --------- | ---------------------------- | ----- |
| `REQUEST` | Ask for token / Verify token | 📨    |
| `INFORM`  | Pass token to another agent  | 📢    |
| `CONFIRM` | Token is valid               | ✅    |
| `REFUSE`  | Token is invalid             | ❌    |

## 🎯 Remember This!

**3 Agents, 3 Steps:**

1. 👤 **A** requests → 🏦 **Ledger** creates
2. 👤 **A** sends → 👥 **B** receives
3. 👥 **B** verifies → 🏦 **Ledger** confirms

**Key Pattern:**

- All agents use `CyclicBehaviour` to continuously listen 📡
- Ledger registers itself in DF (Directory Facilitator) 📖
- Other agents find Ledger using `DFService.search()` 🔍
- Tokens stored in `HashMap<String, AID>` 🗂️

## 🛠️ Technologies

- ☕ Java
- 🤖 JADE Framework
- 📚 FIPA Specifications

---

## 📝 Full Code (Easy to Remember)

### 🏦 LedgerAgent.java - The Token Bank

**Remember: SRD (Setup, Register, Distribute)**

- **S**etup → **R**egister in DF → **D**istribute/Verify tokens

```java
package ICA3;

import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import java.util.*;

public class LedgerAgent extends Agent {

    // 🗂️ Storage: HashMap to track who owns which token
    Map<String, AID> tokenStore = new HashMap<>();

    // 📖 Step 1: Register yourself in the Directory Facilitator (like a phone book)
    private void registerAgentInDF() {
        try {
            DFAgentDescription agentDescription = new DFAgentDescription();
            agentDescription.setName(getAID());

            ServiceDescription agentServiceDescription = new ServiceDescription();
            agentServiceDescription.setType("ledger"); // 🏷️ Tag yourself as "ledger"
            agentServiceDescription.setName(getLocalName() + "-ledger");
            agentDescription.addServices(agentServiceDescription);

            DFService.register(this, agentDescription);
            System.out.println("Agent registered successfully...");
        } catch(FIPAException fx) {
            System.out.println("Error occure-registration failed...");
        }
    }

    // 🎲 Step 2: Create unique token (UUID = Universally Unique ID)
    private String createToken() {
        return "TXN-" + UUID.randomUUID().toString();
    }

    // 📤 Step 3: Send message helper
    private void sendMessageFromServerToClient(ACLMessage reply, AID recvId) {
        reply.addReceiver(recvId);
        send(reply);
        System.out.println("server send the reply...");
    }

    // 🧠 Step 4: Brain of Ledger - Handle requests
    private void identifyIntensionAndRespond(ACLMessage msg) {
        AID senderAgentId = msg.getSender();

        switch(msg.getPerformative()) {
            case ACLMessage.REQUEST:
                if(msg.getContent().contains("false")) {
                    // 🆕 CREATE TOKEN: Request for new token
                    String token = createToken();
                    tokenStore.put(token, senderAgentId); // Save it
                    ACLMessage reply = new ACLMessage(ACLMessage.REQUEST);
                    reply.setContent(token);
                    sendMessageFromServerToClient(reply, senderAgentId);
                } else {
                    // ✅ VERIFY TOKEN: Check if token exists
                    String token = msg.getContent();
                    if(tokenStore.containsKey(token)) {
                        tokenStore.remove(token); // ♻️ One-time use!
                        ACLMessage reply = new ACLMessage(ACLMessage.CONFIRM);
                        reply.setContent(token);
                        sendMessageFromServerToClient(reply, senderAgentId);
                    } else {
                        // ❌ Token not found
                        ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);
                        reply.setContent("Token is not available...");
                        sendMessageFromServerToClient(reply, senderAgentId);
                    }
                }
                break;
            default:
                System.err.println("No valid performative...");
        }
    }

    // 🚀 Main setup
    @Override
    protected void setup() {
        System.out.println(getLocalName() + " is ready");
        registerAgentInDF(); // Register first!

        // 📡 Listen forever (Cyclic = infinite loop)
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if(msg != null) {
                    identifyIntensionAndRespond(msg);
                } else {
                    block(); // Wait for message
                }
            }
        });
    }

    // 🧹 Cleanup when shutting down
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fx) {
            System.out.println("Agent de registration failed");
        }
        System.out.println("Agent de register successful...");
    }
}
```

---

### 👤 AgentA.java - The Initiator

**Remember: FAR (Find, Ask, Relay)**

- **F**ind Ledger → **A**sk for token → **R**elay to B

```java
package ICA3;

import jade.core.*;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class AgentA extends Agent {

    // 🔍 Step 1: Find another agent by searching DF (Directory Facilitator)
    private AID findAgentFromDF(String agentType) {
        try {
            DFAgentDescription agentDescriptionTemplate = new DFAgentDescription();
            ServiceDescription agentServiceDescription = new ServiceDescription();
            agentServiceDescription.setType(agentType); // Search by type (e.g., "ledger")
            agentDescriptionTemplate.addServices(agentServiceDescription);

            DFAgentDescription[] availableAgents = DFService.search(this, agentDescriptionTemplate);
            if(availableAgents.length > 0) {
                AID agentId = availableAgents[0].getName();
                System.err.println("Agent A found (" + agentType + ")...");
                return agentId;
            } else {
                System.err.println("Agent not found...");
                return null;
            }
        } catch (FIPAException fe) {
            System.err.println("Error in agent finding...");
        }
        return null;
    }

    // 📤 Step 2: Send message helper
    private void sendMessageFromClient(ACLMessage reply, AID recvId) {
        reply.addReceiver(recvId);
        send(reply);
        System.out.println("client A send msg...");
    }

    // 🧠 Step 3: Handle incoming messages
    private void identifyIntensionAndRespond(ACLMessage reply) {
        switch (reply.getPerformative()) {
            case ACLMessage.REQUEST:
                // 🎫 Got token from Ledger, now pass it to Agent B
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(reply.getContent()); // Token content
                System.out.println("client A got the token " + reply.getContent() + "...");
                sendMessageFromClient(msg, getAID("B")); // Send to Agent B
                break;
            default:
                System.out.print("No valid performative...");
        }
    }

    // 🚀 Main setup
    @Override
    protected void setup() {
        System.out.println(getLocalName() + " is ready");

        // 🎯 ONE-TIME: Start the process (OneShotBehaviour = runs once)
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                AID agentId = findAgentFromDF("ledger"); // Find Ledger
                if(agentId != null) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setContent("false"); // 🔑 "false" = give me a new token
                    sendMessageFromClient(msg, agentId);
                } else {
                    System.out.println("Cannot communicate: Agent not found...");
                }
            }
        });

        // 📡 CONTINUOUS: Listen for replies (CyclicBehaviour = infinite loop)
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage reply = receive();
                if(reply != null) {
                    identifyIntensionAndRespond(reply);
                } else {
                    block(); // Wait for message
                }
            }
        });
    }
}
```

---

### 👥 AgentB.java - The Verifier

**Remember: RFV (Receive, Find, Verify)**

- **R**eceive token → **F**ind Ledger → **V**erify token

```java
package ICA3;

import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class AgentB extends Agent {

    // 🔍 Step 1: Find Ledger agent from DF
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
            } else {
                return null;
            }
        } catch (FIPAException fe) {
            System.err.println("Error in agent finding...");
            return null;
        }
    }

    // 📤 Step 2: Send message helper
    private void sendMessageFromClient(ACLMessage reply, AID recvId) {
        reply.addReceiver(recvId);
        send(reply);
        System.out.println("client B send msg...");
    }

    // 🧠 Step 3: Handle different message types
    private void identifyIntensionAndRespond(ACLMessage msg) {
        switch(msg.getPerformative()) {
            case ACLMessage.INFORM:
                // 📨 Got token from Agent A, verify it with Ledger
                AID agentId = findAgentFromDF("ledger");
                System.out.println("Agent B got the token from agent A " + msg.getContent() + "...");
                if(agentId != null) {
                    ACLMessage reply = new ACLMessage(ACLMessage.REQUEST);
                    reply.setContent(msg.getContent()); // Forward token to Ledger
                    sendMessageFromClient(reply, agentId);
                } else {
                    System.out.println("Cannot communicate: Agent not found...");
                }
                break;
            case ACLMessage.CONFIRM:
                // ✅ Token is valid!
                System.out.println("Token verify can proceed the transaction...");
                break;
            case ACLMessage.REFUSE:
                // ❌ Token is invalid!
                System.out.println("Token not verify cannot proceed the transaction...");
                break;
            default:
                System.out.print("No valid performative...");
        }
    }

    // 🚀 Main setup
    @Override
    protected void setup() {
        System.out.println(getLocalName() + " is ready");

        // 📡 Listen forever (CyclicBehaviour = infinite loop)
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if(msg != null) {
                    identifyIntensionAndRespond(msg);
                } else {
                    block(); // Wait for message
                }
            }
        });
    }
}
```

---

## 🎓 Memory Tips

### Message Flow Mnemonic: **"A-L-A-B-L-B"**

1. **A** → **L**: Agent A asks Ledger for token
2. **L** → **A**: Ledger sends token to A
3. **A** → **B**: Agent A passes token to B
4. **B** → **L**: Agent B verifies with Ledger
5. **L** → **B**: Ledger confirms/refuses

### Code Pattern: **"2B + DF"**

- **2 Behaviours**: OneShotBehaviour (start) + CyclicBehaviour (listen)
- **DF**: Always use Directory Facilitator to find agents

### ACLMessage Types: **"RIRC"**

- **R**EQUEST: Ask for something
- **I**NFORM: Tell information
- **R**EFUSE: Say no
- **C**ONFIRM: Say yes

---

💡 _Tip: Token is created once, used once, then deleted!_
