import java.util.HashMap;

public class WorldState {
	int[] color;
	Controls controls;
	HashMap<Integer, Player> players;
	Tile[][] maze;
	int id, mazeSeed;

	public WorldState() {
		this(new int[] { 0, 0, 0 }, new Controls(), new HashMap<Integer, Player>(), new Tile[0][0], -1, 0);
	}

	public WorldState(int[] color, Controls controls, HashMap<Integer, Player> players, Tile[][] maze, int id, int mazeSeed) {
		this.color = color;
		this.controls = controls;
		this.players = players;
		this.maze = maze;
		this.id = id;
		this.mazeSeed = mazeSeed;
	}
}
