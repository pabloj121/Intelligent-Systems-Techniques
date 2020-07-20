package practica_busqueda;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;
import tools.pathfinder.Node;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by dperez on 14/01/16.
 * Modified by Pablo Jimenez on 10/04/19
 */
public class PabloPathFinder {

    public AStar astar;
    public StateObservation state;

    public boolean VERBOSE = false;

    //All types are obstacles except the ones included in this array
    public ArrayList<Integer> obstacleItypes;
    public HashSet<Node> dead_positions = new HashSet<Node>();
    Vector2d fescala;
    public ArrayList<Observation> grid[][];

    private static int[] x_arrNeig = null;
    private static int[] y_arrNeig = null;

    public PabloPathFinder(ArrayList<Integer> obstacleItypes)
    {
        this.obstacleItypes = obstacleItypes;

    }

    public void run(StateObservation stateObs, Integer prueba)
    {
        this.state = stateObs;
        this.grid = stateObs.getObservationGrid();
        this.astar = new AStar(this);

        // Calculamos el factor de escala entre mundos
        fescala = new Vector2d((double) stateObs.getWorldDimension().width
                / stateObs.getObservationGrid().length,
                (double) stateObs.getWorldDimension().height
                        / stateObs.getObservationGrid()[0].length);


        init();
        runAll();

        if(VERBOSE)
        {
            // imprime el camino asociado a cada clave
            for(Integer pathId : astar.pathCache.keySet())
            {
                ArrayList<Node> nodes = astar.pathCache.get(pathId);
                astar.printPath(pathId, nodes);
            }
        }
    }


    private void init()
    {
        if(x_arrNeig == null)
        {
            //TODO: This is a bit of a hack, it wouldn't work with other (new) action sets.
            ArrayList<Types.ACTIONS> actions = this.state.getAvailableActions();
            if(actions.size() == 3)
            {
                //left, right
                x_arrNeig = new int[]{-1, 1};
                y_arrNeig = new int[]{0,  0};
            }else
            {
                //up, down, left, right
                x_arrNeig = new int[]{0,    0,    -1,    1};
                y_arrNeig = new int[]{-1,   1,     0,    0};
            }
        }
    }

    private void runAll()
    {
        for(int i = 0; i < grid.length; ++i)
        {
            for(int j = 0; j < grid[i].length; ++j)
            {
                boolean obstacleCell = isObstacle(i,j);
                if(!obstacleCell)
                {
                    if(VERBOSE) System.out.println("Running from (" + i +  "," + j + ")");
                    runAll(i,j);
                }

            }
        }
    }

    public ArrayList<Node> getPath(Vector2d start, Vector2d end)
    {
        return astar.getPath(new Node(start), new Node(end));
    }

    private void runAll(int i, int j) {
        Node start = new Node(new Vector2d(i,j));
        Node goal = null; //To get all routes.

        astar.findPath(start, goal);
    }

    // Se marca una posicion en la que el jugador ha muerto (usando metodo advance)
    public void MarcarPosicion(Node posicion){
        dead_positions.add(posicion);
    }

    // Reinicamos proceso de busqueda, ahora con menos vecinos
    public void Reiniciar(){
        astar.emptyCache();
    }


    // Imprime cuántas rutas hay disponibles
    public void imprimePathCache(){
        System.out.println("tam del patCache: " + this.astar.pathCache.size() + "\n");
    }


    public boolean isObstacle(int row, int col)
    {
        if(row<0 || row>=grid.length) return true;
        if(col<0 || col>=grid[row].length) return true;

        for(Observation obs : grid[row][col])
        {
            if(obstacleItypes.contains(obs.itype))
                return true;
        }
        return rocaEncima(row,col);
        // return false;
    }


    public boolean rocaEncima(int row, int col){
        Vector2d pos = new Vector2d(row,col);
        ArrayList<core.game.Observation>[] boulders = state.getMovablePositions(state.getAvatarPosition());

        for(int j = 0; j < boulders[0].size(); j++){
            boulders[0].get(j).position.x /= fescala.x;
            boulders[0].get(j).position.y /= fescala.y;

            if(pos.x == boulders[0].get(j).position.x){
                // Encima de pos hay una roca
                if(Math.abs(pos.y - boulders[0].get(j).position.y) == 1.0){
                    return true;
                }
            }
        }
        return false;
    }


    public ArrayList<Node> getNeighbours(Node node) {
        ArrayList<Node> neighbours = new ArrayList<Node>();
        int x = (int) (node.position.x);
        int y = (int) (node.position.y);

        for(int i = 0; i < x_arrNeig.length; ++i)
        {
            if(!isObstacle(x+x_arrNeig[i], y+y_arrNeig[i])) {
                //if (!dead_positions.contains(aux)){ // si no he muerto ahi entonces lo añado
                neighbours.add(new Node(new Vector2d(x+x_arrNeig[i], y+y_arrNeig[i])));
            }
        }
        return neighbours;
    }

}
