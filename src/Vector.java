import java.text.DecimalFormat;

public class Vector implements java.io.Serializable {
	private static final long serialVersionUID = -3016580506510438526L;
	public double x, y;

	public Vector() {
		this(0, 0);
	}

	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void update(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void doDelta(Vector pair) {
		x += pair.x;
		y += pair.y;
	}

	public void factor(double f) {
		x *= f;
		y *= f;
	}

	public void doDeltaTo(Vector target, Vector delta) {
		if (x < target.x)
			if (x + delta.x > target.x)
				x = target.x;
			else
				x += delta.x;
		else if (x > target.x)
			if (x - delta.x < target.x)
				x = target.x;
			else
				x -= delta.x;

		if (y < target.y)
			if (y + delta.y > target.y)
				y = target.y;
			else
				y += delta.y;
		else if (y > target.y)
			if (y - delta.y < target.y)
				y = target.y;
			else
				y -= delta.y;
	}
	
	public Vector plus(Vector v) {
		return new Vector(x + v.x, y + v.y);
	}
	
	public Vector minus(Vector v) {
		return new Vector(x - v.x, y - v.y);
	}

	public Vector toPositive() {
		return new Vector(Math.abs(x), Math.abs(y));
	}
	
	public Vector setLength(double l) {
		double len = this.length();
		x = x * l / len;
		y = y * l / len;
		
		return this;
	}

	public double length() {
		return Math.sqrt(x * x + y * y);
	}
	
	public Vector lerp(Vector v, double distance) {
		return this.plus(v.minus(this).setLength(distance));
	}

	public boolean equals(Vector pair) {
		return x == pair.x && y == pair.y;
	}

	public Vector clone() {
		return new Vector(x, y);
	}

	public String toString() {
		DecimalFormat f = new DecimalFormat("0.###");
		return "<" + f.format(x) + ", " + f.format(y) + ">";
	}
}
