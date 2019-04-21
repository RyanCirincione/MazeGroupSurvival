import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class MazeGame extends JPanel {
	private static final long serialVersionUID = -3817295832102153653L;
	Network network;

	public static void main(String[] args) throws IOException {
		JFrame frame = new JFrame("Maze Tag");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Determine host
		boolean host = false;
		if (JOptionPane.showConfirmDialog(frame, "Will you be the host?") == JOptionPane.YES_OPTION) {
			frame.setTitle("Maze Tag --HOST--");
			host = true;
		}

		MazeGame panel = new MazeGame(host);
		frame.getContentPane().add(panel);

		// Looped thread for streaming over the network
		Thread networkThread = new Thread() {
			public void run() {
				while (true) {
					if (panel.network.host) {
						for (Integer i : panel.worldState.players.keySet()) {
							Player p = panel.worldState.players.get(i);
							panel.network.addEvent(Network.Event.EventType.PLAYER_X, i, (int) p.position.x);
							panel.network.addEvent(Network.Event.EventType.PLAYER_Y, i, (int) p.position.y);
							panel.network.addEvent(Network.Event.EventType.PLAYER_VEL_X, i, (int) p.velocity.x);
							panel.network.addEvent(Network.Event.EventType.PLAYER_VEL_Y, i, (int) p.velocity.y);
						}
					} else {
						Player p = panel.worldState.players.get(panel.worldState.id);
						panel.network.addEvent(Network.Event.EventType.PLAYER_X, panel.worldState.id, (int) p.position.x);
						panel.network.addEvent(Network.Event.EventType.PLAYER_Y, panel.worldState.id, (int) p.position.y);
						panel.network.addEvent(Network.Event.EventType.PLAYER_VEL_X, panel.worldState.id, (int) p.velocity.x);
						panel.network.addEvent(Network.Event.EventType.PLAYER_VEL_Y, panel.worldState.id, (int) p.velocity.y);
					}

					try {
						panel.network.update(panel.worldState);
					} catch (ConcurrentModificationException e) {
						System.out.println("Concurrent mod!");
					}

					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		networkThread.start();

		// Looping thread for updating and rendering
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				panel.tick();
				panel.repaint();
			}
		}, 0, 1000 / 60);

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	static final int MAZE_SIZE = 83, S_WIDTH = 800, S_HEIGHT = 600, TILE_SIZE = 48, PLAYER_SIZE = 8, DASH_RANGE = (int) (TILE_SIZE * 1.85), DASH_COOLDOWN = 300,
			TAG_COOLDOWN = 60;
	WorldState worldState;
	Controls controls;
	int tagCooldown;
	// TODO tagging host doesnt seem to tp client?

	public MazeGame(boolean host) {
		tagCooldown = 0;
		worldState = new WorldState(null);
		controls = new Controls(worldState);
		worldState.controls = controls;
		network = new Network(host, worldState);

		// Colorize on initialization
		String c = JOptionPane.showInputDialog(null, "Enter RGB of your desired player color. (Ex: \"84 0 255\")", "Maze Group Survival", JOptionPane.QUESTION_MESSAGE);
		if (c.equals("")) {
			c = "" + (int) (Math.random() * 255) + " " + (int) (Math.random() * 255) + " " + (int) (Math.random() * 255);
		}
		worldState.color = new int[] { Integer.parseInt(c.split(" ")[0]), Integer.parseInt(c.split(" ")[1]), Integer.parseInt(c.split(" ")[2]) };
		if (worldState.color[0] > 250 && worldState.color[1] > 250 && worldState.color[2] > 250) {
			worldState.color[0] = 128;
			worldState.color[1] = 64;
			worldState.color[2] = 12;
		}

		// Add own player to list
		if (network.host) {
			worldState.id = 0;
			worldState.players.put(worldState.id, new Player(new Color(worldState.color[0], worldState.color[1], worldState.color[2])));

			// Create player's kill zones
			for (int i = 0; i < Player.NUM_KILL_ZONES; i++) {
				worldState.players.get(worldState.id).killZones.add(new Vector(Math.random() * MAZE_SIZE * TILE_SIZE, Math.random() * MAZE_SIZE * TILE_SIZE));
			}

			System.out.println("--- Host ---");
		} else {
			worldState.id = worldState.players.size();
			worldState.players.put(worldState.id, new Player(new Color(worldState.color[0], worldState.color[1], worldState.color[2])));

			// Create player's kill zones
			int[] data = new int[Player.NUM_KILL_ZONES * 2];
			for (int i = 0; i < Player.NUM_KILL_ZONES; i++) {
				Vector v = new Vector(Math.random() * MAZE_SIZE * TILE_SIZE, Math.random() * MAZE_SIZE * TILE_SIZE);
				worldState.players.get(worldState.id).killZones.add(v);
				data[i] = (int) v.x;
				data[i + 1] = (int) v.y;
			}

			network.addEvent(Network.Event.EventType.NEW_PLAYER, worldState.id, worldState.color[0], worldState.color[1], worldState.color[2], data[0], data[1], data[2], data[3],
					data[4], data[5], data[6], data[7]);

			System.out.println("--- Client: " + worldState.id + " ---");
		}

		// Create the maze
		worldState.maze = new Tile[MAZE_SIZE][MAZE_SIZE];
		for (int x = 0; x < worldState.maze.length; x++)
			for (int y = 0; y < worldState.maze[x].length; y++)
				worldState.maze[x][y] = Tile.WALL;
		generateMaze(worldState.maze, worldState.mazeSeed);

		this.addKeyListener(worldState.controls);
		this.addMouseListener(worldState.controls);
		this.addMouseMotionListener(worldState.controls);
		this.setFocusable(true);
		this.requestFocus();

		this.setPreferredSize(new Dimension(S_WIDTH, S_HEIGHT));
	}

	public void tick() {
		// Calculate dash target
		Vector m = worldState.controls.mouseHeld;
		worldState.dashTarget = null;
		if (m != null) {
			m = m.clone();
			m.set(m.x - S_WIDTH / 2, m.y - S_HEIGHT / 2);
			if (m.length() > DASH_RANGE) {
				m.setLength(DASH_RANGE);
			}

			while (!goodTeleportSpot(m.plus(worldState.players.get(worldState.id).position), worldState.maze) && m.length() > PLAYER_SIZE * 3) {
				m.setLength(m.length() * 0.975);
			}

			m.doDelta(worldState.players.get(worldState.id).position);

			if (goodTeleportSpot(m, worldState.maze)) {
				worldState.dashTarget = m;
			}
		}

		if (worldState.dashCooldown > 0) {
			worldState.dashCooldown--;
		}

		worldState.players.get(worldState.id).movement(worldState);

		// Calcuate a tag
		if (tagCooldown > 0) {
			tagCooldown--;
		}

		if (network.host && tagCooldown <= 0) {
			Player pIt = worldState.players.get(worldState.it);
			Rectangle itRect = new Rectangle((int) (pIt.position.x - PLAYER_SIZE), (int) (pIt.position.y - PLAYER_SIZE), PLAYER_SIZE * 2, PLAYER_SIZE * 2);

			for (Integer i : worldState.players.keySet()) {
				if (i == worldState.it) {
					continue;
				}

				Player p = worldState.players.get(i);
				if (itRect.intersects(new Rectangle((int) (p.position.x - PLAYER_SIZE), (int) (p.position.y - PLAYER_SIZE), PLAYER_SIZE * 2, PLAYER_SIZE * 2))) {
					// Before tag, if current it is this player, teleport
					if (worldState.id == worldState.it) {
						worldState.players.get(worldState.id).position.set(Math.random() * MAZE_SIZE * TILE_SIZE, Math.random() * MAZE_SIZE * TILE_SIZE);
					}

					tagCooldown = TAG_COOLDOWN;
					worldState.it = i;
					network.addEvent(Network.Event.EventType.TAG, worldState.it);
					break;
				}
			}
		}

		// Add bombs
		if (network.host && worldState.bombs.size() < 50) {
			Vector v = new Vector(Math.random() * worldState.maze.length, Math.random() * worldState.maze[0].length);
			while (worldState.maze[(int) v.x][(int) v.y] == Tile.WALL) {
				v = new Vector(Math.random() * worldState.maze.length, Math.random() * worldState.maze[0].length);
			}
			v.factor(TILE_SIZE);

			worldState.bombs.add(new Bomb(v));
			network.addEvent(Network.Event.EventType.NEW_BOMB, (int) (v.x), (int) (v.y));
		}

		// Update bombs
		for (int i = 0; i < worldState.bombs.size(); i++) {
			Bomb b = worldState.bombs.get(i);
			b.tick(worldState);
			if (network.host && b.velocity.length() > 0.01) {
				network.addEvent(Network.Event.EventType.BOMB_UPDATE, i, (int) (b.position.x), (int) (b.position.y), (int) (b.velocity.x), (int) (b.velocity.y));
			}
		}
		
		// Destroy bombs
		if (network.host) {
			for(int i = 0; i < worldState.bombs.size(); i++) {
				Bomb b = worldState.bombs.get(i);
				Player it = worldState.players.get(worldState.it);
				
				for(Vector v : it.killZones) {
					if(b.position.minus(v).length() < Bomb.RADIUS + Player.KILL_ZONE_RADIUS) {
						network.addEvent(Network.Event.EventType.DESTROY_BOMB, i, worldState.it);

						it.health--;
						worldState.bombs.remove(i--);
						break;
					}
				}
			}
		}
	}

	public void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr;
		Vector camera = worldState.players.get(worldState.id).position.clone();

		if (worldState.id == worldState.it) {
			g.scale(0.5, 0.5);
			g.translate(S_WIDTH / 2, S_HEIGHT / 2);
		}

		// Draw the maze
		for (int x = (int) (camera.x / TILE_SIZE - S_WIDTH * 2 / TILE_SIZE - 3); x < camera.x / TILE_SIZE + S_WIDTH * 2 / TILE_SIZE + 3; x++) {
			for (int y = (int) (camera.y / TILE_SIZE - S_HEIGHT * 2 / TILE_SIZE - 3); y < camera.y / TILE_SIZE + S_HEIGHT * 2 / TILE_SIZE + 3; y++) {
				if (x >= 0 && y >= 0 && x < worldState.maze.length && y < worldState.maze[x].length) {
					if (worldState.maze[x][y] == Tile.WALL) {
						g.setColor(Color.black);
						g.fillRect((int) (x * TILE_SIZE - camera.x + S_WIDTH / 2), (int) (y * TILE_SIZE - camera.y + S_HEIGHT / 2), TILE_SIZE, TILE_SIZE);
					} else {
						g.setColor(Color.white.darker());
						g.fillRect((int) (x * TILE_SIZE - camera.x + S_WIDTH / 2), (int) (y * TILE_SIZE - camera.y + S_HEIGHT / 2), TILE_SIZE, TILE_SIZE);
					}
				}
			}
		}

		// Draw bombs
		if (worldState.id != worldState.it) {
			g.setColor(Color.orange);
			for (Bomb b : worldState.bombs) {
				g.fillOval((int) (b.position.x - camera.x + S_WIDTH / 2 - Bomb.RADIUS), (int) (b.position.y - camera.y + S_HEIGHT / 2 - Bomb.RADIUS), 2 * Bomb.RADIUS + 1,
						2 * Bomb.RADIUS + 1);
			}
		}

		// Draw the players
		for (Integer i : worldState.players.keySet()) {
			Player p = worldState.players.get(i);
			if (worldState.it == i && Math.random() < 0.5) {
				g.setColor(p.color.darker());
			} else {
				g.setColor(p.color);
			}
			g.fillRoundRect((int) (p.position.x - camera.x + S_WIDTH / 2 - PLAYER_SIZE), (int) (p.position.y - camera.y + S_HEIGHT / 2 - PLAYER_SIZE), PLAYER_SIZE * 2,
					PLAYER_SIZE * 2, 2, 2);
		}

		// Draw dash target
		if (worldState.dashCooldown <= 0 && worldState.dashTarget != null) {
			Player p = worldState.players.get(worldState.id);
			Vector t = worldState.dashTarget;
			g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 128));
			g.fillRoundRect((int) (t.x - camera.x + S_WIDTH / 2 - PLAYER_SIZE), (int) (t.y - camera.y + S_HEIGHT / 2 - PLAYER_SIZE), PLAYER_SIZE * 2, PLAYER_SIZE * 2, 2, 2);
		}

		// Draw cooldown indicator
		g.setColor(Color.cyan);
		g.setStroke(new BasicStroke(3));
		g.drawArc(S_WIDTH / 2 - PLAYER_SIZE, S_HEIGHT / 2 - PLAYER_SIZE, PLAYER_SIZE * 2, PLAYER_SIZE * 2, 90,
				(int) (360 * worldState.dashCooldown / (DASH_COOLDOWN * (worldState.id == worldState.it ? 1.3 : 1))));

		// Draw player killzone
		if (worldState.id != worldState.it) {
			Player player = worldState.players.get(worldState.it);
			g.setColor(new Color(player.color.getRGB() & 0x80ffffff, true));
			for (Vector v : player.killZones) {
				g.fillOval((int) (v.x - Player.KILL_ZONE_RADIUS - camera.x + S_WIDTH / 2), (int) (v.y - Player.KILL_ZONE_RADIUS - camera.y + S_HEIGHT / 2),
						Player.KILL_ZONE_RADIUS * 2, Player.KILL_ZONE_RADIUS * 2);
			}
		}

		// Draw box for person whose it vision
		if (worldState.id == worldState.it) {
			g.setColor(Color.blue);
			g.drawRect(0, 0, S_WIDTH, S_HEIGHT);
		}

		if (worldState.id == worldState.it) {
			g.translate(-S_WIDTH / 2, -S_HEIGHT / 2);
			g.scale(2, 2);
		}

		// Draw player health
		g.setColor(Color.BLACK);
		g.fillRect(S_WIDTH - 110, 0, 110, 5 + 30 * worldState.players.size());
		for (Integer i : worldState.players.keySet()) {
			Player player = worldState.players.get(i);

			g.setColor(player.color);
			g.fillRect(S_WIDTH - 105, 5 + 30 * i, (int) (100 * (double) player.health / Player.MAX_HEALTH), 25);

			g.setColor(Color.BLACK);
			g.drawString("" + player.health + "/" + Player.MAX_HEALTH, S_WIDTH - 100, 25 + 30 * i);
		}
	}

	public static boolean goodTeleportSpot(Vector spot, Tile[][] maze) {
		boolean goodSpot = true;
		for (int x = (int) ((spot.x - MazeGame.PLAYER_SIZE) / MazeGame.TILE_SIZE); x < (spot.x + MazeGame.PLAYER_SIZE) / MazeGame.TILE_SIZE && goodSpot; x++) {
			for (int y = (int) ((spot.y - MazeGame.PLAYER_SIZE) / MazeGame.TILE_SIZE); y < (spot.y + MazeGame.PLAYER_SIZE) / MazeGame.TILE_SIZE && goodSpot; y++) {
				if (x < 0 || y < 0 || x >= maze.length || y >= maze[x].length || maze[x][y] == Tile.WALL) {
					goodSpot = false;
				}
			}
		}

		if (spot.x < 0 || spot.y < 0 || spot.x > MazeGame.MAZE_SIZE * MazeGame.TILE_SIZE || spot.y > MazeGame.MAZE_SIZE * MazeGame.TILE_SIZE) {
			goodSpot = false;
		}

		return goodSpot;
	}

	public static void generateMaze(Tile[][] maze, int mazeSeed) {
		Random rand = new Random(mazeSeed);
		ArrayList<Vector[]> walls = new ArrayList<Vector[]>();

		maze[maze.length / 2][maze[maze.length / 2].length / 2] = Tile.SPACE;
		walls.add(new Vector[] { new Vector(maze.length / 2, maze[maze.length / 2].length / 2), new Vector(maze.length / 2 + 1, maze[maze.length / 2].length / 2),
				new Vector(maze.length / 2 + 2, maze[maze.length / 2].length / 2) });
		walls.add(new Vector[] { new Vector(maze.length / 2, maze[maze.length / 2].length / 2), new Vector(maze.length / 2, maze[maze.length / 2].length / 2 - 1),
				new Vector(maze.length / 2, maze[maze.length / 2].length / 2 - 2) });
		walls.add(new Vector[] { new Vector(maze.length / 2, maze[maze.length / 2].length / 2), new Vector(maze.length / 2 - 1, maze[maze.length / 2].length / 2),
				new Vector(maze.length / 2 - 2, maze[maze.length / 2].length / 2) });
		walls.add(new Vector[] { new Vector(maze.length / 2, maze[maze.length / 2].length / 2), new Vector(maze.length / 2, maze[maze.length / 2].length / 2 - 1),
				new Vector(maze.length / 2, maze[maze.length / 2].length / 2 - 2) });

		while (walls.size() > 0) {
			int index = (int) (rand.nextDouble() * walls.size());
			Vector[] current = walls.get(index);

			if (maze[(int) current[2].x][(int) current[2].y] == Tile.SPACE) {
				walls.remove(index);
				continue;
			}

			maze[(int) current[1].x][(int) current[1].y] = Tile.SPACE;
			maze[(int) current[2].x][(int) current[2].y] = Tile.SPACE;

			if (current[2].x + 2 < maze.length && maze[(int) current[2].x + 2][(int) current[2].y] != Tile.SPACE)
				walls.add(new Vector[] { new Vector(current[2].x, current[2].y), new Vector(current[2].x + 1, current[2].y), new Vector(current[2].x + 2, current[2].y) });
			if (current[2].y + 2 < maze[(int) current[2].x].length && maze[(int) current[2].x][(int) current[2].y + 2] != Tile.SPACE)
				walls.add(new Vector[] { new Vector(current[2].x, current[2].y), new Vector(current[2].x, current[2].y + 1), new Vector(current[2].x, current[2].y + 2) });
			if (current[2].x - 2 >= 0 && maze[(int) current[2].x - 2][(int) current[2].y] != Tile.SPACE)
				walls.add(new Vector[] { new Vector(current[2].x, current[2].y), new Vector(current[2].x - 1, current[2].y), new Vector(current[2].x - 2, current[2].y) });
			if (current[2].y - 2 >= 0 && maze[(int) current[2].x][(int) current[2].y - 2] != Tile.SPACE)
				walls.add(new Vector[] { new Vector(current[2].x, current[2].y), new Vector(current[2].x, current[2].y - 1), new Vector(current[2].x, current[2].y - 2) });
		}

		int x = 0, y = 0;
		for (int i = 0; i < MAZE_SIZE * MAZE_SIZE / 50; i++) {
			while (((x + y) % 2 == 0 || (maze[x][y] == Tile.SPACE && rand.nextDouble() < 0.4)) || x == 0 || y == 0 || x == MAZE_SIZE - 1 || y == MAZE_SIZE - 1) {
				x = (int) (rand.nextDouble() * MAZE_SIZE);
				y = (int) (rand.nextDouble() * MAZE_SIZE);
			}

			maze[x][y] = Tile.SPACE;
		}
	}
}
