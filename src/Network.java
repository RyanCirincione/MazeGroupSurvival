import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Network {

	public static void sendServerData(DataOutputStream out) {
		byte[] result = new byte[0];
		for (int p = 0; p < MazeGame.players.size(); p++)
			result = concatByteArray(result, intToByteArray((int) MazeGame.players.get(p).position.x),
					intToByteArray((int) MazeGame.players.get(p).position.y), intToByteArray((int) MazeGame.players.get(p).velocity.x),
					intToByteArray((int) MazeGame.players.get(p).velocity.y));

		try {
			out.write(result);
		} catch (IOException e) {
			System.out.println("Server failed to send data");
		}
	}

	public static void sendClientData(DataOutputStream out) {
		try {
			out.write(concatByteArray(intToByteArray(MazeGame.id), intToByteArray((int) MazeGame.players.get(MazeGame.id).position.x),
					intToByteArray((int) MazeGame.players.get(MazeGame.id).position.y), intToByteArray((int) MazeGame.players.get(MazeGame.id).velocity.x),
					intToByteArray((int) MazeGame.players.get(MazeGame.id).velocity.y)));
		} catch (IOException e) {
			System.out.println("Client failed to send data; id " + MazeGame.id);
		}
	}

	public static void receiveClientData(DataInputStream in) {
		try {
			byte[] data = new byte[4];
			in.read(data, 0, 4);
			int i = byteArrayToInt(data);

			in.read(data, 0, 4);
			MazeGame.players.get(i).position.x = byteArrayToInt(data);
			in.read(data, 0, 4);
			MazeGame.players.get(i).position.y = byteArrayToInt(data);
			in.read(data, 0, 4);
			MazeGame.players.get(i).velocity.x = byteArrayToInt(data);
			in.read(data, 0, 4);
			MazeGame.players.get(i).velocity.y = byteArrayToInt(data);
		} catch (IOException e) {
			System.out.println("Server failed to receive data");
		}
	}

	public static void receiveServerData(DataInputStream in) {
		for (int p = 0; p < MazeGame.players.size(); p++) {
			if (p == MazeGame.id) {
				try {
					in.read(new byte[16], 0, 16);
				} catch (IOException e) {
					System.out.println("Client failed to receive data; id " + MazeGame.id);
				}
				continue;
			}

			try {
				byte[] data = new byte[4];
				in.read(data, 0, 4);
				MazeGame.players.get(p).position.x = byteArrayToInt(data);
				in.read(data, 0, 4);
				MazeGame.players.get(p).position.y = byteArrayToInt(data);
				in.read(data, 0, 4);
				MazeGame.players.get(p).velocity.x = byteArrayToInt(data);
				in.read(data, 0, 4);
				MazeGame.players.get(p).velocity.y = byteArrayToInt(data);
			} catch (IOException e) {
				System.out.println("Client failed to receive data; id " + MazeGame.id);
			}
		}
	}

	public static int byteArrayToInt(byte[] b) {
		return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
	}

	public static byte[] intToByteArray(int a) {
		return new byte[] { (byte) ((a >> 24) & 0xFF), (byte) ((a >> 16) & 0xFF), (byte) ((a >> 8) & 0xFF),
				(byte) (a & 0xFF) };
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
}
