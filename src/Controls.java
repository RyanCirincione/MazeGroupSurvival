import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class Controls implements KeyListener, MouseListener, MouseMotionListener
{
	public boolean right, up, left, down;
	public Vector mouseHeld;
	WorldState worldState;
	
	public Controls(WorldState w)
	{
		this(w, false, false, false, false);
	}
	
	public Controls(WorldState w, boolean r, boolean u, boolean l, boolean d)
	{
		right = r;
		up = u;
		left = l;
		down = d;
		mouseHeld = null;
		worldState = w;
	}
	
	public Controls clone()
	{
		return new Controls(worldState, right, up, left, down);
	}
	
	public void mouseDragged(MouseEvent e) {
		worldState.controls.mouseHeld.update(e.getX(), e.getY());
	}

	public void mouseMoved(MouseEvent arg0) {
		
	}

	public void mouseClicked(MouseEvent arg0) {
		
	}

	public void mouseEntered(MouseEvent arg0) {
		
	}

	public void mouseExited(MouseEvent arg0) {
		
	}
	
	public void mousePressed(MouseEvent e) {
		mouseHeld = new Vector(e.getX(), e.getY());
	}

	public void mouseReleased(MouseEvent e) {
		if (worldState.dashCooldown <= 0 && worldState.dashTarget != null) {
			worldState.dashCooldown = (int) (MazeGame.DASH_COOLDOWN * (worldState.id == worldState.it ? 1.3 : 1));
			worldState.players.get(worldState.id).position = worldState.dashTarget;
		}
		worldState.controls.mouseHeld = null;
	}
	
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_D:
			right = true;
			break;
		case KeyEvent.VK_S:
			down = true;
			break;
		case KeyEvent.VK_A:
			left = true;
			break;
		case KeyEvent.VK_W:
			up = true;
			break;
		}
	}
	
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_D:
			right = false;
			break;
		case KeyEvent.VK_S:
			down = false;
			break;
		case KeyEvent.VK_A:
			left = false;
			break;
		case KeyEvent.VK_W:
			up = false;
			break;
		}
	}

	public void keyTyped(KeyEvent arg0) {
		
	}
}
