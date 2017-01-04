
public class Player
{
	final int SPEED = 4;
	public Vector position, velocity;
	
	public Player(Vector pos, Vector vel)
	{
		position = pos.clone();
		velocity = vel.clone();
	}
	
	public void movement(Controls controls)
	{
		velocity.x = (controls.right ? SPEED : 0) + (controls.left ? -SPEED : 0);
		velocity.y = (controls.down ? SPEED : 0) + (controls.up ? -SPEED : 0);

		for(int i = 0; i < 5 && checkCollide(new Vector(position.x + velocity.x, position.y)); i++)
		{
			velocity.x /= 2;
			if(i == 4)
				velocity.x = 0;
		}
		for(int i = 0; i < 5 && checkCollide(new Vector(position.x, position.y + velocity.y)); i++)
		{
			velocity.y /= 2;
			if(i == 4)
				velocity.y = 0;
		}
		
		if(!checkCollide(new Vector(position.x + velocity.x, position.y + velocity.y)))
			position.doDelta(velocity);
	}
	
	private boolean checkCollide(Vector pos)
	{
		for(int x = (int) ((pos.x - MazeGame.PLAYER_SIZE)/MazeGame.TILE_SIZE); x < (pos.x + MazeGame.PLAYER_SIZE)/MazeGame.TILE_SIZE; x++)
			for(int y = (int) ((pos.y - MazeGame.PLAYER_SIZE)/MazeGame.TILE_SIZE); y < (pos.y + MazeGame.PLAYER_SIZE)/MazeGame.TILE_SIZE; y++)
				if(MazeGame.maze[x][y] == Tile.WALL)
					return true;
		return false;
	}
}
