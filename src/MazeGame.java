import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class MazeGame extends JPanel
{
	private static final long serialVersionUID = -3817295832102153653L;
	private static Socket socket;
	private static ArrayList<DataOutputStream> out;
	private static ArrayList<DataInputStream> in;
	private static int id, numPlayers;
	private static boolean host;
	
	public static void main(String[] args) throws IOException
	{
		JFrame frame = new JFrame("Maze Group Survival");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		socket = null;
		host = false;
		out = new ArrayList<DataOutputStream>();
		in = new ArrayList<DataInputStream>();
		numPlayers = 0;
		
		if(JOptionPane.showConfirmDialog(frame, "Will you be the host?") == JOptionPane.YES_OPTION)
		{
			host = true;
			id = 0;
			numPlayers++;
			ServerSocket listener = new ServerSocket(9489);
			boolean cont = true;
			int idCounter = 1;
			while(cont)
			{
				JOptionPane.showMessageDialog(frame, "Click OK to begin waiting for players.");
				socket = listener.accept();
				listener.close();
				cont = JOptionPane.showConfirmDialog(frame, "Player found! Wait for another?")
						== JOptionPane.YES_OPTION;
				numPlayers++;

				out.add(new DataOutputStream(socket.getOutputStream()));
				in.add(new DataInputStream(socket.getInputStream()));

				out.get(idCounter-1).writeInt(idCounter);
				out.get(idCounter-1).writeInt(numPlayers);
				idCounter++;
			}
		}
		else
		{
			String serverAddress = JOptionPane.showInputDialog(frame, "Enter IP Address of the Host:",
		            "Maze Group Survival", JOptionPane.QUESTION_MESSAGE);
			socket = new Socket(serverAddress, 9489);

			out.add(new DataOutputStream(socket.getOutputStream()));
			in.add(new DataInputStream(socket.getInputStream()));

			id = in.get(0).readInt();
			numPlayers = in.get(0).readInt();
		}
		
		Thread t = new Thread(){
			public void run()
			{
				while(true)
					if(host)
					{
						for(DataOutputStream o : out)
							sendServerData(o);
						for(DataInputStream i : in)
							receiveClientData(i);
					}
					else
					{
						sendClientData(out.get(0));
						receiveServerData(in.get(0));
					}
			}
		};
		t.start();
		
		MazeGame panel = new MazeGame();
		frame.getContentPane().add(panel);
		
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask(){
			public void run()
			{
				panel.tick();
				panel.repaint();
			}
		}, 0, 1000/60);
		
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	static final int MAZE_SIZE = 503, S_WIDTH = 800, S_HEIGHT = 600, TILE_SIZE = 32, PLAYER_SIZE = 8; 
	static enum Tile {WALL, SPACE};
	static Controls controls;
	static ArrayList<Player> players;
	static Tile[][] maze;
	
	public MazeGame()
	{
		players = new ArrayList<Player>();
		controls = new Controls();
		
		for(int i = 0; i < numPlayers; i++)
			players.add(new Player(new Vector(MAZE_SIZE/2 * TILE_SIZE, MAZE_SIZE/2 * TILE_SIZE), new Vector(0, 0)));
		
		maze = new Tile[MAZE_SIZE][MAZE_SIZE];
		if(host)
		{
			for(int x = 0; x < maze.length; x++)
				for(int y = 0; y < maze[x].length; y++)
					maze[x][y] = Tile.WALL;
			generateMaze(maze);
			
			byte[] mazeData = new byte[MAZE_SIZE*MAZE_SIZE];
			for(int x = 0; x < maze.length; x++)
				for(int y = 0; y < maze[x].length; y++)
					mazeData[y + x*MAZE_SIZE] = (byte) (maze[x][y] == Tile.SPACE ? 0 : 1);
			
			for(int i = 0; i < out.size(); i++)
				try{
				out.get(i).write(mazeData);
				} catch(IOException e) {
					System.out.println("Error sending maze to id " + (i+1));
				}
		}
		else
		{
			byte[] mazeData = new byte[MAZE_SIZE*MAZE_SIZE];
			try {
				in.get(0).read(mazeData);
			} catch (IOException e) {
				System.out.println("Error reading maze; id " + id);
			}

			for(int x = 0; x < maze.length; x++)
				for(int y = 0; y < maze[x].length; y++)
					maze[x][y] = mazeData[y + x*MAZE_SIZE] == 0 ? Tile.SPACE : Tile.WALL;
		}
		
		this.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e)
			{
				switch(e.getKeyCode())
				{
				case KeyEvent.VK_D:
					controls.right = true;
					break;
				case KeyEvent.VK_S:
					controls.down = true;
					break;
				case KeyEvent.VK_A:
					controls.left = true;
					break;
				case KeyEvent.VK_W:
					controls.up = true;
					break;
				}
			}
			
			public void keyReleased(KeyEvent e)
			{
				switch(e.getKeyCode())
				{
				case KeyEvent.VK_D:
					controls.right = false;
					break;
				case KeyEvent.VK_S:
					controls.down = false;
					break;
				case KeyEvent.VK_A:
					controls.left = false;
					break;
				case KeyEvent.VK_W:
					controls.up = false;
					break;
				}
			}
		});
		this.setFocusable(true);
		this.requestFocus();
		
		this.setPreferredSize(new Dimension(S_WIDTH, S_HEIGHT));
	}
	
	public void tick()
	{
		players.get(id).movement(controls);
	}
	
	public void paintComponent(Graphics gr)
	{
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr;
		Vector camera = players.get(id).position.clone();
		
		for(int x = (int) (camera.x/TILE_SIZE - S_WIDTH/2/TILE_SIZE - 3); x < camera.x/TILE_SIZE + S_WIDTH/2/TILE_SIZE + 3; x++)
			for(int y = (int) (camera.y/TILE_SIZE - S_HEIGHT/2/TILE_SIZE - 3); y < camera.y/TILE_SIZE + S_HEIGHT/2/TILE_SIZE + 3; y++)
				if(x >= 0 && y >= 0 && x < maze.length && y < maze[x].length)
					if(maze[x][y] == Tile.WALL)
					{
						g.setColor(Color.black);
						g.fillRect((int)(x*TILE_SIZE - camera.x + S_WIDTH/2), (int)(y*TILE_SIZE - camera.y + S_HEIGHT/2),
								TILE_SIZE, TILE_SIZE);
					}
					else
					{
						g.setColor(Color.white.darker());
						g.fillRect((int)(x*TILE_SIZE - camera.x + S_WIDTH/2), (int)(y*TILE_SIZE - camera.y + S_HEIGHT/2),
								TILE_SIZE, TILE_SIZE);
					}
		
		g.setColor(Color.orange);
		for(Player p : players)
			g.fillOval((int)(p.position.x - camera.x + S_WIDTH/2 - PLAYER_SIZE),
					(int)(p.position.y - camera.y + S_HEIGHT/2 - PLAYER_SIZE),
					PLAYER_SIZE*2, PLAYER_SIZE*2);
	}
	
	public static void sendServerData(DataOutputStream out)
	{
		for(int p = 0; p < players.size(); p++)
		{
			try {
				out.write(intToByteArray((int)players.get(p).position.x));
				out.write(intToByteArray((int)players.get(p).position.y));
				out.write(intToByteArray((int)players.get(p).velocity.x));
				out.write(intToByteArray((int)players.get(p).velocity.y));
			} catch (IOException e) {
				System.out.println("Server failed to send data to id " + p);
			}
		}
	}
	public static void sendClientData(DataOutputStream out)
	{
		try {
			out.writeInt(id);
			out.write(intToByteArray((int)players.get(id).position.x));
			out.write(intToByteArray((int)players.get(id).position.y));
			out.write(intToByteArray((int)players.get(id).velocity.x));
			out.write(intToByteArray((int)players.get(id).velocity.y));
		} catch (IOException e) {
			System.out.println("Client failed to send data; id " + id);
		}
	}
	public static void receiveServerData(DataInputStream in)
	{
		try {
			int i = in.readInt();
			byte[] data = new byte[4];
			in.read(data, 0, 4);
			players.get(i).position.x = byteArrayToInt(data);
			in.read(data, 0, 4);
			players.get(i).position.y = byteArrayToInt(data);
			in.read(data, 0, 4);
			players.get(i).velocity.x = byteArrayToInt(data);
			in.read(data, 0, 4);
			players.get(i).velocity.y = byteArrayToInt(data);
		} catch (IOException e) {
			System.out.println("Server failed to receive data");
		}
	}
	public static void receiveClientData(DataInputStream in)
	{
		for(int p = 0; p < players.size(); p++)
		{
			if(p == id)
			{
				try {
					in.read(new byte[16], 0, 16);
				} catch (IOException e) {
					System.out.println("Client failed to receive data; id " + id);
				}
				continue;
			}
			
			try {
				byte[] data = new byte[4];
				in.read(data, 0, 4);
				players.get(p).position.x = byteArrayToInt(data);
				in.read(data, 0, 4);
				players.get(p).position.y = byteArrayToInt(data);
				in.read(data, 0, 4);
				players.get(p).velocity.x = byteArrayToInt(data);
				in.read(data, 0, 4);
				players.get(p).velocity.y = byteArrayToInt(data);
			} catch (IOException e) {
				System.out.println("Client failed to receive data; id " + id);
			}
		}
	}
	public static int byteArrayToInt(byte[] b) 
	{
	    return   b[3] & 0xFF |
	            (b[2] & 0xFF) << 8 |
	            (b[1] & 0xFF) << 16 |
	            (b[0] & 0xFF) << 24;
	}
	public static byte[] intToByteArray(int a)
	{
	    return new byte[] {
	        (byte) ((a >> 24) & 0xFF),
	        (byte) ((a >> 16) & 0xFF),   
	        (byte) ((a >> 8) & 0xFF),   
	        (byte) (a & 0xFF)
	    };
	}
	public void generateMaze(Tile[][] maze)
	{
		ArrayList<Vector[]> walls = new ArrayList<Vector[]>();
		
		maze[maze.length/2][maze[maze.length/2].length/2] = Tile.SPACE;
		walls.add(new Vector[]{new Vector(maze.length/2, maze[maze.length/2].length/2),
				new Vector(maze.length/2+1, maze[maze.length/2].length/2),
				new Vector(maze.length/2+2, maze[maze.length/2].length/2)});
		walls.add(new Vector[]{new Vector(maze.length/2, maze[maze.length/2].length/2),
				new Vector(maze.length/2, maze[maze.length/2].length/2-1),
				new Vector(maze.length/2, maze[maze.length/2].length/2-2)});
		walls.add(new Vector[]{new Vector(maze.length/2, maze[maze.length/2].length/2),
				new Vector(maze.length/2-1, maze[maze.length/2].length/2),
				new Vector(maze.length/2-2, maze[maze.length/2].length/2)});
		walls.add(new Vector[]{new Vector(maze.length/2, maze[maze.length/2].length/2),
				new Vector(maze.length/2, maze[maze.length/2].length/2-1),
				new Vector(maze.length/2, maze[maze.length/2].length/2-2)});
		
		while(walls.size() > 0)
		{
			int index = (int)(Math.random()*walls.size());
			Vector[] current = walls.get(index);
			
			if(maze[(int) current[2].x][(int) current[2].y] == Tile.SPACE)
			{
				walls.remove(index);
				continue;
			}

			maze[(int) current[1].x][(int) current[1].y] = Tile.SPACE;
			maze[(int) current[2].x][(int) current[2].y] = Tile.SPACE;
			
			if(current[2].x+2 < maze.length && maze[(int) current[2].x+2][(int) current[2].y] != Tile.SPACE)
				walls.add(new Vector[]{new Vector(current[2].x, current[2].y),
						new Vector(current[2].x+1, current[2].y),
						new Vector(current[2].x+2, current[2].y)});
			if(current[2].y+2 < maze[(int) current[2].x].length &&
					maze[(int) current[2].x][(int) current[2].y+2] != Tile.SPACE)
				walls.add(new Vector[]{new Vector(current[2].x, current[2].y),
						new Vector(current[2].x, current[2].y+1),
						new Vector(current[2].x, current[2].y+2)});
			if(current[2].x-2 >= 0 && maze[(int) current[2].x-2][(int) current[2].y] != Tile.SPACE)
				walls.add(new Vector[]{new Vector(current[2].x, current[2].y),
						new Vector(current[2].x-1, current[2].y),
						new Vector(current[2].x-2, current[2].y)});
			if(current[2].y-2 >= 0 && maze[(int) current[2].x][(int) current[2].y-2] != Tile.SPACE)
				walls.add(new Vector[]{new Vector(current[2].x, current[2].y),
						new Vector(current[2].x, current[2].y-1),
						new Vector(current[2].x, current[2].y-2)});
		}
	}
}
