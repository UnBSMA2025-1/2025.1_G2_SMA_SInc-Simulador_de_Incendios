import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

    public class SeedStatusAgent extends Agent {

        @Override
        protected void setup() {
            System.out.println(getLocalName() + " seed status started...");

            addBehaviour(new TickerBehaviour(this, 3000) {
                @Override
                protected void onTick() {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("seed");
                    template.addServices(sd);
                    int count = 0;

                    try {
                        // busca SeedAgents no DF
                        DFAgentDescription[] result = DFService.search(myAgent, template);

                        // envia "vivo? para cada seed
                        for (DFAgentDescription dfd : result) {
                            AID seedAID = dfd.getName();
                            ACLMessage ping = new ACLMessage(ACLMessage.REQUEST);
                            ping.addReceiver(seedAID);
                            ping.setContent("vivo?");
                            send(ping);
                        }

                        long waitUntil = System.currentTimeMillis() + 300; // espera 300ms por respostas

                        while (System.currentTimeMillis() < waitUntil) {
                            MessageTemplate mt = MessageTemplate.MatchContent("vivo!");
                            ACLMessage reply = receive(mt);
                            if (reply != null) {
                                count++;
                            } else {
                                block(50); // espera até 50ms para mensagens
                            }
                        }

                        System.out.println("[SeedStatusAgent] Número de seeds ativos: " + count + "/" + result.length);

                        MapPanel.setActiveSeed(count>0);

                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    if(count == 0){
                        doDelete();
                    }
                }
            });
        }
    }

