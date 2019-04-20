import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;

public class Network {
	ArrayList<Event> events;
	ArrayList<DataOutputStream> out;
	ArrayList<DataInputStream> in;
	static final int PORT = 9489;
	boolean host;

	public Network(boolean h, WorldState worldState) {
		events = new ArrayList<Event>();
		host = h;
		out = new ArrayList<DataOutputStream>();
		in = new ArrayList<DataInputStream>();

		if (host) {
			try {
				@SuppressWarnings("resource")
				ServerSocket listener = new ServerSocket(PORT);

				Thread t = new Thread() {
					public void run() {
						while (true) {
							try {
								Socket socket = listener.accept();

								out.add(new DataOutputStream(socket.getOutputStream()));
								in.add(new DataInputStream(socket.getInputStream()));

								sendFullWorld(out.get(out.size() - 1), worldState);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				};
				t.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			String serverAddress = JOptionPane.showInputDialog(null, "Enter IP Address of the Host:", "Maze Group Survival", JOptionPane.QUESTION_MESSAGE);
			if (serverAddress == null) {
				System.exit(0);
			} else if(serverAddress.equals("")) {
				serverAddress = "localhost";
			}
			
			int port = PORT, i = serverAddress.indexOf(":");
			if(i >= 0) {
				port = Integer.parseInt(serverAddress.substring(i + 1));
				serverAddress = serverAddress.substring(0, i);
			}

			Socket socket;
			try {
				socket = new Socket(serverAddress, port);

				out.add(new DataOutputStream(socket.getOutputStream()));
				in.add(new DataInputStream(socket.getInputStream()));

				receiveFullWorld(in.get(0), worldState);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void update(WorldState worldState) {
		if (host) {
			for(int i = 0; i < out.size(); i++) {
				try {
					sendServerData(out.get(i));
				} catch (IOException e) {
					out.remove(i--);
				}
			}
			events.clear();

			for(int i = 0; i < in.size(); i++) {
				try {
					receiveClientData(in.get(i), worldState);
				} catch (IOException e) {
					in.remove(i--);
				}
			}
		} else {
			sendClientData(out.get(0));
			events.clear();
			receiveServerData(in.get(0), worldState);
		}
	}

	public void addEvent(Event.EventType type, int... data) {
		byte[] b = new byte[0];
		for (int i = 0; i < data.length; i++) {
			b = concatByteArray(b, intToByteArray(data[i]));
		}

		events.add(new Event(type, b));
	}

	public void sendFullWorld(DataOutputStream out, WorldState worldState) {
		byte[] data = intToByteArray(worldState.mazeSeed);

		data = concatByteArray(data, intToByteArray(worldState.players.size()));
		for (Integer i : worldState.players.keySet()) {
			Player p = worldState.players.get(i);

			data = concatByteArray(data, intToByteArray(i));
			data = concatByteArray(data, intToByteArray((int) p.position.x));
			data = concatByteArray(data, intToByteArray((int) p.position.y));
			data = concatByteArray(data, intToByteArray(p.color.getRed()), intToByteArray(p.color.getGreen()), intToByteArray(p.color.getBlue()));
		}
		data = concatByteArray(data, intToByteArray(worldState.it));
		
		data = concatByteArray(data, intToByteArray(worldState.bombs.size()));
		for(Vector v : worldState.bombs) {
			data = concatByteArray(data, intToByteArray((int) v.x));
			data = concatByteArray(data, intToByteArray((int) v.y));
		}

		try {
			out.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void receiveFullWorld(DataInputStream in, WorldState worldState) {
		byte[] data = new byte[4];

		try {
			in.read(data);
			worldState.mazeSeed = byteArrayToInt(data);

			in.read(data);
			int players = byteArrayToInt(data);

			for (int i = 0; i < players; i++) {
				in.read(data);
				int id = byteArrayToInt(data);
				in.read(data);
				int x = byteArrayToInt(data);
				in.read(data);
				int y = byteArrayToInt(data);
				in.read(data);
				int r = byteArrayToInt(data);
				in.read(data);
				int g = byteArrayToInt(data);
				in.read(data);
				int b = byteArrayToInt(data);

				worldState.players.put(id, new Player(new Vector(x, y), new Color(r, g, b)));
			}
			
			in.read(data);
			int it = byteArrayToInt(data);
			worldState.it = it;
			
			in.read(data);
			int bombs = byteArrayToInt(data);
			
			for(int i = 0; i < bombs; i++) {
				in.read(data);
				int x = byteArrayToInt(data);
				in.read(data);
				int y = byteArrayToInt(data);
				
				worldState.bombs.add(new Vector(x, y));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendServerData(DataOutputStream out) throws IOException {
		byte[] data = intToByteArray(events.size());

		for (Event e : events) {
			data = concatByteArray(data, intToByteArray(e.type.encode()));
			data = concatByteArray(data, e.data);
		}

		out.write(data);
	}

	public void sendClientData(DataOutputStream out) {
		byte[] data = intToByteArray(events.size());

		for (Event e : events) {
			data = concatByteArray(data, intToByteArray(e.type.encode()));
			data = concatByteArray(data, e.data);
		}

		try {
			out.write(data);
		} catch (IOException e1) {
			System.err.println("Failed to send data to server. Data:\n" + Arrays.toString(data));
			e1.printStackTrace();
		}
	}

	public void receiveClientData(DataInputStream in, WorldState worldState) throws IOException {
		byte[] b = new byte[4];
		in.read(b);
		int len = byteArrayToInt(b);

		for (int i = 0; i < len; i++) {
			in.read(b);
			switch (Event.EventType.decode(byteArrayToInt(b))) {
			case PLAYER_X:
				in.read(b);
				int id = byteArrayToInt(b);
				in.read(b);
				int x = byteArrayToInt(b);

				if (worldState.players.get(id) != null) {
					worldState.players.get(id).position.x = x;
					this.addEvent(Event.EventType.PLAYER_X, id, x);
				}

				break;
			case PLAYER_Y:
				in.read(b);
				id = byteArrayToInt(b);
				in.read(b);
				int y = byteArrayToInt(b);

				if (worldState.players.get(id) != null) {
					worldState.players.get(id).position.y = y;
					this.addEvent(Event.EventType.PLAYER_Y, id, y);
				}

				break;
			case PLAYER_VEL_X:
				in.read(b);
				id = byteArrayToInt(b);
				in.read(b);
				x = byteArrayToInt(b);

				if (worldState.players.get(id) != null) {
					worldState.players.get(id).velocity.x = x;
					this.addEvent(Event.EventType.PLAYER_VEL_X, id, x);
				}

				break;
			case PLAYER_VEL_Y:
				in.read(b);
				id = byteArrayToInt(b);
				in.read(b);
				y = byteArrayToInt(b);

				if (worldState.players.get(id) != null) {
					worldState.players.get(id).velocity.y = y;
					this.addEvent(Event.EventType.PLAYER_VEL_Y, id, y);
				}

				break;
			case NEW_PLAYER:
				in.read(b);
				id = byteArrayToInt(b);
				in.read(b);
				int r = byteArrayToInt(b);
				in.read(b);
				int g = byteArrayToInt(b);
				in.read(b);
				int bl = byteArrayToInt(b);

				worldState.players.put(id, new Player(new Color(r, g, bl)));
				this.addEvent(Event.EventType.NEW_PLAYER, id, r, g, bl);

				break;
			case TAG:
			case NEW_BOMB:
			case DESTROY_BOMB:
				System.out.println("Client sent server-reserved event. Code: " + byteArrayToInt(b));
				break;
			default:
				System.out.println("Unknown event received. Code: " + byteArrayToInt(b));
			}
		}
	}

	public void receiveServerData(DataInputStream in, WorldState worldState) {
		try {
			byte[] b = new byte[4];
			in.read(b);
			int len = byteArrayToInt(b);

			for (int i = 0; i < len; i++) {
				in.read(b);
				switch (Event.EventType.decode(byteArrayToInt(b))) {
				case PLAYER_X:
					in.read(b);
					int id = byteArrayToInt(b);
					in.read(b);
					int x = byteArrayToInt(b);

					if (id == worldState.id) {
						break;
					}

					if (worldState.players.get(id) != null) {
						worldState.players.get(id).position.x = x;
					}

					break;
				case PLAYER_Y:
					in.read(b);
					id = byteArrayToInt(b);
					in.read(b);
					int y = byteArrayToInt(b);

					if (id == worldState.id) {
						break;
					}

					if (worldState.players.get(id) != null) {
						worldState.players.get(id).position.y = y;
					}

					break;
				case PLAYER_VEL_X:
					in.read(b);
					id = byteArrayToInt(b);
					in.read(b);
					x = byteArrayToInt(b);

					if (id == worldState.id) {
						break;
					}

					if (worldState.players.get(id) != null) {
						worldState.players.get(id).velocity.x = x;
					}

					break;
				case PLAYER_VEL_Y:
					in.read(b);
					id = byteArrayToInt(b);
					in.read(b);
					y = byteArrayToInt(b);

					if (id == worldState.id) {
						break;
					}

					if (worldState.players.get(id) != null) {
						worldState.players.get(id).velocity.y = y;
					}

					break;
				case NEW_PLAYER:
					in.read(b);
					id = byteArrayToInt(b);
					in.read(b);
					int r = byteArrayToInt(b);
					in.read(b);
					int g = byteArrayToInt(b);
					in.read(b);
					int bl = byteArrayToInt(b);

					worldState.players.put(id, new Player(new Color(r, g, bl)));

					break;
				case TAG:
					in.read(b);
					id = byteArrayToInt(b);
					
					worldState.it = id;
					break;
				case NEW_BOMB:
					in.read(b);
					x = byteArrayToInt(b);
					in.read(b);
					y = byteArrayToInt(b);
					
					worldState.bombs.add(new Vector(x, y));
					
					break;
				case DESTROY_BOMB:
					in.read(b);
					i = byteArrayToInt(b);
					
					worldState.bombs.remove(i);
					
					break;
				default:
					System.out.println("Unknown event received. Code: " + byteArrayToInt(b));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int byteArrayToInt(byte[] b) {
		return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
	}

	public static byte[] intToByteArray(int a) {
		return new byte[] { (byte) ((a >> 24) & 0xFF), (byte) ((a >> 16) & 0xFF), (byte) ((a >> 8) & 0xFF), (byte) (a & 0xFF) };
	}

	public static byte[] concatByteArray(byte[]... bytes) {
		int l = 0, x = 0;
		for (byte[] b : bytes)
			l += b.length;

		byte[] result = new byte[l];
		for (byte[] b : bytes)
			for (byte by : b)
				result[x++] = by;

		return result;
	}

	public static class Event {
		EventType type;
		byte[] data;

		public Event(EventType t, byte[] d) {
			type = t;
			data = d;
		}

		public static enum EventType {
			UNKNOWN, PLAYER_X, PLAYER_Y, PLAYER_VEL_X, PLAYER_VEL_Y, NEW_PLAYER, TAG, NEW_BOMB, DESTROY_BOMB;

			public int encode() {
				switch (this) {
				case PLAYER_X:
					return 0;
				case PLAYER_Y:
					return 1;
				case PLAYER_VEL_X:
					return 2;
				case PLAYER_VEL_Y:
					return 3;
				case NEW_PLAYER:
					return 4;
				case TAG:
					return 5;
				case NEW_BOMB:
					return 6;
				case DESTROY_BOMB:
					return 7;
				case UNKNOWN:
					return -1;
				}

				return -1;
			}

			public static EventType decode(int code) {
				switch (code) {
				case 0:
					return PLAYER_X;
				case 1:
					return PLAYER_Y;
				case 2:
					return PLAYER_VEL_X;
				case 3:
					return PLAYER_VEL_Y;
				case 4:
					return NEW_PLAYER;
				case 5:
					return TAG;
				case 6:
					return NEW_BOMB;
				case 7:
					return DESTROY_BOMB;
				case -1:
					return UNKNOWN;
				}

				return UNKNOWN;
			}
		}
	}
}
