
public class Player
{
	public Vector position, velocity;
	
	public Player(Vector pos, Vector vel)
	{
		position = pos.clone();
		velocity = vel.clone();
	}
	
	public void movement(Controls controls)
	{
		velocity.x = (controls.right ? 5 : 0) + (controls.left ? -5 : 0);
		velocity.y = (controls.down ? 5 : 0) + (controls.up ? -5 : 0);
		position.doDelta(velocity);
	}
}
