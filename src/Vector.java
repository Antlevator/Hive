import java.awt.Point;
import java.awt.geom.Point2D;

class Vector {
	int components;
	Matrix matrix;
	
	public Vector(int components) {
		this.components = components;
		matrix = new Matrix(1, components);
	}
	
	public Vector(double ... vals) {
		this.components = vals.length;
		matrix = new Matrix(1, vals.length);
		matrix.fill(vals);
	}
	
	private Vector() {}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		for(int i = 0; i < matrix.mat[0].length; i++) {
			sb.append(matrix.mat[0][i]);
			if(i != matrix.mat[0].length-1) {
				sb.append(", ");
			}
		}
		sb.append(">");
		return sb.toString();
	}
	
	/*
	 * Multiply the vector by a given matrix.
	 */
	public static Vector multiply(Matrix projmat, Vector vector) {
		Vector result = new Vector();
		result.matrix = Matrix.multiply(projmat, vector.matrix);
		result.components = result.matrix.rows;
		return result;
	}
	
	/*
	 * Subtracts v2 from v1 and returns the result
	 * 
	 */
	public static Vector subtract(Vector v1, Vector v2) {
		return new Vector(v1.x() - v2.x(), v1.y() - v2.y(), v1.z() - v2.z());
	}
	
	/*
	 * Adds v2 to v1 and returns the result
	 * 
	 */
	public static Vector add(Vector v1, Vector v2) {
		return new Vector(v1.x() + v2.x(), v1.y() + v2.y(), v1.z() + v2.z());
	}
	
	/*
	 * Adds (mag2 * v2) to (mag1 * v1) and returns the result
	 * Useful if v1 and v2 are basis vectors
	 */
	public static Vector add(double mag1, Vector v1, double mag2, Vector v2) {
		return new Vector((mag1 * v1.x()) + (mag2 * v2.x()), (mag1 * v1.y()) + (mag2 * v2.y()), (mag1 * v1.z()) + (mag2 * v2.z()));
	}
	
	public double x() { return matrix.mat[0][0]; }
	public double y() { return matrix.mat[0][1]; }
	public double z() { return matrix.mat[0][2]; }

	public double len() {
		return Math.sqrt(x() * x() + y() * y() + z() * z());
	}
	
	public double angle() {
		return Math.atan2(y(), x());
	}

	public void scale(double scalar) {
		for(int i = 0; i < matrix.mat[0].length; i++) {
			matrix.mat[0][i] *= scalar;
		}
	}
	
	public static Vector scale(Vector vector, double scalar) {
		Vector retvec = Vector.copy(vector);
		retvec.matrix.mat[0][0] *= scalar;
		retvec.matrix.mat[0][1] *= scalar;
		retvec.matrix.mat[0][2] *= scalar;
		return retvec;
	}
	
	public void setHorizontalByRotation(double len, double rotation) {
		matrix.mat[0][0] = len * Math.cos(rotation);
		matrix.mat[0][1] = len * Math.sin(rotation);
	}
	
	public void rotateHorizontal(double angle) {
		double lx = matrix.mat[0][0];
		double ly = matrix.mat[0][1];
		matrix.mat[0][0] = lx * Math.cos(angle) - ly * Math.sin(angle);
		matrix.mat[0][1] = lx * Math.sin(angle) + ly * Math.cos(angle);
	}
	
	public static Vector cross(Vector a, Vector b) {
		return new Vector(
				a.y() * b.z() - a.z() * b.y(),
				a.x() * b.z() - a.z() * b.x(),
				a.x() * b.y() - a.y() * b.x()
			);
	}
	
	public static double angleBetween(Vector u, Vector v) {
//		return Math.acos(Vector.dot(u, v) / (u.len() * v.len()));
		return Math.atan2(determinant2D(u, v), dot(u, v));
	}
	
	public static double dot(Vector a, Vector b) {
		return a.x() * b.x() + a.y() * b.y() + a.z() * b.z();
	}
	
	public static double determinant2D(Vector a, Vector b) {
		return a.x() * b.y() + a.y() * b.x();
	}

	public static Vector fromSphericalCoordinate(int len, double angh, double angv) {
		Vector v = new Vector(len, 0, 0); // pointing straight towards (+x)
		Matrix trans = new Matrix(3, 3);
		Matrix.axon_matrix(angh, angv, trans);
		Vector v_oriented = Vector.multiply(trans, v);
		return v_oriented;
	}

	public static Vector copy(Vector len) {
		return new Vector(len.x(), len.y(), len.z());
	}

	public void addX(double d) { matrix.mat[0][0] += d; }
	public void addY(double d) { matrix.mat[0][1] += d; }

	public void normalize() {
		scale(1 / len());
	}

	public void divideFloorMultiply(double h) {
		for(int i = 0; i < matrix.mat.length; i++) {
			for(int j = 0; j < matrix.mat[i].length; j++) {
				matrix.mat[i][j] = Math.round(matrix.mat[i][j] / h) * h;
			}
		}
	}

	public Point2D toPoint2D() {
		return new Point2D.Double(matrix.mat[0][0], matrix.mat[0][1]);
	}

	public Point toPoint() {
		return new Point((int)matrix.mat[0][0], (int)matrix.mat[0][1]);
	}
	
}