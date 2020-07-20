/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica_busqueda;


import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import java.util.ArrayList;
import java.util.HashSet;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Pair;
import tools.Vector2d;
import tools.pathfinder.Node;
import static ontology.Types.ACTIONS.ACTION_NIL;

/**
 *
 * @author pablo
 */

// Para que no de error, 'implement all abstract methods'
public class PabloAgent extends AbstractPlayer {

    private Vector2d fescala;
    private Vector2d ultimaPos;
    private PabloPathFinder pf;
    private ArrayList<Node> path;
    public HashSet<Node> dead_positions = new HashSet<Node>();
    private boolean reinicio;
    private boolean simulacion; // para distinguir iteraciones en las que se ha hecho la simulacion del plan
    private int ticks_parado;

    public PabloAgent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Creamos el vector que contendrá el plan
        path = new ArrayList();

        // Creamos una lista de IDs de obstaculos
        ArrayList<Observation>[] obstaculos = stateObs.getImmovablePositions();
        ArrayList<Integer> tiposObs = new ArrayList<Integer>();

        tiposObs.add(7);        // piedra obs.get(0).obsID);
        tiposObs.add(0);        // muro
        tiposObs.add(10);       // escorpiones
        tiposObs.add(11);       // murcielagos

        // Se inicializa el objeto pf con las ids de los obstaculos
        pf = new PabloPathFinder(tiposObs);
        pf.VERBOSE = false; // activa/desactiva el modo de impresion del log
        reinicio = false;

        // Se lanza el algoritmo de pathfinding para poder ser usado en ACT
        pf.run(stateObs, 1);

        // Calculamos el factor de escala entre mundos
        fescala = new Vector2d((double) stateObs.getWorldDimension().width
                / stateObs.getObservationGrid().length,
                (double) stateObs.getWorldDimension().height
                        / stateObs.getObservationGrid()[0].length);

        ticks_parado = 0; // falta comentar

        // ultima posicion del avatar
        ultimaPos = new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                stateObs.getAvatarPosition().y / fescala.y);
    }


    // Si uno de los nodos del plan calculado se ha descubierto anteriormente que hay una alta
    // posibilidad de muerte, devuelve true
    /*public boolean badPlan(ArrayList<Node> plan){
        for(Node i:plan){
            if(dead_positions.contains(i))
                return true;
        }
        return false;
    }*/


    // Metodo con el que obtenemos aquellas gemas que a priori no representan un peligro
    // para el jugador ( NO SE USA )
    /*public ArrayList<Observation> gemasBuenas(ArrayList<Observation>[] gemas){
        ArrayList<Observation> gemas_buenas = new ArrayList();

        for (int i = 0; i < gemas[0].size(); i++) {
            // si la roca por la que vamos iterando no representa un peligro
            if(!pf.isObstacle((int) gemas[0].get(i).position.x, (int) gemas[0].get(i).position.y)) {
                gemas_buenas.add(gemas[0].get(i));
            }
        }
        return gemas_buenas;
    }*/



    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        // Obtenemos la posicion del avatar
        Vector2d avatar = new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
                stateObs.getAvatarPosition().y / fescala.y);

        // Posicion de las rocas a cada tick del juego
        ArrayList<core.game.Observation>[] boulders = stateObs.getMovablePositions();


        // Se actualiza el plan de ruta
        if (((avatar.x != ultimaPos.x) || (avatar.y != ultimaPos.y)) && !path.isEmpty()) {
            //System.out.println("avanzando...\n");
            path.remove(0);
        }

        // Se calcula el numero de gemas que el jugador lleva encima
        int nGemas = 0;
        if (!stateObs.getAvatarResources().isEmpty()) {
            nGemas = stateObs.getAvatarResources().get(6);
        }


        // Si no hay plan de ruta calculado
        if (path.isEmpty()) {

            // Si ya tiene todas las gemas vamos al portal mas cercano
            if (nGemas == 10) { // falta (recien cambiado)
                Vector2d portal;

                ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(stateObs.getAvatarPosition());

                portal = posiciones[0].get(0).position;

                // se le aplica el factor de escala
                portal.x = portal.x / fescala.x;
                portal.y = portal.y / fescala.y;

                // Calculamos un camino desde la posicion del avatar a la
                // posicion del portal
                path = pf.getPath(avatar, portal);
            }
            else {
                Vector2d gema;
                boolean existeGema = true;
                boolean existeRoca;// = false;

                // lista de gemas, se supone que no cogera aquellas gemas que tienen roca encima...
                ArrayList<Observation>[] posiciones = stateObs.getResourcesPositions(stateObs.getAvatarPosition());


                gema = posiciones[0].get(0).position;

                // se le aplica el factor de escala
                gema.x = gema.x / fescala.x;
                gema.y = gema.y / fescala.y;

                // Calculamos un camino desde la posicion del avatar a la
                // posicion del portal
                path = pf.getPath(avatar, gema);
                System.out.println("path before for loop: " + path + "\n");
                int i = 1;

                System.out.println("path post for loop: " + path + "\n");

                // Se recorren todas las gemas mientras el plan sea nulo
                while(path == null && i < posiciones[0].size()){
                    gema = posiciones[0].get(i).position;

                    // se le aplica el factor de escala
                    gema.x = gema.x / fescala.x;
                    gema.y = gema.y / fescala.y;

                    // Calculamos de un camino a la siguiente gema mas cercana
                    path = pf.getPath(avatar, gema);
                    i++;
                }
            }
        }

        if (path != null) {
            System.out.println("hay plan\n");
            Types.ACTIONS siguienteAccion = ACTION_NIL;
            Node siguientePos = path.get(0);


            // se determina el movimiento a seguir a partir de la posicion del avatar
            if (siguientePos.position.x != avatar.x) {
                if (siguientePos.position.x > avatar.x) {
                    siguienteAccion = Types.ACTIONS.ACTION_RIGHT;
                } else {
                    siguienteAccion = Types.ACTIONS.ACTION_LEFT;
                }
            } else {
                if (siguientePos.position.y > avatar.y) {
                    siguienteAccion = Types.ACTIONS.ACTION_DOWN;
                } else {
                    siguienteAccion = Types.ACTIONS.ACTION_UP;
                }
            }

            // Actualizacion de la ultima posicion del avatar
            ultimaPos = avatar;
            return siguienteAccion;
        } else {
            return ACTION_NIL;
        }
        //return ACTION_NIL;
    }


    private Pair<StateObservation, Node> simularacciones(StateObservation estado, ArrayList<Node> plan) {
        Types.ACTIONS siguienteAccion = ACTION_NIL;
        Node next_node = plan.get(0);
        StateObservation estado_actual = estado; //Guardamos la informacion sobre el estado inicial
        Pair<StateObservation, Node> pair;
        ArrayList<Types.ACTIONS> acciones = estado.getAvailableActions();

        // copiamos el estado inicial en viejoEstado para que no tenga influencia uno sobre otro
        StateObservation viejoEstado = estado.copy();

        for (Node paso : plan) {
            // Posicion del avatar en el estado "inicial"
            Vector2d avatar = new Vector2d(estado_actual.getAvatarPosition().x / fescala.x,
                    estado_actual.getAvatarPosition().y / fescala.y);
            next_node = paso;    //plan.get(0);

            // Determinamos accion del avatar teniendo en cuenta su siguiente posicion
            if (next_node.position.x != avatar.x) {
                if (next_node.position.x > avatar.x) {
                    siguienteAccion = Types.ACTIONS.ACTION_RIGHT;
                } else {
                    siguienteAccion = Types.ACTIONS.ACTION_LEFT;
                }
            } else {
                if (next_node.position.y > avatar.y) {
                    siguienteAccion = Types.ACTIONS.ACTION_DOWN;
                } else {
                    siguienteAccion = Types.ACTIONS.ACTION_UP;
                }
            }

            estado_actual.advance(siguienteAccion);

            // Se simula la accion, si ha muerto en ese nodo la simulacion acaba
            if (!estado_actual.isAvatarAlive()){
                //System.out.println("Acada de morir en el nodo " + next_node + "\n");
                pair = new Pair<>(estado_actual, next_node);
                return pair;
            }
            ultimaPos = avatar;
        }

        pair = new Pair<>(estado_actual, next_node);
        return pair;
    }
}