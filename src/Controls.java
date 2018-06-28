
public class Controls
{
	public boolean right, up, left, down;
	public Vector mouseHeld;
	
	public Controls()
	{
		this(false, false, false, false);
	}
	
	public Controls(boolean r, boolean u, boolean l, boolean d)
	{
		right = r;
		up = u;
		left = l;
		down = d;
		mouseHeld = null;
	}
	
	public Controls clone()
	{
		return new Controls(right, up, left, down);
	}
}
