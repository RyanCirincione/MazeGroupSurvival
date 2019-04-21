
public class Bomb {
	public static int RADIUS = 5;
	public Vector position, velocity;

	public Bomb(Vector p) {
		this(p, new Vector(0, 0));
	}

	public Bomb(Vector p, Vector v) {
		position = p;
		velocity = v;
	}

	public void tick(WorldState worldState) {
		for (Integer i : worldState.players.keySet()) {
			if (i == worldState.it) {
				continue;
			}

			Player player = worldState.players.get(i);
			if (position.minus(player.position).length() < RADIUS + MazeGame.PLAYER_SIZE) {
				velocity = velocity.plus(position.minus(player.position).setLength(6));
			}
		}

		if (position.x <= 0) {
			velocity.x = 6;
		}
		if (position.y <= 0) {
			velocity.y = 6;
		}
		if (position.x >= worldState.maze.length * MazeGame.TILE_SIZE) {
			velocity.x = -6;
		}
		if (position.y >= worldState.maze[0].length * MazeGame.TILE_SIZE) {
			velocity.y = -6;
		}

		position.doDelta(velocity);

		if (position.x >= 0 && position.y >= 0 && (int) (position.x / MazeGame.TILE_SIZE) < worldState.maze.length
				&& (int) (position.y / MazeGame.TILE_SIZE) < worldState.maze[0].length
				&& worldState.maze[(int) (position.x / MazeGame.TILE_SIZE)][(int) (position.y / MazeGame.TILE_SIZE)] != Tile.WALL) {
			if (velocity.length() > 0.1) {
				velocity.setLength(velocity.length() * 0.935);
			} else {
				velocity.set(0, 0);
			}
		}
	}
}
