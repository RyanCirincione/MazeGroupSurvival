import java.awt.Color;

public class Player
{
	final int SPEED = 4;
	public Vector position, velocity;
	public Color color;
	
	public Player(Color c) {
		this(new Vector(MazeGame.TILE_SIZE * 1.5, MazeGame.TILE_SIZE * 1.5), c);
	}
	
	public Player(Vector pos, Color c) {
		this(pos, new Vector(0, 0), c);
	}
	
	public Player(Vector pos, Vector vel, Color c)
	{
		position = pos.clone();
		velocity = vel.clone();
		color = c;
	}
	
	public void movement(WorldState worldState)
	{
		Controls controls = worldState.controls;
		velocity.x = (controls.right ? SPEED : 0) + (controls.left ? -SPEED : 0);
		velocity.y = (controls.down ? SPEED : 0) + (controls.up ? -SPEED : 0);

		for(int i = 0; i < 5 && checkCollide(new Vector(position.x + velocity.x, position.y), worldState); i++)
		{
			velocity.x /= 2;
			if(i == 4)
				velocity.x = 0;
		}
		for(int i = 0; i < 5 && checkCollide(new Vector(position.x, position.y + velocity.y), worldState); i++)
		{
			velocity.y /= 2;
			if(i == 4)
				velocity.y = 0;
		}
		
		if(!checkCollide(new Vector(position.x + velocity.x, position.y + velocity.y), worldState))
			position.doDelta(velocity);
	}
	
	private boolean checkCollide(Vector pos, WorldState worldState)
	{
		for(int x = (int) ((pos.x - MazeGame.PLAYER_SIZE)/MazeGame.TILE_SIZE); x < (pos.x + MazeGame.PLAYER_SIZE)/MazeGame.TILE_SIZE; x++)
			for(int y = (int) ((pos.y - MazeGame.PLAYER_SIZE)/MazeGame.TILE_SIZE); y < (pos.y + MazeGame.PLAYER_SIZE)/MazeGame.TILE_SIZE; y++)
				if(worldState.maze[x][y] == Tile.WALL)
					return true;
		return false;
	}
	
	public String toString() {
		return "{" + position + ", " + velocity + ", " + color + "}";
	}
}
