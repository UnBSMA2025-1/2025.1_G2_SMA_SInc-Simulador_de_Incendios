import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.Random;

    public class SeedAgent extends Agent {
        private int x, y;
        private Map map;
        private final Random rnd = new Random();
        private int vegetationType;

        @Override
        protected void setup() {
            Object[] args = getArguments();
            if (args == null || args.length < 3) {
                System.err.println("SeedAgent: argumentos insuficientes");
                doDelete();
                return;
            }

            x = (int) args[0];
            y = (int) args[1];
            map = (Map) args[2];

            Tile tile = map.getTile(x, y);
            if (tile == null) {
                System.err.println("SeedAgent: tile inválido");
                doDelete();
                return;
            }

            if (args.length >= 4) {
                vegetationType = (int) args[3];
            } else {
                vegetationType = 1 + rnd.nextInt(3);
            }
            tile.setType(vegetationType);

            // DF register
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("seed");
            sd.setName(getLocalName() + "-seed");
            dfd.addServices(sd);

            try {
                DFService.register(this, dfd);
                //System.out.println(getLocalName() + " registrado no DF como 'seed'");
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }


            // expansão
            addBehaviour(new TickerBehaviour(this, 200) {
                private int ticks = 0;

                @Override
                protected void onTick() {
                    Tile t = map.getTile(x, y);
                    if (t == null) {
                        doDelete();
                        return;
                    }

                    for (Direction dir : Direction.values()) {
                        if (rnd.nextDouble() < 0.65) {
                            int nx = x + dir.dx;
                            int ny = y + dir.dy;
                            Tile neighbor = map.getTile(nx, ny);
                            if (neighbor == null) continue;

                            if (neighbor.getType() == 0) {
                                map.createSeedAgent(nx, ny, vegetationType);
                            }
                        }
                    }

                    if (map.getGui() != null) {
                        map.getGui().repaint();
                    }

                    ticks++;
                    if (ticks >= 3) {  // ciclos p morrer
                        map.removeSeedAgent(getLocalName());

                        doDelete();
                    }
                }
            });

            // responder com vivo!
            addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {
                    ACLMessage msg = receive();
                    if (msg != null) {
                        if ("vivo?".equals(msg.getContent())) {
                            ACLMessage reply = msg.createReply();
                            reply.setContent("vivo!");
                            send(reply);
                        }
                    } else {
                        block();
                    }
                }
            });
        }

        @Override
        protected void takeDown() {
            try {
                DFService.deregister(this);
                //System.out.println(getLocalName() + " desregistrado do DF");
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
            map.removeSeedAgent(getLocalName());
            //System.out.println("Morri");
            super.takeDown();
        }
    }

