public class Matrix {
	double[][] mat;
	int cols, rows;
	
	/*
	 * Creates a Matrix object.
	 * Matrices are column major order to simplify vector representation
	 */
	public Matrix(int cols, int rows) {
		this.cols = cols;
		this.rows = rows;
		mat = new double[cols][rows];
	}
	
	/*
	 * Fill this matrix with the given values, in column major order.
	 * That is, for a 3x3 matrix, the following input:
	 * 1, 2, 3, 4, 5, 6, 7, 8, 9
	 * would result in a matrix like the following:
	 * | 1  4  7 |
	 * | 2  5  8 |
	 * | 3  6  9 |
	 * represented in memory like this:
	 * {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}
	 */
	public void fill(double ... vals) {
		if(vals.length > cols * rows) {
			throw new IllegalArgumentException("Cannot pass more values than cells in the matrix!");
		}
		int i = 0;
		for(int c = 0; c < cols; c++) {
			for(int r = 0; r < rows; r++) {
				mat[c][r] = vals[i++];
			}
		}
	}
	
	/*
	 * Multiply this matrix by a scalar quantity.
	 */
	public void multiply(int scalar) {
		for(int c = 0; c < cols; c++) {
			for(int r = 0; r < rows; r++) {
				mat[c][r] *= scalar;
			}
		}
	}
	
	/*
	 * Make a copy of this matrix and return the resultant duplicate matrix.
	 */
	public Matrix copy() {
		Matrix copy = new Matrix(this.cols, this.rows);
		for(int c = 0; c < cols; c++) {
			for(int r = 0; r < rows; r++) {
				copy.mat[c][r] = mat[c][r];
			}
		}
		return copy;
	}
	
	public void print() {
		for(int r = 0; r < rows; r++) {
			for(int c = 0; c < cols; c++) {
				System.out.printf("%f ", mat[c][r]);
			}
			System.out.printf("\n");
		}
	}
	
	/*
	 * Multiply this matrix with the given matrix.
	 * Returns a matrix of size (left.cols) x (right.rows)
	 */
	public static Matrix multiply(Matrix left, Matrix right) {
		
		// n = left.cols
		// m = left.rows = right.cols
		// p = right.rows
		
		if(left.cols != right.rows) {
			throw new IllegalArgumentException("Cannot multiply a x b and c x d sized matrices if b != c");
		}
		
		Matrix res = new Matrix(right.cols, left.rows);
		
		double sum;
		for(int i = 0; i < left.rows; i++) {
			for(int j = 0; j < right.cols; j++) {
				sum = 0;
//				System.out.printf("\tgenerating sum...\n");
				for(int m = 0; m < left.cols; m++) {
//					System.out.printf("\t\tsum += left[%d][%d] * right[%d][%d] = (%f * %f) = %f\n", m, i, j, m, left.mat[m][i], right.mat[j][m], left.mat[m][i] * right.mat[j][m]);
					sum += left.mat[m][i] * right.mat[j][m];
				}
//				System.out.printf("\tres[%d][%d] = %f\n", i, j, sum);
				res.mat[j][i] = sum;
			}
		}
		
		return res;
	}
	
	public Matrix transpose() {
		Matrix m = new Matrix(rows, cols);
		for(int c = 0; c < cols; c++) {
			for(int r = 0; r < rows; r++) {
				m.mat[r][c] = mat[c][r];
			}
		}
		return m;
	}
	
	/*
	 * Returns the axonometric projection matrix.
	 * Vectors left multiplied by this matrix will return a vector representing their
	 * position on the screen (x,y), and their depth (z).
	 */
	public static void axon_matrix(double angh, double angv, Matrix result) {
		if(result == null || result.cols != 3 || result.rows != 3) {
			throw new IllegalArgumentException("Result matrix is not a 3x3 matrix!");
		}
//		System.out.printf("horizontal angle: %f, vertical angle: %f\n", angh, angv);
		double a = Math.cos(angh);
		double b = Math.cos(angv);
		double c = Math.sin(angh);
		double f = Math.sin(angv);
		double d = a * b;
		double e = c * b;
//		System.out.printf("cos(h) = %f\n", a);
		result.fill( a,  e,  e, -c,  d,  d,  0, -f,  f);
	}
	
	public static void log_to_ren_matrix(Matrix result, double scalar) {
		if(result == null || result.cols != 2 || result.rows != 2) {
			throw new IllegalArgumentException("Result matrix is not a 2x2 matrix!");
		}
		double hcos30 = scalar * Math.cos(Math.PI / 6);
		double hsin30 = scalar * Math.sin(Math.PI / 6);
		result.fill(hcos30, hsin30, hcos30, -hsin30);
	}
	
	public static void ren_to_log_matrix(Matrix result, double scalar) {
		if(result == null || result.cols != 2 || result.rows != 2) {
			throw new IllegalArgumentException("Result matrix is not a 2x2 matrix!");
		}
		double root3 = Math.sqrt(3);
		result.fill(1/(root3 * scalar), 1/(root3*scalar), 1/scalar, -1/scalar);
	}
	
	
	/*
	 * Returns the inverse of the axonometric projection matrix.
	 * Vectors left multiplied by this matrix will return their original logical position
	 * in 3D space.
	 * 
	 * Due to rounding errors, the result may not be exactly the original, but it will be
	 * within some error of the original.
	 *
	 */
	public static void reverse_axon_matrix(double angh, double angv, Matrix result) {
		if(result == null || result.cols != 3 || result.rows != 3) {
			throw new IllegalArgumentException("Result matrix is not a 3x3 matrix!");
		}
		double a = Math.cos(angh);
		double b = Math.sin(angh);
		double g = .5;
		double c = g / Math.cos(angv);
		double d = g / Math.sin(angv);
		double e = b * c;
		double f = a * c;
		result.fill( a, -b,  0,  e,  f, -d,  e,  f,  d);
	}
}