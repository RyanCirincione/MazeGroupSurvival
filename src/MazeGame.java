import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
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
		JFrame frame = new JFrame("Maze Group Survival");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		boolean host = false;
		if (JOptionPane.showConfirmDialog(frame, "Will you be the host?") == JOptionPane.YES_OPTION) {
			frame.setTitle("Maze Group Survival --HOST--");
			host = true;
		}

		MazeGame panel = new MazeGame(host);
		frame.getContentPane().add(panel);

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

					panel.network.update(panel.worldState);

					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		networkThread.start();

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

	static final int MAZE_SIZE = 503, S_WIDTH = 800, S_HEIGHT = 600, TILE_SIZE = 48, PLAYER_SIZE = 8;
	WorldState worldState;

	public MazeGame(boolean host) {
		worldState = new WorldState();
		network = new Network(host, worldState);

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

		if (network.host) {
			worldState.id = 0;
			worldState.players.put(0,
					new Player(new Color(worldState.color[0], worldState.color[1], worldState.color[2])));

			System.out.println("--- Host ---");
		} else {
			worldState.id = worldState.players.size();
			worldState.players.put(worldState.players.size(),
					new Player(new Color(worldState.color[0], worldState.color[1], worldState.color[2])));

			network.addEvent(Network.Event.EventType.NEW_PLAYER, worldState.id, worldState.color[0], worldState.color[1], worldState.color[2]);

			System.out.println("--- Client: " + worldState.id + " ---");
		}

		// Create the maze and network it
		worldState.maze = new Tile[MAZE_SIZE][MAZE_SIZE];
		for (int x = 0; x < worldState.maze.length; x++)
			for (int y = 0; y < worldState.maze[x].length; y++)
				worldState.maze[x][y] = Tile.WALL;
		generateMaze(worldState.maze, worldState.mazeSeed);

		this.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_D:
					worldState.controls.right = true;
					break;
				case KeyEvent.VK_S:
					worldState.controls.down = true;
					break;
				case KeyEvent.VK_A:
					worldState.controls.left = true;
					break;
				case KeyEvent.VK_W:
					worldState.controls.up = true;
					break;
				}
			}

			public void keyReleased(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_D:
					worldState.controls.right = false;
					break;
				case KeyEvent.VK_S:
					worldState.controls.down = false;
					break;
				case KeyEvent.VK_A:
					worldState.controls.left = false;
					break;
				case KeyEvent.VK_W:
					worldState.controls.up = false;
					break;
				}
			}
		});
		this.setFocusable(true);
		this.requestFocus();

		this.setPreferredSize(new Dimension(S_WIDTH, S_HEIGHT));
	}

	public void tick() {
		worldState.players.get(worldState.id).movement(worldState);
	}

	public void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr;
		Vector camera = worldState.players.get(worldState.id).position.clone();

		for (int x = (int) (camera.x / TILE_SIZE - S_WIDTH / 2 / TILE_SIZE - 3); x < camera.x / TILE_SIZE + S_WIDTH / 2 / TILE_SIZE + 3; x++)
			for (int y = (int) (camera.y / TILE_SIZE - S_HEIGHT / 2 / TILE_SIZE - 3); y < camera.y / TILE_SIZE + S_HEIGHT / 2 / TILE_SIZE + 3; y++)
				if (x >= 0 && y >= 0 && x < worldState.maze.length && y < worldState.maze[x].length)
					if (worldState.maze[x][y] == Tile.WALL) {
						g.setColor(Color.black);
						g.fillRect((int) (x * TILE_SIZE - camera.x + S_WIDTH / 2), (int) (y * TILE_SIZE - camera.y + S_HEIGHT / 2), TILE_SIZE, TILE_SIZE);
					} else {
						g.setColor(Color.white.darker());
						g.fillRect((int) (x * TILE_SIZE - camera.x + S_WIDTH / 2), (int) (y * TILE_SIZE - camera.y + S_HEIGHT / 2), TILE_SIZE, TILE_SIZE);
					}

		for (Integer i : worldState.players.keySet()) {
			Player p = worldState.players.get(i);
			g.setColor(p.color);
			g.fillRoundRect((int) (p.position.x - camera.x + S_WIDTH / 2 - PLAYER_SIZE), (int) (p.position.y - camera.y + S_HEIGHT / 2 - PLAYER_SIZE), PLAYER_SIZE * 2,
					PLAYER_SIZE * 2, 2, 2);
		}
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
		for (int i = 0; i < MAZE_SIZE; i++) {
			while (((x + y) % 2 == 0 || (maze[x][y] == Tile.SPACE && rand.nextDouble() < 0.4)) || x == 0 || y == 0 || x == MAZE_SIZE - 1 || y == MAZE_SIZE - 1) {
				x = (int) (rand.nextDouble() * MAZE_SIZE);
				y = (int) (rand.nextDouble() * MAZE_SIZE);
			}

			maze[x][y] = Tile.SPACE;
		}
	}
}
